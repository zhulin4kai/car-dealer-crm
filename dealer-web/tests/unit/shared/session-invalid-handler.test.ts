import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  type SessionInvalidHandler,
  notifySessionInvalid,
  registerSessionInvalidHandler,
  resetSessionInvalidHandler,
} from '@/shared/auth/session-invalid-handler'

function createDeferred<T = void>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('session invalid handler', () => {
  beforeEach(() => {
    resetSessionInvalidHandler()
  })

  it('does nothing when no handler is registered', async () => {
    await expect(notifySessionInvalid({ code: 512, msg: 'token已过期' })).resolves.toBeUndefined()
  })

  it('invokes the registered handler once for concurrent session-invalid notifications', async () => {
    const handler = vi.fn<SessionInvalidHandler['handleSessionInvalid']>().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    const calls = Array.from({ length: 5 }, () =>
      notifySessionInvalid({ code: 512, msg: 'token已过期' }),
    )
    await Promise.all(calls)

    expect(handler).toHaveBeenCalledTimes(1)
    expect(handler).toHaveBeenCalledWith({ code: 512, msg: 'token已过期' })
  })

  it('returns the same in-flight promise for concurrent notifications', async () => {
    const deferred = createDeferred()
    const handler = vi.fn(() => deferred.promise)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    const first = notifySessionInvalid({ code: 510, msg: 'token为空' })
    const second = notifySessionInvalid({ code: 512, msg: 'token已过期' })

    expect(first).toBe(second)

    deferred.resolve()
    await first
  })

  it('resets in-flight after completion so a new session invalid can trigger again', async () => {
    const handler = vi.fn<SessionInvalidHandler['handleSessionInvalid']>().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    await notifySessionInvalid({ code: 512, msg: 'token已过期' })
    await notifySessionInvalid({ code: 511, msg: 'token无效' })

    expect(handler).toHaveBeenCalledTimes(2)
  })

  it('resets in-flight even when the handler throws', async () => {
    const handler = vi.fn().mockRejectedValue(new Error('router failed'))
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    await expect(notifySessionInvalid({ code: 512, msg: 'token已过期' })).rejects.toThrow(
      'router failed',
    )

    const secondHandler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: secondHandler })
    await notifySessionInvalid({ code: 510, msg: 'token为空' })

    expect(secondHandler).toHaveBeenCalledTimes(1)
  })

})
