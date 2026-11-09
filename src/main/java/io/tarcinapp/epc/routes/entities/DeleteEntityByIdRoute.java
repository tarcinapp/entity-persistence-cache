package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.DeleteEntityByIdPreProcessor;
import io.tarcinapp.epc.processors.entities.DeleteEntityByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteEntityById.
 * Execution flow: DeleteEntityByIdPreProcessor → backend proxy → DeleteEntityByIdPostProcessor
 */
@ApplicationScoped
public class DeleteEntityByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteEntityByIdPreProcessor preProcessor;

    @Inject
    DeleteEntityByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}?httpMethodRestrict=DELETE")
            .routeId("deleteEntityById")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
