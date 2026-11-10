package com.tarcinapp.epc.routes.entities;

import com.tarcinapp.epc.processors.entities.UpdateEntityByIdPreProcessor;
import com.tarcinapp.epc.processors.entities.UpdateEntityByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntityById.
 * Execution flow: UpdateEntityByIdPreProcessor → backend proxy → UpdateEntityByIdPostProcessor
 */
@ApplicationScoped
public class UpdateEntityByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateEntityByIdPreProcessor preProcessor;

    @Inject
    UpdateEntityByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}?httpMethodRestrict=PATCH")
            .routeId("updateEntityById")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
