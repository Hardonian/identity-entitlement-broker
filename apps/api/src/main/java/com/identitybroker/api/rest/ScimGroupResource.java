package com.identitybroker.api.rest;

import com.identitybroker.api.dto.*;
import com.identitybroker.application.ScimProvisioningService;
import com.identitybroker.domain.ExternalGroup;
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

@Path("/scim/v2/Groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SCIM Groups", description = "SCIM 2.0 Group provisioning")
public class ScimGroupResource {

    @Inject
    ScimProvisioningService scimProvisioningService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Operation(summary = "Create a SCIM group")
    @APIResponse(responseCode = "201", description = "Group created")
    public Response createGroup(@Valid ScimGroupRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new com.identitybroker.api.rest.exception.ErrorResponse("VALIDATION_ERROR", "X-Tenant-Id header is required"))
                    .build();
        }
        ExternalGroup group = scimProvisioningService.createGroup(tenantId, request, tenantContext.getCurrentActorId());
        return Response.status(Response.Status.CREATED)
                .entity(ScimGroupResponse.from(group))
                .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a SCIM group by ID")
    @APIResponse(responseCode = "200", description = "Group found")
    @APIResponse(responseCode = "404", description = "Group not found")
    public ScimGroupResponse getGroup(@PathParam("id") UUID id) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return ScimGroupResponse.from(scimProvisioningService.getGroup(tenantId, id));
    }

    @GET
    @Operation(summary = "List SCIM groups with pagination")
    @APIResponse(responseCode = "200", description = "List of groups")
    public ScimGroupListResponse listGroups(@QueryParam("count") @DefaultValue("10") int count,
                                             @QueryParam("startIndex") @DefaultValue("1") int startIndex) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        ScimProvisioningService.ScimListResult result = scimProvisioningService.listGroups(tenantId, count, startIndex);

        ScimGroupListResponse response = new ScimGroupListResponse();
        response.totalResults = result.total;
        response.itemsPerPage = count;
        response.startIndex = startIndex;
        response.Resources = ((List<ExternalGroup>) result.items).stream()
                .map(ScimGroupResponse::from)
                .collect(Collectors.toList());
        return response;
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Partially update a SCIM group")
    @APIResponse(responseCode = "200", description = "Group updated")
    @APIResponse(responseCode = "404", description = "Group not found")
    public ScimGroupResponse updateGroup(@PathParam("id") UUID id, ScimGroupRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return ScimGroupResponse.from(scimProvisioningService.updateGroup(tenantId, id, request, tenantContext.getCurrentActorId()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a SCIM group")
    @APIResponse(responseCode = "204", description = "Group deleted")
    @APIResponse(responseCode = "404", description = "Group not found")
    public Response deleteGroup(@PathParam("id") UUID id) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        scimProvisioningService.deleteGroup(tenantId, id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }
}
