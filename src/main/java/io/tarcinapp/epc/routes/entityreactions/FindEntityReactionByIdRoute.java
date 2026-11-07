package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.FindEntityReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.FindEntityReactionByIdPostProcessor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findEntityReactionById.
 * Execution flow: FindEntityReactionByIdPreProcessor → backend proxy → FindEntityReactionByIdPostProcessor
 */
@Priority(20)
@ApplicationScoped
public class FindEntityReactionByIdRoute extends RouteBuilder {

    @Inject
    FindEntityReactionByIdPreProcessor preProcessor;

    @Inject
    FindEntityReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}?httpMethodRestrict=GET")
            .routeId("findEntityReactionById")
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
