package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.UpdateEntitiesPreProcessor;
import io.tarcinapp.epc.processors.entities.UpdateEntitiesPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntities.
 * Execution flow: UpdateEntitiesPreProcessor → backend proxy → UpdateEntitiesPostProcessor
 */
@ApplicationScoped
public class UpdateEntitiesRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateEntitiesPreProcessor preProcessor;

    @Inject
    UpdateEntitiesPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities?httpMethodRestrict=PATCH")
            .routeId("updateEntities")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
