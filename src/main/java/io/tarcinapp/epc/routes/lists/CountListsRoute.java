package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.CountListsPreProcessor;
import io.tarcinapp.epc.processors.lists.CountListsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countLists.
 * Execution flow: CountListsPreProcessor → backend proxy → CountListsPostProcessor
 */
@ApplicationScoped
public class CountListsRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CountListsPreProcessor preProcessor;

    @Inject
    CountListsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/count?httpMethodRestrict=GET")
            .routeId("countLists")
            .startupOrder(2)
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
