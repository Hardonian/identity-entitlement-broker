package com.identitybroker.api.dto;

import com.identitybroker.domain.ExternalUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ScimUserResponse {

    public List<String> schemas;
    public String id;
    public String externalId;
    public String userName;
    public Name name;
    public String email;
    public boolean active;
    public Meta meta;

    public static ScimUserResponse from(ExternalUser user) {
        ScimUserResponse r = new ScimUserResponse();
        r.schemas = List.of("urn:ietf:params:scim:schemas:core:2.0:User");
        r.id = user.getId().toString();
        r.externalId = user.getExternalId();
        r.userName = user.getUserName();
        r.name = new Name(user.getGivenName(), user.getFamilyName());
        r.email = user.getEmail();
        r.active = user.isActive();
        r.meta = new Meta(
            user.getCreatedAt().toString(),
            user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : user.getCreatedAt().toString(),
            "User"
        );
        return r;
    }

    public static class Name {
        public String givenName;
        public String familyName;

        public Name() {}
        public Name(String givenName, String familyName) {
            this.givenName = givenName;
            this.familyName = familyName;
        }
    }

    public static class Meta {
        public String created;
        public String lastModified;
        public String resourceType;

        public Meta() {}
        public Meta(String created, String lastModified, String resourceType) {
            this.created = created;
            this.lastModified = lastModified;
            this.resourceType = resourceType;
        }
    }
}
