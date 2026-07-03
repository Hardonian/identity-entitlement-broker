package com.identitybroker.application;

import com.identitybroker.api.dto.CreateTenantRequest;
import com.identitybroker.api.dto.TenantResponse;
import com.identitybroker.api.rest.exception.ConflictException;
import com.identitybroker.api.rest.exception.NotFoundException;
import com.identitybroker.domain.AuditEvent;
import com.identitybroker.domain.Tenant;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.persistence.TenantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TenantService {

    @Inject
    TenantRepository tenantRepository;

    @Inject
    AuditService auditService;

    @Transactional
    public Tenant createTenant(CreateTenantRequest request, String actorId) {
        // Check slug uniqueness
        if (tenantRepository.findBySlug(request.slug).isPresent()) {
            throw new ConflictException("Tenant with slug '" + request.slug + "' already exists");
        }

        Tenant tenant = new Tenant(request.name, request.slug);
        tenantRepository.persist(tenant);

        auditService.recordSuccess(null, actorId, "tenant.create", "Tenant",
                tenant.getId().toString(), "Created tenant: " + tenant.getName());

        return tenant;
    }

    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
    }

    public Tenant getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Tenant not found with slug: " + slug));
    }

    public List<TenantResponse> listTenants() {
        return tenantRepository.listAll().stream()
                .map(TenantResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public Tenant updateTenant(UUID id, CreateTenantRequest request, String actorId) {
        Tenant tenant = getTenant(id);

        if (!tenant.getSlug().equals(request.slug) &&
                tenantRepository.findBySlug(request.slug).isPresent()) {
            throw new ConflictException("Tenant with slug '" + request.slug + "' already exists");
        }

        tenant.setName(request.name);
        tenant.setSlug(request.slug);
        tenantRepository.persist(tenant);

        auditService.recordSuccess(null, actorId, "tenant.update", "Tenant",
                tenant.getId().toString(), "Updated tenant: " + tenant.getName());

        return tenant;
    }

    @Transactional
    public void deleteTenant(UUID id, String actorId) {
        Tenant tenant = getTenant(id);
        tenant.setStatus(Tenant.TenantStatus.DISABLED);
        tenantRepository.persist(tenant);

        auditService.recordSuccess(null, actorId, "tenant.delete", "Tenant",
                tenant.getId().toString(), "Soft-deleted tenant: " + tenant.getName());
    }
}
