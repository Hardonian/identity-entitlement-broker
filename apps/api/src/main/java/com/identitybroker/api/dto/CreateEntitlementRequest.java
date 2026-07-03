package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "CreateEntitlementRequest")
public class CreateEntitlementRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID this entitlement belongs to", required = true)
    public UUID productId;

    @NotBlank(message = "Entitlement name is required")
    @Schema(description = "Entitlement name", required = true, example = "SSO Access")
    public String name;

    @NotBlank(message = "Entitlement slug is required")
    @Schema(description = "Entitlement slug", required = true, example = "sso-access")
    public String slug;

    @Schema(description = "Entitlement description", example = "Single sign-on access feature")
    public String description;

    @Schema(description = "Entitlement type", example = "FEATURE", defaultValue = "FEATURE")
    public String type;
}
