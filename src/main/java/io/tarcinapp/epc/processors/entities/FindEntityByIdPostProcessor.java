package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /entities/{id} (findEntityById) — Phase 2.
 * Populates the findById Redis cache from the EPS response on a cache miss.
 */
@ApplicationScoped
public class FindEntityByIdPostProcessor extends AbstractFindByIdCachePostProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
