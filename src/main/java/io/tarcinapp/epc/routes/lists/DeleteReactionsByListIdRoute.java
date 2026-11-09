package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.DeleteReactionsByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.DeleteReactionsByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteReactionsByListId.
 * Execution flow: DeleteReactionsByListIdPreProcessor → backend proxy → DeleteReactionsByListIdPostProcessor
 */
@ApplicationScoped
public class DeleteReactionsByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteReactionsByListIdPreProcessor preProcessor;

    @Inject
    DeleteReactionsByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/reactions?httpMethodRestrict=DELETE")
            .routeId("deleteReactionsByListId")
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
