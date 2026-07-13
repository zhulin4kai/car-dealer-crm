import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  notifyUserManagementGate,
  registerUserManagementGateHandler,
  resetUserManagementGateHandler,
} from '@/shared/auth/user-management-gate-handler'

describe('user management gate handler', () => {
  beforeEach(() => resetUserManagementGateHandler())

  it('does nothing when no handler is registered', async () => {
    await expect(notifyUserManagementGate({ code: 641 })).resolves.toBeUndefined()
  })

  it('coalesces concurrent gate notifications into one navigation', async () => {
    let release: (() => void) | undefined
    const pending = new Promise<void>((resolve) => {
      release = resolve
    })
    const handler = vi.fn(() => pending)
    registerUserManagementGateHandler({ handleUserManagementGate: handler })

    const first = notifyUserManagementGate({ code: 641 })
    const second = notifyUserManagementGate({ code: 642 })

    expect(first).toBe(second)
    expect(handler).toHaveBeenCalledTimes(1)
    release?.()
    await first
  })
})
