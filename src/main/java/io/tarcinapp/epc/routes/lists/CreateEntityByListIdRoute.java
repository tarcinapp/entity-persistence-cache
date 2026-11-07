package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.CreateEntityByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.CreateEntityByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createEntityByListId.
 * Execution flow: CreateEntityByListIdPreProcessor → backend proxy → CreateEntityByListIdPostProcessor
 */
@ApplicationScoped
public class CreateEntityByListIdRoute extends RouteBuilder {

    @Inject
    CreateEntityByListIdPreProcessor preProcessor;

    @Inject
    CreateEntityByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/entities?httpMethodRestrict=POST")
            .routeId("createEntityByListId")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists/${header.id}/entities?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
