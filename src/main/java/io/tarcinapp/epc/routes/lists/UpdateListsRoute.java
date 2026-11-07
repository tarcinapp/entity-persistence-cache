package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.UpdateListsPreProcessor;
import io.tarcinapp.epc.processors.lists.UpdateListsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateLists.
 * Execution flow: UpdateListsPreProcessor → backend proxy → UpdateListsPostProcessor
 */
@ApplicationScoped
public class UpdateListsRoute extends RouteBuilder {

    @Inject
    UpdateListsPreProcessor preProcessor;

    @Inject
    UpdateListsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists?httpMethodRestrict=PATCH")
            .routeId("updateLists")
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
