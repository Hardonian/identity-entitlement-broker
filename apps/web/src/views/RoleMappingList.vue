<script setup>
import { ref, onMounted } from 'vue'
import { getRoleMappings, getTenants, createRoleMapping, updateRoleMapping, deleteRoleMapping } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const { success, error: notifyError } = useNotification()

const roleMappings = ref([])
const tenants = ref([])
const loading = ref(true)
const error = ref('')

// Form modal
const showForm = ref(false)
const editingId = ref(null)
const form = ref({
  tenant_id: '',
  name: '',
  source_type: 'claim',
  source_value: '',
  target_role: '',
  priority: 0,
})
const saving = ref(false)

const columns = [
  { key: 'name', label: 'Name', sortable: true },
  { key: 'source_type', label: 'Source Type', sortable: true },
  { key: 'source_value', label: 'Source Value', sortable: true },
  { key: 'target_role', label: 'Target Role', sortable: true },
  { key: 'priority', label: 'Priority', sortable: true },
]

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [mappingData, tenantData] = await Promise.all([
      getRoleMappings(),
      getTenants(),
    ])
    roleMappings.value = Array.isArray(mappingData) ? mappingData : mappingData?.role_mappings || []
    tenants.value = Array.isArray(tenantData) ? tenantData : tenantData?.tenants || []
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load role mappings'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openAddForm() {
  editingId.value = null
  form.value = { tenant_id: '', name: '', source_type: 'claim', source_value: '', target_role: '', priority: 0 }
  showForm.value = true
}

function openEditForm(mapping) {
  editingId.value = mapping.id || mapping.ID
  form.value = {
    tenant_id: mapping.tenant_id || mapping.TenantID || '',
    name: mapping.name || '',
    source_type: mapping.source_type || mapping.SourceType || 'claim',
    source_value: mapping.source_value || mapping.SourceValue || '',
    target_role: mapping.target_role || mapping.TargetRole || '',
    priority: mapping.priority || mapping.Priority || 0,
  }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.name || !form.value.source_value || !form.value.target_role) return
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      source_type: form.value.source_type,
      source_value: form.value.source_value,
      target_role: form.value.target_role,
      priority: Number(form.value.priority),
    }
    if (editingId.value) {
      await updateRoleMapping(editingId.value, payload)
      success('Role mapping updated')
    } else {
      await createRoleMapping(form.value.tenant_id || undefined, payload)
      success('Role mapping created')
    }
    showForm.value = false
    await fetchData()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to save role mapping')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  if (!confirm('Delete this role mapping?')) return
  try {
    await deleteRoleMapping(id)
    success('Role mapping deleted')
    await fetchData()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete')
  }
}

function sourceTypeClass(type) {
  switch (type) {
    case 'claim': return 'badge-info'
    case 'group': return 'badge-success'
    case 'attribute': return 'badge-warning'
    default: return 'badge-info'
  }
}
</script>

<template>
  <div class="role-mapping-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle text-muted">Map identity provider claims and groups to application roles</p>
      </div>
      <button class="btn btn-primary" @click="openAddForm">+ Add Mapping</button>
    </div>

    <ErrorAlert :message="error" @dismiss="error = ''" />
    <LoadingSpinner v-if="loading" message="Loading role mappings..." />

    <div v-else-if="roleMappings.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
        <h3 class="empty-title">No Role Mappings</h3>
        <p class="empty-message">Create role mappings to define how identity claims translate to application roles.</p>
        <button class="btn btn-primary" @click="openAddForm">+ Add Mapping</button>
      </div>
    </div>

    <DataTable v-else :columns="columns" :data="roleMappings" title="Role Mappings">
      <template #source_type="{ row }">
        <span :class="['badge', sourceTypeClass(row.source_type || row.SourceType)]">
          {{ row.source_type || row.SourceType }}
        </span>
      </template>
      <template #priority="{ row }">
        <span class="badge badge-info">{{ row.priority ?? row.Priority ?? 0 }}</span>
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
          <h2>{{ editingId ? 'Edit' : 'Create' }} Role Mapping</h2>
          <button class="modal-close" @click="showForm = false">&times;</button>
        </div>
        <form @submit.prevent="handleSave">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Mapping Name *</label>
              <input v-model="form.name" type="text" required placeholder="e.g. Admin claim to admin role" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Source Type</label>
                <select v-model="form.source_type">
                  <option value="claim">Claim</option>
                  <option value="group">Group</option>
                  <option value="attribute">Attribute</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Source Value *</label>
                <input v-model="form.source_value" type="text" required placeholder="e.g. admin or group:admins" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Target Role *</label>
                <input v-model="form.target_role" type="text" required placeholder="e.g. administrator" />
              </div>
              <div class="form-group">
                <label class="form-label">Priority</label>
                <input v-model.number="form.priority" type="number" min="0" />
              </div>
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
.role-mapping-page {
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
  max-width: 500px;
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

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
</style>
