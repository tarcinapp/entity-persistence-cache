package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.DeleteEntitiesByListIdPreProcessor;
import com.tarcinapp.epc.processors.lists.DeleteEntitiesByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteEntitiesByListId.
 * Execution flow: DeleteEntitiesByListIdPreProcessor → backend proxy → DeleteEntitiesByListIdPostProcessor
 */
@ApplicationScoped
public class DeleteEntitiesByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteEntitiesByListIdPreProcessor preProcessor;

    @Inject
    DeleteEntitiesByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/entities?httpMethodRestrict=DELETE")
            .routeId("deleteEntitiesByListId")
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
