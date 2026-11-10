package com.tarcinapp.epc.routes.lists;

import com.tarcinapp.epc.processors.lists.CreateChildListPreProcessor;
import com.tarcinapp.epc.processors.lists.CreateChildListPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createChildList.
 * Execution flow: CreateChildListPreProcessor → backend proxy → CreateChildListPostProcessor
 */
@ApplicationScoped
public class CreateChildListRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    CreateChildListPreProcessor preProcessor;

    @Inject
    CreateChildListPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/children?httpMethodRestrict=POST")
            .routeId("createChildList")
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
