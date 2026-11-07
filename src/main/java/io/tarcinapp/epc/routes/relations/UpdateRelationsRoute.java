package io.tarcinapp.epc.routes.relations;

import io.tarcinapp.epc.processors.relations.UpdateRelationsPreProcessor;
import io.tarcinapp.epc.processors.relations.UpdateRelationsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateRelations.
 * Execution flow: UpdateRelationsPreProcessor → backend proxy → UpdateRelationsPostProcessor
 */
@ApplicationScoped
public class UpdateRelationsRoute extends RouteBuilder {

    @Inject
    UpdateRelationsPreProcessor preProcessor;

    @Inject
    UpdateRelationsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations?httpMethodRestrict=PATCH")
            .routeId("updateRelations")
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
