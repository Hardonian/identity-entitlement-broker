package com.identitybroker.api.dto;

import java.util.UUID;

public class EffectiveEntitlementResponse {

    public UUID entitlementId;
    public String entitlementName;
    public String entitlementSlug;
    public UUID productId;
    public String productName;
    public String productSlug;
    public String assignmentType; // "direct" or "group"

    public EffectiveEntitlementResponse() {}

    public EffectiveEntitlementResponse(UUID entitlementId, String entitlementName, String entitlementSlug,
                                         UUID productId, String productName, String productSlug,
                                         String assignmentType) {
        this.entitlementId = entitlementId;
        this.entitlementName = entitlementName;
        this.entitlementSlug = entitlementSlug;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.assignmentType = assignmentType;
    }
}
