package com.tarcinapp.epc.cache;

import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;

/**
 * Abstract base for all collection GET post-processors implementing Phase 3
 * visitor collection caching.
 *
 * <h3>Responsibility</h3>
 * After EPS responds with a 200, this processor stores the response body and
 * headers in Redis under the visitor collection cache prefix — unless the
 * pre-processor set {@code epc.vcache.skip-write}.
 *
 * <h3>Execution order</h3>
 * <ol>
 *   <li>Check {@code epc.vcache.skip-write} — if set, do nothing.</li>
 *   <li>Read the cache key from {@code epc.vcache.key} exchange property.</li>
 *   <li>Read the EPS response: status code, body, Content-Type header.</li>
 *   <li>Call {@link VisitorCollectionCacheService#put} — only status 200 is stored.</li>
 *   <li>Set {@code X-Cache-Status: MISS} on the response header.</li>
 * </ol>
 */
public abstract class AbstractVisitorCollectionCachePostProcessor implements Processor {

    private static final Logger LOG =
            Logger.getLogger(AbstractVisitorCollectionCachePostProcessor.class);

    @Inject
    VisitorCollectionCacheService cacheService;

    /**
     * Returns the record type label for this endpoint.
     * Must return one of: "entity", "list", "relation", "entity-reaction", "list-reaction".
     */
    protected abstract String recordType();

    @Override
    public final void process(Exchange exchange) throws Exception {
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);

        // ── Skip write if flagged by pre-processor ────────────────────────────
        Boolean skipWrite = exchange.getProperty(
                AbstractVisitorCollectionCachePreProcessor.VCACHE_SKIP_WRITE_PROPERTY, Boolean.class);
        if (Boolean.TRUE.equals(skipWrite)) {
            return;
        }

        // ── Retrieve the cache key set by the pre-processor ───────────────────
        String cacheKey = exchange.getProperty(
                AbstractVisitorCollectionCachePreProcessor.VCACHE_KEY_PROPERTY, String.class);
        if (cacheKey == null) {
            return;
        }

        // ── Read EPS response components ──────────────────────────────────────
        Integer statusCode = exchange.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE, Integer.class);
        int status = (statusCode != null) ? statusCode : 200;

        String body        = exchange.getMessage().getBody(String.class);
        String contentType = exchange.getMessage().getHeader("Content-Type", String.class);

        // ── Store in Redis (only 200 OK) ──────────────────────────────────────
        cacheService.put(cacheKey, status, body, contentType, recordType());

        // ── Mark response as a cache miss for observability ───────────────────
        exchange.getMessage().setHeader("X-Cache-Status", "MISS");
        LOG.debugf("vcache populated for %s (status=%d, key=%s) requestId=%s",
                recordType(), status, cacheKey, requestId);
    }
}
