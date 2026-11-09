package io.tarcinapp.epc.routes.entities;

import io.tarcinapp.epc.processors.entities.DeleteReactionsByEntityIdPreProcessor;
import io.tarcinapp.epc.processors.entities.DeleteReactionsByEntityIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteReactionsByEntityId.
 * Execution flow: DeleteReactionsByEntityIdPreProcessor → backend proxy → DeleteReactionsByEntityIdPostProcessor
 */
@ApplicationScoped
public class DeleteReactionsByEntityIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteReactionsByEntityIdPreProcessor preProcessor;

    @Inject
    DeleteReactionsByEntityIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/entities/{id}/reactions?httpMethodRestrict=DELETE")
            .routeId("deleteReactionsByEntityId")
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
