package com.identitybroker.infrastructure.security;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Container request filter that extracts tenant context from the incoming
 * request and populates the {@link TenantContext} bean.
 *
 * <p>In production (DEV_AUTH_ENABLED=false), tenant context is resolved
 * from the JWT {@code tenant_id} claim via the SecurityIdentity.
 *
 * <p>In development mode (DEV_AUTH_ENABLED=true), the {@code X-Tenant-Id}
 * and {@code X-Actor-Id} headers are used instead, allowing direct API
 * testing without a full OIDC flow.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantContextFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
        "/health", "/ready", "/version", "/q/openapi", "/q/swagger-ui", "/q/dev"
    );

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "dev.auth.enabled", defaultValue = "false")
    boolean devAuthEnabled;

    @ConfigProperty(name = "dev.auth.default-tenant-id")
    String defaultTenantId;

    @ConfigProperty(name = "dev.auth.default-actor-id", defaultValue = "dev-admin")
    String defaultActorId;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String path = requestContext.getUriInfo().getPath();

        // Skip public paths
        if (isPublicPath(path)) {
            return;
        }

        UUID tenantId = null;
        String actorId = null;
        String correlationId = null;

        // Resolve tenant and actor
        if (devAuthEnabled) {
            // Development mode: read from headers
            String tenantHeader = requestContext.getHeaderString("X-Tenant-Id");
            String actorHeader = requestContext.getHeaderString("X-Actor-Id");

            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    tenantId = UUID.fromString(tenantHeader);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid X-Tenant-Id header value: {}", tenantHeader);
                }
            }
            if (tenantId == null) {
                try {
                    tenantId = UUID.fromString(defaultTenantId);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid dev.auth.default-tenant-id config value: {}", defaultTenantId);
                }
            }

            actorId = (actorHeader != null && !actorHeader.isBlank()) ? actorHeader : defaultActorId;

        } else {
            // Production mode: resolve from JWT / SecurityIdentity
            if (securityIdentity != null && securityIdentity.getPrincipal() != null) {
                String principal = securityIdentity.getPrincipal().getName();
                actorId = principal;

                // Extract tenant_id claim from JWT
                Object tenantClaim = securityIdentity.getAttribute("tenant_id");
                if (tenantClaim instanceof String) {
                    try {
                        tenantId = UUID.fromString((String) tenantClaim);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid tenant_id claim in JWT: {}", tenantClaim);
                    }
                } else if (tenantClaim != null) {
                    tenantId = UUID.fromString(tenantClaim.toString());
                }
            }
        }

        // Reject if no tenant context resolved
        if (tenantId == null) {
            log.warn("No tenant context resolved for request to {}", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"No tenant context could be resolved. Ensure a valid tenant_id is provided.\"}")
                    .build()
            );
            return;
        }

        // Resolve correlation ID
        correlationId = requestContext.getHeaderString("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated correlation ID {} for request to {}", correlationId, path);
        }

        // Set the tenant context
        tenantContext.setTenantId(tenantId);
        tenantContext.setActorId(actorId);
        tenantContext.setCorrelationId(correlationId);

        log.debug("Tenant context set: tenantId={}, actorId={}, correlationId={}", tenantId, actorId, correlationId);
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }
        // Also skip the root path
        return path.equals("/") || path.equals("");
    }
}
