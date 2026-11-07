package io.tarcinapp.epc.routes.system;

import io.tarcinapp.epc.processors.system.PingPreProcessor;
import io.tarcinapp.epc.processors.system.PingPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel route for ping.
 * Execution flow: PingPreProcessor → backend proxy → PingPostProcessor
 */
@ApplicationScoped
public class PingRoute extends RouteBuilder {

    @Inject
    PingPreProcessor preProcessor;

    @Inject
    PingPostProcessor postProcessor;

    @Override
    public void configure() {
        from("platform-http:/ping?httpMethodRestrict=GET")
            .routeId("ping")
            .process(preProcessor)
            .choice()
                .when(exchangeProperty("CamelRouteStop").isEqualTo(Boolean.TRUE))
                    .log("Route ${routeId} short-circuited by pre-processor — skipping backend and post-processor")
                .otherwise()
                    .removeHeader("Content-Length")
                    .removeHeader("Transfer-Encoding")
                    .toD("{{backend.base-url}}/ping?bridgeEndpoint=true&throwExceptionOnFailure=false")
                    .process(postProcessor)
            .end();
    }
}
