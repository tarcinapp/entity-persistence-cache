package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.FindParentsByListReactionIdPreProcessor;
import io.tarcinapp.epc.processors.listreactions.FindParentsByListReactionIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findParentsByListReactionId.
 * Execution flow: FindParentsByListReactionIdPreProcessor → backend proxy → FindParentsByListReactionIdPostProcessor
 */
@ApplicationScoped
public class FindParentsByListReactionIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindParentsByListReactionIdPreProcessor preProcessor;

    @Inject
    FindParentsByListReactionIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}/parents?httpMethodRestrict=GET")
            .routeId("findParentsByListReactionId")
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
