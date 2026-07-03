package com.identitybroker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSearchRequest {

    private String action;
    private String resourceType;
    private String actorId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
