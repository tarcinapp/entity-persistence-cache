package io.tarcinapp.epc.processors.listreactions;

import io.tarcinapp.epc.cache.AbstractFindByIdCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /list-reactions/{id} (findListReactionById) — Phase 2.
 * Populates the findById Redis cache from the EPS response on a cache miss.
 */
@ApplicationScoped
public class FindListReactionByIdPostProcessor extends AbstractFindByIdCachePostProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
