package io.tarcinapp.epc.processors.entityreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PATCH /entityreactions/{id} (updateEntityReactionById).
 * Acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateEntityReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
