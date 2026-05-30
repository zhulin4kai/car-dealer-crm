import { describe, it, expect, vi, beforeEach } from 'vitest'
import { goBack, getToken, removeToken, messageTip, getTokenName } from '../src/util/util.js'

describe('util.js', () => {
  beforeEach(() => {
    sessionStorage.clear()
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('goBack()', () => {
    it('should use router, not this context', () => {
      // BUG: goBack() uses `this.$router.go(-1)` which won't work
      // in a standalone function context. `this` will be undefined or
      // the module, not a Vue component instance with $router.
      const source = goBack.toString()
      expect(source).not.toContain('this.$router')
      // The correct implementation should use window.history or vue-router's useRouter()
    })
  })

  describe('getToken()', () => {
    it('should return token from sessionStorage', () => {
      const token = 'test-jwt-token-123'
      sessionStorage.setItem('dlyk_token', token)
      const result = getToken()
      expect(result).toBe(token)
    })

    it('should fallback to localStorage when sessionStorage has no token', () => {
      const token = 'localStorage-token-456'
      localStorage.setItem('dlyk_token', token)
      const result = getToken()
      expect(result).toBe(token)
    })

    it('should handle missing token gracefully', () => {
      // BUG: getToken() returns undefined when no token exists because
      // the else branch calls messageConfirm() which is async but doesn't
      // return the promise, so the function implicitly returns undefined.
      const result = getToken()
      // After fixing, this should return null/undefined or redirect,
      // but currently it returns undefined without any meaningful handling.
      expect(result).toBeUndefined()
    })
  })

  describe('getTokenName()', () => {
    it('should return dlyk_token', () => {
      expect(getTokenName()).toBe('dlyk_token')
    })
  })

  describe('removeToken()', () => {
    it('should clear token from sessionStorage', () => {
      sessionStorage.setItem('dlyk_token', 'some-token')
      removeToken()
      expect(sessionStorage.getItem('dlyk_token')).toBeNull()
    })

    it('should clear token from localStorage', () => {
      localStorage.setItem('dlyk_token', 'some-token')
      removeToken()
      expect(localStorage.getItem('dlyk_token')).toBeNull()
    })

    it('should clear token from both storages', () => {
      sessionStorage.setItem('dlyk_token', 'token-a')
      localStorage.setItem('dlyk_token', 'token-b')
      removeToken()
      expect(sessionStorage.getItem('dlyk_token')).toBeNull()
      expect(localStorage.getItem('dlyk_token')).toBeNull()
    })
  })

  describe('messageTip()', () => {
    it('should show correct message type - success', () => {
      // messageTip calls ElMessage with the given type
      // We just verify it doesn't throw and accepts valid types
      expect(() => messageTip('test', 'success')).not.toThrow()
    })

    it('should show correct message type - error', () => {
      expect(() => messageTip('test', 'error')).not.toThrow()
    })

    it('should show correct message type - warning', () => {
      expect(() => messageTip('test', 'warning')).not.toThrow()
    })
  })
})
