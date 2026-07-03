package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.ExternalUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExternalUserRepository implements PanacheRepository<ExternalUser> {

    public List<ExternalUser> findByTenantId(UUID tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<ExternalUser> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public Optional<ExternalUser> findByUserNameAndTenant(String userName, UUID tenantId) {
        return find("userName = ?1 and tenantId = ?2", userName, tenantId).firstResultOptional();
    }

    public long countByTenantId(UUID tenantId) {
        return count("tenantId", tenantId);
    }
}
