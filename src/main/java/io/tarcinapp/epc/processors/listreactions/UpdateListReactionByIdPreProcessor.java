package io.tarcinapp.epc.processors.listreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PATCH /listreactions/{id} (updateListReactionById).
 * Acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateListReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
