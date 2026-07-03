# Runbook: Revoke User Access

## Purpose

Emergency procedure to revoke a user's access to all Identity Entitlement Broker resources. This runbook covers immediate revocation, session termination, audit verification, and post-revocation review.

## Severity Levels

| Level | Description | Response Time |
|---|---|---|
| **P0** | Security incident, compromised account | Immediate (<5 minutes) |
| **P1** | Employee offboarding, role change | Within 1 hour |
| **P2** | Scheduled access removal | Next business day |
| **P3** | Access review finding | Within 1 week |

## Immediate Revocation (P0/P1)

### Step 1: Identify the User

```bash
# Find user by email or external ID
USER_ID=$(curl -s -X GET "http://localhost:8081/scim/v2/Users?filter=userName%20eq%20%22john.doe@acme.com%22" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin" \
  | python3 -c "import sys,json; r=json.load(sys.stdin); print(r['Resources'][0]['id'] if r.get('Resources') else '')")

echo "User ID: $USER_ID"
```

### Step 2: Deactivate the User (SCIM)

```bash
# Option A: PATCH to set active=false
curl -X PATCH http://localhost:8081/scim/v2/Users/$USER_ID \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
    "Operations": [{
      "op": "replace",
      "path": "active",
      "value": false
    }]
  }'

echo "User deactivated"

# Option B: DELETE (also deactivates)
curl -X DELETE http://localhost:8081/scim/v2/Users/$USER_ID \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin"

echo "User deleted (deactivated)"
```

### Step 3: Revoke All Entitlements

```bash
# List all active assignments for the user
ASSIGNMENTS=$(curl -s -X GET "http://localhost:8081/api/v1/assignments?userId=$USER_ID&size=100" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin")

echo "Revoking assignments..."
for ASSIGN_ID in $(echo $ASSIGNMENTS | python3 -c "import sys,json; d=json.load(sys.stdin); [print(c['id']) for c in d.get('content',[]) if c.get('active')]" 2>/dev/null); do
  curl -X DELETE "http://localhost:8081/api/v1/assignments/$ASSIGN_ID" \
    -H "X-Tenant-Id: <tenant-id>" \
    -H "X-Actor-Id: admin"
  echo "Revoked: $ASSIGN_ID"
done
```

### Step 4: Terminate Active Sessions

Terminate the user's active SSO sessions.

**For Keycloak-local sessions:**
```bash
# Get Keycloak admin token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=change-me-in-production" \
  -d "grant_type=password" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Logout the user from Keycloak
curl -X POST "http://localhost:8080/admin/realms/identity-broker/users/$USER_ID/logout" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

echo "User sessions terminated"
```

## Step 5: Verify Revocation

```bash
# Verify user is deactivated
curl -s http://localhost:8081/scim/v2/Users/$USER_ID \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Active: {d.get(\"active\")}')"

# Expected: Active: False

# Verify no effective entitlements
curl -s http://localhost:8081/api/v1/assignments/user/$USER_ID \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin"

# Expected: [] (empty array)

# Test policy decision for the revoked user
curl -s -X POST http://localhost:8081/api/v1/policy/decide \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "<tenant-id>",
    "actor": "john.doe@acme.com",
    "subject": "john.doe@acme.com",
    "action": "access",
    "resource": "identity-core",
    "roles": [],
    "entitlements": []
  }'

# Expected: {"allowed": false, "denyReasons": [...]}
```

## Step 6: Audit Review

```bash
# Check audit events for the revocation
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: admin" \
  -H "Content-Type: application/json" \
  -d '{
    "actorIds": ["admin"],
    "targetIds": ["'$USER_ID'"],
    "eventTypes": ["USER_DEACTIVATED", "ENTITLEMENT_REVOKED"],
    "size": 20
  }' | python3 -m json.tool

# Verify the following events exist:
# - USER_DEACTIVATED (user deactivation)
# - ENTITLEMENT_REVOKED (for each revoked entitlement)
```

## Automated Script

Save the following as `revoke-access.sh`:

```bash
#!/bin/bash
# Usage: ./revoke-access.sh <tenant-id> <user-email> <actor-id>

set -euo pipefail

API="${API:-http://localhost:8081}"
TENANT_ID="$1"
USER_EMAIL="$2"
ACTOR_ID="${3:-admin}"

echo "=== Revoking access for $USER_EMAIL ==="

# Find user
USER_ID=$(curl -s -G "$API/scim/v2/Users" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: $ACTOR_ID" \
  --data-urlencode "filter=userName eq \"$USER_EMAIL\"" \
  | python3 -c "import sys,json; r=json.load(sys.stdin); print(r['Resources'][0]['id'])" 2>/dev/null || true)

if [ -z "$USER_ID" ]; then
  echo "ERROR: User $USER_EMAIL not found"
  exit 1
fi

echo "User ID: $USER_ID"

# Deactivate
echo "Deactivating user..."
curl -s -X PATCH "$API/scim/v2/Users/$USER_ID" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: $ACTOR_ID" \
  -H "Content-Type: application/scim+json" \
  -d '{"schemas":["urn:ietf:params:scim:api:messages:2.0:PatchOp"],"Operations":[{"op":"replace","path":"active","value":false}]}' \
  > /dev/null

# Revoke entitlements
echo "Revoking entitlements..."
ASSIGNMENTS=$(curl -s "$API/api/v1/assignments?userId=$USER_ID&size=100" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: $ACTOR_ID")

for ASSIGN_ID in $(echo "$ASSIGNMENTS" | python3 -c "import sys,json; d=json.load(sys.stdin); [print(c['id']) for c in d.get('content',[]) if c.get('active')]" 2>/dev/null); do
  curl -s -X DELETE "$API/api/v1/assignments/$ASSIGN_ID" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Actor-Id: $ACTOR_ID" > /dev/null
  echo "  Revoked assignment: $ASSIGN_ID"
done

echo "=== Access revoked successfully ==="
```

## Post-Revocation Checklist

- [ ] User deactivated (active=false) in broker
- [ ] All direct entitlements revoked
- [ ] User removed from all groups (or groups deactivated)
- [ ] Active sessions terminated
- [ ] Audit events verified
- [ ] Policy decision returns denied for the user
- [ ] Incident response notified (if P0)
- [ ] Compliance officer notified (if required)
- [ ] Post-mortem scheduled (if security incident)
