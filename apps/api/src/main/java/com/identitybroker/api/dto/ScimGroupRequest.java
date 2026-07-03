package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

@Schema(name = "ScimGroupRequest")
public class ScimGroupRequest {

    @Schema(description = "SCIM schemas", example = "[\"urn:ietf:params:scim:schemas:core:2.0:Group\"]")
    public List<String> schemas;

    @NotBlank(message = "Display name is required")
    @Schema(description = "Group display name", required = true, example = "Engineering")
    public String displayName;

    @Schema(description = "External ID (optional)")
    public String externalId;

    @Schema(description = "Group members")
    public List<Member> members;

    public static class Member {
        @Schema(description = "Member's value (user ID or ref)")
        public String value;

        @Schema(description = "Member type (User or Group)", example = "User")
        public String type;

        @Schema(description = "Member's display name")
        public String display;
    }
}
