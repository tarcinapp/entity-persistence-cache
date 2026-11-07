package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.UpdateListReactionsPreProcessor;
import io.tarcinapp.epc.processors.listreactions.UpdateListReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateListReactions.
 * Execution flow: UpdateListReactionsPreProcessor → backend proxy → UpdateListReactionsPostProcessor
 */
@ApplicationScoped
public class UpdateListReactionsRoute extends RouteBuilder {

    @Inject
    UpdateListReactionsPreProcessor preProcessor;

    @Inject
    UpdateListReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions?httpMethodRestrict=PATCH")
            .routeId("updateListReactions")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
