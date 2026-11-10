package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.VisitorCollectionCacheService;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PATCH /list-reactions (updateListReactions — updateAll by filter).
 * Phase 3: flushes the visitor collection cache.
 * No per-record lock acquisition — updateAll is not currently scoped by EPC.
 */
@ApplicationScoped
public class UpdateListReactionsPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    VisitorCollectionCacheService visitorCacheService;

    @Override
    protected String recordType() { return "list-reaction"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        visitorCacheService.flush(resolveAppShortcode(exchange), recordType());
    }
}
