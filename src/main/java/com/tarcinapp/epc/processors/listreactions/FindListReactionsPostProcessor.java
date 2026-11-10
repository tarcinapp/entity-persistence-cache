package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /list-reactions (findListReactions).
 * Phase 3: populates the Redis visitor collection cache on a cache miss.
 */
@ApplicationScoped
public class FindListReactionsPostProcessor extends AbstractVisitorCollectionCachePostProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
