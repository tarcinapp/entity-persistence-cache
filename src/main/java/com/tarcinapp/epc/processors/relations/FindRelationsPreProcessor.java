package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /relations (findRelations).
 * Phase 3: serves visitor collection queries from the Redis visitor cache.
 */
@ApplicationScoped
public class FindRelationsPreProcessor extends AbstractVisitorCollectionCachePreProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
