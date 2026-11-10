package com.tarcinapp.epc.processors.entityreactions;

import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for POST /entityreactions (createEntityReaction).
 * Acquires distributed scope locks keyed on the interpolated uniqueness and
 * record-limit dimensions from the request payload before forwarding to EPS.
 */
@ApplicationScoped
public class CreateEntityReactionPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "entity-reaction"; }
}
