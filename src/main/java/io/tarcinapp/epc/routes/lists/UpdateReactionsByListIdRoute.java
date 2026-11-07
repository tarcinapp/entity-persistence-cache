package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.UpdateReactionsByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.UpdateReactionsByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateReactionsByListId.
 * Execution flow: UpdateReactionsByListIdPreProcessor → backend proxy → UpdateReactionsByListIdPostProcessor
 */
@ApplicationScoped
public class UpdateReactionsByListIdRoute extends RouteBuilder {

    @Inject
    UpdateReactionsByListIdPreProcessor preProcessor;

    @Inject
    UpdateReactionsByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/reactions?httpMethodRestrict=PATCH")
            .routeId("updateReactionsByListId")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists/${header.id}/reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
