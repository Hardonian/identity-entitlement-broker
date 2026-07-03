package com.identitybroker.application;

import com.identitybroker.api.dto.EffectiveEntitlementResponse;
import com.identitybroker.api.dto.PolicyDecisionRequest;
import com.identitybroker.api.dto.PolicyDecisionResponse;
import com.identitybroker.domain.PolicyDecision;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.policies.LocalPolicyEngine;
import com.identitybroker.infrastructure.policies.OpaClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyService {

    @Inject
    OpaClient opaClient;

    @Inject
    LocalPolicyEngine localPolicyEngine;

    @Inject
    AuditService auditService;

    @Inject
    TenantService tenantService;

    @Inject
    EntitlementService entitlementService;

    @Inject
    RoleMappingService roleMappingService;

    public PolicyDecisionResponse decide(PolicyDecisionRequest request, String actorId) {
        UUID tenantId = UUID.fromString(request.tenantId);

        // Build input map
        Map<String, Object> input = new HashMap<>();
        input.put("tenant_id", request.tenantId);
        input.put("action", request.action);

        // Subject info
        Map<String, Object> subject = new HashMap<>();
        if (request.subject != null) {
            subject.put("id", request.subject.id);
            subject.put("type", request.subject.type);
            subject.put("roles", request.subject.roles != null ? request.subject.roles : List.of());
        }
        input.put("subject", subject);

        // Resource info
        Map<String, Object> resource = new HashMap<>();
        if (request.resource != null) {
            resource.put("type", request.resource.type);
            resource.put("id", request.resource.id);
            resource.put("attributes", request.resource.attributes != null ? request.resource.attributes : Map.of());
        }
        input.put("resource", resource);

        // Context
        input.put("context", request.context != null ? request.context : Map.of());

        // Resolve entitlements for this user
        String subjectId = request.subject != null ? request.subject.id : null;
        List<String> entitlementSlugs = new ArrayList<>();
        List<String> roles = new ArrayList<>();

        if (subjectId != null) {
            try {
                UUID userId = UUID.fromString(subjectId);
                List<EffectiveEntitlementResponse> effectiveEntitlements =
                        entitlementService.getEffectiveEntitlements(tenantId, userId);
                entitlementSlugs = effectiveEntitlements.stream()
                        .map(e -> e.entitlementSlug)
                        .collect(Collectors.toList());

                // Resolve roles from role mappings
                if (request.subject != null && request.subject.roles != null) {
                    for (String sourceRole : request.subject.roles) {
                        String resolved = roleMappingService.resolveRole(tenantId, "OIDC claim", sourceRole);
                        if (resolved != null) roles.add(resolved);
                    }
                }
            } catch (IllegalArgumentException e) {
                // If not a UUID, use the raw string
            }
        }

        input.put("entitlements", entitlementSlugs);
        input.put("roles", roles);

        // Try OPA first
        PolicyDecision decision = null;
        try {
            Map<String, Object> opaResult = opaClient.evaluate(input);
            if (opaResult != null && !opaResult.isEmpty()) {
                Boolean allowed = (Boolean) opaResult.getOrDefault("allowed", false);
                String reason = (String) opaResult.getOrDefault("reason", null);
                String matchedRule = (String) opaResult.get("matchedRule");
                decision = PolicyDecision.builder()
                    .id(UUID.randomUUID())
                    .allowed(allowed != null && allowed)
                    .reason(reason)
                    .matchedRule(matchedRule)
                    .entitlements(entitlementSlugs)
                    .roles(roles)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            }
        } catch (Exception e) {
            // OPA unavailable, fall through to local engine
        }

        // Fallback to local policy engine
        if (decision == null) {
            String actor = actorId != null ? actorId : "unknown";
            String subjectStr = subjectId != null ? subjectId : "unknown";
            String action = request.action;
            String resourceType = request.resource != null ? request.resource.type : null;
            decision = localPolicyEngine.evaluate(tenantId, actor, subjectStr, action,
                    resourceType, entitlementSlugs, roles);
        }

        // Build response
        Map<String, Object> details = new HashMap<>();
        details.put("entitlements", entitlementSlugs);
        details.put("roles", roles);
        details.put("input", input);

        PolicyDecisionResponse response = new PolicyDecisionResponse(
                decision.isAllowed(),
                decision.getReason(),
                details
        );

        // Record audit
        auditService.record(
                "policy.decide",
                "policy",
                request.action,
                decision.isAllowed() ? "SUCCESS" : "DENIED",
                actorId,
                tenantId,
                Map.of("action", request.action, "allowed", decision.isAllowed(), "reason", decision.getReason())
        );

        return response;
    }
}
