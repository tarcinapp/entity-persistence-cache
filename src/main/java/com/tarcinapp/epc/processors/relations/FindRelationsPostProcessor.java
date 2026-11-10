package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /relations (findRelations).
 * Phase 3: populates the Redis visitor collection cache on a cache miss.
 */
@ApplicationScoped
public class FindRelationsPostProcessor extends AbstractVisitorCollectionCachePostProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
