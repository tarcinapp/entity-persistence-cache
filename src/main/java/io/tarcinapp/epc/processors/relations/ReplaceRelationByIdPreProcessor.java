package io.tarcinapp.epc.processors.relations;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PUT /relations/{id} (replaceRelationById).
 * Acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceRelationByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
