package com.identitybroker.api.rest;

import com.identitybroker.api.dto.RoleMappingRequest;
import com.identitybroker.api.dto.RoleMappingResponse;
import com.identitybroker.application.RoleMappingService;
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
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/tenants/{tenantId}/role-mappings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Role Mappings", description = "Role mapping management")
public class RoleMappingResource {

    @Inject
    RoleMappingService roleMappingService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Operation(summary = "Create a role mapping")
    @APIResponse(responseCode = "201", description = "Role mapping created")
    public Response createRoleMapping(@PathParam("tenantId") UUID tenantId, @Valid RoleMappingRequest request) {
        RoleMappingResponse response = roleMappingService.create(tenantId, request, tenantContext.getCurrentActorId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Operation(summary = "List role mappings for a tenant")
    @APIResponse(responseCode = "200", description = "List of role mappings")
    public List<RoleMappingResponse> listRoleMappings(@PathParam("tenantId") UUID tenantId) {
        return roleMappingService.listByTenant(tenantId);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a role mapping by ID")
    @APIResponse(responseCode = "200", description = "Role mapping found")
    @APIResponse(responseCode = "404", description = "Role mapping not found")
    public RoleMappingResponse getRoleMapping(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id) {
        return roleMappingService.getById(id);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a role mapping")
    @APIResponse(responseCode = "200", description = "Role mapping updated")
    @APIResponse(responseCode = "404", description = "Role mapping not found")
    public RoleMappingResponse updateRoleMapping(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id,
                                                  @Valid RoleMappingRequest request) {
        return roleMappingService.update(id, request, tenantContext.getCurrentActorId());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a role mapping")
    @APIResponse(responseCode = "204", description = "Role mapping deleted")
    @APIResponse(responseCode = "404", description = "Role mapping not found")
    public Response deleteRoleMapping(@PathParam("tenantId") UUID tenantId, @PathParam("id") UUID id) {
        roleMappingService.delete(id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }

    @GET
    @Path("/resolve/{sourceType}/{sourceValue}")
    @Operation(summary = "Resolve a role by source type and value")
    @APIResponse(responseCode = "200", description = "Resolved role or null")
    public Map<String, Object> resolveRole(@PathParam("tenantId") UUID tenantId,
                                            @PathParam("sourceType") String sourceType,
                                            @PathParam("sourceValue") String sourceValue) {
        String role = roleMappingService.resolveRole(tenantId, sourceType, sourceValue);
        return Map.of("sourceType", sourceType, "sourceValue", sourceValue, "targetRole", role);
    }
}
