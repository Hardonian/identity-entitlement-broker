package com.identitybroker.api.dto;

import com.identitybroker.domain.Tenant;
import java.time.LocalDateTime;
import java.util.UUID;

public class TenantResponse {

    public UUID id;
    public String name;
    public String slug;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static TenantResponse from(Tenant tenant) {
        TenantResponse r = new TenantResponse();
        r.id = tenant.getId();
        r.name = tenant.getName();
        r.slug = tenant.getSlug();
        r.status = tenant.getStatus().name();
        r.createdAt = tenant.getCreatedAt();
        r.updatedAt = tenant.getUpdatedAt();
        return r;
    }
}
