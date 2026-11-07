package io.tarcinapp.epc.lock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.tarcinapp.epc.gateway.EpsClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * In-process Caffeine cache wrapping EPS's {@code /system-info} endpoint.
 *
 * <h3>Why Caffeine (not Redis)</h3>
 * The /system-info config is EPS-local, changes only at deployment time, and
 * is small enough to fit in heap. A local cache avoids a Redis round-trip on
 * every request — keeping the config fetch on the critical path negligible
 * (~1-2 ms over localhost loopback).
 *
 * <h3>TTL strategy</h3>
 * The cache holds a single entry (key {@code "config"}) that expires after
 * {@code epc.config-cache.ttl-seconds} (default 10 s). After expiry the next
 * caller reloads from EPS and all subsequent callers within the window are
 * served from heap. This means EPC self-heals within 10 s of any EPS
 * configuration change (which in practice only happens at deployment time).
 *
 * <h3>EPS unavailability</h3>
 * If EPS is slow or restarting, callers that hit after cache expiry will block
 * for up to the REST client's read-timeout before receiving a RuntimeException.
 * The LockService treats a failed config fetch as a 503.
 */
@ApplicationScoped
public class ConfigCacheService {

    private static final Logger LOG = Logger.getLogger(ConfigCacheService.class);
    private static final String CACHE_KEY = "config";

    @ConfigProperty(name = "epc.config-cache.ttl-seconds", defaultValue = "10")
    int ttlSeconds;

    @Inject
    @RestClient
    EpsClient epsClient;

    @Inject
    ObjectMapper objectMapper;

    private Cache<String, SystemInfo> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(1)
                .build();
        LOG.infof("ConfigCacheService initialised with TTL=%d seconds.", ttlSeconds);
    }

    /**
     * Returns the fully resolved EPS configuration, fetching from
     * {@code /system-info} at most once per TTL window.
     *
     * @return SystemInfo — never null
     * @throws RuntimeException if EPS is unreachable or returns an error
     */
    public SystemInfo getConfig() {
        return cache.get(CACHE_KEY, key -> fetchFromEps());
    }

    /**
     * Forces eviction of the cached config. Useful in tests.
     */
    public void invalidate() {
        cache.invalidateAll();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private SystemInfo fetchFromEps() {
        LOG.debug("Fetching /system-info from EPS (cache miss or expired).");
        try (Response resp = epsClient.getSystemInfo()) {
            if (resp.getStatus() >= 400) {
                throw new RuntimeException(
                        "EPS /system-info returned HTTP " + resp.getStatus()
                        + " — cannot determine lock key dimensions.");
            }
            String body = resp.readEntity(String.class);
            // /system-info wraps the RecordLimitConfig under a top-level object.
            // The response is a flat JSON object with the same fields as SystemInfo.
            SystemInfo info = objectMapper.readValue(body, SystemInfo.class);
            LOG.debug("EPS /system-info fetched and cached successfully.");
            return info;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch /system-info from EPS: " + e.getMessage(), e);
        }
    }
}
