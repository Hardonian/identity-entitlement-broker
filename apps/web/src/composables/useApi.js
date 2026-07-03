import { ref, unref } from 'vue'

/**
 * Generic API wrapper providing reactive loading, error, and data refs.
 *
 * Usage:
 *   const { data, loading, error, execute } = useApi(fetchFn)
 *   await execute(param1, param2)
 */
export function useApi(asyncFn) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await asyncFn(...args)
      data.value = result
      return result
    } catch (err) {
      const message = err.displayMessage || err.message || 'An unexpected error occurred'
      error.value = message
      throw err
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = null
    loading.value = false
    error.value = null
  }

  return {
    data,
    loading,
    error,
    execute,
    reset,
  }
}
