package com.tarcinapp.epc.processors.entities;

import com.tarcinapp.epc.cache.CacheEvictionHelper;
import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for DELETE /entities/{id} (deleteEntityById).
 * Phase 2: evicts the findById cache entry.
 * No lock acquisition — DELETE carries no payload and therefore no interpolatable
 * lock key dimensions. EPS's own delete is atomic; no TOCTOU race exists on delete.
 */
@ApplicationScoped
public class DeleteEntityByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "entity"; }

    @Override
    protected boolean shouldAcquireLocks() { return false; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
