package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /relations/{id} (findRelationById) — Phase 2.
 * Populates the findById Redis cache from the EPS response on a cache miss.
 */
@ApplicationScoped
public class FindRelationByIdPostProcessor extends AbstractFindByIdCachePostProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
