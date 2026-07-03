# Runbook: Onboard an Enterprise Tenant

## Purpose

This runbook describes the step-by-step process for onboarding a new enterprise tenant to the Identity Entitlement Broker. It covers creating the tenant, registering the IdP, configuring role mappings, testing SSO, provisioning test users, and verifying entitlements.

## Prerequisites

- Access to the Identity Broker API (admin credentials with `super-admin` role)
- Enterprise IdP administrator access to configure the OIDC/SAML application
- Broker API URL: `https://api.identitybroker.example.com` (or `http://localhost:8081` for local)
- Tenant information: Company name, preferred slug, region, tier
- IdP information: Issuer URL, client ID, client secret, JWKS URI

## Step 1: Create the Tenant

Create the enterprise tenant with its configuration.

```bash
curl -X POST https://api.identitybroker.example.com/api/v1/tenants \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "metadata": {
      "region": "us-east-1",
      "tier": "enterprise",
      "contactEmail": "admin@acme.com"
    }
  }'
```

**Expected output:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "active": true,
  "created_at": "2025-06-30T12:00:00Z",
  "updated_at": "2025-06-30T12:00:00Z",
  "metadata": {
    "region": "us-east-1",
    "tier": "enterprise",
    "contactEmail": "admin@acme.com"
  }
}
```

**Save the tenant ID**: `TENANT_ID=a1b2c3d4-e5f6-7890-abcd-ef1234567890`

## Step 2: Register the Identity Provider

Register the enterprise's IdP connection.

### For OIDC Providers (Azure AD, Okta, Google Workspace)

```bash
curl -X POST https://api.identitybroker.example.com/api/v1/tenants/$TENANT_ID/idp \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "providerType": "OIDC",
    "issuer": "https://login.microsoftonline.com/acme-tenant/v2.0",
    "clientIdRef": "vault://acme/azure-ad-client-id",
    "secretRef": "vault://acme/azure-ad-client-secret",
    "jwksUri": "https://login.microsoftonline.com/acme-tenant/discovery/v2.0/keys",
    "ssoLoginUrl": "https://login.microsoftonline.com/acme-tenant/oauth2/v2.0/authorize",
    "claimMappings": {
      "sub": "externalId",
      "email": "email",
      "name": "displayName",
      "groups": "groupMembership"
    },
    "active": true
  }'
```

**Expected output:** IdP connection object with `id`, `issuer`, and `active: true`.

### For SAML Providers

```bash
curl -X POST https://api.identitybroker.example.com/api/v1/tenants/$TENANT_ID/idp \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "providerType": "SAML",
    "issuer": "https://idp.acme.com/saml2",
    "metadataUrl": "https://idp.acme.com/saml2/metadata",
    "attributeMappings": {
      "urn:oid:0.9.2342.19200300.100.1.3": "email",
      "urn:oid:2.5.4.42": "firstName",
      "urn:oid:2.5.4.4": "lastName",
      "urn:oid:1.3.6.1.4.1.5923.1.1.1.6": "groupMembership"
    },
    "active": true
  }'
```

## Step 3: Configure the IdP Application

On the enterprise IdP side, configure the callback/ACS URL:

- **OIDC Redirect URI**: `https://api.identitybroker.example.com/api/v1/auth/callback/$TENANT_ID`
- **SAML ACS URL**: `https://api.identitybroker.example.com/saml2/acs/$TENANT_ID`
- **Logout URL**: `https://api.identitybroker.example.com/api/v1/auth/logout`

Ensure the following claims/attributes are included in the token/assertion:
- `email` (or `upn`) - User's email address
- `groups` - User's group memberships (for role mapping)
- `name` or `given_name`/`family_name` - User display name

## Step 4: Configure Role Mappings

Map the enterprise's external groups to internal broker roles and entitlements.

```bash
# Map "IdentityAdministrators" group to admin role with identity-admin entitlement
curl -X POST https://api.identitybroker.example.com/api/v1/tenants/$TENANT_ID/role-mappings \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceType": "OIDC_CLAIM",
    "sourceValue": "groups",
    "externalRoleName": "IdentityAdministrators",
    "internalRoles": ["admin"],
    "entitlements": ["identity-admin"],
    "priority": 100,
    "active": true
  }'

# Map "AllEmployees" group to viewer role with basic-view entitlement
curl -X POST https://api.identitybroker.example.com/api/v1/tenants/$TENANT_ID/role-mappings \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceType": "OIDC_CLAIM",
    "sourceValue": "groups",
    "externalRoleName": "AllEmployees",
    "internalRoles": ["viewer"],
    "entitlements": ["basic-view"],
    "priority": 500,
    "active": true
  }'
```

**Expected output:** Role mapping objects with assigned IDs.

## Step 5: Test SSO

Verify that the SSO flow works end-to-end.

```bash
# Open in browser (or use a tool like oauth2c)
echo "Navigate to:"
echo "https://api.identitybroker.example.com/auth/login?tenant=acme-corp"
```

Steps to verify:
1. Browser redirects to enterprise IdP login page
2. Enter test user credentials (and MFA if configured)
3. After successful authentication, browser redirects back to the broker
4. Broker returns a JWT with tenant context, roles, and entitlements

Verify the JWT claims:

```bash
# If you have the JWT, decode and inspect it
echo "<jwt>" | cut -d. -f2 | base64 -d 2>/dev/null | python3 -m json.tool

# Example decoded JWT payload:
# {
#   "sub": "user-uuid",
#   "email": "testuser@acme.com",
#   "tenant_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
#   "roles": ["admin", "viewer"],
#   "entitlements": ["identity-admin", "basic-view"]
# }
```

## Step 6: Configure SCIM (Optional)

If the enterprise IdP supports SCIM provisioning, configure it to point at the broker:

- **SCIM Base URL**: `https://api.identitybroker.example.com/scim/v2`
- **SCIM Headers**: `X-Tenant-Id: $TENANT_ID`, `X-Actor-Id: <idp-name>`
- **Authentication**: Bearer token or API key (configured in broker admin settings)

## Step 7: Provision Test Users

Provision a test user via SCIM:

```bash
curl -X POST https://api.identitybroker.example.com/scim/v2/Users \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: admin" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "test.user@acme.com",
    "name": {"givenName": "Test", "familyName": "User"},
    "emails": [{"value": "test.user@acme.com", "type": "work", "primary": true}],
    "active": true
  }'
```

## Step 8: Verify Entitlements

Check that the test user has the correct effective entitlements:

```bash
# Get user ID from SCIM response
USER_ID="<user-uuid-from-scim-response>"

# Get effective entitlements
curl -X GET https://api.identitybroker.example.com/api/v1/assignments/user/$USER_ID \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: admin"
```

## Step 9: Test Policy Decisions

Verify policy decisions work correctly:

```bash
# Test allowed access
curl -X POST https://api.identitybroker.example.com/api/v1/policy/decide \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: admin" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "'$TENANT_ID'",
    "actor": "test.user@acme.com",
    "subject": "test.user@acme.com",
    "action": "access",
    "resource": "identity-core",
    "roles": ["admin"],
    "entitlements": ["identity-admin"]
  }'

# Expected: {"allowed": true, ...}
```

## Step 10: Verify Audit Logging

Check that audit events are being captured:

```bash
curl -X GET https://api.identitybroker.example.com/api/v1/audit \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Actor-Id: admin"

# Expected: Paginated list of audit events showing tenant creation,
# IdP registration, role mapping, and user provisioning
```

## Verification Checklist

- [ ] Tenant created and active
- [ ] IdP registered and responding
- [ ] Callback/ACS URL configured on IdP
- [ ] Claim mappings tested and verified
- [ ] Role mappings created for all relevant groups
- [ ] Test user SSO login successful
- [ ] JWT contains correct tenant_id, roles, and entitlements
- [ ] SCIM provisioning working (if applicable)
- [ ] Policy decisions correctly allow/deny access
- [ ] Audit events being recorded
- [ ] Cross-tenant access is denied
