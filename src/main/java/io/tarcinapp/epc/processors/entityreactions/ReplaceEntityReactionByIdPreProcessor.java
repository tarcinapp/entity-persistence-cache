package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PUT /entityreactions/{id} (replaceEntityReactionById).
 * Acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceEntityReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
