package com.tarcinapp.epc.processors.listreactions;

import com.tarcinapp.epc.cache.CacheEvictionHelper;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PUT /list-reactions/{id} (replaceListReactionById).
 * Phase 2: evicts the findById cache entry before lock acquisition.
 * Phase 1: acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceListReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "list-reaction"; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
