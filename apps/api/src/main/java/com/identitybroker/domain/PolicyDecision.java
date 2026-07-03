package com.identitybroker.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Policy decision POJO — not a JPA entity.
 * Represents the result of a policy evaluation (OPA or local engine).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDecision {

    private UUID id;

    private UUID tenantId;

    private String actor;

    private String subject;

    private String action;

    private String resource;

    @Builder.Default
    private boolean allowed = false;

    private String reason;

    private String matchedRule;

    private List<String> entitlements;

    private List<String> roles;

    private LocalDateTime createdAt;

    /**
     * Create a denied policy decision with a reason.
     */
    public static PolicyDecision deny(String reason) {
        return PolicyDecision.builder()
            .id(UUID.randomUUID())
            .allowed(false)
            .reason(reason)
            .createdAt(LocalDateTime.now())
            .build();
    }

    /**
     * Create an allowed policy decision.
     */
    public static PolicyDecision allow(String matchedRule, List<String> entitlements, List<String> roles) {
        return PolicyDecision.builder()
            .id(UUID.randomUUID())
            .allowed(true)
            .reason("Access granted")
            .matchedRule(matchedRule)
            .entitlements(entitlements)
            .roles(roles)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
