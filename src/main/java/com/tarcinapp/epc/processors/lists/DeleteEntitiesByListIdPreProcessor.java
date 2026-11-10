package com.tarcinapp.epc.processors.lists;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pass-through stub for DeleteEntitiesByListId.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class DeleteEntitiesByListIdPreProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // No pre/post processing required for Phase 1 — transparent pass-through.
    }
}
