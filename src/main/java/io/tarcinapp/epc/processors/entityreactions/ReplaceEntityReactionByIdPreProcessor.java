package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.cache.CacheEvictionHelper;
import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;

/**
 * Pre-processor for PUT /entity-reactions/{id} (replaceEntityReactionById).
 * Phase 2: evicts the findById cache entry before lock acquisition.
 * Phase 1: acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceEntityReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {

    @Inject
    CacheEvictionHelper evictionHelper;

    @Override
    protected String recordType() { return "entity-reaction"; }

    @Override
    protected void beforeLocks(Exchange exchange) {
        evictionHelper.evictById(exchange, recordType());
    }
}
