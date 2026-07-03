package com.identitybroker.api.rest;

import com.identitybroker.api.dto.CreateTenantRequest;
import com.identitybroker.api.dto.TenantResponse;
import com.identitybroker.application.TenantService;
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

@Path("/api/v1/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tenants", description = "Tenant management operations")
public class TenantResource {

    @Inject
    TenantService tenantService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Operation(summary = "Create a new tenant")
    @APIResponse(responseCode = "201", description = "Tenant created successfully")
    @APIResponse(responseCode = "409", description = "Tenant with this slug already exists")
    public Response createTenant(@Valid CreateTenantRequest request) {
        TenantResponse response = TenantResponse.from(
                tenantService.createTenant(request, tenantContext.getCurrentActorId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Operation(summary = "List all tenants")
    @APIResponse(responseCode = "200", description = "List of tenants")
    public List<TenantResponse> listTenants() {
        return tenantService.listTenants();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get tenant by ID")
    @APIResponse(responseCode = "200", description = "Tenant found")
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public TenantResponse getTenant(@PathParam("id") UUID id) {
        return TenantResponse.from(tenantService.getTenant(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a tenant")
    @APIResponse(responseCode = "200", description = "Tenant updated")
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public TenantResponse updateTenant(@PathParam("id") UUID id, @Valid CreateTenantRequest request) {
        return TenantResponse.from(tenantService.updateTenant(id, request, tenantContext.getCurrentActorId()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft-delete a tenant")
    @APIResponse(responseCode = "204", description = "Tenant disabled")
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public Response deleteTenant(@PathParam("id") UUID id) {
        tenantService.deleteTenant(id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }
}
