package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /relations/{id} (findRelationById) — Phase 2.
 * Checks the findById Redis cache; short-circuits the route on a hit.
 */
@ApplicationScoped
public class FindRelationByIdPreProcessor extends AbstractFindByIdCachePreProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
