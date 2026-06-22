import { effectScope } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useLatestRequest } from '@/shared/composables/use-latest-request'

function createDeferred<T = void>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('useLatestRequest', () => {
  it('commits data from the latest request when A resolves first and B resolves second', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferredA = createDeferred<string>()
    const deferredB = createDeferred<string>()

    const factoryA = vi.fn(() => deferredA.promise)
    const factoryB = vi.fn(() => deferredB.promise)

    void state.run(factoryA)
    void state.run(factoryB)

    expect(state.loading.value).toBe(true)

    deferredA.resolve('a')
    await deferredA.promise

    expect(state.data.value).toBeNull()
    expect(state.loading.value).toBe(true)

    deferredB.resolve('b')
    await deferredB.promise

    expect(state.data.value).toBe('b')
    expect(state.loading.value).toBe(false)

    scope.stop()
  })

  it('commits data from B when B resolves first and A resolves later', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferredA = createDeferred<string>()
    const deferredB = createDeferred<string>()

    void state.run(() => deferredA.promise)
    void state.run(() => deferredB.promise)

    deferredB.resolve('b')
    await deferredB.promise

    expect(state.data.value).toBe('b')
    expect(state.loading.value).toBe(false)

    deferredA.resolve('a')
    await deferredA.promise

    expect(state.data.value).toBe('b')
    expect(state.loading.value).toBe(false)

    scope.stop()
  })

  it('keeps loading true until the latest request completes even if old request finishes', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferredA = createDeferred<string>()
    const deferredB = createDeferred<string>()

    void state.run(() => deferredA.promise)
    void state.run(() => deferredB.promise)

    deferredA.resolve('a')
    await deferredA.promise

    expect(state.loading.value).toBe(true)

    deferredB.resolve('b')
    await deferredB.promise

    expect(state.loading.value).toBe(false)

    scope.stop()
  })

  it('does not set error when an old request rejects', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferredA = createDeferred<string>()
    const deferredB = createDeferred<string>()

    void state.run(() => deferredA.promise)
    void state.run(() => deferredB.promise)

    deferredA.reject(new Error('A failed'))
    await deferredA.promise.catch(() => undefined)

    expect(state.error.value).toBeNull()
    expect(state.loading.value).toBe(true)

    deferredB.resolve('b')
    await deferredB.promise

    expect(state.data.value).toBe('b')
    expect(state.error.value).toBeNull()

    scope.stop()
  })

  it('sets error only for the latest request failure', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const factory = vi.fn().mockRejectedValue(new Error('network down'))
    await state.run(factory)

    expect(state.error.value).toBeInstanceOf(Error)
    expect(state.error.value?.message).toBe('network down')
    expect(state.loading.value).toBe(false)

    scope.stop()
  })

  it('does not set error when the latest request is canceled by a newer one', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferredA = createDeferred<string>()
    const factoryA = vi.fn(() => deferredA.promise)
    const factoryB = vi.fn().mockResolvedValue('b')

    void state.run(factoryA)
    void state.run(factoryB)
    await vi.waitFor(() => expect(state.data.value).toBe('b'))

    deferredA.reject(new Error('canceled'))
    await deferredA.promise.catch(() => undefined)

    expect(state.error.value).toBeNull()
    expect(state.data.value).toBe('b')

    scope.stop()
  })

  it('passes AbortSignal to the factory', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    let receivedSignal: AbortSignal | null = null
    const factory = vi.fn((signal: AbortSignal) => {
      receivedSignal = signal
      return Promise.resolve('ok')
    })

    await state.run(factory)

    expect(receivedSignal).toBeInstanceOf(AbortSignal)
    scope.stop()
  })

  it('aborts the previous request when a new one starts', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    let firstSignal: AbortSignal | null = null
    const deferredA = createDeferred<string>()
    const factoryA = vi.fn((signal: AbortSignal) => {
      firstSignal = signal
      return deferredA.promise
    })
    const factoryB = vi.fn().mockResolvedValue('b')

    void state.run(factoryA)
    void state.run(factoryB)
    await vi.waitFor(() => expect(state.data.value).toBe('b'))

    expect(firstSignal?.aborted).toBe(true)

    scope.stop()
  })

  it('cancel() aborts the current request without setting error', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    const deferred = createDeferred<string>()
    const factory = vi.fn(() => deferred.promise)

    void state.run(factory)
    state.cancel()

    expect(state.error.value).toBeNull()

    deferred.reject(new Error('aborted'))
    await deferred.promise.catch(() => undefined)

    expect(state.error.value).toBeNull()

    scope.stop()
  })

  it('resets error when a new run starts after a failure', async () => {
    const scope = effectScope()
    const state = scope.run(() => useLatestRequest<string>())!

    await state.run(vi.fn().mockRejectedValue(new Error('failed')))
    expect(state.error.value).not.toBeNull()

    await state.run(vi.fn().mockResolvedValue('ok'))
    expect(state.error.value).toBeNull()
    expect(state.data.value).toBe('ok')

    scope.stop()
  })
})
