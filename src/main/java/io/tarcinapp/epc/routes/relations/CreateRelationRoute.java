package io.tarcinapp.epc.routes.relations;

import io.tarcinapp.epc.processors.relations.CreateRelationPreProcessor;
import io.tarcinapp.epc.processors.relations.CreateRelationPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createRelation.
 * Execution flow: CreateRelationPreProcessor → backend proxy → CreateRelationPostProcessor
 */
@ApplicationScoped
public class CreateRelationRoute extends RouteBuilder {

    @Inject
    CreateRelationPreProcessor preProcessor;

    @Inject
    CreateRelationPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations?httpMethodRestrict=POST")
            .routeId("createRelation")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/relations?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
