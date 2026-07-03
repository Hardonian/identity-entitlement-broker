package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateTenantRequest")
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Schema(description = "Tenant display name", required = true, example = "Acme Corporation")
    public String name;

    @NotBlank(message = "Tenant slug is required")
    @Schema(description = "URL-friendly tenant identifier", required = true, example = "acme-corp")
    public String slug;
}
