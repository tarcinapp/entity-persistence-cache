package com.tarcinapp.epc.routes.entityreactions;

import com.tarcinapp.epc.processors.entityreactions.FindChildrenByEntityReactionIdPreProcessor;
import com.tarcinapp.epc.processors.entityreactions.FindChildrenByEntityReactionIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findChildrenByEntityReactionId.
 * Execution flow: FindChildrenByEntityReactionIdPreProcessor → backend proxy → FindChildrenByEntityReactionIdPostProcessor
 */
@ApplicationScoped
public class FindChildrenByEntityReactionIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindChildrenByEntityReactionIdPreProcessor preProcessor;

    @Inject
    FindChildrenByEntityReactionIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}/children?httpMethodRestrict=GET")
            .routeId("findChildrenByEntityReactionId")
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
