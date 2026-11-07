package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.FindParentsByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.FindParentsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findParentsByEntityId.
 * Execution flow: FindParentsByEntityIdPreProcessor → backend proxy → FindParentsByEntityIdPostProcessor
 */
@ApplicationScoped
public class FindParentsByEntityIdRoute extends RouteBuilder {

    @Inject
    FindParentsByEntityIdPreProcessor preProcessor;

    @Inject
    FindParentsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/parents?httpMethodRestrict=GET")
            .routeId("findParentsByEntityId")
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
