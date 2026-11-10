package com.tarcinapp.epc.processors.lists;

import com.tarcinapp.epc.cache.AbstractVisitorCollectionCachePreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for GET /lists (findLists).
 * Phase 3: serves visitor collection queries from the Redis visitor cache.
 */
@ApplicationScoped
public class FindListsPreProcessor extends AbstractVisitorCollectionCachePreProcessor {
    @Override
    protected String recordType() { return "list"; }
}
