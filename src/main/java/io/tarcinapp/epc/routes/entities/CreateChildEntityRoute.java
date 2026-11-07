package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.CreateChildEntityPreProcessor;
import io.tarcinapp.epc.processors.entities.CreateChildEntityPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createChildEntity.
 * Execution flow: CreateChildEntityPreProcessor → backend proxy → CreateChildEntityPostProcessor
 */
@ApplicationScoped
public class CreateChildEntityRoute extends RouteBuilder {

    @Inject
    CreateChildEntityPreProcessor preProcessor;

    @Inject
    CreateChildEntityPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/children?httpMethodRestrict=POST")
            .routeId("createChildEntity")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities/${header.id}/children?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
