package io.tarcinapp.epc.lock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Mirrors the {@code RecordLimitConfig} schema exposed by EPS's
 * {@code /system-info} endpoint.
 *
 * <p>EPS serialises this object as part of its system info response.
 * EPC reads it to determine which lock keys to acquire for each write.</p>
 *
 * <p>The {@code enforceSerialisation} field on {@link RecordLimit} is a
 * passthrough: EPS preserves it from its own configuration but never acts on
 * it. EPC reads it to decide whether to acquire a lock for wide-scope limit
 * rules (those with no interpolatable fields).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemInfo {

    // ── Record limit rules ────────────────────────────────────────────────────

    private List<RecordLimit> entityLimits;
    private List<RecordLimit> listLimits;
    private List<RecordLimit> relationLimits;
    private List<RecordLimit> entityReactionLimits;
    private List<RecordLimit> listReactionLimits;

    // ── Uniqueness scopes ─────────────────────────────────────────────────────

    private List<String> entityUniqueness;
    private List<String> listUniqueness;
    private List<String> relationUniqueness;
    private List<String> entityReactionUniqueness;
    private List<String> listReactionUniqueness;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public List<RecordLimit> getEntityLimits() { return entityLimits; }
    public void setEntityLimits(List<RecordLimit> entityLimits) { this.entityLimits = entityLimits; }

    public List<RecordLimit> getListLimits() { return listLimits; }
    public void setListLimits(List<RecordLimit> listLimits) { this.listLimits = listLimits; }

    public List<RecordLimit> getRelationLimits() { return relationLimits; }
    public void setRelationLimits(List<RecordLimit> relationLimits) { this.relationLimits = relationLimits; }

    public List<RecordLimit> getEntityReactionLimits() { return entityReactionLimits; }
    public void setEntityReactionLimits(List<RecordLimit> entityReactionLimits) { this.entityReactionLimits = entityReactionLimits; }

    public List<RecordLimit> getListReactionLimits() { return listReactionLimits; }
    public void setListReactionLimits(List<RecordLimit> listReactionLimits) { this.listReactionLimits = listReactionLimits; }

    public List<String> getEntityUniqueness() { return entityUniqueness; }
    public void setEntityUniqueness(List<String> entityUniqueness) { this.entityUniqueness = entityUniqueness; }

    public List<String> getListUniqueness() { return listUniqueness; }
    public void setListUniqueness(List<String> listUniqueness) { this.listUniqueness = listUniqueness; }

    public List<String> getRelationUniqueness() { return relationUniqueness; }
    public void setRelationUniqueness(List<String> relationUniqueness) { this.relationUniqueness = relationUniqueness; }

    public List<String> getEntityReactionUniqueness() { return entityReactionUniqueness; }
    public void setEntityReactionUniqueness(List<String> entityReactionUniqueness) { this.entityReactionUniqueness = entityReactionUniqueness; }

    public List<String> getListReactionUniqueness() { return listReactionUniqueness; }
    public void setListReactionUniqueness(List<String> listReactionUniqueness) { this.listReactionUniqueness = listReactionUniqueness; }

    // ── Nested types ──────────────────────────────────────────────────────────

    /**
     * A single record-limit rule, exactly matching EPS's {@code RecordLimit}
     * interface.
     *
     * <p>{@code enforceSerialisation} is a passthrough field: EPS preserves
     * it from its own configuration and includes it in {@code /system-info}.
     * EPC reads it to decide whether to acquire a lock for wide-scope/global
     * limit rules that have no interpolatable dimensions.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordLimit {

        private String scope;
        private int limit;
        private String duration;
        /**
         * When {@code true}, signals to EPC that a distributed lock must be
         * acquired even for wide-scope / global limit rules (those with no
         * interpolatable fields like {@code ${_ownerUsers}}).
         * <br>
         * Default is {@code false} (best-effort) when absent.
         */
        private Boolean enforceSerialisation;

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public Boolean getEnforceSerialisation() { return enforceSerialisation; }
        public void setEnforceSerialisation(Boolean enforceSerialisation) { this.enforceSerialisation = enforceSerialisation; }

        public boolean isEnforceSerialisationEnabled() {
            return Boolean.TRUE.equals(enforceSerialisation);
        }
    }
}
