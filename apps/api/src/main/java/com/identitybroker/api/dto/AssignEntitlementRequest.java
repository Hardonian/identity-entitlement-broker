package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "AssignEntitlementRequest")
public class AssignEntitlementRequest {

    @NotNull(message = "Tenant ID is required")
    @Schema(description = "Tenant ID", required = true)
    public UUID tenantId;

    @NotNull(message = "Entitlement ID is required")
    @Schema(description = "Entitlement ID to assign", required = true)
    public UUID entitlementId;

    @Schema(description = "User ID (if assigning to a user)")
    public UUID userId;

    @Schema(description = "Group ID (if assigning to a group)")
    public UUID groupId;
}
