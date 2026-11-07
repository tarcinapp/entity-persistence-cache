package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PUT /entities/{id} (replaceEntityById).
 * Acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceEntityByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
