package com.identitybroker.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ScimUserRequest")
public class ScimUserRequest {

    @Schema(description = "SCIM schemas", example = "[\"urn:ietf:params:scim:schemas:core:2.0:User\"]")
    public java.util.List<String> schemas;

    @NotBlank(message = "User name is required")
    @Schema(description = "Unique user name", required = true, example = "jdoe")
    public String userName;

    @Schema(description = "Given (first) name", example = "John")
    public String nameGiven;

    @Schema(description = "Family (last) name", example = "Doe")
    public String nameFamily;

    @Email(message = "Email must be valid")
    @Schema(description = "Email address", example = "john.doe@acme.com")
    public String email;

    @Schema(description = "External ID (optional, generated if not provided)", example = "eid-abc-123")
    public String externalId;

    @Schema(description = "Whether the user is active", defaultValue = "true")
    public Boolean active;
}
