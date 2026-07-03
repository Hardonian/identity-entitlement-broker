# Runbook: Configure Local OIDC Provider (Keycloak)

## Purpose

Configure Keycloak as a local OIDC identity provider for development and testing of the Identity Entitlement Broker.

## Prerequisites

- Docker and Docker Compose installed
- Stack running: `docker compose up -d mariadb keycloak`
- Keycloak admin credentials from `.env`

## Step 1: Start Keycloak

```bash
# Start the full dev stack
cd /home/scott/identity-entitlement-broker
docker compose up -d mariadb keycloak

# Verify Keycloak is healthy
curl -s http://localhost:8080/health/ready
# Expected: {"status":"UP"}
```

## Step 2: Access Keycloak Admin Console

1. Open `http://localhost:8080` in your browser
2. Click "Administration Console"
3. Login with:
   - Username: `admin` (or `KC_BOOTSTRAP_ADMIN_USERNAME` value)
   - Password: `change-me-in-production` (or `KC_BOOTSTRAP_ADMIN_PASSWORD` value)

## Step 3: Create the Identity Broker Realm

Using the API (recommended for reproducibility):

```bash
# Login as admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=change-me-in-production" \
  -d "grant_type=password" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "Admin token: $ADMIN_TOKEN"

# Create realm
curl -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "identity-broker",
    "enabled": true,
    "displayName": "Identity Broker",
    "sslRequired": "external",
    "registrationAllowed": false,
    "loginWithEmailAllowed": true,
    "duplicateEmailsAllowed": false,
    "resetPasswordAllowed": true,
    "editUsernameAllowed": false
  }'

echo "Realm created: identity-broker"
```

## Step 4: Create the API Client

```bash
# Create client
CLIENT_RESP=$(curl -s -X POST http://localhost:8080/admin/realms/identity-broker/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "identity-broker-api",
    "name": "Identity Broker API",
    "enabled": true,
    "publicClient": false,
    "secret": "dev-client-secret",
    "redirectUris": ["http://localhost:8081/*", "http://localhost:5173/*"],
    "webOrigins": ["http://localhost:8081", "http://localhost:5173"],
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": false,
    "directAccessGrantsEnabled": true,
    "standardFlowEnabled": true
  }' -w "\n%{http_code}")

echo "Client created"
```

## Step 5: Configure Protocol Mappers

Add custom protocol mappers to include `tenant_id`, `roles`, and `entitlements` in tokens:

```bash
# Get client UUID
CLIENT_UUID=$(curl -s http://localhost:8080/admin/realms/identity-broker/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; clients=json.load(sys.stdin); print([c['id'] for c in clients if c['clientId']=='identity-broker-api'][0])")

echo "Client UUID: $CLIENT_UUID"

# Add tenant_id mapper (Hardcoded claim for dev)
curl -X POST http://localhost:8080/admin/realms/identity-broker/clients/$CLIENT_UUID/protocol-mappers/models \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "tenant_id",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-hardcoded-claim-mapper",
    "config": {
      "claim.name": "tenant_id",
      "claim.value": "00000000-0000-0000-0000-000000000001",
      "user.attribute": "tenant_id",
      "id.token.claim": true,
      "access.token.claim": true,
      "userinfo.token.claim": true
    }
  }'

# Add roles mapper (Hardcoded claim for dev)
curl -X POST http://localhost:8080/admin/realms/identity-broker/clients/$CLIENT_UUID/protocol-mappers/models \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "roles",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-hardcoded-claim-mapper",
    "config": {
      "claim.name": "roles",
      "claim.value": "admin,super-admin",
      "user.attribute": "roles",
      "id.token.claim": true,
      "access.token.claim": true,
      "userinfo.token.claim": true
    }
  }'

# Add entitlements mapper (Hardcoded claim for dev)
curl -X POST http://localhost:8080/admin/realms/identity-broker/clients/$CLIENT_UUID/protocol-mappers/models \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "entitlements",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-hardcoded-claim-mapper",
    "config": {
      "claim.name": "entitlements",
      "claim.value": "identity-admin,audit-viewer",
      "user.attribute": "entitlements",
      "id.token.claim": true,
      "access.token.claim": true,
      "userinfo.token.claim": true
    }
  }'
```

## Step 6: Create Test Users

```bash
# Create a test user
curl -X POST http://localhost:8080/admin/realms/identity-broker/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "devuser",
    "email": "devuser@example.com",
    "firstName": "Dev",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "devpassword",
      "temporary": false
    }]
  }'

echo "Test user created: devuser / devpassword"
```

## Step 7: Test Token Generation

```bash
# Request a token with direct access grant
TOKEN_RESP=$(curl -s -X POST http://localhost:8080/realms/identity-broker/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=identity-broker-api" \
  -d "client_secret=dev-client-secret" \
  -d "username=devuser" \
  -d "password=devpassword" \
  -d "grant_type=password")

# Extract and decode the access token
ACCESS_TOKEN=$(echo $TOKEN_RESP | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
echo "Access Token: $ACCESS_TOKEN"

# Decode the JWT payload
echo $ACCESS_TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | python3 -m json.tool

# Expected output should include:
# {
#   "tenant_id": "00000000-0000-0000-0000-000000000001",
#   "roles": "admin,super-admin",
#   "entitlements": "identity-admin,audit-viewer",
#   ...
# }
```

## Step 8: Test Against API

```bash
# Test health endpoint (no auth needed)
curl -s http://localhost:8081/health

# Test an authenticated endpoint
curl -s http://localhost:8081/api/v1/tenants \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Test with dev auth headers (if DEV_AUTH_ENABLED=true)
curl -s http://localhost:8081/api/v1/tenants \
  -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000001" \
  -H "X-Actor-Id: devuser"
```

## Troubleshooting

### Keycloak won't start

Check logs: `docker compose logs keycloak`

Common issues:
- MariaDB not ready: Ensure `depends_on` health check is working
- Database connection: Verify `KC_DB_URL` and credentials
- Port conflict: Ensure port 8080 is not already in use

### Token validation fails

- Verify the JWKS URI: `http://localhost:8080/realms/identity-broker/protocol/openid-connect/certs`
- Check that the client secret matches in Keycloak and API config
- Ensure the token has the correct `aud` (audience) claim

### Claims missing from token

- Verify protocol mappers are correctly configured
- Check that mappers are enabled for all token types (ID, access, userinfo)
- Hardcoded mappers may need the `access.token.claim` flag set to `true`
