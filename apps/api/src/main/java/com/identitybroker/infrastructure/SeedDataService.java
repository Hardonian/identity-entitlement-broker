package com.identitybroker.infrastructure;

import com.identitybroker.domain.*;
import com.identitybroker.infrastructure.persistence.*;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
@Startup
public class SeedDataService {

    @Inject
    TenantRepository tenantRepository;

    @Inject
    IdpConnectionRepository idpConnectionRepository;

    @Inject
    ExternalUserRepository externalUserRepository;

    @Inject
    ExternalGroupRepository externalGroupRepository;

    @Inject
    RoleMappingRepository roleMappingRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    EntitlementRepository entitlementRepository;

    @Inject
    EntitlementAssignmentRepository assignmentRepository;

    @Inject
    AuditEventRepository auditEventRepository;

    private boolean seeded = false;

    @PostConstruct
    @Transactional
    public void seed() {
        if (seeded || tenantRepository.count() > 0) {
            return;
        }
        seeded = true;

        // ----- Tenants -----
        Tenant tenant1 = new Tenant("Acme Corporation", "acme-corp");
        tenant1.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantRepository.persist(tenant1);

        Tenant tenant2 = new Tenant("Globex Industries", "globex-inc");
        tenant2.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantRepository.persist(tenant2);

        // ----- IdP Connections -----
        IdentityProviderConnection idp1 = new IdentityProviderConnection();
        idp1.setTenantId(tenant1.getId());
        idp1.setProviderType(IdentityProviderConnection.ProviderType.OIDC);
        idp1.setIssuer("https://auth.acme.com/oidc");
        idp1.setMetadataUrl("https://auth.acme.com/oidc/.well-known/openid-configuration");
        idp1.setClientId("client-acme-ref");
        idp1.setSecretRef("vault://idp/acme-oidc-secret");
        idp1.setStatus(IdentityProviderConnection.IdpStatus.ACTIVE);
        idpConnectionRepository.persist(idp1);

        IdentityProviderConnection idp2 = new IdentityProviderConnection();
        idp2.setTenantId(tenant2.getId());
        idp2.setProviderType(IdentityProviderConnection.ProviderType.SAML);
        idp2.setIssuer("https://auth.globex.com/saml");
        idp2.setMetadataUrl("https://auth.globex.com/saml/metadata");
        idp2.setClientId("client-globex-ref");
        idp2.setSecretRef("vault://idp/globex-saml-secret");
        idp2.setStatus(IdentityProviderConnection.IdpStatus.ACTIVE);
        idpConnectionRepository.persist(idp2);

        // ----- Users (Tenant 1) -----
        ExternalUser user1 = createUser(tenant1, "jdoe", "John", "Doe", "jdoe-ext-001", "john.doe@acme.com");
        ExternalUser user2 = createUser(tenant1, "asmith", "Alice", "Smith", "asmith-ext-001", "alice.smith@acme.com");
        ExternalUser user3 = createUser(tenant1, "bwayne", "Bruce", "Wayne", "bwayne-ext-001", "bruce.wayne@acme.com");

        // ----- Users (Tenant 2) -----
        ExternalUser user4 = createUser(tenant2, "pparker", "Peter", "Parker", "pparker-ext-001", "peter.parker@globex.com");
        ExternalUser user5 = createUser(tenant2, "mstark", "Mary", "Stark", "mstark-ext-001", "mary.stark@globex.com");
        ExternalUser user6 = createUser(tenant2, "tchalla", "T'Challa", "Udaku", "tchalla-ext-001", "tchalla@globex.com");

        // ----- Groups -----
        ExternalGroup engGroupT1 = createGroup(tenant1, "Engineering", "eng-group-ext-acme");
        ExternalGroup adminGroupT1 = createGroup(tenant1, "Admin", "admin-group-ext-acme");
        ExternalGroup opsGroupT2 = createGroup(tenant2, "Operations", "ops-group-ext-globex");

        // ----- Role Mappings -----
        createRoleMapping(tenant1, "OIDC_CLAIM", "admin", "super-admin");
        createRoleMapping(tenant1, "SAML_ATTRIBUTE", "engineer", "developer");
        createRoleMapping(tenant2, "SAML_ATTRIBUTE", "operator", "ops-user");
        createRoleMapping(tenant2, "OIDC_CLAIM", "manager", "team-lead");

        // ----- Products -----
        Product product1 = createProduct("Identity Core", "identity-core", "Core identity management platform");
        Product product2 = createProduct("Access Manager", "access-manager", "Access control and policy management");
        Product product3 = createProduct("Audit Trail", "audit-trail", "Comprehensive audit and compliance logging");

        // ----- Entitlements -----
        Entitlement ent1 = createEntitlement(product1.getId(), "SSO Access", "sso-access",
                "Single sign-on access");
        Entitlement ent2 = createEntitlement(product1.getId(), "MFA Enforcement", "mfa-enforcement",
                "Multi-factor authentication enforcement");
        Entitlement ent3 = createEntitlement(product1.getId(), "User Provisioning", "user-provisioning",
                "User provisioning API access");
        Entitlement ent4 = createEntitlement(product2.getId(), "Policy Management", "policy-management",
                "Create and manage access policies");
        Entitlement ent5 = createEntitlement(product2.getId(), "Role Management", "role-management",
                "Create and manage roles");
        Entitlement ent6 = createEntitlement(product3.getId(), "Audit Export", "audit-export",
                "Export audit logs");
        Entitlement ent7 = createEntitlement(product3.getId(), "Compliance Reports", "compliance-reports",
                "Generate compliance reports");

        // ----- Entitlement Assignments -----
        assignEntitlement(tenant1.getId(), ent1.getId(), user1.getId(), null, "system");
        assignEntitlement(tenant1.getId(), ent2.getId(), user1.getId(), null, "system");
        assignEntitlement(tenant1.getId(), ent3.getId(), null, engGroupT1.getId(), "system");
        assignEntitlement(tenant1.getId(), ent4.getId(), null, adminGroupT1.getId(), "system");
        assignEntitlement(tenant1.getId(), ent5.getId(), user2.getId(), null, "system");
        assignEntitlement(tenant2.getId(), ent1.getId(), user4.getId(), null, "system");
        assignEntitlement(tenant2.getId(), ent6.getId(), null, opsGroupT2.getId(), "system");
        assignEntitlement(tenant2.getId(), ent7.getId(), user5.getId(), null, "system");

        // ----- Audit Events -----
        createAuditEvent(tenant1.getId(), "system", "tenant.create", "Tenant", tenant1.getId().toString(),
                "SUCCESS", "{\"name\":\"Acme Corporation\"}");
        createAuditEvent(tenant2.getId(), "system", "tenant.create", "Tenant", tenant2.getId().toString(),
                "SUCCESS", "{\"name\":\"Globex Industries\"}");
        createAuditEvent(tenant1.getId(), "system", "idp.register", "IdentityProviderConnection", idp1.getId().toString(),
                "SUCCESS", "{\"providerType\":\"OIDC\"}");
        createAuditEvent(tenant1.getId(), "system", "scim.user.create", "ExternalUser", user1.getId().toString(),
                "SUCCESS", "{\"userName\":\"jdoe\"}");
        createAuditEvent(tenant1.getId(), "system", "scim.user.create", "ExternalUser", user2.getId().toString(),
                "SUCCESS", "{\"userName\":\"asmith\"}");
        createAuditEvent(tenant1.getId(), "system", "scim.group.create", "ExternalGroup", engGroupT1.getId().toString(),
                "SUCCESS", "{\"displayName\":\"Engineering\"}");
        createAuditEvent(tenant1.getId(), "system", "entitlement.assign", "EntitlementAssignment", null,
                "SUCCESS", "{\"entitlement\":\"sso-access\",\"user\":\"jdoe\"}");
        createAuditEvent(tenant2.getId(), "system", "policy.decide", "policy", "access:identity-core",
                "SUCCESS", "{\"allowed\":true}");
    }

    private ExternalUser createUser(Tenant tenant, String userName, String givenName,
                                     String familyName, String externalId, String email) {
        ExternalUser user = new ExternalUser();
        user.setTenantId(tenant.getId());
        user.setUserName(userName);
        user.setGivenName(givenName);
        user.setFamilyName(familyName);
        user.setExternalId(externalId);
        user.setEmail(email);
        user.setActive(true);
        externalUserRepository.persist(user);
        return user;
    }

    private ExternalGroup createGroup(Tenant tenant, String displayName, String externalId) {
        ExternalGroup group = new ExternalGroup();
        group.setTenantId(tenant.getId());
        group.setDisplayName(displayName);
        group.setExternalId(externalId);
        externalGroupRepository.persist(group);
        return group;
    }

    private void createRoleMapping(Tenant tenant, String sourceType,
                                    String sourceValue, String targetRole) {
        RoleMapping mapping = new RoleMapping();
        mapping.setTenantId(tenant.getId());
        mapping.setSourceType(sourceType);
        mapping.setSourceValue(sourceValue);
        mapping.setTargetRole(targetRole);
        roleMappingRepository.persist(mapping);
    }

    private Product createProduct(String name, String slug, String description) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setActive(true);
        productRepository.persist(product);
        return product;
    }

    private Entitlement createEntitlement(UUID productId, String name, String slug,
                                           String description) {
        Entitlement entitlement = new Entitlement();
        entitlement.setProductId(productId);
        entitlement.setName(name);
        entitlement.setSlug(slug);
        entitlement.setDescription(description);
        entitlementRepository.persist(entitlement);
        return entitlement;
    }

    private void assignEntitlement(UUID tenantId, UUID entitlementId, UUID userId, UUID groupId, String assignedBy) {
        EntitlementAssignment assignment = new EntitlementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setEntitlementId(entitlementId);
        assignment.setUserId(userId);
        assignment.setGroupId(groupId);
        assignment.setAssignedBy(assignedBy);
        assignment.setActive(true);
        assignmentRepository.persist(assignment);
    }

    private void createAuditEvent(UUID tenantId, String actorId, String action, String resourceType,
                                   String resourceId, String outcome, String details) {
        AuditEvent event = new AuditEvent(tenantId, actorId, action, resourceType,
                resourceId, outcome, details);
        auditEventRepository.persist(event);
    }
}
