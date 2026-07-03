<script setup>
import { ref } from 'vue'

const props = defineProps({
  event: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close'])

function onBackdropClick(e) {
  if (e.target === e.currentTarget) {
    emit('close')
  }
}

function formatJson(obj) {
  if (!obj) return '{}'
  return JSON.stringify(obj, null, 2)
}
</script>

<template>
  <div v-if="event" class="modal-overlay" @click="onBackdropClick">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Audit Event Detail</h2>
        <button class="modal-close btn-ghost" @click="$emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">ID</span>
            <span class="detail-value">{{ event.id || event.ID || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Action</span>
            <span class="detail-value">{{ event.action || event.Action || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Resource Type</span>
            <span class="detail-value">{{ event.resource_type || event.ResourceType || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Resource ID</span>
            <span class="detail-value">{{ event.resource_id || event.ResourceID || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Actor</span>
            <span class="detail-value">{{ event.actor || event.Actor || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Tenant</span>
            <span class="detail-value">{{ event.tenant_id || event.TenantID || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Outcome</span>
            <span :class="['badge', event.outcome === 'allow' || event.outcome === 'success' ? 'badge-success' : 'badge-danger']">
              {{ event.outcome || event.Outcome || '—' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Timestamp</span>
            <span class="detail-value">{{ event.timestamp || event.Timestamp || event.created_at || event.CreatedAt || '—' }}</span>
          </div>
        </div>

        <div class="raw-json-section">
          <h3 class="raw-json-title">Raw Event Data</h3>
          <pre class="raw-json">{{ formatJson(event) }}</pre>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="$emit('close')">Close</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background-color: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  max-width: 700px;
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
  font-weight: 600;
}

.modal-close {
  font-size: 1.5rem;
  padding: 0 0.25rem;
  cursor: pointer;
  color: var(--color-text-secondary);
  background: none;
  border: none;
  line-height: 1;
}

.modal-close:hover {
  color: var(--color-text-primary);
}

.modal-body {
  padding: 1.25rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.detail-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
}

.detail-value {
  font-size: 0.9rem;
  color: var(--color-text-primary);
  word-break: break-all;
}

.raw-json-section {
  border-top: 1px solid var(--color-border);
  padding-top: 1rem;
  margin-top: 0.5rem;
}

.raw-json-title {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
}

.raw-json {
  background-color: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 0.78rem;
  line-height: 1.5;
  overflow-x: auto;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-all;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
</style>
