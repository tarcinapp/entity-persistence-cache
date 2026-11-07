package io.tarcinapp.epc.gateway;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for entity-persistence-service.
 *
 * <p>Used exclusively by {@link io.tarcinapp.epc.lock.ConfigCacheService} to
 * fetch the fully-resolved uniqueness and record-limit configuration from
 * EPS's {@code /system-info} endpoint.</p>
 *
 * <p>The base URL is controlled by
 * {@code quarkus.rest-client."entity-persistence-service".url} in
 * {@code application.properties}, which defaults to {@code backend.base-url}
 * and can be overridden via {@code BACKEND_BASE_URL}.</p>
 */
@RegisterRestClient(configKey = "entity-persistence-service")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface EpsClient {

    /**
     * GET /system-info — returns the fully resolved EPS configuration,
     * including uniqueness scopes and record-limit rules.
     */
    @GET
    @Path("/system-info")
    Response getSystemInfo();

    /**
     * GET /ping — used for health probing.
     */
    @GET
    @Path("/ping")
    Response ping();
}
