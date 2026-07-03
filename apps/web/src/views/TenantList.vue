<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTenants, deleteTenant, createTenant } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const router = useRouter()
const { success, error: notifyError } = useNotification()

const tenants = ref([])
const loading = ref(true)
const error = ref('')

// Inline create form
const showCreateForm = ref(false)
const newTenant = ref({ name: '', slug: '' })
const creating = ref(false)

const columns = [
  { key: 'name', label: 'Name', sortable: true },
  { key: 'slug', label: 'Slug', sortable: true },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    format: (val) => val || 'active',
  },
  {
    key: 'created_at',
    label: 'Created',
    sortable: true,
    format: (val) => {
      if (!val) return '—'
      try {
        return new Date(val).toLocaleDateString()
      } catch {
        return val
      }
    },
  },
]

async function fetchTenants() {
  loading.value = true
  error.value = ''
  try {
    const data = await getTenants()
    tenants.value = Array.isArray(data) ? data : data?.tenants || []
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load tenants'
  } finally {
    loading.value = false
  }
}

onMounted(fetchTenants)

function viewTenant(id) {
  router.push(`/tenants/${id}`)
}

async function handleDelete(id, name) {
  if (!confirm(`Are you sure you want to delete tenant "${name}"?`)) return
  try {
    await deleteTenant(id)
    success(`Tenant "${name}" deleted`)
    await fetchTenants()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete tenant')
  }
}

async function handleCreate() {
  if (!newTenant.value.name || !newTenant.value.slug) return
  creating.value = true
  try {
    await createTenant(newTenant.value)
    success('Tenant created successfully')
    showCreateForm.value = false
    newTenant.value = { name: '', slug: '' }
    await fetchTenants()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to create tenant')
  } finally {
    creating.value = false
  }
}

function statusClass(status) {
  const s = (status || 'active').toLowerCase()
  if (s === 'active') return 'badge-success'
  if (s === 'inactive' || s === 'disabled') return 'badge-warning'
  return 'badge-danger'
}
</script>

<template>
  <div class="tenant-list-page">
    <div class="page-header">
      <div class="header-info">
        <p class="page-subtitle">Manage multi-tenant identity and entitlement configurations</p>
      </div>
      <button class="btn btn-primary" @click="showCreateForm = !showCreateForm">
        {{ showCreateForm ? 'Cancel' : '+ Add Tenant' }}
      </button>
    </div>

    <!-- Create form -->
    <div v-if="showCreateForm" class="card create-form">
      <h3 class="card-title mb-2">Create New Tenant</h3>
      <form @submit.prevent="handleCreate">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="tenant-name">Tenant Name</label>
            <input id="tenant-name" v-model="newTenant.name" type="text" required placeholder="e.g. Acme Corp" />
          </div>
          <div class="form-group">
            <label class="form-label" for="tenant-slug">Slug</label>
            <input id="tenant-slug" v-model="newTenant.slug" type="text" required placeholder="e.g. acme-corp" />
          </div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="creating">
            {{ creating ? 'Creating...' : 'Create Tenant' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="showCreateForm = false">Cancel</button>
        </div>
      </form>
    </div>

    <ErrorAlert :message="error" @dismiss="error = ''" />

    <!-- Loading -->
    <LoadingSpinner v-if="loading" message="Loading tenants..." />

    <!-- Empty state -->
    <div v-else-if="tenants.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
        </svg>
        <h3 class="empty-title">No Tenants Yet</h3>
        <p class="empty-message">Create your first tenant to start managing identity and entitlements.</p>
        <button class="btn btn-primary" @click="showCreateForm = true">+ Add Tenant</button>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      v-else
      :columns="columns"
      :data="tenants"
      title="All Tenants"
    >
      <template #status="{ row }">
        <span :class="['badge', statusClass(row.status)]">{{ row.status || 'active' }}</span>
      </template>

      <template #created_at="{ value }">
        {{ value ? new Date(value).toLocaleDateString() : '—' }}
      </template>

      <template #actions="{ row }">
        <button class="btn btn-ghost btn-sm" @click="viewTenant(row.id || row.ID)" title="View details">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" />
          </svg>
        </button>
        <button class="btn btn-ghost btn-sm" @click="router.push(`/tenants/${row.id || row.ID}`)" title="Edit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <button class="btn btn-ghost btn-sm" @click="handleDelete(row.id || row.ID, row.name)" title="Delete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
        </button>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
.tenant-list-page {
  max-width: 1000px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.25rem;
  gap: 1rem;
}

.header-info {
  flex: 1;
}

.page-subtitle {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  margin-top: 0.25rem;
}

.create-form {
  margin-bottom: 1.25rem;
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
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.empty-wrapper {
  margin-top: 1rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
  background-color: var(--color-bg-card);
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
  max-width: 360px;
  margin-bottom: 1rem;
}
</style>
