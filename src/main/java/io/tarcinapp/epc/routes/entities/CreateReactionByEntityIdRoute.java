package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.CreateReactionByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.CreateReactionByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createReactionByEntityId.
 * Execution flow: CreateReactionByEntityIdPreProcessor → backend proxy → CreateReactionByEntityIdPostProcessor
 */
@ApplicationScoped
public class CreateReactionByEntityIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateReactionByEntityIdPreProcessor preProcessor;

    @Inject
    CreateReactionByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/reactions?httpMethodRestrict=POST")
            .routeId("createReactionByEntityId")
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
