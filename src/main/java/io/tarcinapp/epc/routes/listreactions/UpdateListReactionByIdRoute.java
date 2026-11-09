package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.UpdateListReactionByIdPreProcessor;
import io.tarcinapp.epc.processors.listreactions.UpdateListReactionByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for updateListReactionById.
 * Execution flow: UpdateListReactionByIdPreProcessor → backend proxy → UpdateListReactionByIdPostProcessor
 */
@ApplicationScoped
public class UpdateListReactionByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    UpdateListReactionByIdPreProcessor preProcessor;

    @Inject
    UpdateListReactionByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}?httpMethodRestrict=PATCH")
            .routeId("updateListReactionById")
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
