package io.tarcinapp.epc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.quarkus.main.events.BeforeStart;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.jboss.logging.Logger;

/**
 * Configures the Camel platform-http component to strip headers with [ or ]
 * characters from HTTP responses.  Query parameter names like filter[limit]
 * are invalid HTTP header names (RFC 7230 §3.2) and would cause Vert.x to
 * throw an IllegalArgumentException.
 */
@ApplicationScoped
public class CamelPlatformHttpConfig {

    private static final Logger LOG = Logger.getLogger(CamelPlatformHttpConfig.class);

    void onBeforeStart(@Observes BeforeStart event) {
        PlatformHttpComponent phc = event.getCamelContext()
                .getComponent("platform-http", PlatformHttpComponent.class);

        if (phc == null) {
            LOG.warn("platform-http component not found — skipping header filter strategy configuration.");
            return;
        }

        DefaultHeaderFilterStrategy strategy = new DefaultHeaderFilterStrategy();
        strategy.setOutFilterPattern(".*[\\[\\]].*");
        phc.setHeaderFilterStrategy(strategy);

        LOG.debug("platform-http HeaderFilterStrategy configured to exclude headers with [ or ] characters.");
    }
}
