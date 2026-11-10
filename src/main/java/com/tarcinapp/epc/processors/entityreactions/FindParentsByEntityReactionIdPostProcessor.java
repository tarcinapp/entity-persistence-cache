package com.tarcinapp.epc.processors.entityreactions;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pass-through stub for FindParentsByEntityReactionId.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class FindParentsByEntityReactionIdPostProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // No pre/post processing required for Phase 1 — transparent pass-through.
    }
}
