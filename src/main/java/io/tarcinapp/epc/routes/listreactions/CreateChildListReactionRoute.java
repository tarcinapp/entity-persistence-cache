package io.tarcinapp.epc.routes.listreactions;

import io.tarcinapp.epc.processors.listreactions.CreateChildListReactionPreProcessor;
import io.tarcinapp.epc.processors.listreactions.CreateChildListReactionPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createChildListReaction.
 * Execution flow: CreateChildListReactionPreProcessor → backend proxy → CreateChildListReactionPostProcessor
 */
@ApplicationScoped
public class CreateChildListReactionRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateChildListReactionPreProcessor preProcessor;

    @Inject
    CreateChildListReactionPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/list-reactions/{id}/children?httpMethodRestrict=POST")
            .routeId("createChildListReaction")
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
