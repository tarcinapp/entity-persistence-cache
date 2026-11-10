package com.tarcinapp.epc.cache;

import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Abstract base for all collection GET pre-processors implementing Phase 3
 * visitor collection caching.
 *
 * <h3>Execution order</h3>
 * <ol>
 *   <li>If {@code filter[include]} is present → skip cache (bypass).</li>
 *   <li>If the query is NOT a visitor query (no {@code set[actives]}, or has
 *       {@code set[audience]}) → skip cache transparently.</li>
 *   <li>If {@code Cache-Control: no-store} → skip cache read and write.</li>
 *   <li>Build cache key:
 *       {@code {appShortcode}:epc:vcache:{recordType}:{sortedQsWithoutFieldset}}</li>
 *   <li>If {@code Cache-Control: no-cache} → skip read but allow write.</li>
 *   <li>On cache hit: short-circuit the route with the cached response.</li>
 *   <li>On cache miss: stash the cache key for the post-processor.</li>
 * </ol>
 *
 * <h3>Exchange properties written</h3>
 * <ul>
 *   <li>{@code epc.vcache.key}        — computed cache key (used by post-processor).</li>
 *   <li>{@code epc.vcache.skip-write} — {@code true} when writing must be suppressed.</li>
 *   <li>{@code CamelRouteStop}        — set to {@code true} on a cache hit.</li>
 * </ul>
 */
public abstract class AbstractVisitorCollectionCachePreProcessor implements Processor {

    private static final Logger LOG =
            Logger.getLogger(AbstractVisitorCollectionCachePreProcessor.class);

    public static final String VCACHE_KEY_PROPERTY        = "epc.vcache.key";
    public static final String VCACHE_SKIP_WRITE_PROPERTY = "epc.vcache.skip-write";

    @ConfigProperty(name = "epc.app-shortcode", defaultValue = "")
    String fallbackAppShortcode;

    @Inject
    VisitorCollectionCacheService cacheService;

    /**
     * Returns the record type label for this endpoint.
     * Must return one of: "entity", "list", "relation", "entity-reaction", "list-reaction".
     */
    protected abstract String recordType();

    @Override
    public final void process(Exchange exchange) throws Exception {
        String requestId    = exchange.getMessage().getHeader("X-Request-Id", String.class);
        String appShortcode = resolveAppShortcode(exchange);
        String rawQs        = exchange.getMessage().getHeader(Exchange.HTTP_QUERY, String.class);
        String cacheControl = exchange.getMessage().getHeader("Cache-Control", String.class);

        // ── filter[include] bypass ────────────────────────────────────────────
        if (cacheService.hasIncludeFilter(rawQs)) {
            LOG.debugf("vcache bypass (filter[include]) for %s requestId=%s", recordType(), requestId);
            exchange.setProperty(VCACHE_SKIP_WRITE_PROPERTY, Boolean.TRUE);
            return;
        }

        // ── Only cache visitor queries ────────────────────────────────────────
        if (!cacheService.isVisitorQuery(rawQs)) {
            LOG.debugf("vcache skip (not a visitor query) for %s requestId=%s", recordType(), requestId);
            exchange.setProperty(VCACHE_SKIP_WRITE_PROPERTY, Boolean.TRUE);
            return;
        }

        // ── Cache-Control: no-store — skip read and write ─────────────────────
        if (isNoStore(cacheControl)) {
            LOG.debugf("vcache bypass (no-store) for %s requestId=%s", recordType(), requestId);
            exchange.setProperty(VCACHE_SKIP_WRITE_PROPERTY, Boolean.TRUE);
            return;
        }

        // ── Build and stash the cache key ─────────────────────────────────────
        String cacheKey = cacheService.buildKey(appShortcode, recordType(), rawQs);
        exchange.setProperty(VCACHE_KEY_PROPERTY, cacheKey);

        // ── Cache-Control: no-cache — bypass read but allow write ─────────────
        if (isNoCache(cacheControl)) {
            LOG.debugf("vcache read-bypass (no-cache) for %s requestId=%s", recordType(), requestId);
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
        if (ifNoneMatch != null && !ifNoneMatch.isBlank() && ifNoneMatch.equals(entry.etag())) {
            LOG.debugf("vcache 304 Not Modified (ETag match) for %s requestId=%s",
                    recordType(), requestId);
            shortCircuit(exchange, 304, null, null, entry.etag());
            return;
        }

        // ── Cache hit — short-circuit the route ───────────────────────────────
        LOG.debugf("vcache HIT — short-circuiting for %s requestId=%s", recordType(), requestId);
        shortCircuit(exchange, 200, entry.body(), entry.contentType(), entry.etag());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveAppShortcode(Exchange exchange) {
        String header = exchange.getMessage().getHeader("X-App-Shortcode", String.class);
        if (header != null && !header.isBlank()) return header;
        if (fallbackAppShortcode != null && !fallbackAppShortcode.isBlank()) return fallbackAppShortcode;
        return "default";
    }

    private boolean isNoStore(String cc) {
        return cc != null && cc.toLowerCase().contains("no-store");
    }

    private boolean isNoCache(String cc) {
        return cc != null && cc.toLowerCase().contains("no-cache");
    }

    private void shortCircuit(Exchange exchange, int status, String body,
                               String contentType, String etag) {
        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        if (contentType != null) {
            exchange.getMessage().setHeader("Content-Type", contentType);
        }
        if (etag != null) {
            exchange.getMessage().setHeader("ETag", etag);
        }
        exchange.getMessage().setHeader("X-Cache-Status", "HIT");
        exchange.getMessage().setBody(body);
    }
}
