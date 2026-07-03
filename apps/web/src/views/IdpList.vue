<script setup>
import { ref, onMounted } from 'vue'
import { getIdpConnections, getTenants, createIdpConnection, updateIdpConnection, deleteIdpConnection } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const { success, error: notifyError } = useNotification()

const connections = ref([])
const tenants = ref([])
const loading = ref(true)
const error = ref('')

// Form modal
const showForm = ref(false)
const editingId = ref(null)
const form = ref({ tenant_id: '', provider_type: 'oidc', issuer: '', client_id: '', client_secret: '' })
const saving = ref(false)

const columns = [
  { key: 'provider_type', label: 'Provider', sortable: true },
  {
    key: 'issuer', label: 'Issuer', sortable: true,
    format: (v) => v && v.length > 45 ? v.substring(0, 45) + '...' : v || '—',
  },
  { key: 'status', label: 'Status', sortable: true },
  {
    key: 'tenant_id', label: 'Tenant', sortable: true,
    format: (v) => {
      const t = tenants.value.find((t) => (t.id || t.ID) === v)
      return t?.name || t?.Name || v || '—'
    },
  },
]

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [connData, tenantData] = await Promise.all([
      getIdpConnections(),
      getTenants(),
    ])
    connections.value = Array.isArray(connData) ? connData : connData?.idp_connections || connData?.connections || []
    tenants.value = Array.isArray(tenantData) ? tenantData : tenantData?.tenants || []
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load data'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openAddForm() {
  editingId.value = null
  form.value = { tenant_id: '', provider_type: 'oidc', issuer: '', client_id: '', client_secret: '' }
  showForm.value = true
}

function openEditForm(conn) {
  editingId.value = conn.id || conn.ID
  form.value = {
    tenant_id: conn.tenant_id || conn.TenantID || '',
    provider_type: conn.provider_type || conn.ProviderType || 'oidc',
    issuer: conn.issuer || conn.Issuer || '',
    client_id: conn.client_id || conn.ClientID || '',
    client_secret: '',
  }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.tenant_id || !form.value.issuer) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateIdpConnection(editingId.value, form.value)
      success('IdP connection updated')
    } else {
      await createIdpConnection(form.value.tenant_id, form.value)
      success('IdP connection created')
    }
    showForm.value = false
    await fetchData()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to save IdP connection')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  if (!confirm('Delete this IdP connection?')) return
  try {
    await deleteIdpConnection(id)
    success('IdP connection deleted')
    await fetchData()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete')
  }
}
</script>

<template>
  <div class="idp-list-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle text-muted">Manage identity provider connections across all tenants</p>
      </div>
      <button class="btn btn-primary" @click="openAddForm">+ Add Connection</button>
    </div>

    <ErrorAlert :message="error" @dismiss="error = ''" />
    <LoadingSpinner v-if="loading" message="Loading IdP connections..." />

    <div v-else-if="connections.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
        </svg>
        <h3 class="empty-title">No IdP Connections</h3>
        <p class="empty-message">Add your first identity provider connection to enable SSO.</p>
        <button class="btn btn-primary" @click="openAddForm">+ Add Connection</button>
      </div>
    </div>

    <DataTable v-else :columns="columns" :data="connections" title="All IdP Connections">
      <template #provider_type="{ row }">
        <span class="badge badge-info">{{ row.provider_type || row.ProviderType }}</span>
      </template>
      <template #status="{ row }">
        <span :class="['badge', (row.status || 'active') === 'active' ? 'badge-success' : 'badge-warning']">
          {{ row.status || 'active' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button class="btn btn-ghost btn-sm" @click="openEditForm(row)" title="Edit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
        </button>
        <button class="btn btn-ghost btn-sm" @click="handleDelete(row.id || row.ID)" title="Delete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
        </button>
      </template>
    </DataTable>

    <!-- Add/Edit Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ editingId ? 'Edit' : 'Add' }} IdP Connection</h2>
          <button class="modal-close" @click="showForm = false">&times;</button>
        </div>
        <form @submit.prevent="handleSave">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Tenant</label>
              <select v-model="form.tenant_id" required :disabled="!!editingId">
                <option value="">Select tenant...</option>
                <option v-for="t in tenants" :key="t.id || t.ID" :value="t.id || t.ID">
                  {{ t.name || t.Name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Provider Type</label>
              <select v-model="form.provider_type" required>
                <option value="oidc">OpenID Connect (OIDC)</option>
                <option value="saml">SAML 2.0</option>
                <option value="ldap">LDAP</option>
                <option value="oauth2">OAuth 2.0</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Issuer URL</label>
              <input v-model="form.issuer" type="url" required placeholder="https://accounts.example.com" />
            </div>
            <div class="form-group">
              <label class="form-label">Client ID</label>
              <input v-model="form.client_id" type="text" placeholder="Client ID" />
            </div>
            <div class="form-group">
              <label class="form-label">Client Secret</label>
              <input v-model="form.client_secret" type="password" placeholder="Leave blank to keep existing on edit" />
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showForm = false">Cancel</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? 'Saving...' : editingId ? 'Update' : 'Create' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.idp-list-page {
  max-width: 1000px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.page-subtitle {
  font-size: 0.9rem;
}

.empty-wrapper {
  margin-top: 1rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem 1rem;
  text-align: center;
  background: var(--color-bg-card);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 0.75rem;
  opacity: 0.5;
}

.empty-title {
  color: var(--color-text-secondary);
  font-size: 1rem;
  margin-bottom: 0.35rem;
}

.empty-message {
  color: var(--color-text-muted);
  font-size: 0.85rem;
  margin-bottom: 1rem;
  max-width: 360px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  max-width: 550px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  font-size: 1.1rem;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: var(--color-text-primary);
}

.modal-body {
  padding: 1.25rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
</style>
