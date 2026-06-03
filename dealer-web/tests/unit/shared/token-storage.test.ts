import { beforeEach, describe, expect, it } from 'vitest'

import {
  clearStoredToken,
  getTokenName,
  readStoredToken,
  writeStoredToken,
} from '@/shared/storage/token-storage'

describe('token storage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('stores session tokens by default', () => {
    writeStoredToken('session-token', false)

    expect(getTokenName()).toBe('dlyk_token')
    expect(readStoredToken()).toEqual({ token: 'session-token', rememberMe: false })
    expect(localStorage.getItem('dlyk_token')).toBeNull()
  })

  it('stores remembered tokens in localStorage', () => {
    writeStoredToken('local-token', true)

    expect(readStoredToken()).toEqual({ token: 'local-token', rememberMe: true })
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
  })

  it('clears both storage locations', () => {
    writeStoredToken('token', true)
    clearStoredToken()

    expect(readStoredToken()).toBeNull()
  })
})
