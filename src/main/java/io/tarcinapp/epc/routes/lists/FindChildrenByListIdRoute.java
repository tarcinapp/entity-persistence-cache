package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.FindChildrenByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.FindChildrenByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findChildrenByListId.
 * Execution flow: FindChildrenByListIdPreProcessor → backend proxy → FindChildrenByListIdPostProcessor
 */
@ApplicationScoped
public class FindChildrenByListIdRoute extends RouteBuilder {

    @Inject
    FindChildrenByListIdPreProcessor preProcessor;

    @Inject
    FindChildrenByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/children?httpMethodRestrict=GET")
            .routeId("findChildrenByListId")
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
