package com.tarcinapp.epc.routes.relations;

import com.tarcinapp.epc.processors.relations.ReplaceRelationByIdPreProcessor;
import com.tarcinapp.epc.processors.relations.ReplaceRelationByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceRelationById.
 * Execution flow: ReplaceRelationByIdPreProcessor → backend proxy → ReplaceRelationByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceRelationByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    ReplaceRelationByIdPreProcessor preProcessor;

    @Inject
    ReplaceRelationByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations/{id}?httpMethodRestrict=PUT")
            .routeId("replaceRelationById")
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
