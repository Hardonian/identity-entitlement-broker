package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.RoleMapping;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleMappingRepository implements PanacheRepository<RoleMapping> {

    public List<RoleMapping> findByTenantId(UUID tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<RoleMapping> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public Optional<RoleMapping> findBySource(UUID tenantId, String sourceType, String sourceValue) {
        return find("tenantId = ?1 and sourceType = ?2 and sourceValue = ?3", tenantId, sourceType, sourceValue).firstResultOptional();
    }
}
