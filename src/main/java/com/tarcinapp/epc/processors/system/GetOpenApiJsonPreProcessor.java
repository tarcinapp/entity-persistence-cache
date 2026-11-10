package com.tarcinapp.epc.processors.system;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;

/**
 * Pass-through stub for GetOpenApiJson.
 * Extend with custom logic as needed.
 */
@ApplicationScoped
public class GetOpenApiJsonPreProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(GetOpenApiJsonPreProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        LOG.debugf("openapi.json requested; requestId=%s",
                exchange.getMessage().getHeader("X-Request-Id", String.class));
    }
}
