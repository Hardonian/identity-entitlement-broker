package com.identitybroker.api.rest;

import com.identitybroker.api.dto.*;
import com.identitybroker.application.EntitlementService;
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

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Entitlements", description = "Product and entitlement management")
public class EntitlementResource {

    @Inject
    EntitlementService entitlementService;

    @Inject
    TenantContext tenantContext;

    // ----- Products -----

    @POST
    @Path("/products")
    @Operation(summary = "Create a product")
    @APIResponse(responseCode = "201", description = "Product created")
    public Response createProduct(@Valid CreateProductRequest request) {
        ProductResponse response = ProductResponse.from(
                entitlementService.createProduct(request, tenantContext.getCurrentActorId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/products")
    @Operation(summary = "List all products")
    @APIResponse(responseCode = "200", description = "List of products")
    public List<ProductResponse> listProducts() {
        return entitlementService.listProducts();
    }

    @GET
    @Path("/products/{id}")
    @Operation(summary = "Get a product by ID")
    @APIResponse(responseCode = "200", description = "Product found")
    @APIResponse(responseCode = "404", description = "Product not found")
    public ProductResponse getProduct(@PathParam("id") UUID id) {
        return ProductResponse.from(entitlementService.getProduct(id));
    }

    // ----- Entitlements -----

    @POST
    @Path("/entitlements")
    @Operation(summary = "Create an entitlement")
    @APIResponse(responseCode = "201", description = "Entitlement created")
    public Response createEntitlement(@Valid CreateEntitlementRequest request) {
        EntitlementResponse response = EntitlementResponse.from(
                entitlementService.createEntitlement(request, tenantContext.getCurrentActorId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/entitlements")
    @Operation(summary = "List all entitlements")
    @APIResponse(responseCode = "200", description = "List of entitlements")
    public List<EntitlementResponse> listEntitlements() {
        return entitlementService.listEntitlements();
    }

    // ----- Assignments -----

    @POST
    @Path("/assignments")
    @Operation(summary = "Assign an entitlement to a user or group")
    @APIResponse(responseCode = "201", description = "Entitlement assigned")
    public Response assignEntitlement(@Valid AssignEntitlementRequest request) {
        EntitlementAssignmentResponse response = EntitlementAssignmentResponse.from(
                entitlementService.assign(request, tenantContext.getCurrentActorId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/assignments/user/{userId}")
    @Operation(summary = "Get effective entitlements for a user")
    @APIResponse(responseCode = "200", description = "Effective entitlements")
    public List<EffectiveEntitlementResponse> getUserEntitlements(@PathParam("userId") UUID userId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return entitlementService.getEffectiveEntitlements(tenantId, userId);
    }

    @GET
    @Path("/assignments/group/{groupId}")
    @Operation(summary = "List assignments for a group")
    @APIResponse(responseCode = "200", description = "Group assignments")
    public List<EntitlementAssignmentResponse> getGroupAssignments(@PathParam("groupId") UUID groupId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        return entitlementService.listByGroup(tenantId, groupId);
    }

    @GET
    @Path("/assignments")
    @Operation(summary = "List all assignments for the current tenant")
    @APIResponse(responseCode = "200", description = "List of assignments")
    public List<EntitlementAssignmentResponse> listAssignments(@QueryParam("tenantId") UUID tenantIdParam) {
        UUID tenantId = tenantIdParam != null ? tenantIdParam : tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id or tenantId query param is required");
        }
        return entitlementService.listByTenant(tenantId);
    }

    @DELETE
    @Path("/assignments/{id}")
    @Operation(summary = "Revoke an entitlement assignment")
    @APIResponse(responseCode = "204", description = "Assignment revoked")
    @APIResponse(responseCode = "404", description = "Assignment not found")
    public Response revokeAssignment(@PathParam("id") UUID id) {
        entitlementService.revoke(id, tenantContext.getCurrentActorId());
        return Response.noContent().build();
    }
}
