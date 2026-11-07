package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.FindChildrenByListReactionIdPreProcessor;
import io.tarcinapp.epc.processors.listreactions.FindChildrenByListReactionIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findChildrenByListReactionId.
 * Execution flow: FindChildrenByListReactionIdPreProcessor → backend proxy → FindChildrenByListReactionIdPostProcessor
 */
@ApplicationScoped
public class FindChildrenByListReactionIdRoute extends RouteBuilder {

    @Inject
    FindChildrenByListReactionIdPreProcessor preProcessor;

    @Inject
    FindChildrenByListReactionIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}/children?httpMethodRestrict=GET")
            .routeId("findChildrenByListReactionId")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions/${header.id}/children?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
