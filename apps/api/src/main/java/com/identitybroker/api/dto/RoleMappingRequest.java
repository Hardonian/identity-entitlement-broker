package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RoleMappingRequest")
public class RoleMappingRequest {

    @NotBlank(message = "Source type is required")
    @Schema(description = "Source type (e.g., OIDC claim, SAML attribute)", required = true, example = "OIDC admin claim")
    public String sourceType;

    @NotBlank(message = "Source value is required")
    @Schema(description = "Source value to match", required = true, example = "admin")
    public String sourceValue;

    @NotBlank(message = "Target role is required")
    @Schema(description = "Target role to map to", required = true, example = "super-admin")
    public String targetRole;

    @Schema(description = "Description of this mapping", example = "Maps OIDC admin claim to super-admin role")
    public String description;
}
