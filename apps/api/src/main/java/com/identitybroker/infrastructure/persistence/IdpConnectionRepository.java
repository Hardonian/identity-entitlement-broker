package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.IdentityProviderConnection;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IdpConnectionRepository implements PanacheRepository<IdentityProviderConnection> {

    public List<IdentityProviderConnection> findByTenantId(UUID tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<IdentityProviderConnection> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
