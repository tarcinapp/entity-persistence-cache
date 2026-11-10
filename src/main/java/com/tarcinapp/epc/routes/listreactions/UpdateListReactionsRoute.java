package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.UpdateListReactionsPreProcessor;
import com.tarcinapp.epc.processors.listreactions.UpdateListReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateListReactions.
 * Execution flow: UpdateListReactionsPreProcessor → backend proxy → UpdateListReactionsPostProcessor
 */
@ApplicationScoped
public class UpdateListReactionsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateListReactionsPreProcessor preProcessor;

    @Inject
    UpdateListReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions?httpMethodRestrict=PATCH")
            .routeId("updateListReactions")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
