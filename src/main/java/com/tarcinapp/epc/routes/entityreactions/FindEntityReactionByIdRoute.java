package com.tarcinapp.epc.routes.entityreactions;

import com.tarcinapp.epc.processors.entityreactions.FindEntityReactionByIdPreProcessor;
import com.tarcinapp.epc.processors.entityreactions.FindEntityReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findEntityReactionById.
 * Execution flow: FindEntityReactionByIdPreProcessor → backend proxy → FindEntityReactionByIdPostProcessor
 */
@ApplicationScoped
public class FindEntityReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindEntityReactionByIdPreProcessor preProcessor;

    @Inject
    FindEntityReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}?httpMethodRestrict=GET")
            .routeId("findEntityReactionById")
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
