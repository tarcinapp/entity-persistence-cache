package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.UpdateEntityReactionsPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.UpdateEntityReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntityReactions.
 * Execution flow: UpdateEntityReactionsPreProcessor → backend proxy → UpdateEntityReactionsPostProcessor
 */
@ApplicationScoped
public class UpdateEntityReactionsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateEntityReactionsPreProcessor preProcessor;

    @Inject
    UpdateEntityReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions?httpMethodRestrict=PATCH")
            .routeId("updateEntityReactions")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
