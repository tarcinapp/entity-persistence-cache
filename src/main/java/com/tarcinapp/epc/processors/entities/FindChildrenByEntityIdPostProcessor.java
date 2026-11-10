package com.tarcinapp.epc.processors.entities;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pass-through stub for FindChildrenByEntityId.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class FindChildrenByEntityIdPostProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // No pre/post processing required for Phase 1 — transparent pass-through.
    }
}
