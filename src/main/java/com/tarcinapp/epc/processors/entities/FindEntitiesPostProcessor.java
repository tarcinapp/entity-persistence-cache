package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /entities (findEntities).
 * Phase 3: populates the Redis visitor collection cache on a cache miss.
 */
@ApplicationScoped
public class FindEntitiesPostProcessor extends AbstractVisitorCollectionCachePostProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
