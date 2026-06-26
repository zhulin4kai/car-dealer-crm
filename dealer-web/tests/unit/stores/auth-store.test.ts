import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { logout as logoutRequest } from '@/modules/user/api/user-api'
import { writeStoredToken } from '@/shared/storage/token-storage'
import { useAuthStore } from '@/stores/auth.store'

vi.mock('@/modules/user/api/user-api', () => ({
  fetchLoginInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    vi.mocked(logoutRequest).mockReset()
  })

  it('restores remembered sessions', () => {
    writeStoredToken('token', true)
    const authStore = useAuthStore()

    authStore.restoreSession()

    expect(authStore.token).toBe('token')
    expect(authStore.rememberMe).toBe(true)
    expect(authStore.isAuthenticated).toBe(true)
  })

  it('clears local session after logout succeeds', async () => {
    writeStoredToken('token', true)
    const authStore = useAuthStore()
    authStore.restoreSession()
    vi.mocked(logoutRequest).mockResolvedValue(undefined)

    await authStore.logout()

    expect(authStore.token).toBeNull()
    expect(localStorage.getItem('dlyk_token')).toBeNull()
  })

  it('keeps local session when logout request fails', async () => {
    writeStoredToken('token', true)
    const authStore = useAuthStore()
    authStore.restoreSession()
    vi.mocked(logoutRequest).mockRejectedValue(new Error('redis delete failed'))

    await expect(authStore.logout()).rejects.toThrow('redis delete failed')

    expect(authStore.token).toBe('token')
    expect(localStorage.getItem('dlyk_token')).toBe('token')
  })
})
