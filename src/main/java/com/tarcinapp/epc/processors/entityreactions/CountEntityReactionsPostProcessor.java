package com.tarcinapp.epc.processors.entityreactions;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pass-through stub for CountEntityReactions.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class CountEntityReactionsPostProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // No pre/post processing required for Phase 1 — transparent pass-through.
    }
}
