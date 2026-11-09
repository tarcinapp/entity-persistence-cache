package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /lists/{id} (findListById) — Phase 2.
 * Checks the findById Redis cache; short-circuits the route on a hit.
 */
@ApplicationScoped
public class FindListByIdPreProcessor extends AbstractFindByIdCachePreProcessor {
    @Override
    protected String recordType() { return "list"; }
}
