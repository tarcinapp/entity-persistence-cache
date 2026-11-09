package io.tarcinapp.epc.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.tarcinapp.epc.cache.FindByIdCacheService;
import io.tarcinapp.epc.gateway.EpsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Fetches the current stored record for a PATCH request and merges the PATCH
 * payload over it, producing the post-patch state to use as the source for
 * lock key interpolation.
 *
 * <h3>Why this is necessary</h3>
 * A PATCH payload contains only the fields the caller intends to change.  Lock
 * keys are derived from the uniqueness/limit scope dimensions (e.g. {@code _kind},
 * {@code _slug}, {@code _ownerUsers}).  If those fields are absent from the PATCH
 * payload, {@link LockKeyBuilder} falls back to {@code __missing__} placeholders,
 * which over-serialises all PATCH requests on the same record type regardless of
 * actual uniqueness overlap.  Using the post-patch state avoids this.
 *
 * <h3>Fetch strategy</h3>
 * <ol>
 *   <li>Check the findById cache for a <em>full-field entry</em>
 *       (key suffix is {@code ""}). Use the cached body if present.</li>
 *   <li>On cache miss: call EPS {@code GET /{recordType}/{id}} directly over
 *       localhost loopback.
 *       <ul>
 *         <li>200 → use response body</li>
 *         <li>404 → signal NOT_FOUND (caller returns 404 to client)</li>
 *         <li>5xx / unreachable → signal EPS_UNAVAILABLE (caller returns 503)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>Merge semantics</h3>
 * Shallow, non-mutating overlay: for each top-level field present in the PATCH
 * payload, the payload value replaces the corresponding field in the fetched
 * record.  All other fields are inherited unchanged.  This mirrors EPS's own
 * PATCH semantics.  The result is used only for lock key building — it is never
 * written back to cache or forwarded to EPS.
 *
 * <h3>Important note on call order</h3>
 * Callers MUST call {@link #fetchAndMerge} <em>before</em> evicting the cache
 * entry — evicting first guarantees a cache miss and forces every PATCH into an
 * EPS fetch.  The fetch → merge → evict → lock order is mandated by the spec.
 */
@ApplicationScoped
public class PatchMergeHelper {

    private static final Logger LOG = Logger.getLogger(PatchMergeHelper.class);

    /** Outcome of {@link #fetchAndMerge}. */
    public enum FetchOutcome { OK, NOT_FOUND, EPS_UNAVAILABLE }

    /** Wraps the merged node and the fetch outcome together. */
    public record MergeResult(FetchOutcome outcome, JsonNode mergedNode) {}

    @ConfigProperty(name = "epc.app-shortcode", defaultValue = "")
    String fallbackAppShortcode;

    @Inject
    FindByIdCacheService cacheService;

    @Inject
    @RestClient
    EpsClient epsClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MeterRegistry meterRegistry;

    /**
     * Fetches the current stored record and merges the PATCH payload over it.
     *
     * @param appShortcode   resolved app shortcode (from header or env fallback)
     * @param recordType     "entity", "list", "relation", "entity-reaction", "list-reaction"
     * @param id             the record {@code _id}
     * @param patchPayload   the parsed PATCH request body (fields to overlay)
     * @return {@link MergeResult} with outcome {@code OK} and the merged node,
     *         or {@code NOT_FOUND} / {@code EPS_UNAVAILABLE} on error
     */
    public MergeResult fetchAndMerge(
            String appShortcode,
            String recordType,
            String id,
            JsonNode patchPayload) {

        JsonNode currentRecord = tryReadFromCache(appShortcode, recordType, id);

        if (currentRecord == null) {
            // Cache miss — fall through to EPS
            counter("epc.lock.record_fetch", recordType, "eps_fallback").increment();
            LOG.infof("LOCK_RECORD_FETCH_EPS_FALLBACK recordType=%s id=%s", recordType, id);
            currentRecord = fetchFromEps(recordType, id);
            if (currentRecord == null) {
                // fetchFromEps returns null to signal 404
                LOG.infof("LOCK_RECORD_NOT_FOUND recordType=%s id=%s", recordType, id);
                return new MergeResult(FetchOutcome.NOT_FOUND, null);
            }
            if (currentRecord.has("__eps_unavailable__")) {
                return new MergeResult(FetchOutcome.EPS_UNAVAILABLE, null);
            }
        } else {
            counter("epc.lock.record_fetch", recordType, "cache_hit").increment();
            LOG.debugf("LOCK_RECORD_FETCH_HIT recordType=%s id=%s", recordType, id);
        }

        JsonNode merged = shallowMerge(currentRecord, patchPayload);
        return new MergeResult(FetchOutcome.OK, merged);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Looks up the zero-suffix (full-field) cache entry for the given record.
     * Returns {@code null} on miss, eviction, or Redis error.
     */
    private JsonNode tryReadFromCache(String appShortcode, String recordType, String id) {
        String cacheKey = cacheService.buildKey(appShortcode, recordType, id, "");
        Optional<FindByIdCacheService.CachedEntry> cached =
                cacheService.get(cacheKey, recordType);
        if (cached.isEmpty()) {
            return null;
        }
        FindByIdCacheService.CachedEntry entry = cached.get();
        if (entry.status() != 200 || entry.body() == null || entry.body().isBlank()) {
            // 404 cached or empty body — treat as miss for merge purposes
            return null;
        }
        try {
            return objectMapper.readTree(entry.body());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to parse cached body for %s/%s — falling through to EPS.", recordType, id);
            return null;
        }
    }

    /**
     * Fetches the record directly from EPS.
     * Returns the parsed {@link JsonNode} on 200, {@code null} on 404, or a
     * sentinel node with a {@code __eps_unavailable__} field on 5xx/error.
     */
    private JsonNode fetchFromEps(String recordType, String id) {
        String epsPath = toEpsPath(recordType);
        try (Response resp = epsClient.getRecordById(epsPath, id)) {
            int status = resp.getStatus();
            if (status == 200) {
                String body = resp.readEntity(String.class);
                return objectMapper.readTree(body);
            }
            if (status == 404) {
                return null;
            }
            // 5xx or unexpected status — signal unavailability
            LOG.errorf("EPS returned HTTP %d for %s/%s — returning 503 to caller.", status, epsPath, id);
            ObjectNode sentinel = objectMapper.createObjectNode();
            sentinel.put("__eps_unavailable__", true);
            return sentinel;
        } catch (Exception e) {
            LOG.errorf(e, "EPS unreachable fetching %s/%s — returning 503 to caller.", epsPath, id);
            ObjectNode sentinel = objectMapper.createObjectNode();
            sentinel.put("__eps_unavailable__", true);
            return sentinel;
        }
    }

    /**
     * Shallow overlay: returns a new {@link ObjectNode} containing all fields
     * from {@code base}, with any field present in {@code overlay} replaced by
     * the overlay value.  Neither input is mutated.
     */
    private JsonNode shallowMerge(JsonNode base, JsonNode overlay) {
        ObjectNode merged = objectMapper.createObjectNode();
        if (base.isObject()) {
            base.properties().forEach(e -> merged.set(e.getKey(), e.getValue()));
        }
        if (overlay != null && overlay.isObject()) {
            overlay.properties().forEach(e -> merged.set(e.getKey(), e.getValue()));
        }
        return merged;
    }

    /** Maps the internal record type label to the plural EPS path segment. */
    private String toEpsPath(String recordType) {
        return switch (recordType) {
            case "entity"          -> "entities";
            case "list"            -> "lists";
            case "relation"        -> "relations";
            case "entity-reaction" -> "entity-reactions";
            case "list-reaction"   -> "list-reactions";
            default -> recordType + "s";
        };
    }

    private Counter counter(String name, String recordType, String source) {
        return Counter.builder(name)
                .tag("record_type", recordType)
                .tag("source", source)
                .register(meterRegistry);
    }
}
