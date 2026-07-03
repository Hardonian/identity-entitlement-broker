package com.identitybroker.api.dto;

import com.identitybroker.domain.EntitlementAssignment;
import java.time.LocalDateTime;
import java.util.UUID;

public class EntitlementAssignmentResponse {

    public UUID id;
    public UUID tenantId;
    public UUID entitlementId;
    public UUID userId;
    public UUID groupId;
    public String assignedBy;
    public LocalDateTime assignedAt;
    public boolean active;

    public static EntitlementAssignmentResponse from(EntitlementAssignment assignment) {
        EntitlementAssignmentResponse r = new EntitlementAssignmentResponse();
        r.id = assignment.getId();
        r.tenantId = assignment.getTenantId();
        r.entitlementId = assignment.getEntitlementId();
        r.userId = assignment.getUserId();
        r.groupId = assignment.getGroupId();
        r.assignedBy = assignment.getAssignedBy();
        r.assignedAt = assignment.getAssignedAt();
        r.active = assignment.isActive();
        return r;
    }
}
