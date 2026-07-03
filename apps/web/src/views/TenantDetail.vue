<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTenant, getIdpConnections, getUsers, getGroups, deleteIdpConnection } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'
import DataTable from '../components/DataTable.vue'

const props = defineProps({ id: String })
const route = useRoute()
const router = useRouter()
const { success, error: notifyError } = useNotification()

const effectiveId = computed(() => props.id || route.params.id)

const tenant = ref(null)
const loading = ref(true)
const error = ref('')

const activeTab = ref('idp')

// IdP Connections
const idpConnections = ref([])
const idpLoading = ref(false)
const idpError = ref('')
const showAddIdp = ref(false)
const newIdp = ref({ provider_type: 'oidc', issuer: '', client_id: '', client_secret: '' })
const creatingIdp = ref(false)

// Users (scoped)
const scopedUsers = ref([])
const usersLoading = ref(false)
const usersError = ref('')

// Groups (scoped)
const scopedGroups = ref([])
const groupsLoading = ref(false)
const groupsError = ref('')

const idpColumns = [
  { key: 'provider_type', label: 'Provider', sortable: true },
  { key: 'issuer', label: 'Issuer', sortable: true,
    format: (v) => v && v.length > 40 ? v.substring(0, 40) + '...' : v || '—',
  },
  { key: 'status', label: 'Status', sortable: true },
]

const userColumns = [
  { key: 'userName', label: 'Username', sortable: true },
  { key: 'emails', label: 'Email',
    format: (v) => {
      if (!v) return '—'
      if (Array.isArray(v)) return v[0]?.value || v[0] || '—'
      return v
    },
  },
  { key: 'displayName', label: 'Display Name', sortable: true },
  { key: 'active', label: 'Active', sortable: true,
    format: (v) => v ? 'Yes' : 'No',
  },
]

const groupColumns = [
  { key: 'displayName', label: 'Display Name', sortable: true },
  { key: 'members', label: 'Members',
    format: (v) => (Array.isArray(v) ? v.length : 0),
  },
]

async function fetchTenant() {
  loading.value = true
  error.value = ''
  try {
    tenant.value = await getTenant(effectiveId.value)
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load tenant'
  } finally {
    loading.value = false
  }
}

async function fetchIdp() {
  idpLoading.value = true
  idpError.value = ''
  try {
    const data = await getIdpConnections(effectiveId.value)
    idpConnections.value = Array.isArray(data) ? data : data?.idp_connections || data?.connections || []
  } catch (err) {
    idpError.value = err.displayMessage || 'Failed to load IdP connections'
  } finally {
    idpLoading.value = false
  }
}

async function fetchUsers() {
  usersLoading.value = true
  usersError.value = ''
  try {
    const data = await getUsers({ tenant_id: effectiveId.value })
    scopedUsers.value = Array.isArray(data) ? data : data?.Resources || data?.users || []
  } catch (err) {
    usersError.value = err.displayMessage || 'Failed to load users'
  } finally {
    usersLoading.value = false
  }
}

async function fetchGroups() {
  groupsLoading.value = true
  groupsError.value = ''
  try {
    const data = await getGroups({ tenant_id: effectiveId.value })
    scopedGroups.value = Array.isArray(data) ? data : data?.Resources || data?.groups || []
  } catch (err) {
    groupsError.value = err.displayMessage || 'Failed to load groups'
  } finally {
    groupsLoading.value = false
  }
}

onMounted(async () => {
  await fetchTenant()
  // Load initial tab data
  if (activeTab.value === 'idp') await fetchIdp()
})

async function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'idp' && idpConnections.value.length === 0) await fetchIdp()
  if (tab === 'users' && scopedUsers.value.length === 0) await fetchUsers()
  if (tab === 'groups' && scopedGroups.value.length === 0) await fetchGroups()
}

async function handleAddIdp() {
  if (!newIdp.value.provider_type || !newIdp.value.issuer) return
  creatingIdp.value = true
  try {
    const { createIdpConnection } = await import('../api/client.js')
    await createIdpConnection(effectiveId.value, newIdp.value)
    success('IdP connection added')
    showAddIdp.value = false
    newIdp.value = { provider_type: 'oidc', issuer: '', client_id: '', client_secret: '' }
    await fetchIdp()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to add IdP connection')
  } finally {
    creatingIdp.value = false
  }
}

async function handleDeleteIdp(id) {
  if (!confirm('Delete this IdP connection?')) return
  try {
    await deleteIdpConnection(id)
    success('IdP connection deleted')
    await fetchIdp()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete IdP connection')
  }
}

function goToList() {
  router.push('/tenants')
}
</script>

<template>
  <div class="tenant-detail-page">
    <button class="btn btn-ghost back-btn mb-2" @click="goToList">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6" /></svg>
      Back to Tenants
    </button>

    <LoadingSpinner v-if="loading" message="Loading tenant..." />
    <ErrorAlert v-else-if="error" :message="error" @dismiss="error = ''" />

    <template v-else-if="tenant">
      <div class="page-header card">
        <div class="tenant-info">
          <h1 class="tenant-name">{{ tenant.name || tenant.Name }}</h1>
          <div class="tenant-meta">
            <span class="meta-item"><strong>Slug:</strong> {{ tenant.slug || tenant.Slug }}</span>
            <span class="meta-item"><strong>ID:</strong> {{ tenant.id || tenant.ID }}</span>
            <span v-if="tenant.created_at || tenant.CreatedAt" class="meta-item">
              <strong>Created:</strong> {{ new Date(tenant.created_at || tenant.CreatedAt).toLocaleDateString() }}
            </span>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'idp' }" @click="switchTab('idp')">IdP Connections</button>
        <button class="tab" :class="{ active: activeTab === 'users' }" @click="switchTab('users')">Users</button>
        <button class="tab" :class="{ active: activeTab === 'groups' }" @click="switchTab('groups')">Groups</button>
      </div>

      <!-- IdP Tab -->
      <div v-if="activeTab === 'idp'">
        <div class="tab-header">
          <h2>IdP Connections</h2>
          <button class="btn btn-primary btn-sm" @click="showAddIdp = !showAddIdp">
            {{ showAddIdp ? 'Cancel' : '+ Add Connection' }}
          </button>
        </div>

        <!-- Add IdP form -->
        <div v-if="showAddIdp" class="card add-form mb-4">
          <form @submit.prevent="handleAddIdp">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Provider Type</label>
                <select v-model="newIdp.provider_type" required>
                  <option value="oidc">OpenID Connect (OIDC)</option>
                  <option value="saml">SAML 2.0</option>
                  <option value="ldap">LDAP</option>
                  <option value="oauth2">OAuth 2.0</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Issuer URL</label>
                <input v-model="newIdp.issuer" type="url" required placeholder="https://accounts.example.com" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Client ID</label>
                <input v-model="newIdp.client_id" type="text" placeholder="Client ID" />
              </div>
              <div class="form-group">
                <label class="form-label">Client Secret</label>
                <input v-model="newIdp.client_secret" type="password" placeholder="Client secret" />
              </div>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="creatingIdp">
                {{ creatingIdp ? 'Adding...' : 'Add Connection' }}
              </button>
            </div>
          </form>
        </div>

        <ErrorAlert :message="idpError" @dismiss="idpError = ''" />
        <LoadingSpinner v-if="idpLoading" message="Loading IdP connections..." />
        <div v-else-if="idpConnections.length === 0 && !idpError" class="empty-section">
          <p class="text-muted">No IdP connections configured for this tenant.</p>
        </div>
        <DataTable v-else :columns="idpColumns" :data="idpConnections">
          <template #provider_type="{ row }">
            <span class="badge badge-info">{{ row.provider_type || row.ProviderType }}</span>
          </template>
          <template #status="{ row }">
            <span :class="['badge', (row.status || 'active') === 'active' ? 'badge-success' : 'badge-warning']">
              {{ row.status || 'active' }}
            </span>
          </template>
          <template #actions="{ row }">
            <button class="btn btn-ghost btn-sm" @click="handleDeleteIdp(row.id || row.ID)" title="Delete">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
            </button>
          </template>
        </DataTable>
      </div>

      <!-- Users Tab -->
      <div v-if="activeTab === 'users'">
        <ErrorAlert :message="usersError" @dismiss="usersError = ''" />
        <LoadingSpinner v-if="usersLoading" message="Loading users..." />
        <div v-else-if="scopedUsers.length === 0 && !usersError" class="empty-section">
          <p class="text-muted">No users found for this tenant.</p>
        </div>
        <DataTable v-else :columns="userColumns" :data="scopedUsers">
          <template #active="{ row }">
            <span :class="['badge', row.active ? 'badge-success' : 'badge-danger']">
              {{ row.active ? 'Active' : 'Inactive' }}
            </span>
          </template>
        </DataTable>
      </div>

      <!-- Groups Tab -->
      <div v-if="activeTab === 'groups'">
        <ErrorAlert :message="groupsError" @dismiss="groupsError = ''" />
        <LoadingSpinner v-if="groupsLoading" message="Loading groups..." />
        <div v-else-if="scopedGroups.length === 0 && !groupsError" class="empty-section">
          <p class="text-muted">No groups found for this tenant.</p>
        </div>
        <DataTable v-else :columns="groupColumns" :data="scopedGroups" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.tenant-detail-page {
  max-width: 1000px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.page-header.card {
  margin-bottom: 1.25rem;
}

.tenant-name {
  font-size: 1.3rem;
  margin-bottom: 0.5rem;
}

.tenant-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.meta-item {
  color: var(--color-text-secondary);
  font-size: 0.85rem;
}

.meta-item strong {
  color: var(--color-text-muted);
}

.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.tab-header h2 {
  font-size: 1.05rem;
}

.add-form {
  margin-bottom: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row > * {
  flex: 1;
}

@media (max-width: 640px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}

.form-actions {
  margin-top: 0.75rem;
}

.empty-section {
  padding: 2rem 1rem;
  text-align: center;
  background-color: var(--color-bg-card);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
