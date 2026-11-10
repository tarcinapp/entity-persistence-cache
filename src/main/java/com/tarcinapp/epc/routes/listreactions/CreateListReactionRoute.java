package com.tarcinapp.epc.routes.listreactions;

import com.tarcinapp.epc.processors.listreactions.CreateListReactionPreProcessor;
import com.tarcinapp.epc.processors.listreactions.CreateListReactionPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createListReaction.
 * Execution flow: CreateListReactionPreProcessor → backend proxy → CreateListReactionPostProcessor
 */
@ApplicationScoped
public class CreateListReactionRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateListReactionPreProcessor preProcessor;

    @Inject
    CreateListReactionPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions?httpMethodRestrict=POST")
            .routeId("createListReaction")
            .process(mdcProcessor)
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor. requestId=${header.X-Request-Id}")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
