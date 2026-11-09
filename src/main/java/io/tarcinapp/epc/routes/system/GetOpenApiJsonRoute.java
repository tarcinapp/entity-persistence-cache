package io.tarcinapp.epc.routes.system;

import io.tarcinapp.epc.processors.system.GetOpenApiJsonPreProcessor;
import io.tarcinapp.epc.processors.system.GetOpenApiJsonPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for getOpenApiJson.
 * Execution flow: GetOpenApiJsonPreProcessor → backend proxy → GetOpenApiJsonPostProcessor
 */
@ApplicationScoped
public class GetOpenApiJsonRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    GetOpenApiJsonPreProcessor preProcessor;

    @Inject
    GetOpenApiJsonPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/openapi.json?httpMethodRestrict=GET")
            .routeId("getOpenApiJson")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/openapi.json?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
