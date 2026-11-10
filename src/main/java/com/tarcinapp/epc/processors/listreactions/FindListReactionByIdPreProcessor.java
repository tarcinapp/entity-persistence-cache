package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.AbstractFindByIdCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /list-reactions/{id} (findListReactionById) — Phase 2.
 * Checks the findById Redis cache; short-circuits the route on a hit.
 */
@ApplicationScoped
public class FindListReactionByIdPreProcessor extends AbstractFindByIdCachePreProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
