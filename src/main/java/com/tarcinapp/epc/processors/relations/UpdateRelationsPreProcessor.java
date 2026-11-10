package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.cache.VisitorCollectionCacheService;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PATCH /relations (updateRelations — updateAll by filter).
 * Phase 3: flushes the visitor collection cache.
 * No per-record lock acquisition — updateAll is not currently scoped by EPC.
 */
@ApplicationScoped
public class UpdateRelationsPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    VisitorCollectionCacheService visitorCacheService;

    @Override
    protected String recordType() { return "relation"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        visitorCacheService.flush(resolveAppShortcode(exchange), recordType());
    }
}
