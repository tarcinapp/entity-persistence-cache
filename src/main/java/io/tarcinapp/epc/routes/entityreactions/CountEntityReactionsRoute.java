package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.CountEntityReactionsPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.CountEntityReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countEntityReactions.
 * Execution flow: CountEntityReactionsPreProcessor → backend proxy → CountEntityReactionsPostProcessor
 */
@ApplicationScoped
public class CountEntityReactionsRoute extends RouteBuilder {

    @Inject
    CountEntityReactionsPreProcessor preProcessor;

    @Inject
    CountEntityReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/count?httpMethodRestrict=GET")
            .routeId("countEntityReactions")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
