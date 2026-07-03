<script setup>
defineProps({
  message: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['dismiss'])
</script>

<template>
  <div v-if="message" class="error-alert">
    <svg class="error-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
    <span class="error-message">{{ message }}</span>
    <button class="error-dismiss" @click="$emit('dismiss')" title="Dismiss">&times;</button>
  </div>
</template>

<style scoped>
.error-alert {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1rem;
  background-color: var(--color-danger-bg);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-lg);
  color: var(--color-danger);
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.error-icon {
  flex-shrink: 0;
}

.error-message {
  flex: 1;
}

.error-dismiss {
  background: none;
  border: none;
  color: var(--color-danger);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  opacity: 0.7;
}

.error-dismiss:hover {
  opacity: 1;
}
</style>
