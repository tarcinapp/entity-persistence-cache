package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.DeleteListByIdPreProcessor;
import io.tarcinapp.epc.processors.lists.DeleteListByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteListById.
 * Execution flow: DeleteListByIdPreProcessor → backend proxy → DeleteListByIdPostProcessor
 */
@ApplicationScoped
public class DeleteListByIdRoute extends RouteBuilder {

    @Inject
    DeleteListByIdPreProcessor preProcessor;

    @Inject
    DeleteListByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}?httpMethodRestrict=DELETE")
            .routeId("deleteListById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists/${header.id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
