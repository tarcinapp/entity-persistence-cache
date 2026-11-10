package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.ReplaceListReactionByIdPreProcessor;
import com.tarcinapp.epc.processors.listreactions.ReplaceListReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceListReactionById.
 * Execution flow: ReplaceListReactionByIdPreProcessor → backend proxy → ReplaceListReactionByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceListReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    ReplaceListReactionByIdPreProcessor preProcessor;

    @Inject
    ReplaceListReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}?httpMethodRestrict=PUT")
            .routeId("replaceListReactionById")
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
