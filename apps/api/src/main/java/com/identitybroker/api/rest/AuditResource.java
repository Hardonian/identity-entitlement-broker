package com.identitybroker.api.rest;

import com.identitybroker.api.dto.AuditEventResponse;
import com.identitybroker.api.dto.AuditSearchResponse;
import com.identitybroker.domain.AuditEvent;
import com.identitybroker.infrastructure.persistence.AuditEventRepository;
import com.identitybroker.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/audit")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Audit", description = "Audit event querying")
public class AuditResource {

    @Inject
    AuditEventRepository auditEventRepository;

    @Inject
    TenantContext tenantContext;

    @GET
    @Operation(summary = "List audit events for the current tenant")
    @APIResponse(responseCode = "200", description = "List of audit events")
    public List<AuditEventResponse> listAuditEvents(@QueryParam("page") @DefaultValue("0") int page,
                                                     @QueryParam("size") @DefaultValue("20") int size,
                                                     @QueryParam("sort") @DefaultValue("createdAt") String sort) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        // Use the findByTenantId method from the repository which returns a PanacheQueryWrapper
        return auditEventRepository.findByTenantId(tenantId, Page.of(page, size)).list().stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a single audit event")
    @APIResponse(responseCode = "200", description = "Audit event found")
    @APIResponse(responseCode = "404", description = "Audit event not found")
    public AuditEventResponse getAuditEvent(@PathParam("id") UUID id) {
        // Use the Optional wrapper for findById to avoid raw Object return
        return auditEventRepository.findByIdOptional(id)
            .map(AuditEventResponse::from)
            .orElseThrow(() -> new com.identitybroker.api.rest.exception.NotFoundException("Audit event not found: " + id));
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search audit events")
    @APIResponse(responseCode = "200", description = "Search results")
    public AuditSearchResponse searchAudit(@QueryParam("action") String action,
                                            @QueryParam("resourceType") String resourceType,
                                            @QueryParam("actorId") String actorId,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("20") int size) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new com.identitybroker.api.rest.exception.NotFoundException("X-Tenant-Id header is required");
        }
        // Use the search method correctly
        AuditEventRepository.PanacheQueryWrapper query = auditEventRepository.search(
                tenantId, action, resourceType, actorId, Page.of(page, size));
        List<AuditEventResponse> results = query.list().stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());
        long total = query.count(); // Change to long to match AuditSearchResponse total field

        return new AuditSearchResponse(results, total, page, size);
    }
}
