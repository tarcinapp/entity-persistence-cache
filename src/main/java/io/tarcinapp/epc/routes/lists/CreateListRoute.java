package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.CreateListPreProcessor;
import io.tarcinapp.epc.processors.lists.CreateListPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for createList.
 * Execution flow: CreateListPreProcessor → backend proxy → CreateListPostProcessor
 */
@ApplicationScoped
public class CreateListRoute extends RouteBuilder {

    @Inject
    CreateListPreProcessor preProcessor;

    @Inject
    CreateListPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists?httpMethodRestrict=POST")
            .routeId("createList")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/lists?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
