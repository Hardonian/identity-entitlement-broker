import axios from 'axios'

const client = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — inject correlation, tenant, and actor headers
client.interceptors.request.use((config) => {
  // Generate UUID v4 for correlation id
  config.headers['X-Correlation-Id'] = crypto.randomUUID
    ? crypto.randomUUID()
    : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0
        const v = c === 'x' ? r : (r & 0x3) | 0x8
        return v.toString(16)
      })

  // Tenant ID from localStorage if present
  const tenantId = localStorage.getItem('active_tenant_id')
  if (tenantId) {
    config.headers['X-Tenant-Id'] = tenantId
  }

  // Actor ID placeholder (could be expanded to session)
  config.headers['X-Actor-Id'] = 'admin-console'

  return config
})

// Response interceptor — global error handling
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        console.warn('[API] Unauthorized — redirecting to login')
      }

      if (status === 403) {
        console.error('[API] Forbidden — insufficient permissions')
      }

      if (status >= 500) {
        console.error('[API] Server error —', status)
      }

      // Extract error message from response
      const message =
        data?.error ||
        data?.message ||
        data?.detail ||
        `Request failed with status ${status}`
      error.displayMessage = message
    } else if (error.request) {
      error.displayMessage = 'No response from server — check your connection'
    } else {
      error.displayMessage = error.message || 'An unexpected error occurred'
    }

    return Promise.reject(error)
  }
)

// =============================================================================
// Health & System
// =============================================================================

export function health() {
  return client.get('/health').then((r) => r.data)
}

export function getVersion() {
  return client.get('/version').then((r) => r.data)
}

// =============================================================================
// Tenants
// =============================================================================

export function getTenants() {
  return client.get('/api/tenants').then((r) => r.data)
}

export function getTenant(id) {
  return client.get(`/api/tenants/${id}`).then((r) => r.data)
}

export function createTenant(data) {
  return client.post('/api/tenants', data).then((r) => r.data)
}

export function updateTenant(id, data) {
  return client.put(`/api/tenants/${id}`, data).then((r) => r.data)
}

export function deleteTenant(id) {
  return client.delete(`/api/tenants/${id}`).then((r) => r.data)
}

// =============================================================================
// IdP Connections
// =============================================================================

export function getIdpConnections(tenantId) {
  const url = tenantId ? `/api/tenants/${tenantId}/idp` : '/api/idp'
  return client.get(url).then((r) => r.data)
}

export function getIdpConnection(id) {
  return client.get(`/api/idp/${id}`).then((r) => r.data)
}

export function createIdpConnection(tenantId, data) {
  return client.post(`/api/tenants/${tenantId}/idp`, data).then((r) => r.data)
}

export function updateIdpConnection(id, data) {
  return client.put(`/api/idp/${id}`, data).then((r) => r.data)
}

export function deleteIdpConnection(id) {
  return client.delete(`/api/idp/${id}`).then((r) => r.data)
}

// =============================================================================
// Users (SCIM)
// =============================================================================

export function getUsers(params) {
  return client.get('/scim/Users', { params }).then((r) => r.data)
}

export function getUser(id) {
  return client.get(`/scim/Users/${id}`).then((r) => r.data)
}

export function createUser(data) {
  return client.post('/scim/Users', data).then((r) => r.data)
}

export function updateUser(id, data) {
  return client.put(`/scim/Users/${id}`, data).then((r) => r.data)
}

export function deleteUser(id) {
  return client.delete(`/scim/Users/${id}`).then((r) => r.data)
}

// =============================================================================
// Groups (SCIM)
// =============================================================================

export function getGroups(params) {
  return client.get('/scim/Groups', { params }).then((r) => r.data)
}

export function getGroup(id) {
  return client.get(`/scim/Groups/${id}`).then((r) => r.data)
}

export function createGroup(data) {
  return client.post('/scim/Groups', data).then((r) => r.data)
}

export function updateGroup(id, data) {
  return client.put(`/scim/Groups/${id}`, data).then((r) => r.data)
}

export function deleteGroup(id) {
  return client.delete(`/scim/Groups/${id}`).then((r) => r.data)
}

// =============================================================================
// Role Mappings
// =============================================================================

export function getRoleMappings(tenantId) {
  const url = tenantId ? `/api/tenants/${tenantId}/role-mappings` : '/api/role-mappings'
  return client.get(url).then((r) => r.data)
}

export function createRoleMapping(tenantId, data) {
  return client.post(`/api/tenants/${tenantId}/role-mappings`, data).then((r) => r.data)
}

export function updateRoleMapping(id, data) {
  return client.put(`/api/role-mappings/${id}`, data).then((r) => r.data)
}

export function deleteRoleMapping(id) {
  return client.delete(`/api/role-mappings/${id}`).then((r) => r.data)
}

// =============================================================================
// Products & Entitlements
// =============================================================================

export function getProducts() {
  return client.get('/api/products').then((r) => r.data)
}

export function createProduct(data) {
  return client.post('/api/products', data).then((r) => r.data)
}

export function getEntitlements() {
  return client.get('/api/entitlements').then((r) => r.data)
}

export function createEntitlement(data) {
  return client.post('/api/entitlements', data).then((r) => r.data)
}

// =============================================================================
// Entitlement Assignments
// =============================================================================

export function assignEntitlement(data) {
  return client.post('/api/entitlements/assign', data).then((r) => r.data)
}

export function getEffectiveEntitlements(userId) {
  return client.get(`/api/entitlements/effective/${userId}`).then((r) => r.data)
}

export function revokeEntitlement(id) {
  return client.delete(`/api/entitlements/assignments/${id}`).then((r) => r.data)
}

// =============================================================================
// Policy Decision
// =============================================================================

export function decidePolicy(data) {
  return client.post('/api/policy/decide', data).then((r) => r.data)
}

export function checkAccess(data) {
  return client.post('/api/policy/check', data).then((r) => r.data)
}

// =============================================================================
// Audit
// =============================================================================

export function getAuditEvents(params) {
  return client.get('/api/audit', { params }).then((r) => r.data)
}

export function getAuditEvent(id) {
  return client.get(`/api/audit/${id}`).then((r) => r.data)
}

export function searchAudit(params) {
  return client.get('/api/audit/search', { params }).then((r) => r.data)
}

export default client
