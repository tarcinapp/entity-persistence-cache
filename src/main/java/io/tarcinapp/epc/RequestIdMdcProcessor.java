package io.tarcinapp.epc;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logmanager.MDC;

/**
 * Camel processor that injects the {@code X-Request-Id} header value into the
 * MDC so that {@code %X{X-Request-Id}} in the console log pattern is populated
 * for every log line emitted during the exchange.
 *
 * <p>Wired as the first {@code .process()} step in every route so that the
 * request ID appears in bracket 5 of all subsequent log output for that route.
 *
 * <p>Works in conjunction with {@link FallbackMdcProvider}: standard
 * {@code MDC.put()} writes to both the Vert.x context map and a per-thread
 * fallback map, so the value is visible on both event-loop and worker threads.
 */
@ApplicationScoped
public class RequestIdMdcProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);
        if (requestId != null) {
            MDC.put("X-Request-Id", requestId);
        } else {
            MDC.remove("X-Request-Id");
        }
    }
}
