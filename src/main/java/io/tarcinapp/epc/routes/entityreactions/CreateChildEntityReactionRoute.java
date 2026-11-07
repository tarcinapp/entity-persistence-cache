package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.CreateChildEntityReactionPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.CreateChildEntityReactionPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createChildEntityReaction.
 * Execution flow: CreateChildEntityReactionPreProcessor → backend proxy → CreateChildEntityReactionPostProcessor
 */
@ApplicationScoped
public class CreateChildEntityReactionRoute extends RouteBuilder {

    @Inject
    CreateChildEntityReactionPreProcessor preProcessor;

    @Inject
    CreateChildEntityReactionPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}/children?httpMethodRestrict=POST")
            .routeId("createChildEntityReaction")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
