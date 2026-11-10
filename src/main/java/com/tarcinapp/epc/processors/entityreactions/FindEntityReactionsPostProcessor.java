package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /entity-reactions (findEntityReactions).
 * Phase 3: populates the Redis visitor collection cache on a cache miss.
 */
@ApplicationScoped
public class FindEntityReactionsPostProcessor extends AbstractVisitorCollectionCachePostProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
