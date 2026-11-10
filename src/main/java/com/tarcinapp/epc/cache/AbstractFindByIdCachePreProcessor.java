package com.tarcinapp.epc.cache;

import jakarta.inject.Inject;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Abstract base for all {@code GET /{recordType}/{id}} pre-processors in Phase 2.
 *
 * <h3>Execution order</h3>
 * <ol>
 *   <li>Resolve app shortcode from {@code X-App-Shortcode} header (or fallback).</li>
 *   <li>If {@code Cache-Control: no-store} or {@code filter[include]} → skip cache entirely.</li>
 *   <li>Build cache key:
 *       {@code {appShortcode}:epc:cache:{recordType}:{id}:{sortedQueryString}}</li>
 *   <li>On cache hit: populate the exchange with the cached response body and
 *       headers, set {@code CamelRouteStop=true} to short-circuit the route.</li>
 *   <li>On cache miss: set {@code epc.cache.key} property for the post-processor
 *       to use when populating the cache after the EPS call.</li>
 *   <li>Honour {@code If-None-Match}: on a hit, compare the cached ETag against
 *       the request's {@code If-None-Match} header and return 304 if they match.</li>
 * </ol>
 *
 * <h3>Cache bypass semantics</h3>
 * <ul>
 *   <li>{@code Cache-Control: no-cache} — skip cache read; populate cache from EPS response.</li>
 *   <li>{@code Cache-Control: no-store} — skip cache read; do NOT populate cache.</li>
 * </ul>
 *
 * <h3>Exchange properties written</h3>
 * <ul>
 *   <li>{@code epc.cache.key} — the computed cache key (used by post-processor).</li>
 *   <li>{@code epc.cache.skip-write} — {@code true} when {@code no-store} is set.</li>
 *   <li>{@code CamelRouteStop} — set to {@code true} on a cache hit to prevent
 *       proxying to EPS.</li>
 * </ul>
 */
public abstract class AbstractFindByIdCachePreProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(AbstractFindByIdCachePreProcessor.class);

    /** Exchange property key for the computed cache key (read by post-processor). */
    public static final String CACHE_KEY_PROPERTY       = "epc.cache.key";
    /** Exchange property key signalling that the post-processor must not write to cache. */
    public static final String CACHE_SKIP_WRITE_PROPERTY = "epc.cache.skip-write";

    @ConfigProperty(name = "epc.app-shortcode", defaultValue = "")
    String fallbackAppShortcode;

    @Inject
    FindByIdCacheService cacheService;

    /**
     * Returns the record type label for this endpoint.
     * Must return one of: "entity", "list", "relation", "entity-reaction", "list-reaction".
     */
    protected abstract String recordType();

    /**
     * Returns the resolved TTL for this record type (and optionally kind).
     * Default implementation delegates to {@link FindByIdCacheService#resolveTypeTtlMs(String)}.
     * Concrete subclasses may override to apply kind-level TTL overrides.
     */
    protected long resolveTtlMs() {
        return cacheService.resolveTypeTtlMs(recordType());
    }

    @Override
    public final void process(Exchange exchange) throws Exception {
        // ── Extract request components ────────────────────────────────────────
        String requestId     = exchange.getMessage().getHeader("X-Request-Id", String.class);
        String appShortcode  = resolveAppShortcode(exchange);
        String id            = resolveId(exchange);
        String rawQs         = exchange.getMessage().getHeader(
                Exchange.HTTP_QUERY, String.class);
        String cacheControl  = exchange.getMessage().getHeader(
                "Cache-Control", String.class);

        // ── filter[include] bypass ────────────────────────────────────────────
        if (cacheService.hasIncludeFilter(rawQs)) {
            LOG.debugf("findById cache bypass (filter[include]) for %s/%s requestId=%s",
                    recordType(), id, requestId);
            exchange.setProperty(CACHE_SKIP_WRITE_PROPERTY, Boolean.TRUE);
            return;
        }

        // ── Cache-Control: no-store — skip read and write ─────────────────────
        boolean noStore  = isNoStore(cacheControl);
        boolean noCache  = isNoCache(cacheControl);
        if (noStore) {
            LOG.debugf("findById cache bypass (no-store) for %s/%s requestId=%s",
                    recordType(), id, requestId);
            exchange.setProperty(CACHE_SKIP_WRITE_PROPERTY, Boolean.TRUE);
            return;
        }

        // ── Build and stash the cache key (post-processor needs it) ───────────
        String cacheKey = cacheService.buildKey(appShortcode, recordType(), id, rawQs);
        exchange.setProperty(CACHE_KEY_PROPERTY, cacheKey);

        // ── Cache-Control: no-cache — bypass read but allow write ─────────────
        if (noCache) {
            LOG.debugf("findById cache read-bypass (no-cache) for %s/%s requestId=%s",
                    recordType(), id, requestId);
            return;
        }

        // ── Cache lookup ──────────────────────────────────────────────────────
        var cached = cacheService.get(cacheKey, recordType());
        if (cached.isEmpty()) {
            return;  // miss — let the route proxy to EPS
        }

        var entry = cached.get();

        // ── If-None-Match conditional request support ─────────────────────────
        String ifNoneMatch = exchange.getMessage().getHeader("If-None-Match", String.class);
        long maxAgeSeconds = resolveTtlMs() / 1000;
        if (ifNoneMatch != null && !ifNoneMatch.isBlank() && ifNoneMatch.equals(entry.etag())) {
            LOG.debugf("findById 304 Not Modified (ETag match) for %s/%s requestId=%s",
                    recordType(), id, requestId);
            shortCircuit(exchange, 304, null, null, entry.etag(), maxAgeSeconds);
            return;
        }

        // ── Cache hit — short-circuit the route ───────────────────────────────
        LOG.debugf("findById cache HIT — short-circuiting for %s/%s requestId=%s",
                recordType(), id, requestId);
        shortCircuit(exchange, entry.status(), entry.body(), entry.contentType(), entry.etag(), maxAgeSeconds);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the record ID from the Camel {@code id} path-parameter header.
     *
     * <p>Camel's platform-http component stores path parameters as
     * {@code List<String>}. Calling {@code getHeader("id", String.class)}
     * serialises it via {@code List.toString()}, producing {@code "[uuid, uuid]"}
     * instead of the plain UUID.  This method unwraps the first element.</p>
     */
    private String resolveId(Exchange exchange) {
        Object raw = exchange.getMessage().getHeader("id");
        if (raw == null) return null;
        if (raw instanceof List<?> list) {
            if (list.isEmpty()) return null;
            Object first = list.get(0);
            return first != null ? first.toString() : null;
        }
        String s = raw.toString().trim();
        return s.isBlank() ? null : s;
    }

    private String resolveAppShortcode(Exchange exchange) {
        String header = exchange.getMessage().getHeader("X-App-Shortcode", String.class);
        if (header != null && !header.isBlank()) return header;
        if (fallbackAppShortcode != null && !fallbackAppShortcode.isBlank()) return fallbackAppShortcode;
        return "default";
    }

    private boolean isNoStore(String cacheControl) {
        return cacheControl != null && cacheControl.toLowerCase().contains("no-store");
    }

    private boolean isNoCache(String cacheControl) {
        return cacheControl != null && cacheControl.toLowerCase().contains("no-cache");
    }

    /**
     * Short-circuits the Camel route by setting the response directly on the
     * exchange and flagging {@code CamelRouteStop=true}.
     *
     * @param maxAgeSeconds the configured TTL in seconds; written as
     *                      {@code Cache-Control: max-age=N} so downstream clients
     *                      know the freshness window.
     */
    private void shortCircuit(Exchange exchange, int status, String body,
                              String contentType, String etag, long maxAgeSeconds) {
        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        if (contentType != null) {
            exchange.getMessage().setHeader("Content-Type", contentType);
        }
        if (etag != null) {
            exchange.getMessage().setHeader("ETag", etag);
        }
        exchange.getMessage().setHeader("Cache-Control", "max-age=" + maxAgeSeconds);
        exchange.getMessage().setHeader("X-Cache-Status", "HIT");
        exchange.getMessage().setBody(body);
    }
}
