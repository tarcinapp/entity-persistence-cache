package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.FindListReactionsPreProcessor;
import io.tarcinapp.epc.processors.listreactions.FindListReactionsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findListReactions.
 * Execution flow: FindListReactionsPreProcessor → backend proxy → FindListReactionsPostProcessor
 */
@ApplicationScoped
public class FindListReactionsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindListReactionsPreProcessor preProcessor;

    @Inject
    FindListReactionsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions?httpMethodRestrict=GET")
            .routeId("findListReactions")
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
