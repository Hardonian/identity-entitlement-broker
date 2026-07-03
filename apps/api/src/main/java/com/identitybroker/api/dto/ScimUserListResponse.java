package com.identitybroker.api.dto;

import java.util.List;

public class ScimUserListResponse {

    public List<String> schemas;
    public int totalResults;
    public int itemsPerPage;
    public int startIndex;
    public List<ScimUserResponse> Resources;

    public ScimUserListResponse() {
        this.schemas = List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse");
    }
}
