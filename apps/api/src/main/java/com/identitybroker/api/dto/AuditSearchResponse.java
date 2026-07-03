package com.identitybroker.api.dto;

import java.util.List;

public class AuditSearchResponse {

    public List<AuditEventResponse> results;
    public Long total;  // Changed from int to Long
    public int page;
    public int size;

    public AuditSearchResponse() {}

    public AuditSearchResponse(List<AuditEventResponse> results, Long total, int page, int size) {
        this.results = results;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
