package com.identitybroker.api.rest;

import com.identitybroker.api.dto.IdpConnectionResponse;
import com.identitybroker.api.dto.RegisterIdpRequest;
import com.identitybroker.application.IdpConnectionService;
import com.identitybroker.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/tenants/{tenantId}/idp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Identity Providers", description = "IdP connection management")
public class IdpConnectionResource {

    @Inject
    IdpConnectionService idpConnectionService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Operation(summary = "Register an identity provider connection")
    @APIResponse(responseCode = "201", description = "IdP connection registered")
    @APIResponse(responseCode = "400", description = "Invalid provider type or missing issuer")
    public Response registerIdp(@PathParam("tenantId") UUID tenantId, @Valid RegisterIdpRequest request) {
        IdpConnectionResponse response = idpConnectionService.register(tenantId, request, tenantContext.getCurrentActorId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Operation(summary = "List IdP connections for a tenant")
    @APIResponse(responseCode = "200", description = "List of IdP connections")
    public List<IdpConnectionResponse> listIdps(@PathParam("tenantId") UUID tenantId) {
        return idpConnectionService.listByTenant(tenantId);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an IdP connection by ID")
    @APIResponse(responseCode = "200", description = "IdP connection found")
    @APIResponse(responseCode = "404", description = "IdP connection not found")
    public IdpConnectionResponse getIdp(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id) {
        return idpConnectionService.getConnection(id);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update an IdP connection")
    @APIResponse(responseCode = "200", description = "IdP connection updated")
    @APIResponse(responseCode = "404", description = "IdP connection not found")
    public IdpConnectionResponse updateIdp(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id,
                                            @Valid RegisterIdpRequest request) {
        return idpConnectionService.update(id, request, tenantContext.getCurrentActorId());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove an IdP connection")
    @APIResponse(responseCode = "204", description = "IdP connection removed")
    @APIResponse(responseCode = "404", description = "IdP connection not found")
    public Response removeIdp(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id) {
        idpConnectionService.remove(id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }
}
