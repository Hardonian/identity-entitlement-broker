<script setup>
import { ref, onMounted } from 'vue'
import { getGroups, getUsers, createGroup, updateGroup, deleteGroup } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const { success, error: notifyError } = useNotification()

const groups = ref([])
const users = ref([])
const loading = ref(true)
const error = ref('')

// Form modal
const showForm = ref(false)
const editingId = ref(null)
const form = ref({ displayName: '', members: [] })
const saving = ref(false)

const columns = [
  { key: 'displayName', label: 'Display Name', sortable: true },
  {
    key: 'members', label: 'Member Count', sortable: false,
    format: (v) => (Array.isArray(v) ? v.length : 0),
  },
  {
    key: 'meta', label: 'Created', sortable: true,
    format: (v) => {
      if (!v) return '—'
      const d = v.created || v.Created
      return d ? new Date(d).toLocaleDateString() : '—'
    },
  },
]

async function fetchGroups() {
  loading.value = true
  error.value = ''
  try {
    const data = await getGroups()
    groups.value = Array.isArray(data) ? data : data?.Resources || data?.groups || []
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load groups'
  } finally {
    loading.value = false
  }
}

async function fetchUsers() {
  try {
    const data = await getUsers()
    users.value = Array.isArray(data) ? data : data?.Resources || data?.users || []
  } catch {
    users.value = []
  }
}

onMounted(async () => {
  await Promise.all([fetchGroups(), fetchUsers()])
})

function openAddForm() {
  editingId.value = null
  form.value = { displayName: '', members: [] }
  showForm.value = true
}

function openEditForm(group) {
  editingId.value = group.id || group.ID
  const existingMembers = Array.isArray(group.members) ? group.members : []
  form.value = {
    displayName: group.displayName || '',
    members: existingMembers.map((m) => m.value || m),
  }
  showForm.value = true
}

function toggleMember(userId) {
  const idx = form.value.members.indexOf(userId)
  if (idx === -1) {
    form.value.members.push(userId)
  } else {
    form.value.members.splice(idx, 1)
  }
}

function isMember(userId) {
  return form.value.members.includes(userId)
}

async function handleSave() {
  if (!form.value.displayName) return
  saving.value = true
  try {
    const payload = {
      schemas: ['urn:ietf:params:scim:schemas:core:2.0:Group'],
      displayName: form.value.displayName,
      members: form.value.members.map((id) => ({ value: id })),
    }
    if (editingId.value) {
      await updateGroup(editingId.value, payload)
      success('Group updated')
    } else {
      await createGroup(payload)
      success('Group created')
    }
    showForm.value = false
    await fetchGroups()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to save group')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id, name) {
  if (!confirm(`Delete group "${name || id}"?`)) return
  try {
    await deleteGroup(id)
    success('Group deleted')
    await fetchGroups()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to delete group')
  }
}
</script>

<template>
  <div class="group-list-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle text-muted">SCIM groups and their member assignments</p>
      </div>
      <button class="btn btn-primary" @click="openAddForm">+ Add Group</button>
    </div>

    <ErrorAlert :message="error" @dismiss="error = ''" />
    <LoadingSpinner v-if="loading" message="Loading groups..." />

    <div v-else-if="groups.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
        </svg>
        <h3 class="empty-title">No Groups Found</h3>
        <p class="empty-message">Groups will appear here once provisioned or you can create one manually.</p>
        <button class="btn btn-primary" @click="openAddForm">+ Add Group</button>
      </div>
    </div>

    <DataTable v-else :columns="columns" :data="groups" title="SCIM Groups">
      <template #actions="{ row }">
        <button class="btn btn-ghost btn-sm" @click="openEditForm(row)" title="Edit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
        </button>
        <button class="btn btn-ghost btn-sm" @click="handleDelete(row.id || row.ID, row.displayName)" title="Delete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
        </button>
      </template>
    </DataTable>

    <!-- Add/Edit Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content wide-modal">
        <div class="modal-header">
          <h2>{{ editingId ? 'Edit' : 'Create' }} SCIM Group</h2>
          <button class="modal-close" @click="showForm = false">&times;</button>
        </div>
        <form @submit.prevent="handleSave">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Display Name *</label>
              <input v-model="form.displayName" type="text" required placeholder="Engineering" />
            </div>
            <div class="form-group">
              <label class="form-label">Members ({{ form.members.length }} selected)</label>
              <div class="member-list">
                <div
                  v-for="user in users"
                  :key="user.id || user.ID"
                  class="member-item"
                  :class="{ selected: isMember(user.id || user.ID) }"
                  @click="toggleMember(user.id || user.ID)"
                >
                  <input
                    type="checkbox"
                    :checked="isMember(user.id || user.ID)"
                    @click.stop="toggleMember(user.id || user.ID)"
                  />
                  <span>{{ user.displayName || user.userName || user.id }}</span>
                  <span v-if="user.displayName && user.userName" class="text-muted">({{ user.userName }})</span>
                </div>
                <div v-if="users.length === 0" class="text-muted" style="padding: 0.5rem 0;">
                  No users available. Add users first.
                </div>
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
.group-list-page {
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

.wide-modal {
  max-width: 600px;
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

.member-list {
  max-height: 250px;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.25rem;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.5rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.88rem;
  color: var(--color-text-primary);
  transition: background-color var(--transition-fast);
}

.member-item:hover {
  background-color: var(--color-bg-hover);
}

.member-item.selected {
  background-color: var(--color-accent-light);
}

.member-item input[type="checkbox"] {
  width: auto;
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
</style>
