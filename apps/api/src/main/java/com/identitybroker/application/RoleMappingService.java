package com.identitybroker.application;

import com.identitybroker.api.dto.RoleMappingRequest;
import com.identitybroker.api.dto.RoleMappingResponse;
import com.identitybroker.api.rest.exception.NotFoundException;
import com.identitybroker.domain.RoleMapping;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.persistence.RoleMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleMappingService {

    @Inject
    RoleMappingRepository roleMappingRepository;

    @Inject
    AuditService auditService;

    @Transactional
    public RoleMappingResponse create(UUID tenantId, RoleMappingRequest request, String actorId) {
        RoleMapping mapping = new RoleMapping();
        mapping.setTenantId(tenantId);
        mapping.setSourceType(request.sourceType);
        mapping.setSourceValue(request.sourceValue);
        mapping.setTargetRole(request.targetRole);
        mapping.setDescription(request.description);

        roleMappingRepository.persist(mapping);

        auditService.recordSuccess(tenantId, actorId, "rolemapping.create", "RoleMapping",
                mapping.getId().toString(), "Created role mapping: " + request.sourceType + " -> " + request.targetRole);

        return RoleMappingResponse.from(mapping);
    }

    public List<RoleMappingResponse> listByTenant(UUID tenantId) {
        return roleMappingRepository.findByTenantId(tenantId).stream()
                .map(RoleMappingResponse::from)
                .collect(Collectors.toList());
    }

    public RoleMappingResponse getById(UUID id) {
        RoleMapping mapping = roleMappingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role mapping not found: " + id));
        return RoleMappingResponse.from(mapping);
    }

    @Transactional
    public RoleMappingResponse update(UUID id, RoleMappingRequest request, String actorId) {
        RoleMapping mapping = roleMappingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role mapping not found: " + id));

        mapping.setSourceType(request.sourceType);
        mapping.setSourceValue(request.sourceValue);
        mapping.setTargetRole(request.targetRole);
        mapping.setDescription(request.description);
        roleMappingRepository.persist(mapping);

        auditService.recordSuccess(mapping.getTenantId(), actorId, "rolemapping.update", "RoleMapping",
                mapping.getId().toString(), "Updated role mapping");

        return RoleMappingResponse.from(mapping);
    }

    @Transactional
    public void delete(UUID id, String actorId) {
        RoleMapping mapping = roleMappingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role mapping not found: " + id));

        roleMappingRepository.delete(mapping);

        auditService.recordSuccess(mapping.getTenantId(), actorId, "rolemapping.delete", "RoleMapping",
                mapping.getId().toString(), "Deleted role mapping");
    }

    public String resolveRole(UUID tenantId, String sourceType, String sourceValue) {
        return roleMappingRepository.findBySource(tenantId, sourceType, sourceValue)
                .map(RoleMapping::getTargetRole)
                .orElse(null);
    }
}
