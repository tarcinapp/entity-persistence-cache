package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.cache.CacheEvictionHelper;
import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PATCH /relations/{id} (updateRelationById).
 * Phase 2: evicts the findById cache entry before lock acquisition.
 * Phase 1: acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateRelationByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "relation"; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
