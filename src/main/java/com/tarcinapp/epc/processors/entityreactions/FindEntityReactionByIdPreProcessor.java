package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.cache.AbstractFindByIdCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /entity-reactions/{id} (findEntityReactionById) — Phase 2.
 * Checks the findById Redis cache; short-circuits the route on a hit.
 */
@ApplicationScoped
public class FindEntityReactionByIdPreProcessor extends AbstractFindByIdCachePreProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
