package identity

# METADATA
# title: Identity Entitlement Broker Policy
# description: Policy rules for access control, tenant isolation, and entitlement management
# authors:
#   - Identity Broker Team
# entrypoint: true

import future.keywords.in
import future.keywords.if

# ---------------------------------------------------------------------------
# Default values
# ---------------------------------------------------------------------------
default allow = false
default deny = []
default matched_rule = ""

# ---------------------------------------------------------------------------
# Input schema (expected structure)
# ---------------------------------------------------------------------------
# input = {
#   "tenant_id": string,        # Tenant context UUID
#   "actor": string,            # User or service making the request
#   "subject": string,          # Target user (often same as actor for self-access)
#   "action": string,           # "access" | "read" | "manage" | "impersonate" | "provision"
#   "resource": string,         # Resource product slug (e.g., "identity-core")
#   "resource_type": string,    # Optional: "user" | "group" | "product" | "entitlement" | "policy"
#   "roles": array[string],     # Roles assigned to the actor within the tenant context
#   "entitlements": array[string],  # Effective entitlements the actor possesses
#   "tenant_roles": object,     # Optional: role mappings for the tenant
# }
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Tenant-level data (provided by data.tenants)
# ---------------------------------------------------------------------------
tenants := data.tenants

# Retrieve the current tenant's configuration
tenant_config := tenants[input.tenant_id]

# Check if the tenant exists and is active
tenant_active {
    tenant_config != null
    tenant_config.active == true
}

# ---------------------------------------------------------------------------
# Main allow rule
# ---------------------------------------------------------------------------
# allow is true if the tenant is active and any specific rule passes
allow {
    tenant_active
    allow_access
}

allow {
    tenant_active
    allow_manage
}

allow {
    tenant_active
    allow_impersonate
}

allow {
    tenant_active
    allow_provision
}

# ---------------------------------------------------------------------------
# Specific access rules
# ---------------------------------------------------------------------------

# allow_access: Users may access/read resources they have entitlement for
# METADATA
# description: >
#   Allows access to a resource if the user's roles grant entitlement
#   to that resource's product within the tenant context.
allow_access if {
    input.action == "access"
    role_has_entitlement
}

allow_access if {
    input.action == "read"
    role_has_entitlement
}

# allow_manage: Only super-admins can perform management actions
# METADATA
# description: >
#   Management actions (create, update, delete resources) require
#   the super-admin role within the tenant.
allow_manage if {
    input.action == "manage"
    input.roles[_] == "super-admin"
}

# allow_impersonate: Super-admins and support-admins may impersonate users
# METADATA
# description: >
#   Impersonation is permitted for super-admins and support-admins
#   to facilitate troubleshooting and delegated administration.
allow_impersonate if {
    input.action == "impersonate"
    input.roles[_] == "super-admin"
}

allow_impersonate if {
    input.action == "impersonate"
    input.roles[_] == "support-admin"
}

# allow_provision: SCIM and integration services may provision users
# METADATA
# description: >
#   Provisioning actions (create, update, deactivate users) require
#   super-admin or integration role.
allow_provision if {
    input.action == "provision"
    input.roles[_] == "super-admin"
}

allow_provision if {
    input.action == "provision"
    input.roles[_] == "integration"
}

# ---------------------------------------------------------------------------
# Entitlement verification
# ---------------------------------------------------------------------------

# role_has_entitlement: Check if any of the user's roles are mapped to
# entitlements that include access to the requested resource product.
# METADATA
# description: >
#   Iterates over each role assigned to the user and checks whether
#   that role has a mapping to an entitlement that covers the requested
#   resource product.
role_has_entitlement if {
    some role
    input.roles[_] == role

    # Look up role mappings for this tenant
    role_mapping := tenant_config.role_mappings[role]

    # Check if any entitlement for this role covers the requested resource
    some entitlement
    role_mapping.entitlements[_] == entitlement

    # The entitlement's product must match the requested resource
    entitlement_config := tenant_config.entitlements[entitlement]
    entitlement_config.product == input.resource
}

# ---------------------------------------------------------------------------
# Deny rules (explicit reasoning)
# ---------------------------------------------------------------------------

# Deny if the tenant is not found
deny contains msg if {
    tenant_config == null
    msg := sprintf("tenant '%s' not found or inactive", [input.tenant_id])
}

# Deny if the tenant is inactive
deny contains msg if {
    tenant_config != null
    tenant_config.active == false
    msg := sprintf("tenant '%s' is not active", [input.tenant_id])
}

# Deny if no specific allow rule matched
deny contains msg if {
    not allow
    msg := sprintf("no policy allows action '%s' on resource '%s' for actor '%s'", [input.action, input.resource, input.actor])
}

# Deny if the actor has no roles assigned
deny contains msg if {
    count(input.roles) == 0
    msg := sprintf("actor '%s' has no roles assigned in tenant '%s'", [input.actor, input.tenant_id])
}

# Deny if the action is unknown
deny contains msg if {
    input.action not in {"access", "read", "manage", "impersonate", "provision"}
    msg := sprintf("unknown action '%s'", [input.action])
}

# Deny impersonation without required role
deny contains msg if {
    input.action == "impersonate"
    not allow_impersonate
    msg := sprintf("actor '%s' is not authorized to impersonate in tenant '%s' (requires super-admin or support-admin)", [input.actor, input.tenant_id])
}

# Deny provisioning without required role
deny contains msg if {
    input.action == "provision"
    not allow_provision
    msg := sprintf("actor '%s' is not authorized to provision in tenant '%s' (requires super-admin or integration)", [input.actor, input.tenant_id])
}

# Deny management without super-admin
deny contains msg if {
    input.action == "manage"
    not allow_manage
    msg := sprintf("actor '%s' is not authorized to manage resources in tenant '%s' (requires super-admin)", [input.actor, input.tenant_id])
}

# Deny access/read when entitlement is missing
deny contains msg if {
    input.action in {"access", "read"}
    not role_has_entitlement
    msg := sprintf("actor '%s' lacks entitlement for resource '%s' in tenant '%s'", [input.actor, input.resource, input.tenant_id])
}

# ---------------------------------------------------------------------------
# Matched rule introspection
# ---------------------------------------------------------------------------

# matched_rule returns the name of the first matching allow rule for debugging
matched_rule := "allow_access" if { allow_access }
matched_rule := "allow_manage" if { allow_manage and not allow_access }
matched_rule := "allow_impersonate" if { allow_impersonate and not allow_access and not allow_manage }
matched_rule := "allow_provision" if { allow_provision and not allow_access and not allow_manage and not allow_impersonate }
matched_rule := "denied" if { not allow }
