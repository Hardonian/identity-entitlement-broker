package identity

import future.keywords.in

# ---------------------------------------------------------------------------
# OPA Unit Tests for Identity Entitlement Broker Policies
# ---------------------------------------------------------------------------

# Test: Standard access allowed for user with correct role and entitlement
test_allow_access if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "admin": {
                    "entitlements": ["identity-admin"]
                }
            },
            "entitlements": {
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}

# Test: Read action also allowed with proper entitlement
test_allow_read if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "read",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "admin": {
                    "entitlements": ["identity-admin"]
                }
            },
            "entitlements": {
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}

# Test: Deny access when no entitlement is present for the resource
test_deny_no_entitlement if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "bob@acme.com",
        "subject": "bob@acme.com",
        "action": "access",
        "resource": "premium-analytics",
        "roles": ["viewer"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "viewer": {
                    "entitlements": ["basic-view"]
                }
            },
            "entitlements": {
                "basic-view": {
                    "product": "dashboard-basic"
                },
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}

# Test: Verify deny produces correct reason for missing entitlement
test_deny_reason_missing_entitlement if {
    deny == ["actor 'bob@acme.com' lacks entitlement for resource 'premium-analytics' in tenant '00000000-0000-0000-0000-000000000001'"] with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "bob@acme.com",
        "subject": "bob@acme.com",
        "action": "access",
        "resource": "premium-analytics",
        "roles": ["viewer"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "viewer": {
                    "entitlements": ["basic-view"]
                }
            },
            "entitlements": {
                "basic-view": {
                    "product": "dashboard-basic"
                },
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}

# Test: Super-admin allowed for manage action
test_manage_admin if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "admin@acme.com",
        "subject": "admin@acme.com",
        "action": "manage",
        "resource": "identity-core",
        "roles": ["super-admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Non-super-admin denied for manage action
test_deny_manage_non_admin if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "user@acme.com",
        "subject": "user@acme.com",
        "action": "manage",
        "resource": "identity-core",
        "roles": ["viewer"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "viewer": {
                    "entitlements": ["basic-view"]
                }
            },
            "entitlements": {
                "basic-view": {
                    "product": "dashboard-basic"
                }
            }
        }
    }
}

# Test: Support-admin allowed for impersonation
test_impersonate_support if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "support@acme.com",
        "subject": "enduser@acme.com",
        "action": "impersonate",
        "resource": "user-data",
        "roles": ["support-admin"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Super-admin allowed for impersonation
test_impersonate_super_admin if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "admin@acme.com",
        "subject": "enduser@acme.com",
        "action": "impersonate",
        "resource": "user-data",
        "roles": ["super-admin"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Integration role allowed for provisioning
test_provision_integration if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "scim-service",
        "subject": "newuser@acme.com",
        "action": "provision",
        "resource": "user-provisioning",
        "roles": ["integration"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Super-admin also allowed for provisioning
test_provision_super_admin if {
    allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "admin@acme.com",
        "subject": "newuser@acme.com",
        "action": "provision",
        "resource": "user-provisioning",
        "roles": ["super-admin"],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Cross-tenant access denied (tenant B cannot access tenant A's data)
test_cross_tenant if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000002",
        "actor": "bob@evilcorp.com",
        "subject": "bob@evilcorp.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        # Tenant 002 only has role_mappings for identity-basic, not identity-core
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "admin": {
                    "entitlements": ["identity-admin"]
                }
            },
            "entitlements": {
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        },
        "00000000-0000-0000-0000-000000000002": {
            "active": true,
            "role_mappings": {
                "admin": {
                    "entitlements": ["identity-basic"]
                }
            },
            "entitlements": {
                "identity-basic": {
                    "product": "identity-basic"
                }
            }
        }
    }
}

# Test: Non-existent tenant denied
test_unknown_tenant if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-FAKE00000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Inactive tenant denied
test_inactive_tenant if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": false,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Empty roles denied for non-provisioning actions
test_empty_roles_denied if {
    deny contains "actor 'viewer@acme.com' has no roles assigned in tenant '00000000-0000-0000-0000-000000000001'" with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "viewer@acme.com",
        "subject": "viewer@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": [],
        "entitlements": []
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Unknown action denied
test_unknown_action if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "admin@acme.com",
        "subject": "admin@acme.com",
        "action": "delete-all",
        "resource": "identity-core",
        "roles": ["super-admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: matched_rule introspection for allowed access
test_matched_rule_access if {
    matched_rule == "allow_access" with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "admin": {
                    "entitlements": ["identity-admin"]
                }
            },
            "entitlements": {
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}

# Test: matched_rule introspection for denied
test_matched_rule_denied if {
    matched_rule == "denied" with input as {
        "tenant_id": "00000000-0000-0000-0000-FAKE00000001",
        "actor": "alice@acme.com",
        "subject": "alice@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["admin"],
        "entitlements": ["identity-admin"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {},
            "entitlements": {}
        }
    }
}

# Test: Cross-product access denied (user has admin but for different product)
test_wrong_product if {
    not allow with input as {
        "tenant_id": "00000000-0000-0000-0000-000000000001",
        "actor": "analyst@acme.com",
        "subject": "analyst@acme.com",
        "action": "access",
        "resource": "identity-core",
        "roles": ["analyst"],
        "entitlements": ["analytics-dash"]
    } with data.tenants as {
        "00000000-0000-0000-0000-000000000001": {
            "active": true,
            "role_mappings": {
                "analyst": {
                    "entitlements": ["analytics-dash"]
                }
            },
            "entitlements": {
                "analytics-dash": {
                    "product": "analytics-platform"
                },
                "identity-admin": {
                    "product": "identity-core"
                }
            }
        }
    }
}
