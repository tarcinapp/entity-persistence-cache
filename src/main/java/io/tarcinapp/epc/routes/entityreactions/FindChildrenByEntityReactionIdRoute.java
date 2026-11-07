package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.FindChildrenByEntityReactionIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.FindChildrenByEntityReactionIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findChildrenByEntityReactionId.
 * Execution flow: FindChildrenByEntityReactionIdPreProcessor → backend proxy → FindChildrenByEntityReactionIdPostProcessor
 */
@ApplicationScoped
public class FindChildrenByEntityReactionIdRoute extends RouteBuilder {

    @Inject
    FindChildrenByEntityReactionIdPreProcessor preProcessor;

    @Inject
    FindChildrenByEntityReactionIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}/children?httpMethodRestrict=GET")
            .routeId("findChildrenByEntityReactionId")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/entity-reactions/${header.id}/children?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
