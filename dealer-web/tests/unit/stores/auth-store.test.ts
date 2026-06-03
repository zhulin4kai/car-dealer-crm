import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { writeStoredToken } from '@/shared/storage/token-storage'
import { useAuthStore } from '@/stores/auth.store'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('restores remembered sessions', () => {
    writeStoredToken('token', true)
    const authStore = useAuthStore()

    authStore.restoreSession()

    expect(authStore.token).toBe('token')
    expect(authStore.rememberMe).toBe(true)
    expect(authStore.isAuthenticated).toBe(true)
  })
})
