package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.ExternalGroup;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExternalGroupRepository implements PanacheRepository<ExternalGroup> {

    public Optional<ExternalGroup> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
