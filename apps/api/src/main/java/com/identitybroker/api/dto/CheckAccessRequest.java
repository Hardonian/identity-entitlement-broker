package com.identitybroker.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CheckAccessRequest")
public class CheckAccessRequest {

    @Schema(description = "Tenant ID", required = true, example = "tenant-uuid")
    public String tenantId;

    @Schema(description = "User ID", required = true, example = "user-uuid")
    public String userId;

    @Schema(description = "Product slug to check access for", required = true, example = "identity-core")
    public String productSlug;
}
