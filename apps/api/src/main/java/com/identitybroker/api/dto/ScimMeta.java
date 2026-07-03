package com.identitybroker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimMeta {

    private String resourceType;
    private LocalDateTime created;
    private LocalDateTime lastModified;
}
