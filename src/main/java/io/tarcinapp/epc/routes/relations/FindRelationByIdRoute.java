package io.tarcinapp.epc.routes.relations;

import io.tarcinapp.epc.processors.relations.FindRelationByIdPreProcessor;
import io.tarcinapp.epc.processors.relations.FindRelationByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findRelationById.
 * Execution flow: FindRelationByIdPreProcessor → backend proxy → FindRelationByIdPostProcessor
 */
@ApplicationScoped
public class FindRelationByIdRoute extends RouteBuilder {

    @Inject
    FindRelationByIdPreProcessor preProcessor;

    @Inject
    FindRelationByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations/{id}?httpMethodRestrict=GET")
            .routeId("findRelationById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/relations/${header.id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
