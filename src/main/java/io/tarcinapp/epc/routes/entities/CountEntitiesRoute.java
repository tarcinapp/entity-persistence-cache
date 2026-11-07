package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.CountEntitiesPreProcessor;
import io.tarcinapp.epc.processors.entities.CountEntitiesPostProcessor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for countEntities.
 * Execution flow: CountEntitiesPreProcessor → backend proxy → CountEntitiesPostProcessor
 */
@Priority(10)
@ApplicationScoped
public class CountEntitiesRoute extends RouteBuilder {

    @Inject
    CountEntitiesPreProcessor preProcessor;

    @Inject
    CountEntitiesPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/count?httpMethodRestrict=GET")
            .routeId("countEntities")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities/count?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
