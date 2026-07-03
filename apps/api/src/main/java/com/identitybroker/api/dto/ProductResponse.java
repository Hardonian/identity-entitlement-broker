package com.identitybroker.api.dto;

import com.identitybroker.domain.Product;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductResponse {

    public UUID id;
    public String name;
    public String slug;
    public String description;
    public boolean active;
    public LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        ProductResponse r = new ProductResponse();
        r.id = product.getId();
        r.name = product.getName();
        r.slug = product.getSlug();
        r.description = product.getDescription();
        r.active = product.isActive();
        r.createdAt = product.getCreatedAt();
        return r;
    }
}
