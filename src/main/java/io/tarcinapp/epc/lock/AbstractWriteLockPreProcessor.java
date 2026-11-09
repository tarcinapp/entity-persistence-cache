package io.tarcinapp.epc.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.redisson.api.RLock;

/**
 * Abstract base for all write-path (POST / PUT / PATCH / DELETE by ID)
 * pre-processors that must acquire distributed scope locks before forwarding
 * the request to EPS.
 *
 * <h3>Execution order (per spec §Concurrency and thundering herd)</h3>
 * <ol>
 *   <li>Acquire scope locks</li>
 *   <li>Forward write to EPS (done by the route after this processor returns)</li>
 * </ol>
 * Lock release happens in the matching {@link AbstractWriteLockPostProcessor}.
 *
 * <h3>App shortcode resolution</h3>
 * EPC reads {@code X-App-Shortcode} from the incoming header (injected by the
 * gateway). If absent, it falls back to the {@code epc.app-shortcode} env var.
 * The header takes precedence when both are present.
 *
 * <h3>Error mapping</h3>
 * <ul>
 *   <li>{@link LockService.LockTimeoutException} → HTTP 423 Locked</li>
 *   <li>{@link LockService.RedisUnavailableException} → HTTP 503 Service Unavailable</li>
 * </ul>
 *
 * <h3>Exchange property used for cleanup</h3>
 * The list of acquired lock handles is stored on the Exchange as
 * {@code epc.acquired.locks} so the post-processor can release them in
 * {@code doFinally} even on an exception path.
 */
public abstract class AbstractWriteLockPreProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(AbstractWriteLockPreProcessor.class);

    /** Exchange property key for acquired lock handles. */
    public static final String ACQUIRED_LOCKS_PROPERTY = "epc.acquired.locks";
    /** Exchange property key for lock type label (metrics). */
    public static final String LOCK_TYPE_PROPERTY = "epc.lock.type";
    /**
     * Exchange property key for the merged payload node produced by PATCH
     * pre-processors.  Set by {@code Update*ByIdPreProcessor.beforeLocks()};
     * read back in {@link #resolveMergedPayloadNode(Exchange)}.
     */
    public static final String MERGED_PAYLOAD_NODE_PROPERTY = "epc.patch.merged.node";
    /**
     * Exchange property key set to {@code true} by PATCH pre-processors when
     * the record-fetch step returned a non-OK outcome (404 or 503).
     * Checked in {@link #isMergedNodeSignalStop(Exchange)}.
     */
    public static final String MERGED_PAYLOAD_STOP_PROPERTY = "epc.patch.merged.stop";

    @ConfigProperty(name = "epc.app-shortcode", defaultValue = "")
    String fallbackAppShortcode;

    @Inject
    ConfigCacheService configCacheService;

    @Inject
    LockService lockService;

    @Inject
    LockKeyBuilder lockKeyBuilder;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Returns the record type label used in lock keys.
     * Implementations return one of: "entity", "list", "relation",
     * "entity-reaction", "list-reaction".
     */
    protected abstract String recordType();

    /**
     * Returns the lock-type label used in metrics.
     * Overridden by delete-by-id processors where no scope lock is needed
     * (DELETE /entities/{id} does not carry a payload and cannot determine
     * scope — no lock is required, only cache eviction in Phase 2).
     * Default implementation returns the combined lock type label.
     */
    protected boolean shouldAcquireLocks() {
        return true;
    }

    /**
     * Optional hook called at the very start of {@link #process(Exchange)},
     * before lock acquisition. Default implementation is a no-op.
     *
     * <p>Phase 2 subclasses (write-by-id processors) override this to evict
     * the findById cache entry before forwarding the write to EPS.</p>
     */
    protected void beforeLocks(Exchange exchange) throws Exception {
        // no-op by default
    }

    /**
     * Returns the payload node to use for lock key interpolation.
     *
     * <p>The default implementation reads the
     * {@link #MERGED_PAYLOAD_NODE_PROPERTY} exchange property set by PATCH
     * pre-processors in their {@code beforeLocks()} override, and returns it
     * when present.  Returns {@code null} when the property is absent (i.e. for
     * POST, PUT, and DELETE operations), causing {@code process()} to fall back
     * to parsing the raw request body.</p>
     *
     * <p>Subclasses do not need to override this method — they simply set the
     * exchange property in {@code beforeLocks()} via
     * {@link #MERGED_PAYLOAD_NODE_PROPERTY}.</p>
     */
    protected JsonNode resolveMergedPayloadNode(Exchange exchange) throws Exception {
        return exchange.getProperty(MERGED_PAYLOAD_NODE_PROPERTY, JsonNode.class);
    }

    /**
     * Returns {@code true} if the route should stop after
     * {@link #beforeLocks(Exchange)} completes.
     *
     * <p>The default implementation reads the
     * {@link #MERGED_PAYLOAD_STOP_PROPERTY} exchange property set by PATCH
     * pre-processors when the record-fetch step returned 404 or 503.  In those
     * cases the route is already short-circuited with the appropriate error
     * response; lock acquisition must be skipped.</p>
     *
     * <p>Subclasses do not need to override this method — they simply set the
     * exchange property via {@link #MERGED_PAYLOAD_STOP_PROPERTY}.</p>
     */
    protected boolean isMergedNodeSignalStop(Exchange exchange) {
        return Boolean.TRUE.equals(exchange.getProperty(MERGED_PAYLOAD_STOP_PROPERTY, Boolean.class));
    }

    @Override
    public final void process(Exchange exchange) throws Exception {
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);

        beforeLocks(exchange);
        if (!shouldAcquireLocks()) {
            return;
        }
        // PATCH processors signal route-stop (404/503) via isMergedNodeSignalStop()
        if (isMergedNodeSignalStop(exchange)) {
            return;
        }

        String appShortcode = resolveAppShortcode(exchange);
        SystemInfo config;
        try {
            config = configCacheService.getConfig();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch /system-info from EPS — returning 503. requestId=%s",
                    requestId);
            sendError(exchange, 503, "Service Unavailable",
                      "EPC could not fetch EPS configuration: " + e.getMessage());
            return;
        }

        // Use the pre-built merged node when supplied by a PATCH pre-processor,
        // otherwise fall back to parsing the raw request body.
        JsonNode payloadNode = resolveMergedPayloadNode(exchange);
        if (payloadNode == null) {
            String bodyStr = exchange.getMessage().getBody(String.class);
            try {
                payloadNode = (bodyStr != null && !bodyStr.isBlank())
                        ? objectMapper.readTree(bodyStr)
                        : objectMapper.createObjectNode();
            } catch (Exception e) {
                LOG.warnf("Could not parse request body as JSON — using empty node for lock key building. requestId=%s",
                        requestId);
                payloadNode = objectMapper.createObjectNode();
            }
        }

        // Collect all lock keys from uniqueness rules + limit rules
        List<String> allKeys = new ArrayList<>();
        allKeys.addAll(buildUniquenessKeys(appShortcode, config, payloadNode));
        allKeys.addAll(buildLimitKeys(appShortcode, config, payloadNode));

        if (allKeys.isEmpty()) {
            LOG.debugf("No lock keys required for this request (no applicable rules). requestId=%s",
                    requestId);
            return;
        }

        LOG.debugf("Acquiring %d lock(s) for %s write: %s requestId=%s",
                allKeys.size(), recordType(), allKeys, requestId);

        // Acquire all locks — throws on timeout (423) or Redis failure (503)
        try {
            List<RLock> locks = lockService.acquireAll(allKeys, determineLockTypeLabel(allKeys, config));
            exchange.setProperty(ACQUIRED_LOCKS_PROPERTY, locks);
            exchange.setProperty(LOCK_TYPE_PROPERTY, determineLockTypeLabel(allKeys, config));
            LOG.debugf("All locks acquired for %s write. requestId=%s", recordType(), requestId);
        } catch (LockService.LockTimeoutException e) {
            LOG.warnf("Lock timeout for %s write: %s requestId=%s",
                    recordType(), e.getMessage(), requestId);
            sendError(exchange, 423, "Locked",
                      "Another request is currently processing a write for the same resource. "
                      + "Please retry after a moment.");
        } catch (LockService.RedisUnavailableException e) {
            LOG.errorf(e, "Redis unavailable during lock acquisition for %s write. requestId=%s",
                    recordType(), requestId);
            sendError(exchange, 503, "Service Unavailable",
                      "EPC lock service is temporarily unavailable: " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Resolves the application shortcode from the {@code X-App-Shortcode} header,
     * falling back to the {@code epc.app-shortcode} env var.
     * Exposed as {@code protected} so PATCH pre-processors can use it inside
     * {@code beforeLocks()} to build the correct shortcode for
     * {@link PatchMergeHelper#fetchAndMerge}.
     */
    protected String resolveAppShortcode(Exchange exchange) {
        String header = exchange.getMessage().getHeader("X-App-Shortcode", String.class);
        if (header != null && !header.isBlank()) {
            return header;
        }
        if (fallbackAppShortcode != null && !fallbackAppShortcode.isBlank()) {
            return fallbackAppShortcode;
        }
        return "default";
    }

    private List<String> buildUniquenessKeys(
            String appShortcode, SystemInfo config, JsonNode payload) {

        List<String> scopes = getUniquenessScopes(config);
        if (scopes == null || scopes.isEmpty()) return List.of();

        List<String> keys = new ArrayList<>();
        for (String scope : scopes) {
            keys.addAll(lockKeyBuilder.buildUniquenessLockKeys(
                    appShortcode, recordType(), scope, payload));
        }
        return keys;
    }

    private List<String> buildLimitKeys(
            String appShortcode, SystemInfo config, JsonNode payload) {

        List<SystemInfo.RecordLimit> limits = getLimits(config);
        if (limits == null || limits.isEmpty()) return List.of();

        List<String> keys = new ArrayList<>();
        for (SystemInfo.RecordLimit limit : limits) {
            keys.addAll(lockKeyBuilder.buildLimitLockKeys(
                    appShortcode, recordType(), limit, payload));
        }
        return keys;
    }

    /**
     * Returns uniqueness scopes for this record type from EPS config.
     * Subclasses may override for custom record types; the default dispatches
     * on {@link #recordType()}.
     */
    protected List<String> getUniquenessScopes(SystemInfo config) {
        return switch (recordType()) {
            case "entity"          -> config.getEntityUniqueness();
            case "list"            -> config.getListUniqueness();
            case "relation"        -> config.getRelationUniqueness();
            case "entity-reaction" -> config.getEntityReactionUniqueness();
            case "list-reaction"   -> config.getListReactionUniqueness();
            default -> null;
        };
    }

    /**
     * Returns record-limit rules for this record type from EPS config.
     */
    protected List<SystemInfo.RecordLimit> getLimits(SystemInfo config) {
        return switch (recordType()) {
            case "entity"          -> config.getEntityLimits();
            case "list"            -> config.getListLimits();
            case "relation"        -> config.getRelationLimits();
            case "entity-reaction" -> config.getEntityReactionLimits();
            case "list-reaction"   -> config.getListReactionLimits();
            default -> null;
        };
    }

    private String determineLockTypeLabel(List<String> keys, SystemInfo config) {
        // Use "uniqueness" if there are uniqueness rules, "limit" if only limit rules
        List<String> uniquenessScopes = getUniquenessScopes(config);
        boolean hasUniqueness = uniquenessScopes != null && !uniquenessScopes.isEmpty();
        boolean hasLimits = getLimits(config) != null && !getLimits(config).isEmpty();
        if (hasUniqueness && hasLimits) return "mixed";
        if (hasUniqueness) return "uniqueness";
        if (hasLimits) return "limit";
        return "unknown";
    }

    /**
     * Extracts the record ID from the Camel {@code id} path-parameter header.
     *
     * <p>Camel's platform-http component stores path parameters as
     * {@code List<String>}.  Exposed as {@code protected} so PATCH pre-processors
     * can resolve the ID inside {@code beforeLocks()} when calling
     * {@link PatchMergeHelper#fetchAndMerge}.</p>
     */
    protected String resolveId(Exchange exchange) {
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

    /**
     * Short-circuits the route with the given HTTP status code and JSON error body.
     * Exposed as {@code protected} so PATCH pre-processors can send 404/503 from
     * within {@code beforeLocks()}.
     */
    protected void sendError(Exchange exchange, int status, String error, String message) {
        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        exchange.getMessage().setHeader("Content-Type", "application/json");
        exchange.getMessage().setBody(
                String.format("{\"statusCode\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                              status, error, message.replace("\"", "'")));
    }
}
