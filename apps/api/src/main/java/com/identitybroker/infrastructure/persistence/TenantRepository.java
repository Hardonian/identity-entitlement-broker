package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.Tenant;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TenantRepository implements PanacheRepository<Tenant> {

    public Optional<Tenant> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }

    public Optional<Tenant> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
