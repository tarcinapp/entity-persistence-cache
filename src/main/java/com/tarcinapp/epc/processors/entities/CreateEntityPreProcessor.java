package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.cache.VisitorCollectionCacheService;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for POST /entities (createEntity).
 * Phase 1: acquires distributed scope locks.
 * Phase 3: flushes the visitor collection cache so newly created public+active
 * records appear immediately in visitor queries.
 */
@ApplicationScoped
public class CreateEntityPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    VisitorCollectionCacheService visitorCacheService;

    @Override
    protected String recordType() { return "entity"; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        visitorCacheService.flush(resolveAppShortcode(exchange), recordType());
    }
}
