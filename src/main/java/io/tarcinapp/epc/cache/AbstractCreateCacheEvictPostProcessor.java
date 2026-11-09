package io.tarcinapp.epc.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tarcinapp.epc.lock.AbstractWriteLockPostProcessor;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.jboss.logging.Logger;

/**
 * Abstract base for all POST (create) post-processors in Phase 2.
 *
 * <h3>Responsibility</h3>
 * After EPS creates a new record, this processor evicts any previously cached
 * {@code 404 Not Found} response for the new record's {@code _id}. This ensures
 * that a subsequent {@code GET /{type}/{newId}} fetches the fresh record from
 * EPS rather than replaying a stale 404.
 *
 * <h3>Why this is needed</h3>
 * A 404 is cached to prevent repeated MongoDB lookups for polling patterns.
 * Once the record is created, the cached 404 must be evicted immediately.
 *
 * <h3>Execution order</h3>
 * <ol>
 *   <li>Release any scope locks acquired by the pre-processor (handled by parent).</li>
 *   <li>Read the {@code _id} from the EPS response body.</li>
 *   <li>If present and status is 2xx, evict
 *       {@code {appShortcode}:epc:cache:{recordType}:{newId}:*}.</li>
 * </ol>
 *
 * <h3>EPS error responses</h3>
 * If EPS returns a non-2xx status, no eviction is performed — the record was
 * not created, so there is nothing to evict.
 */
public abstract class AbstractCreateCacheEvictPostProcessor extends AbstractWriteLockPostProcessor {

    private static final Logger LOG = Logger.getLogger(AbstractCreateCacheEvictPostProcessor.class);

    @Inject
    CacheEvictionHelper evictionHelper;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Returns the record type label for this create endpoint.
     */
    protected abstract String recordType();

    /**
     * Evicts the stale 404 entry for the newly created record after lock release.
     */
    @Override
    protected void afterLockRelease(Exchange exchange) throws Exception {
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);

        Integer statusCode = exchange.getMessage().getHeader(
                Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (statusCode == null || statusCode < 200 || statusCode >= 300) {
            return;  // EPS rejected the create — nothing to evict
        }

        String body = exchange.getMessage().getBody(String.class);
        String newId = extractId(body, requestId);
        if (newId != null) {
            evictionHelper.evictNewId(exchange, recordType(), newId);
            LOG.debugf("POST create — evicted stale 404 for %s/%s requestId=%s",
                    recordType(), newId, requestId);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String extractId(String body, String requestId) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode idNode = root.path("_id");
            if (idNode.isMissingNode() || idNode.isNull()) return null;
            return idNode.asText();
        } catch (Exception e) {
            LOG.debugf(e, "Could not extract _id from POST response body — skipping 404 eviction. requestId=%s",
                    requestId);
            return null;
        }
    }
}
