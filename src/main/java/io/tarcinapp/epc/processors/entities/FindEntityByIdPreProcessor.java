package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /entities/{id} (findEntityById) — Phase 2.
 * Checks the findById Redis cache; short-circuits the route on a hit.
 */
@ApplicationScoped
public class FindEntityByIdPreProcessor extends AbstractFindByIdCachePreProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
