package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.DeleteEntityReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.DeleteEntityReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteEntityReactionById.
 * Execution flow: DeleteEntityReactionByIdPreProcessor → backend proxy → DeleteEntityReactionByIdPostProcessor
 */
@ApplicationScoped
public class DeleteEntityReactionByIdRoute extends RouteBuilder {

    @Inject
    DeleteEntityReactionByIdPreProcessor preProcessor;

    @Inject
    DeleteEntityReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}?httpMethodRestrict=DELETE")
            .routeId("deleteEntityReactionById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions/${header.id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
