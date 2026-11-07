package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PATCH /lists/{id} (updateListById).
 * Acquires scope locks from the patch payload before forwarding to EPS.
 */
@ApplicationScoped
public class UpdateListByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "list"; }
}
