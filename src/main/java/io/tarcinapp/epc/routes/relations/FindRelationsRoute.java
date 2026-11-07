package io.tarcinapp.epc.routes.relations;

import io.tarcinapp.epc.processors.relations.FindRelationsPreProcessor;
import io.tarcinapp.epc.processors.relations.FindRelationsPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findRelations.
 * Execution flow: FindRelationsPreProcessor → backend proxy → FindRelationsPostProcessor
 */
@ApplicationScoped
public class FindRelationsRoute extends RouteBuilder {

    @Inject
    FindRelationsPreProcessor preProcessor;

    @Inject
    FindRelationsPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations?httpMethodRestrict=GET")
            .routeId("findRelations")
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
