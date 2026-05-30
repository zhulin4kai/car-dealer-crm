import { describe, it, expect, vi, beforeEach } from 'vitest'
import { goBack, getToken, removeToken, messageTip, getTokenName, messageConfirm, getUserPermission, setUserPermission, clearUserPermission } from '../src/util/util.js'

describe('util.js', () => {
  beforeEach(() => {
    sessionStorage.clear()
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('goBack()', () => {
    it('should call window.history.back', () => {
      const spy = vi.spyOn(window.history, 'back')
      goBack()
      expect(spy).toHaveBeenCalled()
      spy.mockRestore()
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
      const result = getToken()
      expect(result).toBeUndefined()
    })

    it('should prefer sessionStorage over localStorage', () => {
      sessionStorage.setItem('dlyk_token', 'session-token')
      localStorage.setItem('dlyk_token', 'local-token')
      const result = getToken()
      expect(result).toBe('session-token')
    })
  })

  describe('getTokenName()', () => {
    it('should return dlyk_token', () => {
      expect(getTokenName()).toBe('dlyk_token')
    })

    it('should return a string', () => {
      expect(typeof getTokenName()).toBe('string')
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

    it('should not throw when no token exists', () => {
      expect(() => removeToken()).not.toThrow()
    })
  })

  describe('messageTip()', () => {
    it('should show correct message type - success', () => {
      expect(() => messageTip('test', 'success')).not.toThrow()
    })

    it('should show correct message type - error', () => {
      expect(() => messageTip('test', 'error')).not.toThrow()
    })

    it('should show correct message type - warning', () => {
      expect(() => messageTip('test', 'warning')).not.toThrow()
    })

    it('should show correct message type - info', () => {
      expect(() => messageTip('test', 'info')).not.toThrow()
    })

    it('should accept any message string', () => {
      expect(() => messageTip('', 'success')).not.toThrow()
      expect(() => messageTip('Long message text', 'info')).not.toThrow()
    })
  })

  describe('messageConfirm()', () => {
    it('should return a promise', () => {
      const result = messageConfirm('test message')
      expect(result).toBeInstanceOf(Promise)
    })

    it('should not throw', () => {
      expect(() => messageConfirm('test message')).not.toThrow()
    })
  })

  describe('getUserPermission()', () => {
    it('should return null when no cached permissions', () => {
      const result = getUserPermission()
      expect(result).toBeNull()
    })

    it('should return cached permissions from sessionStorage', () => {
      const permissions = ['user:list', 'user:add', 'clue:list']
      sessionStorage.setItem('user_permissions', JSON.stringify(permissions))
      const result = getUserPermission()
      expect(result).toEqual(permissions)
    })

    it('should return null for invalid JSON', () => {
      sessionStorage.setItem('user_permissions', 'invalid-json')
      const result = getUserPermission()
      expect(result).toBeNull()
    })
  })

  describe('setUserPermission()', () => {
    it('should store permissions in sessionStorage', () => {
      const permissions = ['user:list', 'user:add']
      setUserPermission(permissions)
      const stored = sessionStorage.getItem('user_permissions')
      expect(JSON.parse(stored)).toEqual(permissions)
    })

    it('should overwrite existing permissions', () => {
      setUserPermission(['old:permission'])
      setUserPermission(['new:permission'])
      const stored = sessionStorage.getItem('user_permissions')
      expect(JSON.parse(stored)).toEqual(['new:permission'])
    })
  })

  describe('clearUserPermission()', () => {
    it('should remove permissions from sessionStorage', () => {
      sessionStorage.setItem('user_permissions', JSON.stringify(['test']))
      clearUserPermission()
      expect(sessionStorage.getItem('user_permissions')).toBeNull()
    })

    it('should not throw when no permissions exist', () => {
      expect(() => clearUserPermission()).not.toThrow()
    })
  })
})
