package com.tarcinapp.epc.processors.lists;

import com.tarcinapp.epc.cache.VisitorCollectionCacheService;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for POST /lists (createList).
 * Phase 1: acquires distributed scope locks.
 * Phase 3: flushes the visitor collection cache.
 */
@ApplicationScoped
public class CreateListPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    VisitorCollectionCacheService visitorCacheService;

    @Override
    protected String recordType() { return "list"; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        visitorCacheService.flush(resolveAppShortcode(exchange), recordType());
    }
}
