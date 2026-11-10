package com.tarcinapp.epc.routes.relations;

import com.tarcinapp.epc.processors.relations.CountRelationsPreProcessor;
import com.tarcinapp.epc.processors.relations.CountRelationsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countRelations.
 * Execution flow: CountRelationsPreProcessor → backend proxy → CountRelationsPostProcessor
 */
@ApplicationScoped
public class CountRelationsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CountRelationsPreProcessor preProcessor;

    @Inject
    CountRelationsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations/count?httpMethodRestrict=GET")
            .routeId("countRelations")
            .startupOrder(3)
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/relations/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
