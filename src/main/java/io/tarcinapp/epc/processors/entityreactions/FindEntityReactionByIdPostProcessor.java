package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /entity-reactions/{id} (findEntityReactionById) — Phase 2.
 * Populates the findById Redis cache from the EPS response on a cache miss.
 */
@ApplicationScoped
public class FindEntityReactionByIdPostProcessor extends AbstractFindByIdCachePostProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
