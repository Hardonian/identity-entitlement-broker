package com.identitybroker.infrastructure.policies;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

/**
 * REST client for the Open Policy Agent (OPA) decision API.
 *
 * <p>This client communicates with OPA's data API to evaluate policy decisions.
 * The base URI is configured via {@code quarkus.rest-client.opa-client.url}
 * in application.properties.
 *
 * <p>When OPA is unavailable, the {@link LocalPolicyEngine} is used as a fallback.
 */
@RegisterRestClient(configKey = "opa-client")
@RegisterClientHeaders
@Path("/v1/data")
public interface OpaClient {

    /**
     * Evaluate a policy decision against OPA.
     *
     * @param input the policy input containing at minimum tenant context,
     *              actor, subject, action, and resource fields
     * @return the OPA decision result containing allowed, reason, and matchedRule
     */
    @POST
    @Path("/identity/allow")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> evaluate(Map<String, Object> input);
}
