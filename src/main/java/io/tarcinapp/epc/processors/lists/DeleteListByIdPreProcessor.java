package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.cache.CacheEvictionHelper;
import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for DELETE /lists/{id} (deleteListById).
 * Phase 2: evicts the findById cache entry.
 * No lock acquisition — DELETE carries no payload with interpolatable key dimensions.
 */
@ApplicationScoped
public class DeleteListByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "list"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
