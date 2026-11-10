package com.tarcinapp.epc.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Constructs Redis lock keys from EPS uniqueness/limit scope strings and an
 * incoming request payload.
 *
 * <h3>Uniqueness lock keys</h3>
 * Given a uniqueness scope such as:
 * <pre>
 *   where[_kind]=${_kind}&amp;where[_slug]=${_slug}&amp;set[owners][userIds]=${_ownerUsers}
 * </pre>
 * EPC extracts the interpolatable dimensions ({@code _kind}, {@code _slug},
 * {@code _ownerUsers}) from the request payload and builds one lock key per
 * distinct owner-dimension combination.
 *
 * <p>When an interpolated field is an array (e.g. {@code _ownerUsers:
 * ["user-1","user-2"]}), <em>one lock is acquired per element</em>.  This
 * ensures that two requests sharing any element of the array dimension are
 * serialised.</p>
 *
 * <p>Format:
 * <pre>
 *   {appShortcode}:epc:lock:uniqueness:{recordType}:{dim1}:{dim2}:...
 * </pre>
 * Example:
 * <pre>
 *   books:epc:lock:uniqueness:entity:book:the-great-gatsby:user-1
 *   books:epc:lock:uniqueness:entity:book:the-great-gatsby:user-2
 * </pre>
 * </p>
 *
 * <h3>Record-limit lock keys</h3>
 * Locks for record-limit rules are acquired <em>only</em> when
 * {@code enforceSerialisation=true} is set on the rule.  When absent or
 * {@code false}, no lock is acquired and EPS's own count check is the sole
 * enforcement gate (best-effort).
 *
 * <p>When {@code enforceSerialisation=true} and the scope contains
 * interpolatable fields (e.g. {@code where[_kind]=${_kind}&amp;set[owners][userIds]=${_ownerUsers}}):
 * <pre>
 *   {appShortcode}:epc:lock:limit:{recordType}:{scopeHash}:{dim1}:{dim2}:...
 * </pre>
 * When {@code enforceSerialisation=true} and the scope has no interpolatable fields:
 * <pre>
 *   {appShortcode}:epc:lock:limit:{recordType}:global       (scope: "")
 *   {appShortcode}:epc:lock:limit:{recordType}:kind:{kind}  (scope: where[_kind]=book)
 * </pre>
 * </p>
 */
@ApplicationScoped
public class LockKeyBuilder {

    private static final Logger LOG = Logger.getLogger(LockKeyBuilder.class);

    /** Matches ${fieldName} interpolation placeholders in scope strings. */
    private static final Pattern INTERPOLATION_PATTERN =
            Pattern.compile("\\$\\{([^}]+)\\}");

    @Inject
    ObjectMapper objectMapper;

    /**
     * Builds lock keys for a uniqueness scope rule.
     *
     * @param appShortcode  resolved app shortcode (from header or env fallback)
     * @param recordType    e.g. "entity", "list", "relation"
     * @param uniquenessScope  raw scope string from EPS config
     * @param payloadNode   parsed JSON body of the incoming request
     * @return list of Redis lock key strings (one per owner-dimension array element)
     */
    public List<String> buildUniquenessLockKeys(
            String appShortcode,
            String recordType,
            String uniquenessScope,
            JsonNode payloadNode) {

        List<String> dimensions = extractInterpolatableDimensions(uniquenessScope);
        if (dimensions.isEmpty()) {
            // Uniqueness rules always have at least one interpolatable field —
            // a rule with only literals is useless. Skip defensively.
            LOG.warnf("Uniqueness scope has no interpolatable fields: '%s' — no lock acquired.", uniquenessScope);
            return List.of();
        }

        // For each non-array dimension, we get one value.
        // For an array dimension (e.g. _ownerUsers), we get N values and produce N lock keys.
        // We need the Cartesian product of all dimension values, but in practice the
        // array dimension is always a single one (ownership). We implement the general
        // case: expand each array field into separate positions.
        List<List<String>> dimensionValues = new ArrayList<>();
        for (String dim : dimensions) {
            dimensionValues.add(extractFieldValues(payloadNode, dim));
        }

        // Flatten into lock keys: one key per combination across all dimensions.
        // For the common case (one array field + N scalar fields):
        //   keys = #elements in the array field
        List<String> keys = new ArrayList<>();
        buildKeyCombinations(
                appShortcode.toLowerCase() + ":epc:lock:uniqueness:" + recordType,
                dimensionValues,
                0,
                new ArrayList<>(),
                keys);

        return keys;
    }

    /**
     * Builds lock keys for a record-limit scope rule.
     *
     * <p>Returns an empty list unless {@code enforceSerialisation=true} is set
     * on the rule.  When serialisation is enforced, keys are derived from the
     * interpolatable dimensions in the scope string; if no dimensions are
     * present a global or kind-level key is returned.</p>
     *
     * @param appShortcode  resolved app shortcode
     * @param recordType    e.g. "entity", "list"
     * @param limit         the RecordLimit rule from EPS config
     * @param payloadNode   parsed JSON body of the incoming request
     * @return list of Redis lock key strings (may be empty)
     */
    public List<String> buildLimitLockKeys(
            String appShortcode,
            String recordType,
            SystemInfo.RecordLimit limit,
            JsonNode payloadNode) {

        if (!limit.isEnforceSerialisationEnabled()) {
            return List.of();
        }

        String scope = limit.getScope() == null ? "" : limit.getScope().trim();
        List<String> dimensions = extractInterpolatableDimensions(scope);

        if (dimensions.isEmpty()) {
            String key = buildWideScopeLockKey(appShortcode.toLowerCase(), recordType, scope);
            return List.of(key);
        }

        String scopeHash = Integer.toHexString(scope.hashCode());
        List<List<String>> dimensionValues = new ArrayList<>();
        for (String dim : dimensions) {
            dimensionValues.add(extractFieldValues(payloadNode, dim));
        }

        List<String> keys = new ArrayList<>();
        buildKeyCombinations(
                appShortcode.toLowerCase() + ":epc:lock:limit:" + recordType + ":" + scopeHash,
                dimensionValues,
                0,
                new ArrayList<>(),
                keys);

        return keys;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Extracts all {@code ${fieldName}} placeholders from a scope string.
     */
    List<String> extractInterpolatableDimensions(String scope) {
        List<String> dims = new ArrayList<>();
        Matcher m = INTERPOLATION_PATTERN.matcher(scope);
        while (m.find()) {
            String fieldName = m.group(1).trim();
            if (!dims.contains(fieldName)) {
                dims.add(fieldName);
            }
        }
        return dims;
    }

    /**
     * Extracts the value(s) of a field from the payload JSON node.
     * Supports dot-separated paths (e.g. {@code _ownerUsers}).
     * Returns an array of strings — one element for scalars, N elements for arrays.
     * Returns {@code ["__missing__"]} if the field is absent (never null-crashes).
     */
    private List<String> extractFieldValues(JsonNode node, String fieldPath) {
        JsonNode target = node;
        for (String segment : fieldPath.split("\\.")) {
            if (target == null || target.isNull()) break;
            target = target.get(segment);
        }

        if (target == null || target.isNull() || target.isMissingNode()) {
            LOG.warnf("Field '%s' not found in request payload — using placeholder in lock key.", fieldPath);
            return List.of("__missing__");
        }

        if (target.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode element : target) {
                values.add(element.asText());
            }
            return values.isEmpty() ? List.of("__empty__") : values;
        }

        return List.of(target.asText());
    }

    /**
     * Recursively builds the Cartesian product of dimension value lists and
     * appends each resulting key to {@code result}.
     */
    private void buildKeyCombinations(
            String prefix,
            List<List<String>> dimensionValues,
            int index,
            List<String> current,
            List<String> result) {

        if (index == dimensionValues.size()) {
            result.add(prefix + ":" + String.join(":", current));
            return;
        }

        for (String value : dimensionValues.get(index)) {
            current.add(sanitize(value));
            buildKeyCombinations(prefix, dimensionValues, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Builds a lock key for a wide-scope (no interpolatable fields) limit rule
     * when {@code enforceSerialisation=true}.
     */
    private String buildWideScopeLockKey(String appShortcode, String recordType, String scope) {
        if (scope.isEmpty()) {
            return appShortcode + ":epc:lock:limit:" + recordType + ":global";
        }
        // Attempt to extract a static kind value from scope: where[_kind]=book
        String kindKey = extractStaticKind(scope);
        if (kindKey != null) {
            return appShortcode + ":epc:lock:limit:" + recordType + ":kind:" + sanitize(kindKey);
        }
        // Fall back to a hash of the scope string
        String scopeHash = Integer.toHexString(scope.hashCode());
        return appShortcode + ":epc:lock:limit:" + recordType + ":scope:" + scopeHash;
    }

    /**
     * Tries to extract a static {@code _kind} value from a scope like
     * {@code where[_kind]=book} (no interpolation placeholder).
     */
    private String extractStaticKind(String scope) {
        Pattern p = Pattern.compile("where\\[_kind\\]=([^&${}]+)");
        Matcher m = p.matcher(scope);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Sanitises a value for inclusion in a Redis key by replacing characters
     * that Redis key conventions discourage (spaces, colons, braces).
     */
    private String sanitize(String value) {
        return value.replaceAll("[:\\s{}]", "_");
    }
}
