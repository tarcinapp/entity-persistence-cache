package com.tarcinapp.epc.routes.entities;

import com.tarcinapp.epc.processors.entities.FindEntitiesPreProcessor;
import com.tarcinapp.epc.processors.entities.FindEntitiesPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findEntities.
 * Execution flow: FindEntitiesPreProcessor → backend proxy → FindEntitiesPostProcessor
 */
@ApplicationScoped
public class FindEntitiesRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindEntitiesPreProcessor preProcessor;

    @Inject
    FindEntitiesPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities?httpMethodRestrict=GET")
            .routeId("findEntities")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
