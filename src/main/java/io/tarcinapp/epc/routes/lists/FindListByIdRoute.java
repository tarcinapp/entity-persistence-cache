package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.FindListByIdPreProcessor;
import io.tarcinapp.epc.processors.lists.FindListByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findListById.
 * Execution flow: FindListByIdPreProcessor → backend proxy → FindListByIdPostProcessor
 */
@ApplicationScoped
public class FindListByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    FindListByIdPreProcessor preProcessor;

    @Inject
    FindListByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}?httpMethodRestrict=GET")
            .routeId("findListById")
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
