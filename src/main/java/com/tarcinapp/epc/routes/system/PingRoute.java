package com.tarcinapp.epc.routes.system;

import com.tarcinapp.epc.processors.system.PingPreProcessor;
import com.tarcinapp.epc.processors.system.PingPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for ping.
 * Execution flow: PingPreProcessor → backend proxy → PingPostProcessor
 */
@ApplicationScoped
public class PingRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    PingPreProcessor preProcessor;

    @Inject
    PingPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/ping?httpMethodRestrict=GET")
            .routeId("ping")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/ping?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
