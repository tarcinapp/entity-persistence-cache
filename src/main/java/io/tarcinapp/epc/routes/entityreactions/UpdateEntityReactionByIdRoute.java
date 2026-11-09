package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.UpdateEntityReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.UpdateEntityReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntityReactionById.
 * Execution flow: UpdateEntityReactionByIdPreProcessor → backend proxy → UpdateEntityReactionByIdPostProcessor
 */
@ApplicationScoped
public class UpdateEntityReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateEntityReactionByIdPreProcessor preProcessor;

    @Inject
    UpdateEntityReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}?httpMethodRestrict=PATCH")
            .routeId("updateEntityReactionById")
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
