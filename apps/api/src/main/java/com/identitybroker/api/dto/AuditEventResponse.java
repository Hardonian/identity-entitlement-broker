package com.identitybroker.api.dto;

import com.identitybroker.domain.AuditEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuditEventResponse {

    public UUID id;
    public UUID tenantId;
    public String actorId;
    public String action;
    public String resourceType;
    public String resourceId;
    public String outcome;
    public String details;
    public LocalDateTime createdAt;

    public static AuditEventResponse from(AuditEvent event) {
        AuditEventResponse r = new AuditEventResponse();
        r.id = event.getId();
        r.tenantId = event.getTenantId();
        r.actorId = event.getActorId();
        r.action = event.getAction();
        r.resourceType = event.getResourceType();
        r.resourceId = event.getResourceId();
        r.outcome = event.getOutcome();
        r.details = event.getDetails();
        r.createdAt = event.getCreatedAt();
        return r;
    }
}
