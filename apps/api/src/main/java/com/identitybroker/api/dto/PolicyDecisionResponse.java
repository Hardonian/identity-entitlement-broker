package com.identitybroker.api.dto;

import java.util.Map;

public class PolicyDecisionResponse {

    public boolean allowed;
    public String reason;
    public Map<String, Object> details;

    public PolicyDecisionResponse() {}

    public PolicyDecisionResponse(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public PolicyDecisionResponse(boolean allowed, String reason, Map<String, Object> details) {
        this.allowed = allowed;
        this.reason = reason;
        this.details = details;
    }
}
