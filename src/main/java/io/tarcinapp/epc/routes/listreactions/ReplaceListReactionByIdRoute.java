package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.ReplaceListReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.listreactions.ReplaceListReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceListReactionById.
 * Execution flow: ReplaceListReactionByIdPreProcessor → backend proxy → ReplaceListReactionByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceListReactionByIdRoute extends RouteBuilder {

    @Inject
    ReplaceListReactionByIdPreProcessor preProcessor;

    @Inject
    ReplaceListReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}?httpMethodRestrict=PUT")
            .routeId("replaceListReactionById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/list-reactions/${header.id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
