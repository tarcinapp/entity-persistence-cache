package com.tarcinapp.epc.routes.system;

import com.tarcinapp.epc.processors.system.GetSystemInfoPreProcessor;
import com.tarcinapp.epc.processors.system.GetSystemInfoPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for getSystemInfo.
 * Execution flow: GetSystemInfoPreProcessor → backend proxy → GetSystemInfoPostProcessor
 */
@ApplicationScoped
public class GetSystemInfoRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    GetSystemInfoPreProcessor preProcessor;

    @Inject
    GetSystemInfoPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/system-info?httpMethodRestrict=GET")
            .routeId("getSystemInfo")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/system-info?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
