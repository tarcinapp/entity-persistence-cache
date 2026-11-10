package com.tarcinapp.epc.processors.lists;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Post-processor for GET /lists (findLists).
 * Phase 3: populates the Redis visitor collection cache on a cache miss.
 */
@ApplicationScoped
public class FindListsPostProcessor extends AbstractVisitorCollectionCachePostProcessor {
    @Override
    protected String recordType() { return "list"; }
}
