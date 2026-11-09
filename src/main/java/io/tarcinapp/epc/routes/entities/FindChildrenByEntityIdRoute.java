package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.FindChildrenByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.FindChildrenByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findChildrenByEntityId.
 * Execution flow: FindChildrenByEntityIdPreProcessor → backend proxy → FindChildrenByEntityIdPostProcessor
 */
@ApplicationScoped
public class FindChildrenByEntityIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindChildrenByEntityIdPreProcessor preProcessor;

    @Inject
    FindChildrenByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/children?httpMethodRestrict=GET")
            .routeId("findChildrenByEntityId")
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
