package io.tarcinapp.epc.processors.entities;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PATCH /entities/{id} (updateEntityById).
 * Acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateEntityByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "entity"; }
}
