package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.cache.VisitorCollectionCacheService;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PATCH /entities (updateEntities — updateAll by filter).
 * Phase 3: flushes the visitor collection cache. The affected _ids are
 * unknowable without a pre-fetch, so a full record-type flush is the correct
 * strategy here.
 * No per-record lock acquisition — updateAll is not currently scoped by EPC.
 */
@ApplicationScoped
public class UpdateEntitiesPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    VisitorCollectionCacheService visitorCacheService;

    @Override
    protected String recordType() { return "entity"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        visitorCacheService.flush(resolveAppShortcode(exchange), recordType());
    }
}
