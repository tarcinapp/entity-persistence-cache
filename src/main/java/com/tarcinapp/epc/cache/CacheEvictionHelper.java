package com.tarcinapp.epc.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * CDI helper that performs findById cache eviction for write-path pre-processors.
 *
 * <h3>Why eviction happens in the pre-processor</h3>
 * Eviction fires before the write reaches EPS to eliminate the window where a
 * concurrent reader could re-populate the cache with the old value between lock
 * acquisition and write completion. If EPS ultimately rejects the write, the
 * eviction was unnecessary but harmless — the next GET incurs one extra MongoDB
 * round-trip.
 *
 * <h3>Usage</h3>
 * Inject this bean into any write-path pre-processor and call
 * {@link #evictById(Exchange, String)} at the start of {@code process()}.
 * The caller must then continue with its own logic (e.g. lock acquisition via
 * the parent class).
 *
 * <h3>POST (create) endpoints</h3>
 * POST endpoints have no {@code id} path parameter. Cache eviction for creates
 * (to evict previously cached 404 responses for the new record's ID) is handled
 * in a separate {@link CreateCacheEvictPostProcessor}.
 */
@ApplicationScoped
public class CacheEvictionHelper {

    private static final Logger LOG = Logger.getLogger(CacheEvictionHelper.class);

    @ConfigProperty(name = "epc.app-shortcode", defaultValue = "")
    String fallbackAppShortcode;

    @Inject
    FindByIdCacheService cacheService;

    /**
     * Evicts all cached variants of the record identified by the {@code id}
     * path parameter in the exchange.
     *
     * @param exchange   the Camel exchange from the inbound request
     * @param recordType "entity", "list", "relation", "entity-reaction", or "list-reaction"
     */
    public void evictById(Exchange exchange, String recordType) {
        String id = resolveId(exchange);
        if (id == null || id.isBlank()) {
            return;
        }
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);
        String appShortcode = resolveShortcode(exchange);
        String pattern = cacheService.buildEvictPattern(appShortcode, recordType, id);
        cacheService.evict(pattern, recordType);
        LOG.debugf("Write eviction done for %s/%s requestId=%s", recordType, id, requestId);
    }

    /**
     * Evicts the cached 404 (or any cached variant) for a newly created record
     * whose ID is provided explicitly.  Called from POST post-processors.
     *
     * @param exchange   the Camel exchange from the backend response
     * @param recordType record type label
     * @param newId      the {@code _id} of the newly created record
     */
    public void evictNewId(Exchange exchange, String recordType, String newId) {
        if (newId == null || newId.isBlank()) return;
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);
        String appShortcode = resolveShortcode(exchange);
        String pattern = cacheService.buildEvictPattern(appShortcode, recordType, newId);
        cacheService.evict(pattern, recordType);
        LOG.debugf("POST create 404-eviction done for %s/%s requestId=%s",
                recordType, newId, requestId);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Extracts the record ID from the Camel {@code id} path-parameter header.
     *
     * <p>Camel's platform-http component stores path parameters as
     * {@code List<String>} when the route uses a {@code {id}} template.
     * Calling {@code getHeader("id", String.class)} serialises the list via
     * {@code List.toString()}, producing {@code "[uuid, uuid]"} instead of the
     * plain UUID string.  This method unwraps the first element when the header
     * value is a List, and falls back to a plain String otherwise.</p>
     *
     * @param exchange the Camel exchange from the inbound request
     * @return the raw record ID string, or {@code null} if absent
     */
    @SuppressWarnings("unchecked")
    String resolveId(Exchange exchange) {
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

    private String resolveShortcode(Exchange exchange) {
        String header = exchange.getMessage().getHeader("X-App-Shortcode", String.class);
        if (header != null && !header.isBlank()) return header;
        if (fallbackAppShortcode != null && !fallbackAppShortcode.isBlank())
            return fallbackAppShortcode;
        return "default";
    }
}
