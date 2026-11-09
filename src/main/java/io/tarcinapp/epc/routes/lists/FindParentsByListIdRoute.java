package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.FindParentsByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.FindParentsByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findParentsByListId.
 * Execution flow: FindParentsByListIdPreProcessor → backend proxy → FindParentsByListIdPostProcessor
 */
@ApplicationScoped
public class FindParentsByListIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindParentsByListIdPreProcessor preProcessor;

    @Inject
    FindParentsByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/parents?httpMethodRestrict=GET")
            .routeId("findParentsByListId")
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
