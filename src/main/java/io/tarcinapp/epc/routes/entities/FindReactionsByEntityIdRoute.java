package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.FindReactionsByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.FindReactionsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findReactionsByEntityId.
 * Execution flow: FindReactionsByEntityIdPreProcessor → backend proxy → FindReactionsByEntityIdPostProcessor
 */
@ApplicationScoped
public class FindReactionsByEntityIdRoute extends RouteBuilder {

    @Inject
    FindReactionsByEntityIdPreProcessor preProcessor;

    @Inject
    FindReactionsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/reactions?httpMethodRestrict=GET")
            .routeId("findReactionsByEntityId")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities/${header.id}/reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
