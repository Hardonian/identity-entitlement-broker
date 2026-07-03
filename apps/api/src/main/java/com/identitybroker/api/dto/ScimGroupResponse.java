package com.identitybroker.api.dto;

import com.identitybroker.domain.ExternalGroup;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ScimGroupResponse {

    public List<String> schemas;
    public String id;
    public String externalId;
    public String displayName;
    public List<Member> members;
    public Meta meta;

    public static ScimGroupResponse from(ExternalGroup group) {
        ScimGroupResponse r = new ScimGroupResponse();
        r.schemas = List.of("urn:ietf:params:scim:schemas:core:2.0:Group");
        r.id = group.getId().toString();
        r.externalId = group.getExternalId();
        r.displayName = group.getDisplayName();
        r.members = List.of();
        r.meta = new Meta(
            group.getCreatedAt().toString(),
            group.getUpdatedAt() != null ? group.getUpdatedAt().toString() : group.getCreatedAt().toString(),
            "Group"
        );
        return r;
    }

    public static class Member {
        public String value;
        public String type;
        public String display;

        public Member() {}
        public Member(String value, String type, String display) {
            this.value = value;
            this.type = type;
            this.display = display;
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
