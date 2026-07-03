package com.identitybroker.api.rest;

import com.identitybroker.api.dto.*;
import com.identitybroker.application.ScimProvisioningService;
import com.identitybroker.domain.ExternalUser;
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
import java.util.stream.Collectors;

@Path("/scim/v2/Users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SCIM Users", description = "SCIM 2.0 User provisioning")
public class ScimUserResource {

    @Inject
    ScimProvisioningService scimProvisioningService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Operation(summary = "Create a SCIM user")
    @APIResponse(responseCode = "201", description = "User created")
    public Response createUser(@Valid ScimUserRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new com.identitybroker.api.rest.exception.ErrorResponse("VALIDATION_ERROR", "X-Tenant-Id header is required"))
                    .build();
        }
        ExternalUser user = scimProvisioningService.createUser(tenantId, request, tenantContext.getCurrentActorId());
        return Response.status(Response.Status.CREATED)
                .entity(ScimUserResponse.from(user))
                .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a SCIM user by ID")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public ScimUserResponse getUser(@PathParam("id") UUID id) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return ScimUserResponse.from(scimProvisioningService.getUser(tenantId, id));
    }

    @GET
    @Operation(summary = "List SCIM users with pagination")
    @APIResponse(responseCode = "200", description = "List of users")
    public ScimUserListResponse listUsers(@QueryParam("count") @DefaultValue("10") int count,
                                           @QueryParam("startIndex") @DefaultValue("1") int startIndex) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        ScimProvisioningService.ScimListResult result = scimProvisioningService.listUsers(tenantId, count, startIndex);

        ScimUserListResponse response = new ScimUserListResponse();
        response.totalResults = result.total;
        response.itemsPerPage = count;
        response.startIndex = startIndex;
        response.Resources = ((List<ExternalUser>) result.items).stream()
                .map(ScimUserResponse::from)
                .collect(Collectors.toList());
        return response;
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Partially update a SCIM user")
    @APIResponse(responseCode = "200", description = "User updated")
    @APIResponse(responseCode = "404", description = "User not found")
    public ScimUserResponse updateUser(@PathParam("id") UUID id, ScimUserRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return ScimUserResponse.from(scimProvisioningService.updateUser(tenantId, id, request, tenantContext.getCurrentActorId()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deactivate a SCIM user")
    @APIResponse(responseCode = "204", description = "User deactivated")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response deleteUser(@PathParam("id") UUID id) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        scimProvisioningService.deleteUser(tenantId, id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }
}
