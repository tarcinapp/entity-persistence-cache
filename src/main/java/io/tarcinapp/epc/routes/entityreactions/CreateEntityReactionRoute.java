package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.CreateEntityReactionPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.CreateEntityReactionPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createEntityReaction.
 * Execution flow: CreateEntityReactionPreProcessor → backend proxy → CreateEntityReactionPostProcessor
 */
@ApplicationScoped
public class CreateEntityReactionRoute extends RouteBuilder {

    @Inject
    CreateEntityReactionPreProcessor preProcessor;

    @Inject
    CreateEntityReactionPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions?httpMethodRestrict=POST")
            .routeId("createEntityReaction")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
