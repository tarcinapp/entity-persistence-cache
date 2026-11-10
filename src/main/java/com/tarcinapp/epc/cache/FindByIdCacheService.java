package com.tarcinapp.epc.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Redis-backed findById response cache for Phase 2 of EPC.
 *
 * <h3>Purpose</h3>
 * Caches the full HTTP response body and a selected subset of response headers
 * for {@code GET /{recordType}/{id}} requests. On a cache hit the pre-processor
 * short-circuits the Camel route and returns the cached response directly,
 * avoiding an EPS (and MongoDB) round-trip.
 *
 * <h3>Cache key format</h3>
 * <pre>
 * {appShortcode}:epc:cache:{recordType}:{id}:{sortedQueryString}
 * </pre>
 * The query string component is sorted by parameter name so that equivalent
 * queries expressed in different parameter order produce the same key.
 *
 * <h3>filter[include] bypass</h3>
 * Any request whose query string contains {@code filter[include]} or its
 * percent-encoded form {@code filter%5Binclude%5D} is bypassed — neither read
 * from cache nor written to it.
 *
 * <h3>Cached status codes</h3>
 * Only {@code 200 OK} and {@code 404 Not Found} are cached; all others are
 * passed through without caching.
 *
 * <h3>TTL hierarchy</h3>
 * <ol>
 *   <li>{@code epc.cache.ttl.{recordType}} (type-level override)</li>
 *   <li>{@code epc.cache.default-ttl} (global fallback, default 60 000 ms)</li>
 * </ol>
 * Kind-level TTL overrides ({@code epc.cache.ttl.{recordType}.kinds.{kind}})
 * are defined in the spec and wired in concrete pre-processors that have
 * access to the kind-specific env var injection.
 *
 * <h3>Redis unavailability</h3>
 * Cache reads and writes degrade gracefully on Redis exceptions — logged at
 * WARN and the request falls through to EPS. Lock acquisition (Phase 1) is the
 * hard Redis dependency; the cache is not.
 *
 * <h3>Metrics</h3>
 * <ul>
 *   <li>{@code epc.cache.hit}  — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.cache.miss} — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.cache.put}  — counter, tag: {@code record_type}</li>
 *   <li>{@code epc.cache.evict} — counter, tag: {@code record_type}</li>
 * </ul>
 */
@ApplicationScoped
public class FindByIdCacheService {

    private static final Logger LOG = Logger.getLogger(FindByIdCacheService.class);

    @ConfigProperty(name = "epc.cache.enabled", defaultValue = "true")
    boolean cacheEnabled;

    @ConfigProperty(name = "epc.cache.default-ttl", defaultValue = "60000")
    long defaultTtlMs;

    @ConfigProperty(name = "epc.cache.ttl.entity")
    Optional<Long> ttlEntity;

    @ConfigProperty(name = "epc.cache.ttl.list")
    Optional<Long> ttlList;

    @ConfigProperty(name = "epc.cache.ttl.relation")
    Optional<Long> ttlRelation;

    @ConfigProperty(name = "epc.cache.ttl.entity-reaction")
    Optional<Long> ttlEntityReaction;

    @ConfigProperty(name = "epc.cache.ttl.list-reaction")
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
        LOG.infof("FindByIdCacheService initialised. enabled=%s, defaultTtlMs=%d.",
                cacheEnabled, defaultTtlMs);
    }

    // ── Domain types ─────────────────────────────────────────────────────────

    /**
     * Represents a cached HTTP response entry.
     */
    public record CachedEntry(int status, String body, String contentType, String etag) {}

    // ── Read ─────────────────────────────────────────────────────────────────

    /**
     * Looks up a cached response for the given cache key.
     *
     * @param cacheKey   the fully-formed Redis key
     * @param recordType label for metrics
     * @return populated Optional on hit; empty on miss or Redis error
     */
    public Optional<CachedEntry> get(String cacheKey, String recordType) {
        if (!cacheEnabled) return Optional.empty();
        try {
            String raw = valueCommands.get(cacheKey);
            if (raw == null) {
                counter("epc.cache.miss", recordType).increment();
                LOG.debugf("Cache MISS: %s", cacheKey);
                return Optional.empty();
            }
            CachedEntry entry = deserialize(raw);
            counter("epc.cache.hit", recordType).increment();
            LOG.debugf("Cache HIT: %s (status=%d)", cacheKey, entry.status());
            return Optional.of(entry);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on cache GET '%s' — bypassing cache.", cacheKey);
            return Optional.empty();
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Stores a response in the cache.
     * Only status codes 200 and 404 are cached; all others are silently ignored.
     *
     * @param cacheKey    the fully-formed Redis key
     * @param status      HTTP status code of the EPS response
     * @param body        response body string
     * @param contentType value of the Content-Type response header (may be null)
     * @param recordType  label for metrics
     * @param ttlMs       effective TTL in milliseconds; 0 means use default
     */
    public void put(String cacheKey, int status, String body, String contentType,
                    String recordType, long ttlMs) {
        if (!cacheEnabled) return;
        if (status != 200 && status != 404) {
            LOG.debugf("Skip cache PUT for status %d: %s", status, cacheKey);
            return;
        }
        try {
            String etag = computeEtag(body);
            String serialized = serialize(status, body, contentType, etag);
            long effectiveTtlMs = ttlMs > 0 ? ttlMs : defaultTtlMs;
            valueCommands.set(cacheKey, serialized,
                    new SetArgs().px(Duration.ofMillis(effectiveTtlMs)));
            counter("epc.cache.put", recordType).increment();
            LOG.debugf("Cache PUT: %s (status=%d, ttl=%dms, etag=%s)",
                    cacheKey, status, effectiveTtlMs, etag);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on cache PUT '%s' — skipping.", cacheKey);
        }
    }

    // ── Evict ─────────────────────────────────────────────────────────────────

    /**
     * Evicts all cache entries matching {@code pattern} (wildcard KEYS scan).
     * Uses {@code KEYS} — acceptable because keyspaces are small per app and
     * the pattern is maximally specific (exact {appShortcode}:{id} prefix).
     *
     * @param pattern    Redis key pattern, e.g.
     *                   {@code books:epc:cache:entity:abc-123:*}
     * @param recordType label for metrics
     */
    public void evict(String pattern, String recordType) {
        if (!cacheEnabled) return;
        try {
            List<String> keys = keyCommands.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                LOG.debugf("Cache EVICT — no keys matched: %s", pattern);
                return;
            }
            @SuppressWarnings("unchecked")
            String[] keyArray = keys.toArray(new String[0]);
            keyCommands.del(keyArray);
            counter("epc.cache.evict", recordType).increment(keys.size());
            LOG.debugf("Cache EVICT — evicted %d key(s) matching: %s", keys.size(), pattern);
        } catch (Exception e) {
            LOG.warnf(e, "Redis error on cache EVICT '%s' — skipping.", pattern);
        }
    }

    // ── Key construction ─────────────────────────────────────────────────────

    /**
     * Builds the Redis cache key.
     * <pre>
     * {appShortcode}:epc:cache:{recordType}:{id}:{sortedQueryString}
     * </pre>
     *
     * @param appShortcode  from {@code X-App-Shortcode} header or fallback
     * @param recordType    "entity" | "list" | "relation" | "entity-reaction" | "list-reaction"
     * @param id            the record {@code _id}
     * @param rawQueryString the raw query string from the inbound request (may be null)
     */
    public String buildKey(String appShortcode, String recordType, String id, String rawQueryString) {
        String qs = normalizeQueryString(rawQueryString);
        return appShortcode + ":epc:cache:" + recordType + ":" + id + ":" + qs;
    }

    /**
     * Builds the wildcard eviction pattern that matches all cached variants
     * (different field projections) of the same record ID.
     * <pre>
     * {appShortcode}:epc:cache:{recordType}:{escapedId}:*
     * </pre>
     *
     * <p>The {@code id} segment is escaped for Redis glob syntax so that any
     * literal {@code [}, {@code ]}, {@code ?}, or {@code *} characters in the
     * ID do not get misinterpreted as pattern metacharacters by {@code KEYS}.
     * MongoDB ObjectIDs and UUIDs only contain {@code [0-9a-f-]} so this has no
     * effect under normal operation — it is defence-in-depth.</p>
     */
    public String buildEvictPattern(String appShortcode, String recordType, String id) {
        return appShortcode + ":epc:cache:" + recordType + ":" + escapeRedisGlob(id) + ":*";
    }

    /**
     * Returns {@code true} when the query string contains {@code filter[include]}
     * or its percent-encoded form — these requests must bypass the cache because
     * embedded nested records cannot be invalidated correctly.
     */
    public boolean hasIncludeFilter(String rawQueryString) {
        if (rawQueryString == null || rawQueryString.isBlank()) return false;
        String lower = rawQueryString.toLowerCase();
        return lower.contains("filter[include]") || lower.contains("filter%5binclude%5d");
    }

    // ── TTL resolution ────────────────────────────────────────────────────────

    /**
     * Resolves the effective TTL for a given record type (type-level, then global
     * fallback). Kind-level overrides are handled in the concrete pre-processors.
     *
     * @param recordType "entity" | "list" | "relation" | "entity-reaction" | "list-reaction"
     * @return TTL in milliseconds
     */
    public long resolveTypeTtlMs(String recordType) {
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
     * Escapes Redis glob metacharacters ({@code *}, {@code ?}, {@code [},
     * {@code ]}, {@code \}) in a string intended for use as a literal segment
     * inside a {@code KEYS} pattern.  Each metacharacter is prefixed with a
     * backslash so Redis treats it as a literal character.
     */
    private String escapeRedisGlob(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("*",  "\\*")
                .replace("?",  "\\?")
                .replace("[",  "\\[")
                .replace("]",  "\\]");
    }

    /**
     * Sorts query parameters alphabetically by key and returns the normalised
     * query string. Returns an empty string when there are no parameters.
     */
    private String normalizeQueryString(String rawQs) {
        if (rawQs == null || rawQs.isBlank()) return "";
        TreeMap<String, String> params = new TreeMap<>();
        for (String pair : rawQs.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                params.put(pair, "");
            } else {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
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

    /**
     * Serialises the cache entry into a JSON string.
     * Format: {@code {"s":{status},"ct":"...","e":"...","b":"..."}}
     */
    private String serialize(int status, String body, String contentType, String etag) {
        String escapedBody = escape(body);
        String escapedCt   = escape(contentType);
        String escapedEtag = escape(etag);
        return "{\"s\":" + status
                + ",\"ct\":\"" + escapedCt + "\""
                + ",\"e\":\"" + escapedEtag + "\""
                + ",\"b\":\"" + escapedBody + "\"}";
    }

    /** Deserialises a stored JSON string into a {@link CachedEntry}. */
    private CachedEntry deserialize(String raw) {
        int statusStart = raw.indexOf("\"s\":") + 4;
        int statusEnd   = raw.indexOf(',', statusStart);
        int status      = Integer.parseInt(raw.substring(statusStart, statusEnd).trim());

        String contentType = unescape(extractField(raw, "\"ct\":\"", "\",\"e\":"));
        String etag        = unescape(extractField(raw, "\"e\":\"",  "\",\"b\":"));
        String body        = unescape(extractBodyField(raw));

        return new CachedEntry(status, body, contentType, etag);
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

    /** The body field is the last field; it extends to the closing {@code "}. */
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
