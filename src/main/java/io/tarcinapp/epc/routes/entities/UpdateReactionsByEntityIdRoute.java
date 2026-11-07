package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.UpdateReactionsByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.UpdateReactionsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateReactionsByEntityId.
 * Execution flow: UpdateReactionsByEntityIdPreProcessor → backend proxy → UpdateReactionsByEntityIdPostProcessor
 */
@ApplicationScoped
public class UpdateReactionsByEntityIdRoute extends RouteBuilder {

    @Inject
    UpdateReactionsByEntityIdPreProcessor preProcessor;

    @Inject
    UpdateReactionsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/reactions?httpMethodRestrict=PATCH")
            .routeId("updateReactionsByEntityId")
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
