package com.identitybroker.infrastructure.audit;

import com.identitybroker.api.dto.AuditEventResponse;
import com.identitybroker.api.dto.AuditSearchRequest;
import com.identitybroker.api.dto.AuditSearchResponse;
import com.identitybroker.domain.AuditEvent;
import com.identitybroker.infrastructure.persistence.AuditEventRepository;
import com.identitybroker.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recording and querying audit events.
 *
 * <p>Automatically enriches audit records with tenant context (actor,
 * tenant ID, correlation ID) from the {@link TenantContext} bean.
 */
@ApplicationScoped
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Inject
    AuditEventRepository auditEventRepository;

    @Inject
    TenantContext tenantContext;

    /**
     * Record an audit event.
     *
     * @param action       the action performed (e.g., "tenant.create", "entitlement.assign")
     * @param resourceType the type of resource affected (e.g., "tenant", "entitlement")
     * @param resourceId   the ID of the resource affected
     * @param outcome      the outcome (e.g., "SUCCESS", "FAILURE", "DENIED")
     * @param actorId      the actor who performed the action (falls back to TenantContext)
     * @param tenantId     the tenant scope (falls back to TenantContext)
     * @param metadata     additional metadata to store as JSON
     * @return the persisted AuditEvent
     */
    @Transactional
    public AuditEvent record(String action, String resourceType, String resourceId,
                             String outcome, String actorId, UUID tenantId,
                             Map<String, Object> metadata) {
        String resolvedActorId = (actorId != null) ? actorId : tenantContext.getActorId();
        UUID resolvedTenantId = (tenantId != null) ? tenantId : tenantContext.getTenantId();

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(metadata);
            } catch (Exception e) {
                log.warn("Failed to serialize audit metadata to JSON", e);
            }
        }

        AuditEvent event = new AuditEvent(resolvedTenantId, resolvedActorId, action,
                resourceType, resourceId, outcome, metadataJson);

        auditEventRepository.persist(event);
        log.debug("Audit event recorded: action={}, resourceType={}, resourceId={}, outcome={}",
            action, resourceType, resourceId, outcome);

        return event;
    }

    /**
     * Convenience method that uses TenantContext for actor and tenant.
     */
    @Transactional
    public AuditEvent record(String action, String resourceType, String resourceId, String outcome) {
        return record(action, resourceType, resourceId, outcome, null, null, null);
    }

    /**
     * Convenience method including metadata.
     */
    @Transactional
    public AuditEvent record(String action, String resourceType, String resourceId,
                             String outcome, Map<String, Object> metadata) {
        return record(action, resourceType, resourceId, outcome, null, null, metadata);
    }

    /**
     * Convenience method recording a SUCCESS outcome with a details message.
     * Used by service classes throughout the application.
     */
    @Transactional
    public AuditEvent recordSuccess(UUID tenantId, String actorId, String action,
                                     String resourceType, String resourceId, String details) {
        Map<String, Object> metadata = null;
        if (details != null) {
            metadata = new HashMap<>();
            metadata.put("message", details);
        }
        return record(action, resourceType, resourceId, "SUCCESS", actorId, tenantId, metadata);
    }

    /**
     * Query audit events by tenant with optional filters.
     */
    public AuditSearchResponse search(AuditSearchRequest request) {
        UUID tenantId = tenantContext.getTenantId();
        Page page = Page.of(request.getPage(), request.getSize());

        AuditEventRepository.PanacheQueryWrapper query = auditEventRepository.search(
            tenantId, request.getAction(), request.getResourceType(),
            request.getActorId(), page);

        List<AuditEvent> events = query.list();
        long totalCount = query.count();

        List<AuditEventResponse> responses = events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new AuditSearchResponse(responses, totalCount, request.getPage(), request.getSize());
    }

    /**
     * Find events by resource type and ID (cross-tenant within same tenant context).
     */
    public List<AuditEvent> findByResource(String resourceType, String resourceId) {
        return auditEventRepository.findByResource(resourceType, resourceId);
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.from(event);
    }
}
