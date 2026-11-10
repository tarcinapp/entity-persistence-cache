package com.tarcinapp.epc.routes.entityreactions;

import com.tarcinapp.epc.processors.entityreactions.CountEntityReactionsPreProcessor;
import com.tarcinapp.epc.processors.entityreactions.CountEntityReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countEntityReactions.
 * Execution flow: CountEntityReactionsPreProcessor → backend proxy → CountEntityReactionsPostProcessor
 */
@ApplicationScoped
public class CountEntityReactionsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CountEntityReactionsPreProcessor preProcessor;

    @Inject
    CountEntityReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/count?httpMethodRestrict=GET")
            .routeId("countEntityReactions")
            .startupOrder(4)
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
