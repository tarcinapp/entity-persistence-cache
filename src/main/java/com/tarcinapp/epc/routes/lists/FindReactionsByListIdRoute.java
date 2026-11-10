package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.FindReactionsByListIdPreProcessor;
import com.tarcinapp.epc.processors.lists.FindReactionsByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findReactionsByListId.
 * Execution flow: FindReactionsByListIdPreProcessor → backend proxy → FindReactionsByListIdPostProcessor
 */
@ApplicationScoped
public class FindReactionsByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindReactionsByListIdPreProcessor preProcessor;

    @Inject
    FindReactionsByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/reactions?httpMethodRestrict=GET")
            .routeId("findReactionsByListId")
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
