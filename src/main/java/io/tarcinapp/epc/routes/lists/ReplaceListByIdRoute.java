package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.ReplaceListByIdPreProcessor;
import io.tarcinapp.epc.processors.lists.ReplaceListByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for replaceListById.
 * Execution flow: ReplaceListByIdPreProcessor → backend proxy → ReplaceListByIdPostProcessor
 */
@ApplicationScoped
public class ReplaceListByIdRoute extends RouteBuilder {

    @Inject
    ReplaceListByIdPreProcessor preProcessor;

    @Inject
    ReplaceListByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}?httpMethodRestrict=PUT")
            .routeId("replaceListById")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}${header.CamelHttpPath}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
