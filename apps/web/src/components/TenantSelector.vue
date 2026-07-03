<script setup>
import { ref, onMounted } from 'vue'
import { getTenants } from '../api/client.js'

const tenants = ref([])
const selectedTenantId = ref(localStorage.getItem('active_tenant_id') || '')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getTenants()
    tenants.value = Array.isArray(data) ? data : data?.tenants || []
  } catch {
    // silently fail — tenant selector is non-critical
    tenants.value = []
  } finally {
    loading.value = false
  }
})

function onSelect(event) {
  const val = event.target.value
  selectedTenantId.value = val
  if (val) {
    localStorage.setItem('active_tenant_id', val)
  } else {
    localStorage.removeItem('active_tenant_id')
  }
  // Reload the page to trigger refetch with new tenant header
  window.location.reload()
}

function getTenantName(id) {
  const t = tenants.value.find((t) => t.id === id || t.ID === id)
  return t?.name || t?.Name || id
}
</script>

<template>
  <div class="tenant-selector">
    <label v-if="selectedTenantId" class="current-tenant" :title="'Active tenant: ' + getTenantName(selectedTenantId)">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
      </svg>
      <span class="tenant-name">{{ getTenantName(selectedTenantId) }}</span>
    </label>
    <select
      v-model="selectedTenantId"
      @change="onSelect"
      class="tenant-dropdown"
      :disabled="loading"
    >
      <option value="">All Tenants</option>
      <option
        v-for="t in tenants"
        :key="t.id || t.ID"
        :value="t.id || t.ID"
      >
        {{ t.name || t.Name }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.tenant-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.current-tenant {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--color-text-secondary);
  font-size: 0.8rem;
  white-space: nowrap;
}

.tenant-name {
  color: var(--color-accent-text);
  font-weight: 500;
}

.tenant-dropdown {
  min-width: 160px;
}
</style>
