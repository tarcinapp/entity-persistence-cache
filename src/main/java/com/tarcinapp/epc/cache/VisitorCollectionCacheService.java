package com.tarcinapp.epc.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Redis-backed visitor collection response cache for Phase 3 of EPC.
 *
 * <h3>Purpose</h3>
 * Caches collection GET responses that carry the visitor set injection
 * ({@code set[actives]=true&set[publics]=true}) — the only collection queries
 * that are fully identity-free and therefore shareable across all callers.
 *
 * <h3>Cache key format</h3>
 * <pre>
 * {appShortcode}:epc:vcache:{recordType}:{sortedQueryStringWithoutFieldset}
 * </pre>
 * The {@code fieldset} parameter is stripped before key construction because it
 * is resolved by the EPG response phase (not by EPS) and therefore does not
 * change the EPS response body that EPC caches.  All other parameters
 * ({@code filter[fields]}, {@code filter[limit]}, {@code filter[skip]}, etc.)
 * are preserved — they change what EPS returns.
 *
 * <h3>Visitor detection</h3>
 * A request is treated as a visitor collection query when the query string
 * contains {@code set[actives]} (injected by EPG for visitors) but does NOT
 * contain {@code set[audience]} (injected by EPG for members).  This allows
 * EPC to make the visitor/member distinction without parsing a JWT.
 *
 * <h3>filter[include] bypass</h3>
 * Requests containing {@code filter[include]} (or percent-encoded form) bypass
 * the visitor cache entirely — embedded nested records cannot be invalidated
 * correctly.
 *
 * <h3>Cached status codes</h3>
 * Only {@code 200 OK} is cached; {@code 204 No Content} and error responses
 * are not.
 *
 * <h3>Invalidation strategy</h3>
 * Two strategies, controlled by {@code epc.visitor-cache.evict-on-write}:
 * <ul>
 *   <li><b>evict-on-write=true</b> — every write operation (POST/PUT/PATCH/DELETE)
 *       issues a SCAN+DEL flush of all visitor collection cache entries for the
 *       affected record type.  Eliminates the stale window for single-record writes;
 *       TTL remains the backstop for the brief race between flush and write commit.</li>
 *   <li><b>evict-on-write=false (default)</b> — TTL is the sole invalidation
 *       mechanism.  Writes do not touch the cache.  Newly created, modified, or
 *       deactivated records will not appear/disappear in visitor collection responses
 *       until the cache entry expires.</li>
 * </ul>
 *
 * <h3>TTL configuration</h3>
 * Resolved in order:
 * <ol>
 *   <li>{@code epc.visitor-cache.ttl.{recordType}} — per-type override</li>
 *   <li>{@code epc.visitor-cache.default-ttl}        — global fallback (default 30 000 ms)</li>
 * </ol>
 *
 * <h3>Metrics</h3>
 * <ul>
 *   <li>{@code epc.vcache.hit}   — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.vcache.miss}  — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.vcache.put}   — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.vcache.flush} — counter, tag: {@code record_type}</li>
 * </ul>
 */
@ApplicationScoped
public class VisitorCollectionCacheService {

    private static final Logger LOG = Logger.getLogger(VisitorCollectionCacheService.class);

    /** The {@code fieldset} query param is EPG-layer only; strip it from the cache key. */
    private static final String FIELDSET_PARAM = "fieldset";

    @ConfigProperty(name = "epc.visitor-cache.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "epc.visitor-cache.evict-on-write", defaultValue = "false")
    boolean evictOnWrite;

    @ConfigProperty(name = "epc.visitor-cache.default-ttl", defaultValue = "30000")
    long defaultTtlMs;

    @ConfigProperty(name = "epc.visitor-cache.ttl.entity")
    Optional<Long> ttlEntity;

    @ConfigProperty(name = "epc.visitor-cache.ttl.list")
    Optional<Long> ttlList;

    @ConfigProperty(name = "epc.visitor-cache.ttl.relation")
    Optional<Long> ttlRelation;

    @ConfigProperty(name = "epc.visitor-cache.ttl.entity-reaction")
    Optional<Long> ttlEntityReaction;

    @ConfigProperty(name = "epc.visitor-cache.ttl.list-reaction")
    Optional<Long> ttlListReaction;

    @Inject
    RedisDataSource redisDataSource;

    @Inject
    MeterRegistry meterRegistry;

    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;

    @PostConstruct
    void init() {
        valueCommands = redisDataSource.value(String.class);
        keyCommands   = redisDataSource.key(String.class);
        LOG.infof("VisitorCollectionCacheService initialised. enabled=%s, defaultTtlMs=%d.",
                enabled, defaultTtlMs);
    }

    // ── Domain types ──────────────────────────────────────────────────────────

    /** A cached HTTP response entry for a collection query. */
    public record CachedEntry(String body, String contentType, String etag) {}

    // ── Visitor detection ─────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the raw query string represents a visitor
     * collection query — i.e. it contains {@code set[actives]} (injected by EPG
     * for visitors) but not {@code set[audience]} (injected for members).
     *
     * <p>Both plain and percent-encoded bracket forms are checked.</p>
     */
    public boolean isVisitorQuery(String rawQueryString) {
        if (rawQueryString == null || rawQueryString.isBlank()) return false;
        String lower = rawQueryString.toLowerCase();
        boolean hasActives  = lower.contains("set[actives]")  || lower.contains("set%5bactives%5d");
        boolean hasAudience = lower.contains("set[audience]") || lower.contains("set%5baudience%5d");
        return hasActives && !hasAudience;
    }

    /**
     * Returns {@code true} when the query string contains {@code filter[include]}
     * or its percent-encoded form — these requests bypass the cache entirely.
     */
    public boolean hasIncludeFilter(String rawQueryString) {
        if (rawQueryString == null || rawQueryString.isBlank()) return false;
        String lower = rawQueryString.toLowerCase();
        return lower.contains("filter[include]") || lower.contains("filter%5binclude%5d");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Looks up a cached collection response for the given cache key.
     *
     * @param cacheKey   the fully-formed Redis key
     * @param recordType label for metrics
     * @return populated Optional on hit; empty on miss or Redis error
     */
    public Optional<CachedEntry> get(String cacheKey, String recordType) {
        if (!enabled) return Optional.empty();
        try {
            String raw = valueCommands.get(cacheKey);
            if (raw == null) {
                counter("epc.vcache.miss", recordType).increment();
                LOG.debugf("Visitor cache MISS: %s", cacheKey);
                return Optional.empty();
            }
            CachedEntry entry = deserialize(raw);
            counter("epc.vcache.hit", recordType).increment();
            LOG.debugf("Visitor cache HIT: %s", cacheKey);
            return Optional.of(entry);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on visitor cache GET '%s' — bypassing.", cacheKey);
            return Optional.empty();
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Stores a collection response in the visitor cache.
     * Only status {@code 200 OK} is cached.
     *
     * @param cacheKey    the fully-formed Redis key
     * @param status      HTTP status code of the EPS response
     * @param body        response body string
     * @param contentType value of the Content-Type response header (may be null)
     * @param recordType  label for metrics
     */
    public void put(String cacheKey, int status, String body, String contentType, String recordType) {
        if (!enabled) return;
        if (status != 200) {
            LOG.debugf("Visitor cache PUT skipped for status %d: %s", status, cacheKey);
            return;
        }
        try {
            long ttlMs = resolveTtlMs(recordType);
            String etag = computeEtag(body);
            String serialized = serialize(body, contentType, etag);
            valueCommands.set(cacheKey, serialized, new SetArgs().px(Duration.ofMillis(ttlMs)));
            counter("epc.vcache.put", recordType).increment();
            LOG.debugf("Visitor cache PUT: %s (ttl=%dms, etag=%s)", cacheKey, ttlMs, etag);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on visitor cache PUT '%s' — skipping.", cacheKey);
        }
    }

    // ── Flush ─────────────────────────────────────────────────────────────────

    /**
     * Flushes all visitor collection cache entries for the given record type
     * using a SCAN-based iteration ({@code {appShortcode}:epc:vcache:{recordType}:*}).
     *
     * <p>Uses {@code SCAN} (not {@code KEYS}) to avoid blocking Redis under
     * high-cardinality keyspaces (many cached pages × many filter combinations).</p>
     *
     * @param appShortcode from the {@code X-App-Shortcode} header or fallback
     * @param recordType   "entity" | "list" | "relation" | "entity-reaction" | "list-reaction"
     */
    public void flush(String appShortcode, String recordType) {
        if (!enabled) return;
        if (!evictOnWrite) {
            LOG.debugf("Visitor cache FLUSH skipped (evict-on-write=false) for %s", recordType);
            return;
        }
        String pattern = appShortcode + ":epc:vcache:" + recordType + ":*";
        try {
            List<String> toDelete = new ArrayList<>();
            KeyScanArgs args = new KeyScanArgs().match(pattern).count(100);
            KeyScanCursor<String> cursor = keyCommands.scan(args);
            while (cursor.hasNext()) {
                Set<String> batch = cursor.next();
                if (batch != null) toDelete.addAll(batch);
            }
            if (toDelete.isEmpty()) {
                LOG.debugf("Visitor cache FLUSH — no keys matched: %s", pattern);
                return;
            }
            String[] keyArray = toDelete.toArray(new String[0]);
            keyCommands.del(keyArray);
            counter("epc.vcache.flush", recordType).increment(toDelete.size());
            LOG.debugf("Visitor cache FLUSH — deleted %d key(s) matching: %s",
                    toDelete.size(), pattern);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on visitor cache FLUSH '%s' — skipping.", pattern);
        }
    }

    // ── Key construction ──────────────────────────────────────────────────────

    /**
     * Builds the Redis cache key for a visitor collection query.
     * <pre>
     * {appShortcode}:epc:vcache:{recordType}:{sortedQsWithoutFieldset}
     * </pre>
     * The {@code fieldset} parameter is stripped: it is applied by EPG's
     * {@code ApplyFieldsetConfig} response-phase filter, not by EPS, so it does
     * not change the EPS response body that EPC stores.
     */
    public String buildKey(String appShortcode, String recordType, String rawQueryString) {
        String qs = normalizeQueryStringWithoutFieldset(rawQueryString);
        return appShortcode + ":epc:vcache:" + recordType + ":" + qs;
    }

    // ── TTL resolution ────────────────────────────────────────────────────────

    public long resolveTtlMs(String recordType) {
        Optional<Long> typeTtl = switch (recordType) {
            case "entity"          -> ttlEntity;
            case "list"            -> ttlList;
            case "relation"        -> ttlRelation;
            case "entity-reaction" -> ttlEntityReaction;
            case "list-reaction"   -> ttlListReaction;
            default -> Optional.empty();
        };
        return typeTtl.filter(v -> v > 0).orElse(defaultTtlMs);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Sorts query parameters alphabetically and removes the {@code fieldset}
     * parameter from the key.  Returns an empty string for blank/null input.
     */
    private String normalizeQueryStringWithoutFieldset(String rawQs) {
        if (rawQs == null || rawQs.isBlank()) return "";
        TreeMap<String, String> params = new TreeMap<>();
        for (String pair : rawQs.split("&")) {
            int eq = pair.indexOf('=');
            String key   = (eq < 0) ? pair : pair.substring(0, eq);
            String value = (eq < 0) ? ""   : pair.substring(eq + 1);
            if (FIELDSET_PARAM.equalsIgnoreCase(key)) continue; // strip EPG-only param
            params.put(key, value);
        }
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(k);
            if (!v.isEmpty()) sb.append('=').append(v);
        });
        return sb.toString();
    }

    /** Computes an MD5-based ETag for the response body. */
    private String computeEtag(String body) {
        if (body == null || body.isEmpty()) return "\"d41d8cd98f00b204e9800998ecf8427e\"";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("\"");
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.append("\"").toString();
        } catch (Exception e) {
            return "\"unknown\"";
        }
    }

    private String serialize(String body, String contentType, String etag) {
        return "{\"ct\":\"" + escape(contentType) + "\""
                + ",\"e\":\""  + escape(etag) + "\""
                + ",\"b\":\""  + escape(body) + "\"}";
    }

    private CachedEntry deserialize(String raw) {
        String contentType = unescape(extractField(raw, "\"ct\":\"", "\",\"e\":"));
        String etag        = unescape(extractField(raw, "\"e\":\"",  "\",\"b\":"));
        String body        = unescape(extractBodyField(raw));
        return new CachedEntry(body, contentType, etag);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String extractField(String json, String startMarker, String endMarker) {
        int s = json.indexOf(startMarker);
        if (s < 0) return "";
        s += startMarker.length();
        int e = json.indexOf(endMarker, s);
        return (e < 0) ? "" : json.substring(s, e);
    }

    private String extractBodyField(String json) {
        String marker = "\"b\":\"";
        int s = json.indexOf(marker);
        if (s < 0) return "";
        s += marker.length();
        int e = json.lastIndexOf("\"}");
        return (e <= s) ? "" : json.substring(s, e);
    }

    private Counter counter(String name, String recordType) {
        return Counter.builder(name)
                .tag("record_type", recordType)
                .register(meterRegistry);
    }
}
