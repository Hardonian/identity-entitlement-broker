# Runbook: Respond to Unauthorized Access Incident

## Purpose

Incident response procedure for suspected or detected unauthorized access to the Identity Entitlement Broker. This runbook provides a systematic approach to detecting, containing, investigating, and recovering from access incidents.

## Incident Triage

### Detection Sources

Unauthorized access may be detected through:

1. **Audit log anomalies**: Unexpected `POLICY_DENIED` or `ACCESS_DENIED` events
2. **Security alerts**: SIEM alerts on unusual access patterns
3. **User reports**: Users reporting unexpected behavior or data exposure
4. **Compliance audit**: Internal or external audit findings
5. **Automated monitoring**: Policy decision success rate anomalies

### Initial Assessment

```bash
# Step 0: Gather initial information
echo "=== Incident Assessment ==="
echo "Time of detection: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"

# Check recent audit events for denials
echo "=== Recent Denial Events ==="
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <affected-tenant-id>" \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "eventTypes": ["POLICY_DENIED", "ACCESS_DENIED", "SSO_LOGIN_FAILED"],
    "from": "2025-06-30T00:00:00Z",
    "to": "2025-06-30T23:59:59Z",
    "size": 50
  }' | python3 -m json.tool
```

## Phase 1: Containment (First 15 Minutes)

### 1.1 Isolate the Affected Tenant

If the incident is isolated to a single tenant, temporarily suspend the tenant's IdP connections:

```bash
# Deactivate all IdP connections for the tenant
IDP_LIST=$(curl -s http://localhost:8081/api/v1/tenants/<tenant-id>/idp \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder")

for IDP_ID in $(echo "$IDP_LIST" | python3 -c "import sys,json; [print(i['id']) for i in json.load(sys.stdin)]" 2>/dev/null); do
  curl -X PUT "http://localhost:8081/api/v1/tenants/<tenant-id>/idp/$IDP_ID" \
    -H "X-Tenant-Id: <tenant-id>" \
    -H "X-Actor-Id: incident-responder" \
    -H "Content-Type: application/json" \
    -d '{"active": false}'
  echo "IdP $IDP_ID deactivated"
done
```

### 1.2 Revoke Compromised Credentials

If a specific user or service account is compromised:

```bash
# Follow the revoke-user-access runbook
bash docs/runbooks/revoke-access.sh <tenant-id> <compromised-email> incident-responder
```

### 1.3 Block the Actor

If the incident involves a specific actor (service account, API key), remove the actor's provisioning/integration role:

```bash
# Remove actor's role mappings
ROLE_MAPS=$(curl -s http://localhost:8081/api/v1/tenants/<tenant-id>/role-mappings \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder")

# Find and deactivate mappings that grant the actor access
# (This is tenant-specific and may require manual review)
```

## Phase 2: Investigation (15-60 Minutes)

### 2.1 Trace the Incident

```bash
# Search audit events for the suspected actor
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "actorIds": ["<suspected-actor>"],
    "from": "<incident-start-time>",
    "to": "<incident-end-time>",
    "size": 100,
    "sort": "timestamp,asc"
  }' | python3 -m json.tool

# Search for all actions by the actor across tenants (requires super-admin)
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "actorIds": ["<suspected-actor>"],
    "from": "<incident-start-time>",
    "to": "<incident-end-time>",
    "size": 200
  }' | python3 -m json.tool
```

### 2.2 Review Policy Decisions

```bash
# Check every policy decision made by or about the subject
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "eventTypes": ["POLICY_EVALUATED", "POLICY_DENIED"],
    "targetIds": ["<affected-user-id>"],
    "size": 100
  }' | python3 -m json.tool
```

### 2.3 Check Role Mapping Changes

```bash
# Check recent role mapping modifications
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "eventTypes": [
      "ROLE_MAPPING_CREATED",
      "ROLE_MAPPING_UPDATED",
      "ROLE_MAPPING_DELETED"
    ],
    "from": "<24-hours-before-incident>",
    "size": 50
  }' | python3 -m json.tool
```

## Phase 3: Evidence Preservation

### 3.1 Export Audit Events

```bash
# Export all relevant audit events to a file for preservation
curl -s -X POST http://localhost:8081/api/v1/audit/search \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder" \
  -H "Content-Type: application/json" \
  -d '{
    "actorIds": ["<suspected-actor>"],
    "from": "<72-hours-before-incident>",
    "to": "<now>",
    "size": 10000
  }' | python3 -m json.tool > /tmp/incident-evidence-$(date +%s).json

echo "Evidence exported to /tmp/incident-evidence-*.json"
```

### 3.2 Export OPA Decisions

```bash
# Export OPA decision logs (if available from OPA sidecar)
curl -s http://localhost:8181/v1/logs > /tmp/opa-decision-logs-$(date +%s).json 2>/dev/null || \
  echo "OPA decision logs not available (requires OPA verbose logging)"
```

### 3.3 Take Application Snapshots

```bash
# Export current state of relevant entities
curl -s http://localhost:8081/api/v1/tenants/<tenant-id>/role-mappings \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "X-Actor-Id: incident-responder" > /tmp/role-mappings-snapshot.json

echo "Snapshots saved"
```

## Phase 4: Recovery

### 4.1 Restore Access

After the incident is contained and investigated:

1. **Reactivate IdP connections** (if they were disabled)
2. **Restore role mappings** (if any were modified by the attacker)
3. **Re-enable user accounts** (if they were inappropriately deactivated)
4. **Verify no residual unauthorized access** remains
5. **Update credentials** for any compromised service accounts

### 4.2 Run Post-Incident Verification

```bash
# Run the full smoke test to verify system integrity
bash tests/smoke/smoke-test.sh
```

### 4.3 Verify Policy Integrity

```bash
# Run OPA policy tests to ensure policies are intact
opa test policies/opa -v
```

## Phase 5: Post-Mortem

### 5.1 Root Cause Analysis

Answer these questions:

1. **How was access obtained?** (Compromised credentials, misconfigured IdP, OPA policy bypass)
2. **What data was accessed?** (User records, entitlements, audit logs, system configuration)
3. **What was the blast radius?** (Single user, single tenant, multiple tenants)
4. **Why wasn't it detected earlier?** (Missing audit monitoring, delayed detection)
5. **What controls failed?** (Authentication, authorization, tenant isolation, logging)

### 5.2 Remediation Items

Based on findings, create remediation tickets:

| Priority | Item | Owner |
|---|---|---|
| P0 | Rotate all credentials for affected accounts | Security team |
| P0 | Patch vulnerability (if applicable) | Engineering |
| P1 | Add additional audit monitoring | Engineering |
| P1 | Update incident response runbook | Security team |
| P2 | Implement additional tenant isolation controls | Engineering |
| P2 | Security awareness training (if human error) | Management |

### 5.3 Update Security Controls

- Review and update OPA policies if they were insufficient
- Add additional audit event types if gaps were identified
- Implement additional detection rules in monitoring systems
- Update runbooks based on lessons learned

## Communication Template

```
Subject: Security Incident Report - [INCIDENT-XXX]

Summary:
- Date/Time: [when it happened]
- Duration: [how long it lasted]
- Affected Tenant(s): [tenant IDs or names]
- Affected User(s): [user IDs or emails]
- Incident Type: [Unauthorized access / Data exposure / Account compromise]

Containment:
- [Steps taken to contain the incident]

Root Cause:
- [What allowed it to happen]

Impact:
- [What data was potentially accessed or modified]
- [Number of records affected]

Remediation:
- [Steps taken to prevent recurrence]

Lessons Learned:
- [What the team learned]
```
