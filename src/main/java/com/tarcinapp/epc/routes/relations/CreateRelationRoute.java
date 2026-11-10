package com.tarcinapp.epc.routes.relations;

import com.tarcinapp.epc.processors.relations.CreateRelationPreProcessor;
import com.tarcinapp.epc.processors.relations.CreateRelationPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createRelation.
 * Execution flow: CreateRelationPreProcessor → backend proxy → CreateRelationPostProcessor
 */
@ApplicationScoped
public class CreateRelationRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateRelationPreProcessor preProcessor;

    @Inject
    CreateRelationPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations?httpMethodRestrict=POST")
            .routeId("createRelation")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/relations?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
