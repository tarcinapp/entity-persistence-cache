package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.ReplaceEntityByIdPreProcessor;
import io.tarcinapp.epc.processors.entities.ReplaceEntityByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceEntityById.
 * Execution flow: ReplaceEntityByIdPreProcessor → backend proxy → ReplaceEntityByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceEntityByIdRoute extends RouteBuilder {

    @Inject
    ReplaceEntityByIdPreProcessor preProcessor;

    @Inject
    ReplaceEntityByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}?httpMethodRestrict=PUT")
            .routeId("replaceEntityById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
