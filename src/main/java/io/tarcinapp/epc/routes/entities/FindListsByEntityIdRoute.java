package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.FindListsByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.FindListsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findListsByEntityId.
 * Execution flow: FindListsByEntityIdPreProcessor → backend proxy → FindListsByEntityIdPostProcessor
 */
@ApplicationScoped
public class FindListsByEntityIdRoute extends RouteBuilder {

    @Inject
    FindListsByEntityIdPreProcessor preProcessor;

    @Inject
    FindListsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/lists?httpMethodRestrict=GET")
            .routeId("findListsByEntityId")
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
