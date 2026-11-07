package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PATCH /relations/{id} (updateRelationById).
 * Acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateRelationByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
