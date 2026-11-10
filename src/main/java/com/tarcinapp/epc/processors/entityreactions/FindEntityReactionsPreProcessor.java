package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /entity-reactions (findEntityReactions).
 * Phase 3: serves visitor collection queries from the Redis visitor cache.
 */
@ApplicationScoped
public class FindEntityReactionsPreProcessor extends AbstractVisitorCollectionCachePreProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
