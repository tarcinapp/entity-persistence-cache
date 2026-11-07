package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.FindListsPreProcessor;
import io.tarcinapp.epc.processors.lists.FindListsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findLists.
 * Execution flow: FindListsPreProcessor → backend proxy → FindListsPostProcessor
 */
@ApplicationScoped
public class FindListsRoute extends RouteBuilder {

    @Inject
    FindListsPreProcessor preProcessor;

    @Inject
    FindListsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists?httpMethodRestrict=GET")
            .routeId("findLists")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
