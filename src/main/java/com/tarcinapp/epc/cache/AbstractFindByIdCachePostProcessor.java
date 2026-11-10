package com.tarcinapp.epc.cache;

import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;

/**
 * Abstract base for all {@code GET /{recordType}/{id}} post-processors in Phase 2.
 *
 * <h3>Responsibility</h3>
 * After the backend (EPS) responds, this processor caches the response body and
 * headers in Redis — unless the pre-processor flagged that caching should be
 * skipped (via {@code epc.cache.skip-write} exchange property).
 *
 * <h3>Execution order</h3>
 * <ol>
 *   <li>Check {@code epc.cache.skip-write} — if set, do nothing.</li>
 *   <li>Read the cache key from {@code epc.cache.key} exchange property.</li>
 *   <li>Read the EPS response: status code, body, Content-Type header.</li>
 *   <li>Call {@link FindByIdCacheService#put} only for status 200 or 404.</li>
 *   <li>Set {@code X-Cache-Status: MISS} on the response header.</li>
 * </ol>
 *
 * <h3>EPS errors</h3>
 * 4xx other than 404, and all 5xx responses are NOT cached. The post-processor
 * delegates this decision to {@link FindByIdCacheService#put}, which silently
 * ignores non-cacheable status codes.
 */
public abstract class AbstractFindByIdCachePostProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(AbstractFindByIdCachePostProcessor.class);

    @Inject
    FindByIdCacheService cacheService;

    /**
     * Returns the record type label for this endpoint.
     * Must return one of: "entity", "list", "relation", "entity-reaction", "list-reaction".
     */
    protected abstract String recordType();

    /**
     * Returns the effective TTL in milliseconds for this record type.
     * Default implementation delegates to {@link FindByIdCacheService#resolveTypeTtlMs(String)}.
     * Concrete subclasses may override to apply kind-level TTL overrides.
     */
    protected long resolveTtlMs() {
        return cacheService.resolveTypeTtlMs(recordType());
    }

    @Override
    public final void process(Exchange exchange) throws Exception {
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);

        // ── Skip write if flagged by pre-processor ────────────────────────────
        Boolean skipWrite = exchange.getProperty(
                AbstractFindByIdCachePreProcessor.CACHE_SKIP_WRITE_PROPERTY, Boolean.class);
        if (Boolean.TRUE.equals(skipWrite)) {
            return;
        }

        // ── Retrieve the cache key set by the pre-processor ───────────────────
        String cacheKey = exchange.getProperty(
                AbstractFindByIdCachePreProcessor.CACHE_KEY_PROPERTY, String.class);
        if (cacheKey == null) {
            // Pre-processor did not set a cache key — skip write silently.
            return;
        }

        // ── Read EPS response components ──────────────────────────────────────
        Integer statusCode = exchange.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE, Integer.class);
        int status = (statusCode != null) ? statusCode : 200;

        String body = exchange.getMessage().getBody(String.class);
        String contentType = exchange.getMessage().getHeader("Content-Type", String.class);

        // ── Store in Redis ────────────────────────────────────────────────────
        cacheService.put(cacheKey, status, body, contentType, recordType(), resolveTtlMs());

        // ── Mark response as a cache miss for observability ───────────────────
        exchange.getMessage().setHeader("X-Cache-Status", "MISS");
        LOG.debugf("findById cache populated for %s (status=%d, key=%s) requestId=%s",
                recordType(), status, cacheKey, requestId);
    }
}
