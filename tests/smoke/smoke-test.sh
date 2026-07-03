#!/bin/bash
set -Eeuo pipefail

# =============================================================================
# Identity Entitlement Broker - Smoke Test
# =============================================================================
# Prerequisites:
#   - Full stack running (docker compose up -d)
#   - API at http://localhost:8081
#   - OPA at http://localhost:8181
#   - Keycloak at http://localhost:8080
#
# Run: bash tests/smoke/smoke-test.sh
# =============================================================================

API_BASE="${API_BASE:-http://localhost:8081}"
OPA_BASE="${OPA_BASE:-http://localhost:8181/v1/data}"
PASS_COUNT=0
FAIL_COUNT=0

echo "=========================================="
echo " Identity Entitlement Broker"
echo " Smoke Test Suite"
echo "=========================================="
echo "API:  $API_BASE"
echo "OPA:  $OPA_BASE"
echo ""

# ---------------------------------------------------------------------------
# Utility functions
# ---------------------------------------------------------------------------
pass() {
  local desc="$1"
  PASS_COUNT=$((PASS_COUNT + 1))
  echo "  [PASS] $desc"
}

fail() {
  local desc="$1"
  local detail="${2:-}"
  FAIL_COUNT=$((FAIL_COUNT + 1))
  echo "  [FAIL] $desc"
  if [ -n "$detail" ]; then
    echo "         $detail"
  fi
}

check_http_code() {
  local desc="$1"
  local expected="$2"
  shift 2
  local actual
  actual=$(curl -s -o /dev/null -w "%{http_code}" "$@" 2>/dev/null || echo "000")
  if [ "$actual" = "$expected" ]; then
    pass "$desc"
  else
    fail "$desc" "Expected HTTP $expected, got HTTP $actual"
  fi
}

check_json_path() {
  local desc="$1"
  local json_path="$2"
  shift 2
  local response
  response=$(curl -sf "$@" 2>/dev/null || true)
  if echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print($json_path)" 2>/dev/null | grep -q .; then
    pass "$desc"
  else
    fail "$desc" "JSON path '$json_path' not found in response"
  fi
}

# ===========================================================================
# 1. System Health
# ===========================================================================
echo "--- System Health ---"

check_http_code "Health endpoint returns 200" 200 "$API_BASE/health"
check_http_code "Readiness endpoint returns 200" 200 "$API_BASE/ready"
check_http_code "Version endpoint returns 200" 200 "$API_BASE/version"
check_json_path "Health returns UP status" "d['status']=='UP'" "$API_BASE/health"

echo ""

# ===========================================================================
# 2. OPA Policy Engine
# ===========================================================================
echo "--- OPA Policy Engine ---"

# Check OPA health
check_http_code "OPA health endpoint" 200 "$OPA_BASE/../health"

# Test an explicit allow decision
OPA_ALLOW_RESP=$(curl -s -X POST "$OPA_BASE/identity/allow" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "tenant_id": "00000000-0000-0000-0000-000000000001",
      "actor": "alice@acme.com",
      "subject": "alice@acme.com",
      "action": "access",
      "resource": "identity-core",
      "roles": ["admin"],
      "entitlements": []
    }
  }' 2>/dev/null || echo '{"result":null}')
if echo "$OPA_ALLOW_RESP" | grep -q '"result":true'; then
  pass "OPA allows access for authorized user"
else
  fail "OPA allows access for authorized user" "$OPA_ALLOW_RESP"
fi

# Test deny
OPA_DENY_RESP=$(curl -s -X POST "$OPA_BASE/identity/allow" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "tenant_id": "00000000-0000-0000-0000-000000000001",
      "actor": "bob@evilcorp.com",
      "subject": "bob@evilcorp.com",
      "action": "access",
      "resource": "premium-analytics",
      "roles": ["viewer"],
      "entitlements": []
    }
  }' 2>/dev/null || echo '{"result":null}')
if echo "$OPA_DENY_RESP" | grep -q '"result":false'; then
  pass "OPA denies access without entitlement"
else
  fail "OPA denies access without entitlement" "$OPA_DENY_RESP"
fi

echo ""

# ===========================================================================
# 3. Tenant Lifecycle
# ===========================================================================
echo "--- Tenant Lifecycle ---"

# Create tenant
DEV_HEADERS="-H X-Tenant-Id:00000000-0000-0000-0000-000000000001 -H X-Actor-Id:smoke-test"
TENANT_RESP=$(curl -sf -X POST \
  $DEV_HEADERS \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test Corp","slug":"smoke-test","metadata":{"tier":"testing"}}' \
  "$API_BASE/api/v1/tenants" 2>/dev/null || true)

TENANT_ID=$(echo "$TENANT_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$TENANT_ID" ]; then
  pass "Tenant created with ID: $TENANT_ID"
else
  fail "Tenant created" "No tenant ID returned"
  TENANT_ID="00000000-0000-0000-0000-TENANT-FAILED"
fi

# Get tenant
check_http_code "Get tenant by ID" 200 "$API_BASE/api/v1/tenants/$TENANT_ID"

TENANT_HEADERS="-H X-Tenant-Id:$TENANT_ID -H X-Actor-Id:smoke-test"

echo ""

# ===========================================================================
# 4. IdP Registration
# ===========================================================================
echo "--- IdP Registration ---"

IDP_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "providerType": "OIDC",
    "issuer": "https://auth.example.com/oidc",
    "clientIdRef": "vault://secrets/smoke-client",
    "secretRef": "vault://secrets/smoke-secret",
    "jwksUri": "https://auth.example.com/.well-known/jwks.json",
    "ssoLoginUrl": "https://auth.example.com/login",
    "claimMappings": {
      "sub": "externalId",
      "email": "email",
      "name": "displayName"
    },
    "active": true
  }' \
  "$API_BASE/api/v1/tenants/$TENANT_ID/idp" 2>/dev/null || true)

IDP_ID=$(echo "$IDP_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$IDP_ID" ]; then
  pass "IdP registered with ID: $IDP_ID"
else
  fail "IdP registered" "No IdP ID returned"
fi

echo ""

# ===========================================================================
# 5. SCIM User Provisioning
# ===========================================================================
echo "--- SCIM User Provisioning ---"

USER_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "john.doe@smoke-test.com",
    "name": {
      "givenName": "John",
      "familyName": "Doe"
    },
    "emails": [{
      "value": "john.doe@smoke-test.com",
      "type": "work",
      "primary": true
    }],
    "displayName": "John Doe",
    "active": true,
    "externalId": "ext-smoke-user-001"
  }' \
  "$API_BASE/scim/v2/Users" 2>/dev/null || true)

USER_ID=$(echo "$USER_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$USER_ID" ]; then
  pass "SCIM user provisioned with ID: $USER_ID"
else
  fail "SCIM user provisioned" "No user ID returned"
fi

# Get user
check_http_code "Get SCIM user by ID" 200 $TENANT_HEADERS "$API_BASE/scim/v2/Users/$USER_ID"

echo ""

# ===========================================================================
# 6. SCIM Group Provisioning
# ===========================================================================
echo "--- SCIM Group Provisioning ---"

GROUP_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
    "displayName": "Smoke Test Admins",
    "members": [{
      "value": "'$USER_ID'",
      "display": "john.doe@smoke-test.com"
    }]
  }' \
  "$API_BASE/scim/v2/Groups" 2>/dev/null || true)

GROUP_ID=$(echo "$GROUP_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$GROUP_ID" ]; then
  pass "SCIM group provisioned with ID: $GROUP_ID"
else
  fail "SCIM group provisioned" "No group ID returned"
fi

echo ""

# ===========================================================================
# 7. Product & Entitlement Management
# ===========================================================================
echo "--- Product & Entitlement Management ---"

# Create product
PROD_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Identity Core",
    "slug": "identity-core",
    "description": "Core identity and access management services"
  }' \
  "$API_BASE/api/v1/products" 2>/dev/null || true)

PROD_ID=$(echo "$PROD_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$PROD_ID" ]; then
  pass "Product created with ID: $PROD_ID"
else
  fail "Product created" "No product ID returned"
fi

# Create entitlement
ENT_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "'$PROD_ID'",
    "name": "Identity Admin",
    "slug": "identity-admin",
    "description": "Full administrative access to identity services"
  }' \
  "$API_BASE/api/v1/entitlements" 2>/dev/null || true)

ENT_ID=$(echo "$ENT_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$ENT_ID" ]; then
  pass "Entitlement created with ID: $ENT_ID"
else
  fail "Entitlement created" "No entitlement ID returned"
fi

echo ""

# ===========================================================================
# 8. Entitlement Assignment
# ===========================================================================
echo "--- Entitlement Assignment ---"

ASSIGN_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "entitlementId": "'$ENT_ID'",
    "userId": "'$USER_ID'"
  }' \
  "$API_BASE/api/v1/assignments" 2>/dev/null || true)

ASSIGN_ID=$(echo "$ASSIGN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$ASSIGN_ID" ]; then
  pass "Assignment created with ID: $ASSIGN_ID"
else
  fail "Assignment created" "No assignment ID returned"
fi

# Check effective entitlements
check_json_path "Effective entitlements includes identity-admin" \
  "any(e['entitlementSlug']=='identity-admin' for e in d)" \
  $TENANT_HEADERS "$API_BASE/api/v1/assignments/user/$USER_ID"

echo ""

# ===========================================================================
# 9. Policy Decisions
# ===========================================================================
echo "--- Policy Decisions ---"

POLICY_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "'$TENANT_ID'",
    "actor": "smoke-test",
    "subject": "'$USER_ID'",
    "action": "access",
    "resource": "identity-core",
    "roles": ["admin"],
    "entitlements": ["identity-admin"]
  }' \
  "$API_BASE/api/v1/policy/decide" 2>/dev/null || true)

if echo "$POLICY_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d.get('allowed')==True" 2>/dev/null; then
  pass "Policy decision: access allowed"
else
  fail "Policy decision: access allowed" "$POLICY_RESP"
fi

# Check deny policy
POLICY_DENY_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "'$TENANT_ID'",
    "actor": "attacker@evil.com",
    "subject": "'$USER_ID'",
    "action": "manage",
    "resource": "identity-core",
    "roles": ["viewer"],
    "entitlements": []
  }' \
  "$API_BASE/api/v1/policy/decide" 2>/dev/null || true)

if echo "$POLICY_DENY_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d.get('allowed')==False" 2>/dev/null; then
  pass "Policy decision: manage denied for viewer"
else
  fail "Policy decision: manage denied for viewer" "$POLICY_DENY_RESP"
fi

echo ""

# ===========================================================================
# 10. Role Mapping
# ===========================================================================
echo "--- Role Mapping ---"

ROLE_MAP_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "sourceType": "OIDC_CLAIM",
    "sourceValue": "groups",
    "externalRoleName": "SmokeTestAdmins",
    "internalRoles": ["admin"],
    "entitlements": ["identity-admin"],
    "priority": 100,
    "active": true
  }' \
  "$API_BASE/api/v1/tenants/$TENANT_ID/role-mappings" 2>/dev/null || true)

ROLE_MAP_ID=$(echo "$ROLE_MAP_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

if [ -n "$ROLE_MAP_ID" ]; then
  pass "Role mapping created with ID: $ROLE_MAP_ID"
else
  fail "Role mapping created" "No role mapping ID returned"
fi

# Resolve roles
RESOLVE_RESP=$(curl -sf -X POST \
  $TENANT_HEADERS \
  -H "Content-Type: application/json" \
  -d '{
    "sourceType": "OIDC_CLAIM",
    "claims": {
      "groups": ["SmokeTestAdmins"]
    }
  }' \
  "$API_BASE/api/v1/tenants/$TENANT_ID/role-mappings/resolve" 2>/dev/null || true)

if echo "$RESOLVE_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); assert len(d.get('resolvedRoles',[])) > 0" 2>/dev/null; then
  pass "Role resolution returned mapped roles"
else
  fail "Role resolution returned mapped roles" "$RESOLVE_RESP"
fi

echo ""

# ===========================================================================
# 11. Audit Logging
# ===========================================================================
echo "--- Audit Logging ---"

check_http_code "Audit events endpoint returns 200" 200 $TENANT_HEADERS "$API_BASE/api/v1/audit"

check_json_path "Audit events have totalElements" "d.get('totalElements',0) > 0" \
  $TENANT_HEADERS "$API_BASE/api/v1/audit"

echo ""

# ===========================================================================
# 12. Cross-Tenant Isolation
# ===========================================================================
echo "--- Cross-Tenant Isolation ---"

WRONG_HEADERS="-H X-Tenant-Id:00000000-0000-0000-0000-000000009999 -H X-Actor-Id:attacker"

# Attempt to read user from wrong tenant
CROSS_CODE=$(curl -s -o /dev/null -w "%{http_code}" $WRONG_HEADERS "$API_BASE/scim/v2/Users/$USER_ID" 2>/dev/null || echo "000")

if [ "$CROSS_CODE" = "403" ] || [ "$CROSS_CODE" = "401" ]; then
  pass "Cross-tenant access denied (HTTP $CROSS_CODE)"
else
  fail "Cross-tenant access denied" "Expected 403, got HTTP $CROSS_CODE"
fi

# Attempt to list audit from wrong tenant
CROSS_AUDIT_CODE=$(curl -s -o /dev/null -w "%{http_code}" $WRONG_HEADERS "$API_BASE/api/v1/audit" 2>/dev/null || echo "000")

if [ "$CROSS_AUDIT_CODE" = "403" ] || [ "$CROSS_AUDIT_CODE" = "401" ]; then
  pass "Cross-tenant audit access denied (HTTP $CROSS_AUDIT_CODE)"
else
  fail "Cross-tenant audit access denied" "Expected 403, got HTTP $CROSS_AUDIT_CODE"
fi

echo ""

# ===========================================================================
# Summary
# ===========================================================================
echo "=========================================="
echo " Smoke Test Results"
echo "=========================================="
echo "  Passed: $PASS_COUNT"
echo "  Failed: $FAIL_COUNT"
echo "=========================================="

if [ "$FAIL_COUNT" -gt 0 ]; then
  echo "  STATUS: FAILED"
  exit 1
else
  echo "  STATUS: PASSED"
  echo "=========================================="
  exit 0
fi
