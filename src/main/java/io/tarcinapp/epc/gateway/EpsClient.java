package io.tarcinapp.epc.gateway;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for entity-persistence-service.
 *
 * <p>Used by {@link io.tarcinapp.epc.lock.ConfigCacheService} to fetch the
 * fully-resolved uniqueness and record-limit configuration from EPS's
 * {@code /system-info} endpoint, and by
 * {@link io.tarcinapp.epc.lock.PatchMergeHelper} to fetch the current stored
 * record when the findById cache entry is absent during PATCH pre-processing.</p>
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

    /**
     * GET /{recordType}/{id} — fetches a single record by ID directly from EPS.
     *
     * <p>Used by {@link io.tarcinapp.epc.lock.PatchMergeHelper} to obtain the
     * current stored state when the full-field findById cache entry is absent
     * (e.g. auth disabled, cold cache, or TTL expiry).</p>
     *
     * @param recordType  the plural EPS path segment: "entities", "lists",
     *                    "relations", "entity-reactions", or "list-reactions"
     * @param id          the record {@code _id}
     */
    @GET
    @Path("/{recordType}/{id}")
    Response getRecordById(@PathParam("recordType") String recordType,
                           @PathParam("id") String id);
}
