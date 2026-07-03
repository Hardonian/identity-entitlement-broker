<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getAuditEvents } from '../api/client.js'
import AuditDetailModal from '../components/AuditDetailModal.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const events = ref([])
const loading = ref(true)
const error = ref('')

// Search filters
const filters = ref({
  action: '',
  resource_type: '',
  actor: '',
  outcome: '',
})

// Auto-refresh
const autoRefresh = ref(false)
let refreshInterval = null

// Pagination
const page = ref(1)
const pageSize = 25
const totalEvents = ref(0)

// Detail modal
const selectedEvent = ref(null)

const columns = [
  { key: 'timestamp', label: 'Time', sortable: true },
  { key: 'action', label: 'Action', sortable: true },
  { key: 'resource_type', label: 'Resource Type', sortable: true },
  { key: 'resource_id', label: 'Resource' },
  { key: 'actor', label: 'Actor', sortable: true },
  { key: 'outcome', label: 'Outcome', sortable: true },
]

async function fetchEvents() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      limit: pageSize,
      offset: (page.value - 1) * pageSize,
    }
    // Add non-empty filters
    if (filters.value.action) params.action = filters.value.action
    if (filters.value.resource_type) params.resource_type = filters.value.resource_type
    if (filters.value.actor) params.actor = filters.value.actor
    if (filters.value.outcome) params.outcome = filters.value.outcome

    const data = await getAuditEvents(params)
    const items = Array.isArray(data) ? data : data?.events || data?.results || []
    events.value = items
    totalEvents.value = data?.total || data?.Total || items.length
  } catch (err) {
    error.value = err.displayMessage || 'Failed to load audit events'
  } finally {
    loading.value = false
  }
}

onMounted(fetchEvents)

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshInterval = setInterval(fetchEvents, 10000)
  } else {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})

function search() {
  page.value = 1
  fetchEvents()
}

function prevPage() {
  if (page.value > 1) {
    page.value--
    fetchEvents()
  }
}

function nextPage() {
  page.value++
  fetchEvents()
}

function openDetail(event) {
  selectedEvent.value = event
}

function closeDetail() {
  selectedEvent.value = null
}

function formatTime(val) {
  if (!val) return '—'
  try {
    const d = new Date(val)
    return d.toLocaleString()
  } catch {
    return val
  }
}

function outcomeClass(outcome) {
  const o = (outcome || '').toLowerCase()
  if (o === 'allow' || o === 'success') return 'badge-success'
  if (o === 'deny' || o === 'failure' || o === 'error') return 'badge-danger'
  return 'badge-warning'
}
</script>

<template>
  <div class="audit-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">Audit Log</h2>
        <p class="page-subtitle text-muted">Search and review identity and entitlement audit events</p>
      </div>
      <label class="auto-refresh-label">
        <input type="checkbox" v-model="autoRefresh" @change="toggleAutoRefresh" />
        <span>Auto-refresh (10s)</span>
      </label>
    </div>

    <!-- Search Filters -->
    <div class="card filters-card">
      <div class="filters-grid">
        <div class="form-group">
          <label class="form-label">Action</label>
          <select v-model="filters.action">
            <option value="">All Actions</option>
            <option value="access">access</option>
            <option value="manage">manage</option>
            <option value="impersonate">impersonate</option>
            <option value="provision">provision</option>
            <option value="create">create</option>
            <option value="update">update</option>
            <option value="delete">delete</option>
            <option value="login">login</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Resource Type</label>
          <select v-model="filters.resource_type">
            <option value="">All Types</option>
            <option value="tenant">tenant</option>
            <option value="user">user</option>
            <option value="group">group</option>
            <option value="role">role</option>
            <option value="entitlement">entitlement</option>
            <option value="policy">policy</option>
            <option value="idp">idp</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Actor</label>
          <input v-model="filters.actor" type="text" placeholder="Filter by actor ID" />
        </div>
        <div class="form-group">
          <label class="form-label">Outcome</label>
          <select v-model="filters.outcome">
            <option value="">All Outcomes</option>
            <option value="allow">Allow</option>
            <option value="deny">Deny</option>
            <option value="success">Success</option>
            <option value="failure">Failure</option>
          </select>
        </div>
      </div>
      <div class="filter-actions">
        <button class="btn btn-primary btn-sm" @click="search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
          Search
        </button>
        <button class="btn btn-secondary btn-sm" @click="filters = { action: '', resource_type: '', actor: '', outcome: '' }; search()">Clear</button>
      </div>
    </div>

    <!-- Results -->
    <ErrorAlert :message="error" @dismiss="error = ''" />

    <LoadingSpinner v-if="loading" message="Loading audit events..." />

    <div v-else-if="events.length === 0 && !error" class="empty-wrapper">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" />
        </svg>
        <h3 class="empty-title">No Audit Events</h3>
        <p class="empty-message">No events match your search criteria. Try adjusting filters.</p>
      </div>
    </div>

    <div v-else class="results-table">
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Action</th>
              <th>Resource Type</th>
              <th>Resource</th>
              <th>Actor</th>
              <th>Outcome</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="event in events"
              :key="event.id || event.ID"
              class="clickable-row"
              @click="openDetail(event)"
            >
              <td class="time-cell">{{ formatTime(event.timestamp || event.Timestamp || event.created_at || event.CreatedAt) }}</td>
              <td><span class="badge badge-info">{{ event.action || event.Action }}</span></td>
              <td>{{ event.resource_type || event.ResourceType || '—' }}</td>
              <td class="text-truncate" style="max-width: 150px;">{{ event.resource_id || event.ResourceID || '—' }}</td>
              <td class="text-truncate" style="max-width: 120px;">{{ event.actor || event.Actor || '—' }}</td>
              <td>
                <span :class="['badge', outcomeClass(event.outcome || event.Outcome)]">
                  {{ event.outcome || event.Outcome || '—' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <span class="text-muted">{{ totalEvents }} total events</span>
        <div class="pagination-controls">
          <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="prevPage">Previous</button>
          <span class="page-indicator">Page {{ page }}</span>
          <button class="btn btn-ghost btn-sm" :disabled="events.length < pageSize" @click="nextPage">Next</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <AuditDetailModal :event="selectedEvent" @close="closeDetail" />
  </div>
</template>

<style scoped>
.audit-page {
  max-width: 1100px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.page-title {
  font-size: 1.2rem;
}

.page-subtitle {
  font-size: 0.9rem;
  margin-top: 0.2rem;
}

.auto-refresh-label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  white-space: nowrap;
}

.auto-refresh-label input[type="checkbox"] {
  width: auto;
}

.filters-card {
  margin-bottom: 1rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .filters-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .filters-grid {
    grid-template-columns: 1fr;
  }
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
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
  opacity: 0.5;
  margin-bottom: 0.75rem;
  color: var(--color-text-muted);
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
}

.results-table {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover td {
  background-color: rgba(51, 65, 85, 0.5);
}

.time-cell {
  white-space: nowrap;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-indicator {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}
</style>
