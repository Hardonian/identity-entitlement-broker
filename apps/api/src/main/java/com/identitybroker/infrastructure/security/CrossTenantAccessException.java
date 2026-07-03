package com.identitybroker.infrastructure.security;

import java.util.UUID;

/**
 * Exception thrown when a cross-tenant access violation is detected.
 * This occurs when a request attempts to access a resource belonging to
 * a tenant different from the one resolved in the request context.
 */
public class CrossTenantAccessException extends RuntimeException {

    private final UUID tenantId;
    private final String resourceId;

    public CrossTenantAccessException(UUID tenantId, String resourceId, String message) {
        super(message);
        this.tenantId = tenantId;
        this.resourceId = resourceId;
    }

    public CrossTenantAccessException(UUID tenantId, String resourceId) {
        this(tenantId, resourceId,
            "Cross-tenant access denied: tenant " + tenantId + " cannot access resource " + resourceId);
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getResourceId() {
        return resourceId;
    }
}
