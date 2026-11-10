package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.UpdateListByIdPreProcessor;
import com.tarcinapp.epc.processors.lists.UpdateListByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateListById.
 * Execution flow: UpdateListByIdPreProcessor → backend proxy → UpdateListByIdPostProcessor
 */
@ApplicationScoped
public class UpdateListByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateListByIdPreProcessor preProcessor;

    @Inject
    UpdateListByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}?httpMethodRestrict=PATCH")
            .routeId("updateListById")
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
