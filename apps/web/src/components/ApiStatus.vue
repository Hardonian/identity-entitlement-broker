<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { health } from '../api/client.js'

const props = defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
})

const status = ref('checking') // 'online' | 'offline' | 'checking'
const lastCheck = ref(null)
let interval = null

async function checkHealth() {
  try {
    const result = await health()
    status.value = result?.status === 'ok' || result?.status === 'healthy' ? 'online' : 'online'
    lastCheck.value = new Date()
  } catch {
    status.value = 'offline'
    lastCheck.value = new Date()
  }
}

onMounted(() => {
  checkHealth()
  interval = setInterval(checkHealth, 30000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})

function formatTime(date) {
  return date?.toLocaleTimeString() || '—'
}
</script>

<template>
  <div class="api-status" :title="'API Status: ' + status + ' (last checked: ' + formatTime(lastCheck) + ')'">
    <span class="status-dot" :class="status"></span>
    <span v-if="!compact" class="status-label">API {{ status === 'online' ? 'Connected' : status === 'offline' ? 'Disconnected' : 'Checking...' }}</span>
    <span v-if="!compact && lastCheck" class="status-time">{{ formatTime(lastCheck) }}</span>
  </div>
</template>

<style scoped>
.api-status {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.5rem;
  border-radius: var(--radius-md);
  font-size: 0.75rem;
  color: var(--color-text-muted);
  cursor: default;
}

.api-status:hover {
  background-color: var(--color-bg-hover);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.online {
  background-color: var(--color-success);
  box-shadow: 0 0 4px var(--color-success);
}

.status-dot.offline {
  background-color: var(--color-danger);
  box-shadow: 0 0 4px var(--color-danger);
}

.status-dot.checking {
  background-color: var(--color-warning);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.status-label {
  white-space: nowrap;
}

.status-time {
  color: var(--color-text-muted);
  font-size: 0.7rem;
}
</style>
