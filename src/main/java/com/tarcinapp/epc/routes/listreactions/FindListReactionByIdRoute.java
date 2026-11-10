package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.FindListReactionByIdPreProcessor;
import com.tarcinapp.epc.processors.listreactions.FindListReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findListReactionById.
 * Execution flow: FindListReactionByIdPreProcessor → backend proxy → FindListReactionByIdPostProcessor
 */
@ApplicationScoped
public class FindListReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindListReactionByIdPreProcessor preProcessor;

    @Inject
    FindListReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}?httpMethodRestrict=GET")
            .routeId("findListReactionById")
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
