package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateProductRequest")
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Schema(description = "Product name", required = true, example = "Identity Core")
    public String name;

    @NotBlank(message = "Product slug is required")
    @Schema(description = "URL-friendly product slug", required = true, example = "identity-core")
    public String slug;

    @Schema(description = "Product description", example = "Core identity management platform")
    public String description;
}
