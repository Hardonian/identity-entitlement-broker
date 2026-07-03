package com.identitybroker.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request-scoped tenant context providing tenant, actor, and correlation
 * information to downstream services during a single HTTP request.
 *
 * <p>Populated by {@link TenantContextFilter} at the start of each request
 * and cleared after request completion to prevent context leaking.
 */
@RequestScoped
@Getter
@Setter
public class TenantContext {

    private UUID tenantId;
    private String actorId;
    private String correlationId;

    /**
     * Reset all context fields. Should be called in a finally block or
     * after request completion to prevent context leaking between requests.
     */
    public void clear() {
        this.tenantId = null;
        this.actorId = null;
        this.correlationId = null;
    }

    /** Convenience alias for getTenantId() */
    public UUID getCurrentTenantId() {
        return tenantId;
    }

    /** Convenience alias for getActorId() */
    public String getCurrentActorId() {
        return actorId;
    }
}
