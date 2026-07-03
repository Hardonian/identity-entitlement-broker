<script setup>
import { ref, onMounted } from 'vue'
import { decidePolicy, getTenants } from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'

const { error: notifyError } = useNotification()

const tenants = ref([])

// Form
const form = ref({
  tenant_id: localStorage.getItem('active_tenant_id') || '',
  actor: '',
  subject: '',
  action: 'access',
  resource: '',
})
const submitting = ref(false)

// Result
const result = ref(null)

// History
const history = ref([])

onMounted(async () => {
  try {
    const data = await getTenants()
    tenants.value = Array.isArray(data) ? data : data?.tenants || []
  } catch {
    tenants.value = []
  }
})

async function handleSubmit() {
  if (!form.value.actor || !form.value.resource) return
  submitting.value = true
  result.value = null
  try {
    const payload = {
      tenant_id: form.value.tenant_id || undefined,
      actor: form.value.actor,
      subject: form.value.subject || undefined,
      action: form.value.action,
      resource: form.value.resource,
    }
    const res = await decidePolicy(payload)
    result.value = res

    // Add to history
    history.value.unshift({
      id: Date.now(),
      ...payload,
      decision: res.decision || res.Decision || (res.allowed ? 'allow' : 'deny'),
      reason: res.reason || res.Reason || '',
      matched_rule: res.matched_rule || res.MatchedRule || '',
      timestamp: new Date().toLocaleTimeString(),
    })
  } catch (err) {
    notifyError(err.displayMessage || 'Policy decision failed')
    result.value = { error: true, message: err.displayMessage }
  } finally {
    submitting.value = false
  }
}

function clearResult() {
  result.value = null
}

function clearHistory() {
  history.value = []
}

function getDecision(result) {
  if (!result) return null
  const d = result.decision || result.Decision || result.allowed
  if (d === true || d === 'allow' || d === 'Allow') return 'allow'
  if (d === false || d === 'deny' || d === 'Deny') return 'deny'
  return 'indeterminate'
}
</script>

<template>
  <div class="policy-tester-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">Policy Tester</h2>
        <p class="page-subtitle text-muted">Test policy decisions against the entitlement broker</p>
      </div>
    </div>

    <div class="tester-layout">
      <!-- Form -->
      <div class="card form-card">
        <h3 class="card-title mb-4">Decision Request</h3>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">Tenant</label>
            <select v-model="form.tenant_id">
              <option value="">— Default —</option>
              <option v-for="t in tenants" :key="t.id || t.ID" :value="t.id || t.ID">
                {{ t.name || t.Name }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Actor *</label>
            <input v-model="form.actor" type="text" required placeholder="user:jdoe or service:api-gateway" />
          </div>

          <div class="form-group">
            <label class="form-label">Subject</label>
            <input v-model="form.subject" type="text" placeholder="Target user or entity (optional)" />
          </div>

          <div class="form-group">
            <label class="form-label">Action *</label>
            <select v-model="form.action" required>
              <option value="access">access</option>
              <option value="manage">manage</option>
              <option value="impersonate">impersonate</option>
              <option value="provision">provision</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Resource *</label>
            <input v-model="form.resource" type="text" required placeholder="e.g. /api/v1/users or app:dashboard" />
          </div>

          <button type="submit" class="btn btn-primary btn-block" :disabled="submitting">
            {{ submitting ? 'Evaluating...' : 'Evaluate Policy' }}
          </button>
        </form>
      </div>

      <!-- Result -->
      <div class="result-area">
        <!-- Loading -->
        <div v-if="submitting" class="card result-card loading-result">
          <div class="spinner"></div>
          <p class="text-muted">Evaluating policy decision...</p>
        </div>

        <!-- Error result -->
        <div v-else-if="result && result.error" class="card result-card error-result">
          <div class="decision-badge decision-error">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            ERROR
          </div>
          <p class="result-reason">{{ result.message }}</p>
          <button class="btn btn-secondary btn-sm" @click="clearResult">Clear</button>
        </div>

        <!-- Success result -->
        <div v-else-if="result && !result.error" class="card result-card">
          <div v-if="getDecision(result) === 'allow'" class="decision-badge decision-allow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            ALLOWED
          </div>
          <div v-else-if="getDecision(result) === 'deny'" class="decision-badge decision-deny">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            DENIED
          </div>
          <div v-else class="decision-badge decision-unknown">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" /><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" /><line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            INDETERMINATE
          </div>

          <div class="result-details">
            <div v-if="result.reason || result.Reason" class="detail-row">
              <span class="detail-label">Reason</span>
              <span class="detail-value">{{ result.reason || result.Reason }}</span>
            </div>
            <div v-if="result.matched_rule || result.MatchedRule" class="detail-row">
              <span class="detail-label">Matched Rule</span>
              <span class="detail-value">{{ result.matched_rule || result.MatchedRule }}</span>
            </div>
            <div v-if="result.decision_time || result.DecisionTime" class="detail-row">
              <span class="detail-label">Decision Time</span>
              <span class="detail-value">{{ result.decision_time || result.DecisionTime }}</span>
            </div>
          </div>

          <button class="btn btn-secondary btn-sm" @click="clearResult">Clear</button>
        </div>

        <!-- Empty state -->
        <div v-else class="card result-card empty-result">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
          <p class="text-muted">Submit a policy request to see the decision here</p>
        </div>

        <!-- History -->
        <div v-if="history.length > 0" class="card history-card">
          <div class="history-header">
            <h3 class="card-title">Decision History</h3>
            <button class="btn btn-ghost btn-sm" @click="clearHistory">Clear All</button>
          </div>
          <div class="history-list">
            <div v-for="entry in history" :key="entry.id" class="history-item">
              <div class="history-decision">
                <span v-if="entry.decision === 'allow' || entry.decision === true" class="badge badge-success">ALLOW</span>
                <span v-else class="badge badge-danger">DENY</span>
              </div>
              <div class="history-info">
                <span class="history-action">{{ entry.action }}</span>
                <span class="history-resource">{{ entry.resource }}</span>
              </div>
              <span class="history-actor text-muted">{{ entry.actor }}</span>
              <span class="history-time text-muted">{{ entry.timestamp }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.policy-tester-page {
  max-width: 1100px;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-title {
  font-size: 1.2rem;
}

.page-subtitle {
  font-size: 0.9rem;
  margin-top: 0.2rem;
}

.tester-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 1.25rem;
  align-items: start;
}

@media (max-width: 768px) {
  .tester-layout {
    grid-template-columns: 1fr;
  }
}

.form-card {
  position: sticky;
  top: calc(var(--header-height) + 1.5rem);
}

.btn-block {
  width: 100%;
  justify-content: center;
}

.result-area {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.result-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem;
}

.loading-result {
  gap: 1rem;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.decision-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  margin-bottom: 1rem;
  padding: 0.5rem 1.5rem;
  border-radius: var(--radius-lg);
}

.decision-allow {
  color: var(--color-success);
  background-color: var(--color-success-bg);
}

.decision-deny {
  color: var(--color-danger);
  background-color: var(--color-danger-bg);
}

.decision-error {
  color: var(--color-danger);
  background-color: var(--color-danger-bg);
}

.decision-unknown {
  color: var(--color-warning);
  background-color: var(--color-warning-bg);
}

.result-details {
  width: 100%;
  text-align: left;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  gap: 0.5rem;
  padding: 0.35rem 0;
  font-size: 0.88rem;
  border-bottom: 1px solid var(--color-border);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: var(--color-text-muted);
  min-width: 110px;
  font-weight: 500;
}

.detail-value {
  color: var(--color-text-primary);
  word-break: break-all;
}

.result-reason {
  color: var(--color-danger);
  margin-bottom: 1rem;
}

.empty-result {
  gap: 1rem;
}

.empty-icon {
  opacity: 0.3;
  color: var(--color-text-muted);
}

.history-card {
  padding: 1rem;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: var(--radius-md);
  background-color: var(--color-bg-primary);
  font-size: 0.85rem;
}

.history-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.history-action {
  font-weight: 500;
  color: var(--color-text-primary);
}

.history-resource {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.history-actor {
  font-size: 0.8rem;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-time {
  font-size: 0.75rem;
  white-space: nowrap;
}
</style>
