package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.AuditEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for {@link AuditEvent} entities.
 *
 * <p>Uses PanacheRepository rather than extending PanacheEntity to avoid
 * base class conflicts with the UUID-based primary key pattern used across
 * this domain.
 */
@ApplicationScoped
public class AuditEventRepository implements PanacheRepository<AuditEvent> {

    /**
     * Find audit events by tenant ID with pagination.
     */
    public PanacheQueryWrapper findByTenantId(UUID tenantId, Page page) {
        return new PanacheQueryWrapper(
            find("tenantId = ?1 ORDER BY createdAt DESC", tenantId).page(page)
        );
    }

    /**
     * Find audit events by action with pagination.
     */
    public PanacheQueryWrapper findByAction(String action, Page page) {
        return new PanacheQueryWrapper(
            find("action = ?1 ORDER BY createdAt DESC", action).page(page)
        );
    }

    /**
     * Find audit events by resource type and resource ID (cross-tenant within same tenant context).
     */
    public List<AuditEvent> findByResource(String resourceType, String resourceId) {
        return list("resourceType = ?1 AND resourceId = ?2 ORDER BY createdAt DESC",
            resourceType, resourceId);
    }

    /**
     * Search audit events with optional filters and pagination.
     *
     * @param tenantId     the tenant ID (required)
     * @param action       filter by action (optional, ignored if null/blank)
     * @param resourceType filter by resource type (optional, ignored if null/blank)
     * @param actorId      filter by actor ID (optional, ignored if null/blank)
     * @param page         pagination configuration
     * @return a query wrapper with the result set
     */
    public PanacheQueryWrapper search(UUID tenantId, String action, String resourceType,
                                       String actorId, Page page) {
        // Simple query using named parameters (func requires it)
        StringBuilder query = new StringBuilder("tenantId = :tenantId");
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (action != null && !action.isBlank()) {
            query.append(" AND action = :action");
            params.put("action", action);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            query.append(" AND resourceType = :resourceType");
            params.put("resourceType", resourceType);
        }
        if (actorId != null && !actorId.isBlank()) {
            query.append(" AND actorId = :actorId");
            params.put("actorId", actorId);
        }

        query.append(" ORDER BY createdAt DESC");

        // Use the proper find method for named parameters
        return new PanacheQueryWrapper(
            find(query.toString(), params).page(page)
        );
    }

    /**
     * Wrapper around PanacheQuery that provides convenience methods
     * for paginated results without exposing the generic type chain.
     */
    public static class PanacheQueryWrapper {
        private final io.quarkus.hibernate.orm.panache.PanacheQuery<AuditEvent> query;

        PanacheQueryWrapper(io.quarkus.hibernate.orm.panache.PanacheQuery<AuditEvent> query) {
            this.query = query;
        }

        public List<AuditEvent> list() {
            return query.list();
        }

        public long count() {
            return query.count();
        }

        public int pageCount() {
            return query.pageCount();
        }
    }
}
