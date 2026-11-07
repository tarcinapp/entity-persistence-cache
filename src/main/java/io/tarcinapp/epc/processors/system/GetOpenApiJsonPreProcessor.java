package io.tarcinapp.epc.processors.system;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pass-through stub for GetOpenApiJson.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class GetOpenApiJsonPreProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // No pre/post processing required for Phase 1 — transparent pass-through.
    }
}
