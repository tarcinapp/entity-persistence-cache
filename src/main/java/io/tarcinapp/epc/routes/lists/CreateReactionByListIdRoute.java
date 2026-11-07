package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.CreateReactionByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.CreateReactionByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createReactionByListId.
 * Execution flow: CreateReactionByListIdPreProcessor → backend proxy → CreateReactionByListIdPostProcessor
 */
@ApplicationScoped
public class CreateReactionByListIdRoute extends RouteBuilder {

    @Inject
    CreateReactionByListIdPreProcessor preProcessor;

    @Inject
    CreateReactionByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/reactions?httpMethodRestrict=POST")
            .routeId("createReactionByListId")
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
