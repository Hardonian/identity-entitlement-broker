package com.identitybroker.api.dto;

import com.identitybroker.domain.RoleMapping;
import java.time.LocalDateTime;
import java.util.UUID;

public class RoleMappingResponse {

    public UUID id;
    public UUID tenantId;
    public String sourceType;
    public String sourceValue;
    public String targetRole;
    public String description;
    public LocalDateTime createdAt;

    public static RoleMappingResponse from(RoleMapping mapping) {
        RoleMappingResponse r = new RoleMappingResponse();
        r.id = mapping.getId();
        r.tenantId = mapping.getTenantId();
        r.sourceType = mapping.getSourceType();
        r.sourceValue = mapping.getSourceValue();
        r.targetRole = mapping.getTargetRole();
        r.description = mapping.getDescription();
        r.createdAt = mapping.getCreatedAt();
        return r;
    }
}
