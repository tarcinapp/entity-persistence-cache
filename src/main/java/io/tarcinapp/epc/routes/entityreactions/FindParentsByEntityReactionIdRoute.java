package io.tarcinapp.epc.routes.entityreactions;

import io.tarcinapp.epc.processors.entityreactions.FindParentsByEntityReactionIdPreProcessor;
import io.tarcinapp.epc.processors.entityreactions.FindParentsByEntityReactionIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findParentsByEntityReactionId.
 * Execution flow: FindParentsByEntityReactionIdPreProcessor → backend proxy → FindParentsByEntityReactionIdPostProcessor
 */
@ApplicationScoped
public class FindParentsByEntityReactionIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindParentsByEntityReactionIdPreProcessor preProcessor;

    @Inject
    FindParentsByEntityReactionIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entity-reactions/{id}/parents?httpMethodRestrict=GET")
            .routeId("findParentsByEntityReactionId")
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
