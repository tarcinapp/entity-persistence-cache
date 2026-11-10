package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /entities (findEntities).
 * Phase 3: serves visitor collection queries from the Redis visitor cache
 * ({@code set[actives]=true&set[publics]=true}) and short-circuits on a hit.
 */
@ApplicationScoped
public class FindEntitiesPreProcessor extends AbstractVisitorCollectionCachePreProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
