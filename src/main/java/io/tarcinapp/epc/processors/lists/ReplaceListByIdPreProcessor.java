package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for PUT /lists/{id} (replaceListById).
 * Acquires scope locks from the replacement payload before forwarding to EPS.
 */
@ApplicationScoped
public class ReplaceListByIdPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "list"; }
}
