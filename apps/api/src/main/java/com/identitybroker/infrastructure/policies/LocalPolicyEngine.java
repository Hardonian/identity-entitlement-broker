package com.identitybroker.infrastructure.policies;

import com.identitybroker.domain.PolicyDecision;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Local policy evaluation engine used as a fallback when OPA is unavailable.
 *
 * <p>Implements a simple rule-based policy engine that mirrors the semantics
 * of the Rego policies deployed to OPA. Rules are evaluated in priority order:
 *
 * <ol>
 *   <li><b>Admin rule:</b> users with role {@code admin} can manage any tenant resource</li>
 *   <li><b>Support rule:</b> users with role {@code support} can impersonate users</li>
 *   <li><b>Entitlement rule:</b> users with the required entitlement can access a product</li>
 *   <li><b>Integration rule:</b> users with role {@code integration} can write provisioning data</li>
 * </ol>
 */
@ApplicationScoped
public class LocalPolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(LocalPolicyEngine.class);

    /**
     * Evaluate a policy decision locally.
     *
     * @param tenantId     the tenant UUID
     * @param actor        the acting user/role
     * @param subject      the subject of the action
     * @param action       the action being performed (e.g., read, write, manage, provision)
     * @param resource     the resource being accessed
     * @param entitlements the user's current entitlements
     * @param roles        the user's current roles
     * @return a fully populated PolicyDecision
     */
    public PolicyDecision evaluate(UUID tenantId, String actor, String subject,
                                   String action, String resource,
                                   List<String> entitlements, List<String> roles) {
        log.debug("Local policy evaluation: actor={}, action={}, resource={}, roles={}, entitlements={}",
            actor, action, resource, roles, entitlements);

        // Rule 1: Admin can manage tenant resources
        if (hasRole(roles, "admin") && action.startsWith("manage")) {
            return PolicyDecision.allow("admin_can_manage_tenant", entitlements, roles);
        }

        // Rule 2: Support can impersonate users
        if (hasRole(roles, "support") && action.equals("impersonate")) {
            return PolicyDecision.allow("support_can_impersonate_user", entitlements, roles);
        }

        // Rule 3: User with entitlement can access product
        if (action.equals("access") && resource != null && hasEntitlementForResource(entitlements, resource)) {
            return PolicyDecision.allow("user_entitlement_grants_access", entitlements, roles);
        }

        // Rule 4: Integration can write provisioning data
        if (hasRole(roles, "integration") && action.equals("provision")) {
            return PolicyDecision.allow("integration_can_write_provisioning", entitlements, roles);
        }

        // Default: deny
        log.info("Local policy DENIED: actor={}, action={}, resource={}, roles={}", actor, action, resource, roles);
        return PolicyDecision.deny("No matching policy rule for this request");
    }

    private boolean hasRole(List<String> roles, String expectedRole) {
        return roles != null && roles.stream()
            .anyMatch(r -> r.equalsIgnoreCase(expectedRole));
    }

    private boolean hasEntitlementForResource(List<String> entitlements, String resource) {
        if (entitlements == null || resource == null) {
            return false;
        }
        // Resource is expected as a slug or identifier matching an entitlement
        return entitlements.stream()
            .anyMatch(e -> e.equalsIgnoreCase(resource) || e.equalsIgnoreCase("entitlement:" + resource));
    }
}
