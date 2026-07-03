package com.identitybroker.infrastructure.security;

import com.identitybroker.domain.Tenant;

import java.util.UUID;

/**
 * Utility for validating tenant isolation — ensures that entities
 * accessed within a request belong to the expected tenant.
 */
public final class TenantIsolationValidator {

    private TenantIsolationValidator() {
        // utility class
    }

    /**
     * Validate that a tenant entity matches the expected tenant ID.
     *
     * @param tenant           the tenant entity to validate
     * @param expectedTenantId the tenant ID from the request context
     * @throws CrossTenantAccessException if the tenant does not match
     */
    public static void validate(Tenant tenant, UUID expectedTenantId) {
        if (tenant == null) {
            throw new CrossTenantAccessException(expectedTenantId, "null",
                "Tenant not found for ID: " + expectedTenantId);
        }
        if (!tenant.getId().equals(expectedTenantId)) {
            throw new CrossTenantAccessException(expectedTenantId, tenant.getId().toString(),
                "Cross-tenant access denied: expected tenant " + expectedTenantId
                    + " but entity belongs to tenant " + tenant.getId());
        }
    }

    /**
     * Validate that a tenant-aware entity's tenant matches the expected tenant ID.
     * Works with any entity that has a {@code getTenant()} method returning a {@link Tenant}.
     *
     * @param tenantAware       a tenant-aware entity
     * @param expectedTenantId  the tenant ID from the request context
     * @throws CrossTenantAccessException if the tenant does not match
     */
    public static void validate(TenantAware tenantAware, UUID expectedTenantId) {
        if (tenantAware == null) {
            return;
        }
        Tenant tenant = tenantAware.getTenant();
        if (tenant == null) {
            throw new CrossTenantAccessException(expectedTenantId, "unknown",
                "Entity has no associated tenant");
        }
        if (!tenant.getId().equals(expectedTenantId)) {
            throw new CrossTenantAccessException(expectedTenantId, tenant.getId().toString(),
                "Cross-tenant access denied: expected tenant " + expectedTenantId
                    + " but entity belongs to tenant " + tenant.getId());
        }
    }

    /**
     * Validate multiple tenant-aware entities in a single call.
     *
     * @param expectedTenantId the tenant ID from the request context
     * @param items            varargs of tenant-aware entities
     * @throws CrossTenantAccessException if any entity's tenant does not match
     */
    public static void validateAll(UUID expectedTenantId, TenantAware... items) {
        for (TenantAware item : items) {
            validate(item, expectedTenantId);
        }
    }

    /**
     * Interface for entities that have a tenant relationship, enabling
     * uniform validation without coupling to specific entity types.
     */
    public interface TenantAware {
        Tenant getTenant();
    }
}
