import { getCurrentInstance, onUnmounted, ref, type Ref } from 'vue'

export interface UseLatestRequestReturn<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  error: Ref<Error | null>
  run: (factory: (signal: AbortSignal) => Promise<T>) => Promise<T | null>
  cancel: () => void
}

export function useLatestRequest<T>(): UseLatestRequestReturn<T> {
  const data = ref<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const error = ref<Error | null>(null)

  let latestId = 0
  let abortController: AbortController | null = null

  function cancel(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  async function run(factory: (signal: AbortSignal) => Promise<T>): Promise<T | null> {
    cancel()
    const id = ++latestId
    const controller = new AbortController()
    abortController = controller
    loading.value = true
    error.value = null

    try {
      const result = await factory(controller.signal)
      if (id === latestId && !controller.signal.aborted) {
        data.value = result
        return result
      }
    } catch (e: unknown) {
      if (id === latestId && !controller.signal.aborted) {
        error.value = e instanceof Error ? e : new Error(String(e))
      }
    } finally {
      if (id === latestId) {
        loading.value = false
        abortController = null
      }
    }
    return null
  }

  if (getCurrentInstance()) {
    onUnmounted(() => {
      cancel()
    })
  }

  return { data, loading, error, run, cancel }
}
