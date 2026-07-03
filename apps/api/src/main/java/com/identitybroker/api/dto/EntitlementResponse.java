package com.identitybroker.api.dto;

import com.identitybroker.domain.Entitlement;
import java.time.LocalDateTime;
import java.util.UUID;

public class EntitlementResponse {

    public UUID id;
    public UUID productId;
    public String name;
    public String slug;
    public String description;
    public String type;
    public LocalDateTime createdAt;

    public static EntitlementResponse from(Entitlement entitlement) {
        EntitlementResponse r = new EntitlementResponse();
        r.id = entitlement.getId();
        r.productId = entitlement.getProductId();
        r.name = entitlement.getName();
        r.slug = entitlement.getSlug();
        r.description = entitlement.getDescription();
        r.type = entitlement.getType().name();
        r.createdAt = entitlement.getCreatedAt();
        return r;
    }
}
