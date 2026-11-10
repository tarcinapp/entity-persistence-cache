package com.tarcinapp.epc.routes.entities;

import com.tarcinapp.epc.processors.entities.CountEntitiesPreProcessor;
import com.tarcinapp.epc.processors.entities.CountEntitiesPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countEntities.
 * Execution flow: CountEntitiesPreProcessor → backend proxy → CountEntitiesPostProcessor
 */
@ApplicationScoped
public class CountEntitiesRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CountEntitiesPreProcessor preProcessor;

    @Inject
    CountEntitiesPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/count?httpMethodRestrict=GET")
            .routeId("countEntities")
            .startupOrder(1)
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
