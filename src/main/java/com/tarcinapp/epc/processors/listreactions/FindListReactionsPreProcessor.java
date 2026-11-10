package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /list-reactions (findListReactions).
 * Phase 3: serves visitor collection queries from the Redis visitor cache.
 */
@ApplicationScoped
public class FindListReactionsPreProcessor extends AbstractVisitorCollectionCachePreProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
