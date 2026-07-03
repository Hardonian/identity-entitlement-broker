package com.identitybroker.application;

import com.identitybroker.api.dto.*;
import com.identitybroker.api.rest.exception.NotFoundException;
import com.identitybroker.domain.*;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.persistence.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class EntitlementService {

    @Inject
    ProductRepository productRepository;

    @Inject
    EntitlementRepository entitlementRepository;

    @Inject
    EntitlementAssignmentRepository assignmentRepository;

    @Inject
    ExternalUserRepository externalUserRepository;

    @Inject
    ExternalGroupRepository externalGroupRepository;

    @Inject
    AuditService auditService;

    @Transactional
    public Product createProduct(CreateProductRequest request, String actorId) {
        Product product = new Product();
        product.setName(request.name);
        product.setSlug(request.slug);
        product.setDescription(request.description);
        productRepository.persist(product);

        auditService.recordSuccess(null, actorId, "product.create", "Product",
                product.getId().toString(), "Created product: " + product.getName());

        return product;
    }

    public List<ProductResponse> listProducts() {
        return productRepository.listAll().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    @Transactional
    public Entitlement createEntitlement(CreateEntitlementRequest request, String actorId) {
        // Verify product exists
        if (productRepository.findById(request.productId).isEmpty()) {
            throw new NotFoundException("Product not found: " + request.productId);
        }

        Entitlement entitlement = new Entitlement();
        entitlement.setProductId(request.productId);
        entitlement.setName(request.name);
        entitlement.setSlug(request.slug);
        entitlement.setDescription(request.description);
        if (request.type != null) {
            entitlement.setType(Entitlement.EntitlementType.valueOf(request.type));
        }
        entitlementRepository.persist(entitlement);

        auditService.recordSuccess(null, actorId, "entitlement.create", "Entitlement",
                entitlement.getId().toString(), "Created entitlement: " + entitlement.getName());

        return entitlement;
    }

    public List<EntitlementResponse> listEntitlements() {
        return entitlementRepository.listAll().stream()
                .map(EntitlementResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EntitlementAssignment assign(AssignEntitlementRequest request, String actorId) {
        // Verify entitlement exists
        if (entitlementRepository.findById(request.entitlementId).isEmpty()) {
            throw new NotFoundException("Entitlement not found: " + request.entitlementId);
        }

        EntitlementAssignment assignment = new EntitlementAssignment();
        assignment.setTenantId(request.tenantId);
        assignment.setEntitlementId(request.entitlementId);
        assignment.setUserId(request.userId);
        assignment.setGroupId(request.groupId);
        assignment.setAssignedBy(actorId);
        assignment.setActive(true);

        assignmentRepository.persist(assignment);

        auditService.recordSuccess(request.tenantId, actorId, "entitlement.assign", "EntitlementAssignment",
                assignment.getId().toString(), "Assigned entitlement to " +
                        (request.userId != null ? "user:" + request.userId : "group:" + request.groupId));

        return assignment;
    }

    @Transactional
    public void revoke(UUID assignmentId, String actorId) {
        EntitlementAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found: " + assignmentId));

        assignment.setActive(false);
        assignmentRepository.persist(assignment);

        auditService.recordSuccess(assignment.getTenantId(), actorId, "entitlement.revoke", "EntitlementAssignment",
                assignment.getId().toString(), "Revoked entitlement assignment");
    }

    public List<EffectiveEntitlementResponse> getEffectiveEntitlements(UUID tenantId, UUID userId) {
        List<EffectiveEntitlementResponse> result = new ArrayList<>();

        // Direct user assignments
        List<EntitlementAssignment> directAssignments = assignmentRepository.findByUserId(tenantId, userId);
        for (EntitlementAssignment assignment : directAssignments) {
            entitlementRepository.findById(assignment.getEntitlementId()).ifPresent(ent -> {
                productRepository.findById(ent.getProductId()).ifPresent(prod -> {
                    result.add(new EffectiveEntitlementResponse(
                            ent.getId(), ent.getName(), ent.getSlug(),
                            prod.getId(), prod.getName(), prod.getSlug(),
                            "direct"
                    ));
                });
            });
        }

        // Group assignments (find groups the user belongs to, then get their entitlements)
        List<ExternalGroup> userGroups = externalGroupRepository.list("tenantId", tenantId);
        for (ExternalGroup group : userGroups) {
            List<EntitlementAssignment> groupAssignments = assignmentRepository.findByGroupId(tenantId, group.getId());
            for (EntitlementAssignment ga : groupAssignments) {
                // Check if already added via direct assignment
                boolean alreadyAdded = result.stream()
                        .anyMatch(e -> e.entitlementId.equals(ga.getEntitlementId()));
                if (!alreadyAdded) {
                    entitlementRepository.findById(ga.getEntitlementId()).ifPresent(ent -> {
                        productRepository.findById(ent.getProductId()).ifPresent(prod -> {
                            result.add(new EffectiveEntitlementResponse(
                                    ent.getId(), ent.getName(), ent.getSlug(),
                                    prod.getId(), prod.getName(), prod.getSlug(),
                                    "group"
                            ));
                        });
                    });
                }
            }
        }

        return result;
    }

    public List<EntitlementAssignmentResponse> listByGroup(UUID tenantId, UUID groupId) {
        return assignmentRepository.findByGroupId(tenantId, groupId).stream()
                .map(EntitlementAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    public List<EntitlementAssignmentResponse> listByTenant(UUID tenantId) {
        return assignmentRepository.findByTenantId(tenantId).stream()
                .map(EntitlementAssignmentResponse::from)
                .collect(Collectors.toList());
    }
}
