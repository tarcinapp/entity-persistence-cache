package com.tarcinapp.epc.processors.relations;

import com.tarcinapp.epc.lock.AbstractWriteLockPreProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Pre-processor for POST /relations (createRelation).
 * Acquires distributed scope locks keyed on the interpolated uniqueness and
 * record-limit dimensions from the request payload before forwarding to EPS.
 */
@ApplicationScoped
public class CreateRelationPreProcessor extends AbstractWriteLockPreProcessor {
    @Override
    protected String recordType() { return "relation"; }
}
