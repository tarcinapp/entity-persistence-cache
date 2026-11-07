package io.tarcinapp.epc.processors.lists;

import io.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for POST /lists (createList).
 * Acquires distributed scope locks keyed on the interpolated uniqueness and
 * record-limit dimensions from the request payload before forwarding to EPS.
 */
@ApplicationScoped
public class CreateListPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "list"; }
}
