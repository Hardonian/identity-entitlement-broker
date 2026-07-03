import { ref } from 'vue'

/**
 * Toast notification composable.
 * Provides a reactive array of notifications and helpers to add/remove them.
 *
 * Each notification: { id, type: 'success'|'error'|'warning'|'info', message }
 */
const notifications = ref([])
let nextId = 1

export function useNotification() {
  function addNotification(type, message, duration = 5000) {
    const id = nextId++
    notifications.value.push({ id, type, message })

    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id)
      }, duration)
    }

    return id
  }

  function removeNotification(id) {
    const idx = notifications.value.findIndex((n) => n.id === id)
    if (idx !== -1) {
      notifications.value.splice(idx, 1)
    }
  }

  function clearAll() {
    notifications.value = []
  }

  // Convenience methods
  function success(message, duration) {
    return addNotification('success', message, duration)
  }

  function error(message, duration) {
    return addNotification('error', message, duration)
  }

  function warning(message, duration) {
    return addNotification('warning', message, duration)
  }

  function info(message, duration) {
    return addNotification('info', message, duration)
  }

  return {
    notifications,
    addNotification,
    removeNotification,
    clearAll,
    success,
    error,
    warning,
    info,
  }
}
