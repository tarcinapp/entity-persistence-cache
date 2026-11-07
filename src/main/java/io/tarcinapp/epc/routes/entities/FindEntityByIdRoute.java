package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.FindEntityByIdPreProcessor;
import io.tarcinapp.epc.processors.entities.FindEntityByIdPostProcessor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findEntityById.
 * Execution flow: FindEntityByIdPreProcessor → backend proxy → FindEntityByIdPostProcessor
 */
@Priority(20)
@ApplicationScoped
public class FindEntityByIdRoute extends RouteBuilder {

    @Inject
    FindEntityByIdPreProcessor preProcessor;

    @Inject
    FindEntityByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}?httpMethodRestrict=GET")
            .routeId("findEntityById")
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
