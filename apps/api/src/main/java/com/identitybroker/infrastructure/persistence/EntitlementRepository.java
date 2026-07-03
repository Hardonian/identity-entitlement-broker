package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.Entitlement;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EntitlementRepository implements PanacheRepository<Entitlement> {

    public Optional<Entitlement> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public List<Entitlement> findByProductId(UUID productId) {
        return list("productId", productId);
    }
}
