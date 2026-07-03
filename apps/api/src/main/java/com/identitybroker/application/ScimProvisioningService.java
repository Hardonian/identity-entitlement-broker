package com.identitybroker.application;

import com.identitybroker.api.dto.*;
import com.identitybroker.api.rest.exception.ConflictException;
import com.identitybroker.api.rest.exception.NotFoundException;
import com.identitybroker.domain.ExternalGroup;
import com.identitybroker.domain.ExternalUser;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.messaging.EventPublisher;
import com.identitybroker.infrastructure.persistence.ExternalGroupRepository;
import com.identitybroker.infrastructure.persistence.ExternalUserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScimProvisioningService {

    @Inject
    ExternalUserRepository externalUserRepository;

    @Inject
    ExternalGroupRepository externalGroupRepository;

    @Inject
    AuditService auditService;

    @Inject
    EventPublisher eventPublisher;

    @Transactional
    public ExternalUser createUser(UUID tenantId, ScimUserRequest request, String actorId) {
        // Check userName uniqueness within tenant
        if (externalUserRepository.findByUserNameAndTenant(request.userName, tenantId).isPresent()) {
            throw new ConflictException("User with userName '" + request.userName + "' already exists in this tenant");
        }

        ExternalUser user = new ExternalUser();
        user.setTenantId(tenantId);
        user.setUserName(request.userName);
        user.setExternalId(request.externalId != null ? request.externalId : UUID.randomUUID().toString());
        user.setGivenName(request.nameGiven);
        user.setFamilyName(request.nameFamily);
        user.setEmail(request.email);
        user.setActive(request.active != null ? request.active : true);

        externalUserRepository.persist(user);

        auditService.recordSuccess(tenantId, actorId, "scim.user.create", "ExternalUser",
                user.getId().toString(), "Created SCIM user: " + user.getUserName());

        eventPublisher.publishProvisioningEvent(tenantId.toString(), user.getId().toString(), "user.created");

        return user;
    }

    public ExternalUser getUser(UUID tenantId, UUID id) {
        ExternalUser user = externalUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (!user.getTenantId().equals(tenantId)) {
            throw new com.identitybroker.api.rest.exception.CrossTenantException("User does not belong to this tenant");
        }
        return user;
    }

    public ScimListResult listUsers(UUID tenantId, int count, int startIndex) {
        List<ExternalUser> all = externalUserRepository.findByTenantId(tenantId);
        int total = all.size();
        int fromIndex = Math.min(Math.max(startIndex - 1, 0), total);
        int toIndex = Math.min(fromIndex + count, total);
        List<ExternalUser> page = all.subList(fromIndex, toIndex);
        return new ScimListResult(page, total);
    }

    @Transactional
    public ExternalUser updateUser(UUID tenantId, UUID id, ScimUserRequest request, String actorId) {
        ExternalUser user = getUser(tenantId, id);

        if (request.userName != null && !request.userName.equals(user.getUserName())) {
            if (externalUserRepository.findByUserNameAndTenant(request.userName, tenantId).isPresent()) {
                throw new ConflictException("User with userName '" + request.userName + "' already exists");
            }
            user.setUserName(request.userName);
        }
        if (request.nameGiven != null) user.setGivenName(request.nameGiven);
        if (request.nameFamily != null) user.setFamilyName(request.nameFamily);
        if (request.email != null) user.setEmail(request.email);
        if (request.active != null) user.setActive(request.active);
        if (request.externalId != null) user.setExternalId(request.externalId);

        externalUserRepository.persist(user);

        auditService.recordSuccess(tenantId, actorId, "scim.user.update", "ExternalUser",
                user.getId().toString(), "Updated SCIM user: " + user.getUserName());

        eventPublisher.publishProvisioningEvent(tenantId.toString(), user.getId().toString(), "user.updated");

        return user;
    }

    @Transactional
    public void deleteUser(UUID tenantId, UUID id, String actorId) {
        ExternalUser user = getUser(tenantId, id);
        user.setActive(false);
        externalUserRepository.persist(user);

        auditService.recordSuccess(tenantId, actorId, "scim.user.delete", "ExternalUser",
                user.getId().toString(), "Deactivated SCIM user: " + user.getUserName());

        eventPublisher.publishProvisioningEvent(tenantId.toString(), user.getId().toString(), "user.deactivated");
    }

    @Transactional
    public ExternalGroup createGroup(UUID tenantId, ScimGroupRequest request, String actorId) {
        ExternalGroup group = new ExternalGroup();
        group.setTenantId(tenantId);
        group.setDisplayName(request.displayName);
        group.setExternalId(request.externalId != null ? request.externalId : UUID.randomUUID().toString());

        if (request.members != null && !request.members.isEmpty()) {
            group.setMembers(request.members.stream()
                    .map(m -> "{\"value\":\"" + m.value + "\",\"type\":\"" + (m.type != null ? m.type : "User") + "\"}")
                    .collect(Collectors.joining(",", "[", "]")));
        }

        externalGroupRepository.persist(group);

        auditService.recordSuccess(tenantId, actorId, "scim.group.create", "ExternalGroup",
                group.getId().toString(), "Created SCIM group: " + group.getDisplayName());

        eventPublisher.publishProvisioningEvent(tenantId.toString(), group.getId().toString(), "group.created");

        return group;
    }

    public ExternalGroup getGroup(UUID tenantId, UUID id) {
        ExternalGroup group = externalGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Group not found: " + id));
        if (!group.getTenantId().equals(tenantId)) {
            throw new com.identitybroker.api.rest.exception.CrossTenantException("Group does not belong to this tenant");
        }
        return group;
    }

    public ScimListResult listGroups(UUID tenantId, int count, int startIndex) {
        List<ExternalGroup> all = externalGroupRepository.list("tenantId", tenantId);
        int total = all.size();
        int fromIndex = Math.min(Math.max(startIndex - 1, 0), total);
        int toIndex = Math.min(fromIndex + count, total);
        List<ExternalGroup> page = all.subList(fromIndex, toIndex);
        return new ScimListResult(page, total);
    }

    @Transactional
    public ExternalGroup updateGroup(UUID tenantId, UUID id, ScimGroupRequest request, String actorId) {
        ExternalGroup group = getGroup(tenantId, id);

        if (request.displayName != null) group.setDisplayName(request.displayName);
        if (request.externalId != null) group.setExternalId(request.externalId);

        externalGroupRepository.persist(group);

        auditService.recordSuccess(tenantId, actorId, "scim.group.update", "ExternalGroup",
                group.getId().toString(), "Updated SCIM group: " + group.getDisplayName());

        return group;
    }

    @Transactional
    public void deleteGroup(UUID tenantId, UUID id, String actorId) {
        ExternalGroup group = getGroup(tenantId, id);
        externalGroupRepository.delete(group);

        auditService.recordSuccess(tenantId, actorId, "scim.group.delete", "ExternalGroup",
                group.getId().toString(), "Deleted SCIM group: " + group.getDisplayName());
    }

    public ScimUserResponse toScimUserResponse(ExternalUser user) {
        return ScimUserResponse.from(user);
    }

    public ScimGroupResponse toScimGroupResponse(ExternalGroup group) {
        return ScimGroupResponse.from(group);
    }

    public static class ScimListResult {
        public final List<?> items;
        public final int total;

        public ScimListResult(List<?> items, int total) {
            this.items = items;
            this.total = total;
        }
    }
}
