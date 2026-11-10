package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.ReplaceListByIdPreProcessor;
import com.tarcinapp.epc.processors.lists.ReplaceListByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceListById.
 * Execution flow: ReplaceListByIdPreProcessor → backend proxy → ReplaceListByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceListByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    ReplaceListByIdPreProcessor preProcessor;

    @Inject
    ReplaceListByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}?httpMethodRestrict=PUT")
            .routeId("replaceListById")
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
