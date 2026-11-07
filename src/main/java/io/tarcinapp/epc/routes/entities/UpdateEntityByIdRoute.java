package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.UpdateEntityByIdPreProcessor;
import io.tarcinapp.epc.processors.entities.UpdateEntityByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateEntityById.
 * Execution flow: UpdateEntityByIdPreProcessor → backend proxy → UpdateEntityByIdPostProcessor
 */
@ApplicationScoped
public class UpdateEntityByIdRoute extends RouteBuilder {

    @Inject
    UpdateEntityByIdPreProcessor preProcessor;

    @Inject
    UpdateEntityByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}?httpMethodRestrict=PATCH")
            .routeId("updateEntityById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entities/${header.id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
