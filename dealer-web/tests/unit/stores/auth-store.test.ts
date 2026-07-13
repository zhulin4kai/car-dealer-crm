import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  fetchLoginInfo,
  login as loginRequest,
  logout as logoutRequest,
} from '@/modules/user/api/user-api'
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
    vi.mocked(loginRequest).mockReset()
    vi.mocked(logoutRequest).mockReset()
    vi.mocked(fetchLoginInfo).mockReset()
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

  it('does not persist a token when the login request fails', async () => {
    const authStore = useAuthStore()
    vi.mocked(loginRequest).mockRejectedValue(new Error('session persistence failed'))

    await expect(
      authStore.login({ loginAct: 'admin', loginPwd: 'password', rememberMe: true }),
    ).rejects.toThrow('session persistence failed')

    expect(authStore.token).toBeNull()
    expect(authStore.rememberMe).toBe(false)
    expect(authStore.isAuthenticated).toBe(false)
    expect(localStorage.getItem('dlyk_token')).toBeNull()
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
  })

  it('refreshes only display profile fields while preserving authorization state', async () => {
    const authStore = useAuthStore()
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 21,
      loginAct: 'stable-login',
      roleList: ['sales'],
      permissionList: ['customer:view'],
    })
    await authStore.loadCurrentUser()
    authStore.applyCurrentUserProfile({
      id: 21,
      loginAct: 'server-login',
      name: '新姓名',
      phone: '13900139000',
      email: 'new@example.com',
      avatarUrl: 'https://example.com/avatar.png',
      roles: [],
      effectivePermissions: [],
      profileVersion: 2,
    })

    expect(authStore.currentUser).toMatchObject({
      id: 21,
      loginAct: 'stable-login',
      name: '新姓名',
      phone: '13900139000',
      email: 'new@example.com',
      avatarUrl: 'https://example.com/avatar.png',
    })
    expect(authStore.currentUser?.roleList).toEqual(['sales'])
    expect(authStore.currentUser?.permissionList).toEqual(['customer:view'])
  })
})
