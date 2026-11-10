package com.tarcinapp.epc.routes.entities;

import com.tarcinapp.epc.processors.entities.CreateChildEntityPreProcessor;
import com.tarcinapp.epc.processors.entities.CreateChildEntityPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createChildEntity.
 * Execution flow: CreateChildEntityPreProcessor → backend proxy → CreateChildEntityPostProcessor
 */
@ApplicationScoped
public class CreateChildEntityRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateChildEntityPreProcessor preProcessor;

    @Inject
    CreateChildEntityPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/children?httpMethodRestrict=POST")
            .routeId("createChildEntity")
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
