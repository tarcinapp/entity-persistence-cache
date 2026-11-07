package io.tarcinapp.epc;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Verifies that the backend (entity-persistence-service) is reachable before
 * the application finishes starting. Mirrors the implementation from the
 * entity-persistence-universal-orchestrator template.
 */
@ApplicationScoped
public class BackendStartupCheck {

    private static final Logger LOG = Logger.getLogger(BackendStartupCheck.class);

    @ConfigProperty(name = "backend.base-url")
    String backendBaseUrl;

    @ConfigProperty(name = "backend.health-check.path", defaultValue = "/ping")
    String healthCheckPath;

    @ConfigProperty(name = "backend.health-check.openapi-path", defaultValue = "/openapi.json")
    String openapiCheckPath;

    @ConfigProperty(name = "backend.health-check.timeout-seconds", defaultValue = "10")
    int timeoutSeconds;

    @ConfigProperty(name = "backend.health-check.enabled", defaultValue = "true")
    boolean enabled;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.info("Backend reachability check is disabled — skipping.");
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        checkPath(client, healthCheckPath);
        checkPath(client, openapiCheckPath);
    }

    private void checkPath(HttpClient client, String path) {
        String url = backendBaseUrl + path;
        LOG.infof("Checking backend reachability: GET %s (timeout: %ds) ...", url, timeoutSeconds);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() >= 500) {
                throw new IllegalStateException(String.format(
                        "Backend reachability check FAILED: GET %s returned HTTP %d (>= 500). "
                        + "Application will not start.",
                        url, response.statusCode()));
            }

            LOG.infof("Backend reachability check PASSED: GET %s → HTTP %d.", url, response.statusCode());

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Backend reachability check FAILED: GET %s — %s: %s. "
                    + "Application will not start.",
                    url, e.getClass().getSimpleName(), e.getMessage()), e);
        }
    }
}
