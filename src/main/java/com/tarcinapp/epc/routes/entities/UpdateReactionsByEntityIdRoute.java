package com.tarcinapp.epc.routes.entities;

import com.tarcinapp.epc.processors.entities.UpdateReactionsByEntityIdPreProcessor;
import com.tarcinapp.epc.processors.entities.UpdateReactionsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateReactionsByEntityId.
 * Execution flow: UpdateReactionsByEntityIdPreProcessor → backend proxy → UpdateReactionsByEntityIdPostProcessor
 */
@ApplicationScoped
public class UpdateReactionsByEntityIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateReactionsByEntityIdPreProcessor preProcessor;

    @Inject
    UpdateReactionsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/reactions?httpMethodRestrict=PATCH")
            .routeId("updateReactionsByEntityId")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
