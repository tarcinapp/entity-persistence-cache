package com.tarcinapp.epc;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.CamelEvent.ExchangeCreatedEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.jboss.logging.Logger;

/**
 * Registers a Camel EventNotifier that emits a TRACE-level log for every
 * inbound exchange the moment it is created (i.e. when a request enters any
 * route for the first time).
 *
 * <p>Two log lines are produced per request when TRACE is enabled:
 * <ol>
 *   <li><b>Headers</b> — always logged.</li>
 *   <li><b>Body</b> — logged only when the exchange carries a non-blank body
 *       (i.e. POST / PUT / PATCH payloads).</li>
 * </ol>
 *
 * <p>MDC injection of {@code RequestId} is handled by {@link RequestIdMdcProcessor},
 * which runs on the Vert.x worker thread that owns the exchange and is therefore
 * visible to the logging framework on that thread.
 */
@ApplicationScoped
public class TraceLoggingRouteBuilder extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(TraceLoggingRouteBuilder.class);

    @Override
    public void configure() {
        getContext().getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
            @Override
            public void notify(CamelEvent event) {
                if (!(event instanceof ExchangeCreatedEvent)) {
                    return;
                }
                Exchange exchange = ((ExchangeCreatedEvent) event).getExchange();
                logExchange(exchange);
            }

            @Override
            public boolean isEnabled(CamelEvent event) {
                return event instanceof ExchangeCreatedEvent;
            }
        });
    }

    // ── Static helper ────────────────────────────────────────────────────────

    private static void logExchange(Exchange exchange) {
        String routeId   = exchange.getFromRouteId();
        String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);

        // ── Headers ─────────────────────────────────────────────────────────
        Map<String, Object> headers = exchange.getMessage().getHeaders();
        LOG.tracef("[%s] requestId=%s headers=%s", routeId, requestId, headers);

        // ── Body (only when present) ─────────────────────────────────────────
        if (LOG.isTraceEnabled()) {
            String body = exchange.getMessage().getBody(String.class);
            if (body != null && !body.isBlank()) {
                LOG.tracef("[%s] requestId=%s body=%s", routeId, requestId, body);
            }
        }
    }
}
