package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.CountListReactionsPreProcessor;
import com.tarcinapp.epc.processors.listreactions.CountListReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countListReactions.
 * Execution flow: CountListReactionsPreProcessor → backend proxy → CountListReactionsPostProcessor
 */
@ApplicationScoped
public class CountListReactionsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CountListReactionsPreProcessor preProcessor;

    @Inject
    CountListReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/count?httpMethodRestrict=GET")
            .routeId("countListReactions")
            .startupOrder(5)
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
