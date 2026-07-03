package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.EntitlementAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EntitlementAssignmentRepository implements PanacheRepository<EntitlementAssignment> {

    public Optional<EntitlementAssignment> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public List<EntitlementAssignment> findByTenantId(UUID tenantId) {
        return list("tenantId", tenantId);
    }

    public List<EntitlementAssignment> findByUserId(UUID tenantId, UUID userId) {
        return list("tenantId = ?1 and userId = ?2 and active = true", tenantId, userId);
    }

    public List<EntitlementAssignment> findByGroupId(UUID tenantId, UUID groupId) {
        return list("tenantId = ?1 and groupId = ?2 and active = true", tenantId, groupId);
    }
}
