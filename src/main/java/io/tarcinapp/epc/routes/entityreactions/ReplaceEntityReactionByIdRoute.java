package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.ReplaceEntityReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.ReplaceEntityReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceEntityReactionById.
 * Execution flow: ReplaceEntityReactionByIdPreProcessor → backend proxy → ReplaceEntityReactionByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceEntityReactionByIdRoute extends RouteBuilder {

    @Inject
    ReplaceEntityReactionByIdPreProcessor preProcessor;

    @Inject
    ReplaceEntityReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}?httpMethodRestrict=PUT")
            .routeId("replaceEntityReactionById")
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
