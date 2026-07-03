# Runbook: Debug SCIM Provisioning Failure

## Purpose

Troubleshoot and resolve SCIM provisioning failures in the Identity Entitlement Broker. This runbook provides a systematic approach to identifying the root cause when user or group provisioning via SCIM fails.

## Quick Checklist

- [ ] Is the tenant active and correctly identified?
- [ ] Is the provisioning actor authorized (has `provision` or `integration` role)?
- [ ] Is the SCIM payload valid JSON?
- [ ] Are required SCIM fields present?
- [ ] Does the `userName` already exist in the tenant?
- [ ] Are referenced groups present?
- [ ] Is OPA reachable and returning allow decisions?
- [ ] Is the database connection healthy?

## Step 1: Check Audit Logs

The first place to look is the audit log. Every SCIM operation generates an audit event, regardless of success or failure.

```bash
# List recent audit events for the tenant
curl -s -X GET "http://localhost:8081/api/v1/audit?size=50&sort=timestamp,desc" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" | python3 -m json.tool

# Filter by event type
curl -s -X GET "http://localhost:8081/api/v1/audit?size=50&eventType=USER_CREATED" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" | python3 -m json.tool

# Search for failed operations
curl -s -X POST "http://localhost:8081/api/v1/audit/search" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" \
  -H "Content-Type: application/json" \
  -d '{
    "eventTypes": ["ACCESS_DENIED", "POLICY_DENIED"],
    "size": 20
  }' | python3 -m json.tool
```

**What to look for:**
- `ACCESS_DENIED` or `POLICY_DENIED` events indicating authorization failures
- The `details` field of any error events for error messages
- The `actorId` of successful operations to verify the correct service identity

## Step 2: Verify Tenant Context

Ensure the request is correctly scoped to the intended tenant.

```bash
# Check tenant is active
curl -s http://localhost:8081/api/v1/tenants/<tenant-id> \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Active: {d.get(\"active\")}')"

# Expected: Active: True
```

**Common issues:**
- Using wrong `X-Tenant-Id` header value
- Tenant is deactivated or suspended
- Tenant UUID is malformed

## Step 3: Check Actor Authorization

The SCIM provisioning caller must have the appropriate role.

```bash
# Test the actor's provisioning permission via the policy endpoint
curl -s -X POST http://localhost:8081/api/v1/policy/decide \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "<tenant-id>",
    "actor": "<scim-actor-id>",
    "subject": "<scim-actor-id>",
    "action": "provision",
    "resource": "identity-core",
    "roles": ["<actor-roles>"],
    "entitlements": ["<actor-entitlements>"]
  }' | python3 -m json.tool
```

**Expected:** `"allowed": true`

**If denied:** The actor lacks the `integration` or `super-admin` role. Configure the actor's role mapping.

## Step 4: Test IdP Connection

If provisioning originates from an enterprise IdP, verify the IdP connection is active.

```bash
# List IdP connections for the tenant
curl -s http://localhost:8081/api/v1/tenants/<tenant-id>/idp \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" | python3 -m json.tool
```

**Verify:**
- At least one IdP connection is `active: true`
- The IdP type matches the provisioning protocol (OIDC or SAML)

## Step 5: Validate SCIM Payload

Check the SCIM JSON payload for compliance issues.

```bash
# Test by replaying the SCIM request (in verbose mode to see full response)
curl -v -X POST http://localhost:8081/scim/v2/Users \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: debugger" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "debug.user@example.com",
    "name": {
      "givenName": "Debug",
      "familyName": "User"
    },
    "emails": [
      {"value": "debug.user@example.com", "type": "work", "primary": true}
    ],
    "active": true
  }'
```

**Common payload issues:**

| Issue | Symptom | Resolution |
|---|---|---|
| Missing `schemas` | 400 Bad Request | Add `"schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"]` |
| Missing `userName` | 400 Bad Request | `userName` is required |
| Duplicate `userName` | 409 Conflict | User with this email already exists; use PATCH or check `active` status |
| Invalid email format | 400 Bad Request | Validate email format |
| Missing `name.givenName` | 400 Bad Request | `givenName` and `familyName` are required |
| Invalid `active` value | 400 Bad Request | Must be boolean `true` or `false` |

## Step 6: Check OPA Policy

Verify that OPA is reachable and returning correct decisions.

```bash
# Check OPA health
curl -s http://localhost:8181/health

# Check OPA policy directly
curl -s -X POST http://localhost:8181/v1/data/identity/allow \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "tenant_id": "<tenant-id>",
      "actor": "debugger",
      "subject": "debug.user@example.com",
      "action": "provision",
      "resource": "identity-core",
      "roles": ["integration"],
      "entitlements": ["identity-admin"]
    }
  }'

# Expected: {"result": true}
```

## Step 7: Check Database Connection

Verify the database is accessible and has the correct schema.

```bash
# Check API health which includes database check
curl -s http://localhost:8081/health | python3 -m json.tool

# Look for: database status should be "UP"
```

## Step 8: Check SCIM Schema Compliance

For enterprise IdPs that validate SCIM responses, verify the response structure:

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "userName": "debug.user@example.com",
  "name": {"givenName": "Debug", "familyName": "User"},
  "emails": [{"value": "debug.user@example.com", "type": "work", "primary": true}],
  "active": true,
  "meta": {
    "resourceType": "User",
    "created": "2025-06-30T12:00:00Z",
    "lastModified": "2025-06-30T12:00:00Z",
    "location": "http://localhost:8081/scim/v2/Users/c3d4e5f6-a7b8-9012-cdef-123456789012"
  }
}
```

**Verify:**
- `schemas` includes the SCIM core schema
- `meta.resourceType` is correct
- `meta.location` is a valid URL
- All requested fields are present in the response
- The `id` is a valid UUID

## Step 9: Enable Debug Logging

If the issue persists, enable debug logging in the API:

```bash
# Set log level to DEBUG (if supported by the runtime)
curl -X POST http://localhost:8081/q/admin/logger/org.identitybroker..level \
  -H "Content-Type: application/json" \
  -d '{"level": "DEBUG"}'

# Or set via environment and restart:
# QUARKUS_LOG_LEVEL=DEBUG
```

Then reproduce the provisioning failure and check the API logs:

```bash
docker compose logs api --tail=100
```

## Common Error Codes

| HTTP Status | Error Code | Meaning | Resolution |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | SCIM payload validation failed | Fix the SCIM payload per error details |
| 400 | `MISSING_REQUIRED_FIELD` | Required SCIM field missing | Add the missing field |
| 401 | `UNAUTHORIZED` | Missing or invalid auth | Check bearer token or dev auth headers |
| 403 | `FORBIDDEN` | Actor not authorized to provision | Assign `integration` or `super-admin` role to actor |
| 404 | `NOT_FOUND` | Tenant or referenced entity not found | Verify tenant ID and entity IDs |
| 409 | `CONFLICT` | Duplicate userName | Use PATCH to update existing user instead |
| 500 | `INTERNAL_ERROR` | Unexpected server error | Check server logs and database connectivity |
| 503 | `SERVICE_UNAVAILABLE` | OPA or database unavailable | Verify dependent services are healthy |
