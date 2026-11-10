package com.tarcinapp.epc.processors.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarcinapp.epc.cache.CacheEvictionHelper;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import com.tarcinapp.epc.lock.PatchMergeHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PATCH /entities/{id} (updateEntityById).
 *
 * <h3>Operation order (per spec §Complete PATCH PreProcessor operation order)</h3>
 * <ol>
 *   <li>Read full-field cache entry; on miss fall through to EPS GET.</li>
 *   <li>Merge PATCH payload over fetched record → merged node.</li>
 *   <li>Evict findById cache entry ({@code DEL} wildcard).</li>
 *   <li>Acquire scope locks using merged node (done by the base class after
 *       this method returns).</li>
 *   <li>Forward PATCH to EPS (done by the route).</li>
 * </ol>
 *
 * <p>Steps 1–3 happen inside {@link #beforeLocks(Exchange)}.  The base class
 * reads the merged node from the {@code epc.patch.merged.node} exchange
 * property set here and uses it for lock key interpolation.</p>
 */
@ApplicationScoped
public class UpdateEntityByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Inject
    PatchMergeHelper patchMergeHelper;

    @Inject
    ObjectMapper objectMapper;

    @Override
    protected String recordType() { return "entity"; }

    @Override
    protected void beforeLocks(Exchange exchange) throws Exception {
        String appShortcode = resolveAppShortcode(exchange);
        String id = resolveId(exchange);

        // Parse the sparse PATCH payload
        String bodyStr = exchange.getMessage().getBody(String.class);
        JsonNode patchPayload;
        try {
            patchPayload = (bodyStr != null && !bodyStr.isBlank())
                    ? objectMapper.readTree(bodyStr)
                    : objectMapper.createObjectNode();
        } catch (Exception e) {
            patchPayload = objectMapper.createObjectNode();
        }

        // Step 1 + 2: fetch current record and merge PATCH payload over it
        PatchMergeHelper.MergeResult result =
                patchMergeHelper.fetchAndMerge(appShortcode, recordType(), id, patchPayload);

        switch (result.outcome()) {
            case NOT_FOUND -> {
                sendError(exchange, 404, "Not Found",
                        "Record " + recordType() + "/" + id + " not found.");
                exchange.setProperty(MERGED_PAYLOAD_STOP_PROPERTY, Boolean.TRUE);
                return;
            }
            case EPS_UNAVAILABLE -> {
                sendError(exchange, 503, "Service Unavailable",
                        "EPS is temporarily unavailable; PATCH cannot be safely serialised.");
                exchange.setProperty(MERGED_PAYLOAD_STOP_PROPERTY, Boolean.TRUE);
                return;
            }
            default -> { /* OK — continue */ }
        }

        // Step 3: evict cache (must precede lock acquisition)
        evictionHelper.evictById(exchange, recordType());

        // Stash merged node for base-class lock key interpolation
        exchange.setProperty(MERGED_PAYLOAD_NODE_PROPERTY, result.mergedNode());
    }
}
