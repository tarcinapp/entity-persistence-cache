package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.CreateEntityByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.CreateEntityByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createEntityByListId.
 * Execution flow: CreateEntityByListIdPreProcessor → backend proxy → CreateEntityByListIdPostProcessor
 */
@ApplicationScoped
public class CreateEntityByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateEntityByListIdPreProcessor preProcessor;

    @Inject
    CreateEntityByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/entities?httpMethodRestrict=POST")
            .routeId("createEntityByListId")
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
