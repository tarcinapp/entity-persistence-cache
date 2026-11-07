package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.CreateEntityPreProcessor;
import io.tarcinapp.epc.processors.entities.CreateEntityPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createEntity.
 * Execution flow: CreateEntityPreProcessor → backend proxy → CreateEntityPostProcessor
 */
@ApplicationScoped
public class CreateEntityRoute extends RouteBuilder {

    @Inject
    CreateEntityPreProcessor preProcessor;

    @Inject
    CreateEntityPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities?httpMethodRestrict=POST")
            .routeId("createEntity")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
