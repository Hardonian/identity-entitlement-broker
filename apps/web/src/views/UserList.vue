<script setup>
import { ref, onMounted, computed } from 'vue'
import { getUsers, createUser, updateUser, deleteUser } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const { success, error: notifyError } = useNotification()

const users = ref([])
const rawResponse = ref(null)
const loading = ref(true)
const error = ref('')

// Form modal
const showForm = ref(false)
const editingId = ref(null)
const form = ref({
  userName: '',
  displayName: '',
  name: { givenName: '', familyName: '' },
  emails: [{ value: '', primary: true }],
  active: true,
})
const saving = ref(false)

// Raw JSON modal
const showRawJson = ref(false)
const rawJsonData = ref('')

const columns = [
  { key: 'userName', label: 'Username', sortable: true },
  {
    key: 'emails', label: 'Email', sortable: false,
    format: (v) => {
      if (!v) return '—'
      if (Array.isArray(v)) return v[0]?.value || '—'
      return String(v)
    },
  },
  { key: 'displayName', label: 'Display Name', sortable: true },
  { key: 'active', label: 'Active', sortable: true, format: (v) => v ? 'Yes' : 'No' },
  {
    key: 'meta', label: 'Created', sortable: true,
    format: (v) => {
      if (!v) return '—'
      const d = v.created || v.Created
      return d ? new Date(d).toLocaleDateString() : '—'
    },
  },
]

async function fetchUsers() {
  loading.value = true
  error.value = ''
  try {
    const data = await getUsers()
    rawResponse.value = data
    users.value = Array.isArray(data) ? data : data?.Resources || data?.users || data?.data || []
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load users'
  } finally {
    loading.value = false
  }
}

onMounted(fetchUsers)

function openAddForm() {
  editingId.value = null
  form.value = { userName: '', displayName: '', name: { givenName: '', familyName: '' }, emails: [{ value: '', primary: true }], active: true }
  showForm.value = true
}

function openEditForm(user) {
  editingId.value = user.id || user.ID
  const name = user.name || {}
  form.value = {
    userName: user.userName || '',
    displayName: user.displayName || '',
    name: { givenName: name.givenName || '', familyName: name.familyName || '' },
    emails: Array.isArray(user.emails) && user.emails.length > 0 ? user.emails : [{ value: '', primary: true }],
    active: user.active !== undefined ? user.active : true,
  }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.userName) return
  saving.value = true
  try {
    const payload = {
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:User'],
      userName: form.value.userName,
      displayName: form.value.displayName,
      name: form.value.name,
      emails: form.value.emails,
      active: form.value.active,
    }
    if (editingId.value) {
      await updateUser(editingId.value, payload)
      success('User updated')
    } else {
      await createUser(payload)
      success('User created')
    }
    showForm.value = false
    await fetchUsers()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to save user')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id, name) {
  if (!confirm(`Delete user "${name || id}"?`)) return
  try {
    await deleteUser(id)
    success('User deleted')
    await fetchUsers()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete user')
  }
}

function viewRawJson() {
  rawJsonData.value = JSON.stringify(rawResponse.value, null, 2)
  showRawJson.value = true
}
</script>

<template>
  <div class="user-list-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle text-muted">SCIM users synchronized from connected identity providers</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-ghost btn-sm" @click="viewRawJson" title="View raw SCIM response">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
          Raw JSON
        </button>
        <button class="btn btn-primary" @click="openAddForm">+ Add User</button>
      </div>
    </div>

    <ErrorAlert :message="error" @dismiss="error = ''" />
    <LoadingSpinner v-if="loading" message="Loading users..." />

    <div v-else-if="users.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
        </svg>
        <h3 class="empty-title">No Users Found</h3>
        <p class="empty-message">Users will appear here once they are provisioned via SCIM or you can add one manually.</p>
        <button class="btn btn-primary" @click="openAddForm">+ Add User</button>
      </div>
    </div>

    <DataTable v-else :columns="columns" :data="users" title="SCIM Users">
      <template #active="{ row }">
        <span :class="['badge', row.active ? 'badge-success' : 'badge-danger']">
          {{ row.active ? 'Active' : 'Inactive' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button class="btn btn-ghost btn-sm" @click="openEditForm(row)" title="Edit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
        </button>
        <button class="btn btn-ghost btn-sm" @click="handleDelete(row.id || row.ID, row.displayName || row.userName)" title="Delete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
        </button>
      </template>
    </DataTable>

    <!-- Add/Edit Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ editingId ? 'Edit' : 'Create' }} SCIM User</h2>
          <button class="modal-close" @click="showForm = false">&times;</button>
        </div>
        <form @submit.prevent="handleSave">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Username *</label>
              <input v-model="form.userName" type="text" required placeholder="jdoe" />
            </div>
            <div class="form-group">
              <label class="form-label">Display Name</label>
              <input v-model="form.displayName" type="text" placeholder="John Doe" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Given Name</label>
                <input v-model="form.name.givenName" type="text" placeholder="John" />
              </div>
              <div class="form-group">
                <label class="form-label">Family Name</label>
                <input v-model="form.name.familyName" type="text" placeholder="Doe" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Email</label>
              <input v-model="form.emails[0].value" type="email" placeholder="john@example.com" />
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input type="checkbox" v-model="form.active" />
                <span>Active</span>
              </label>
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

    <!-- Raw JSON Modal -->
    <div v-if="showRawJson" class="modal-overlay" @click.self="showRawJson = false">
      <div class="modal-content wide-modal">
        <div class="modal-header">
          <h2>Raw SCIM Response</h2>
          <button class="modal-close" @click="showRawJson = false">&times;</button>
        </div>
        <div class="modal-body">
          <pre class="raw-json">{{ rawJsonData }}</pre>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showRawJson = false">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-list-page {
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

.header-actions {
  display: flex;
  gap: 0.5rem;
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

.wide-modal {
  max-width: 700px;
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

.form-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.form-checkbox input[type="checkbox"] {
  width: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}

.raw-json {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  font-family: 'SF Mono', monospace;
  font-size: 0.78rem;
  line-height: 1.5;
  overflow-x: auto;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 500px;
  overflow-y: auto;
}
</style>
