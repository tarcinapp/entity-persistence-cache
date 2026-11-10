package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.cache.CacheEvictionHelper;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for DELETE /entity-reactions/{id} (deleteEntityReactionById).
 * Phase 2: evicts the findById cache entry.
 * No lock acquisition — DELETE carries no payload with interpolatable key dimensions.
 */
@ApplicationScoped
public class DeleteEntityReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "entity-reaction"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
