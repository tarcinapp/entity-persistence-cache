package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.DeleteListReactionByIdPreProcessor;
import com.tarcinapp.epc.processors.listreactions.DeleteListReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteListReactionById.
 * Execution flow: DeleteListReactionByIdPreProcessor → backend proxy → DeleteListReactionByIdPostProcessor
 */
@ApplicationScoped
public class DeleteListReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteListReactionByIdPreProcessor preProcessor;

    @Inject
    DeleteListReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}?httpMethodRestrict=DELETE")
            .routeId("deleteListReactionById")
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
