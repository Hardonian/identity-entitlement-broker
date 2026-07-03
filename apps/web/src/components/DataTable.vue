<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true,
  },
  data: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['sort'])

const sortKey = ref('')
const sortDir = ref('asc')

function toggleSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
  emit('sort', { key: sortKey.value, dir: sortDir.value })
}

const sortedData = computed(() => {
  if (!sortKey.value) return props.data
  const key = sortKey.value
  const dir = sortDir.value === 'asc' ? 1 : -1
  return [...props.data].sort((a, b) => {
    const aVal = a[key]
    const bVal = b[key]
    if (aVal == null) return 1
    if (bVal == null) return -1
    if (typeof aVal === 'string') {
      return aVal.localeCompare(bVal) * dir
    }
    return (aVal - bVal) * dir
  })
})

const sortIndicator = (key) => {
  if (sortKey.value !== key) return ''
  return sortDir.value === 'asc' ? ' ▲' : ' ▼'
}
</script>

<template>
  <div class="data-table-wrapper">
    <div v-if="title" class="table-title">{{ title }}</div>

    <!-- Loading state -->
    <div v-if="loading" class="table-state">
      <div class="spinner"></div>
      <span>Loading data...</span>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="table-state error">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Empty state -->
    <div v-else-if="data.length === 0" class="table-state empty">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" /><polyline points="13 2 13 9 20 9" />
      </svg>
      <span>No data available</span>
    </div>

    <!-- Data table -->
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th
              v-for="col in columns"
              :key="col.key"
              :class="{ sortable: col.sortable !== false }"
              @click="col.sortable !== false && toggleSort(col.key)"
            >
              {{ col.label || col.key }}
              <span v-if="col.sortable !== false" class="sort-arrow">{{ sortIndicator(col.key) }}</span>
            </th>
            <th v-if="$slots.actions" class="actions-th">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, rowIdx) in sortedData" :key="row.id || row.ID || rowIdx">
            <td v-for="col in columns" :key="col.key">
              <template v-if="$slots[col.key]">
                <slot :name="col.key" :row="row" :value="row[col.key]"></slot>
              </template>
              <template v-else-if="col.format">
                {{ col.format(row[col.key], row) }}
              </template>
              <template v-else>
                {{ row[col.key] ?? '—' }}
              </template>
            </td>
            <td v-if="$slots.actions" class="actions-cell">
              <slot name="actions" :row="row"></slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.data-table-wrapper {
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-title {
  padding: 0.75rem 1rem;
  font-weight: 600;
  font-size: 0.95rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-primary);
}

.table-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 1rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.table-state.error {
  color: var(--color-danger);
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.6rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

th {
  font-weight: 600;
  color: var(--color-text-secondary);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  user-select: none;
}

th.sortable {
  cursor: pointer;
}

th.sortable:hover {
  color: var(--color-text-primary);
}

.sort-arrow {
  font-size: 0.7rem;
}

td {
  color: var(--color-text-primary);
  font-size: 0.88rem;
}

tr:last-child td {
  border-bottom: none;
}

tr:hover td {
  background-color: rgba(51, 65, 85, 0.4);
}

.actions-th {
  width: 120px;
  text-align: right;
}

.actions-cell {
  text-align: right;
  white-space: nowrap;
}
</style>
