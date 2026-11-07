package io.tarcinapp.epc.processors.listreactions;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PUT /listreactions/{id} (replaceListReactionById).
 * Acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceListReactionByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "list-reaction"; }
}
