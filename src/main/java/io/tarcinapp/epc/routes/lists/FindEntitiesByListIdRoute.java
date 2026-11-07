package io.tarcinapp.epc.routes.lists;

import io.tarcinapp.epc.processors.lists.FindEntitiesByListIdPreProcessor;
import io.tarcinapp.epc.processors.lists.FindEntitiesByListIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for findEntitiesByListId.
 * Execution flow: FindEntitiesByListIdPreProcessor → backend proxy → FindEntitiesByListIdPostProcessor
 */
@ApplicationScoped
public class FindEntitiesByListIdRoute extends RouteBuilder {

    @Inject
    FindEntitiesByListIdPreProcessor preProcessor;

    @Inject
    FindEntitiesByListIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/lists/{id}/entities?httpMethodRestrict=GET")
            .routeId("findEntitiesByListId")
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
