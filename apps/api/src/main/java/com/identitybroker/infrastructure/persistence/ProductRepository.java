package com.identitybroker.infrastructure.persistence;

import com.identitybroker.domain.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Optional<Product> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public Optional<Product> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }
}
