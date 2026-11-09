package io.tarcinapp.epc.routes.relations;

import io.tarcinapp.epc.processors.relations.DeleteRelationByIdPreProcessor;
import io.tarcinapp.epc.processors.relations.DeleteRelationByIdPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.tarcinapp.epc.RequestIdMdcProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for deleteRelationById.
 * Execution flow: DeleteRelationByIdPreProcessor → backend proxy → DeleteRelationByIdPostProcessor
 */
@ApplicationScoped
public class DeleteRelationByIdRoute extends RouteBuilder {

    @Inject
    RequestIdMdcProcessor mdcProcessor;

    @Inject
    DeleteRelationByIdPreProcessor preProcessor;

    @Inject
    DeleteRelationByIdPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/relations/{id}?httpMethodRestrict=DELETE")
            .routeId("deleteRelationById")
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
