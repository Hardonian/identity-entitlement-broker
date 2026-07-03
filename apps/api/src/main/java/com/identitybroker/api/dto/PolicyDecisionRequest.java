package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.Map;

@Schema(name = "PolicyDecisionRequest")
public class PolicyDecisionRequest {

    @NotBlank(message = "Tenant ID is required")
    @Schema(description = "Tenant identifier", required = true, example = "tenant-1")
    public String tenantId;

    @NotBlank(message = "Action is required")
    @Schema(description = "Action to evaluate", required = true, example = "read:users")
    public String action;

    @Schema(description = "Subject (actor) information")
    public SubjectInfo subject;

    @Schema(description = "Resource information")
    public ResourceInfo resource;

    @Schema(description = "Additional context attributes")
    public Map<String, Object> context;

    public static class SubjectInfo {
        @Schema(description = "Subject/actor ID", example = "user-123")
        public String id;

        @Schema(description = "Subject type", example = "user")
        public String type;

        @Schema(description = "Subject roles", example = "[\"admin\"]")
        public java.util.List<String> roles;
    }

    public static class ResourceInfo {
        @Schema(description = "Resource type", example = "user")
        public String type;

        @Schema(description = "Resource ID", example = "user-456")
        public String id;

        @Schema(description = "Resource attributes")
        public Map<String, Object> attributes;
    }
}
