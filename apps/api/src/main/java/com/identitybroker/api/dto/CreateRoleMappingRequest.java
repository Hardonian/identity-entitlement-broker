package com.identitybroker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleMappingRequest {

    @NotBlank(message = "Role mapping name is required")
    private String name;

    @NotNull(message = "Source type is required")
    private String sourceType;

    @NotBlank(message = "Source value is required")
    private String sourceValue;

    @NotBlank(message = "Target role is required")
    private String targetRole;

    @Builder.Default
    private int priority = 0;
}
