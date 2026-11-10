package com.tarcinapp.epc.routes.relations;

import com.tarcinapp.epc.processors.relations.UpdateRelationByIdPreProcessor;
import com.tarcinapp.epc.processors.relations.UpdateRelationByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateRelationById.
 * Execution flow: UpdateRelationByIdPreProcessor → backend proxy → UpdateRelationByIdPostProcessor
 */
@ApplicationScoped
public class UpdateRelationByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateRelationByIdPreProcessor preProcessor;

    @Inject
    UpdateRelationByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations/{id}?httpMethodRestrict=PATCH")
            .routeId("updateRelationById")
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
