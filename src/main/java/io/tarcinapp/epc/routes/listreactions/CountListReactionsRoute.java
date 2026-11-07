package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.CountListReactionsPreProcessor;
import io.tarcinapp.epc.processors.listreactions.CountListReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countListReactions.
 * Execution flow: CountListReactionsPreProcessor → backend proxy → CountListReactionsPostProcessor
 */
@ApplicationScoped
public class CountListReactionsRoute extends RouteBuilder {

    @Inject
    CountListReactionsPreProcessor preProcessor;

    @Inject
    CountListReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/count?httpMethodRestrict=GET")
            .routeId("countListReactions")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
