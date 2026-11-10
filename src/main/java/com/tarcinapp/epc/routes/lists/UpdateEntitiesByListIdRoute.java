package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.UpdateEntitiesByListIdPreProcessor;
import com.tarcinapp.epc.processors.lists.UpdateEntitiesByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntitiesByListId.
 * Execution flow: UpdateEntitiesByListIdPreProcessor → backend proxy → UpdateEntitiesByListIdPostProcessor
 */
@ApplicationScoped
public class UpdateEntitiesByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateEntitiesByListIdPreProcessor preProcessor;

    @Inject
    UpdateEntitiesByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/entities?httpMethodRestrict=PATCH")
            .routeId("updateEntitiesByListId")
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
