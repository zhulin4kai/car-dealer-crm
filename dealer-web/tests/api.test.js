import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

describe('API module consistency', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('clue.js', () => {
    it('getCurrentClues should use leading slash', async () => {
      // FIXED: getCurrentClues now uses '/api/clues' with leading slash
      const clueModule = await import('../src/api/clue.js')

      clueModule.getCurrentClues(1)

      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/clues')
    })

    it('getOwnerList should not exist in clue.js', async () => {
      // FIXED: getOwnerList removed from clue.js, kept in activity.js
      const clueModule = await import('../src/api/clue.js')
      expect(clueModule.getOwnerList).toBeUndefined()
    })
  })

  describe('dict.js', () => {
    it('getDictValueDetail should match backend path', async () => {
      // FIXED: path now includes '/get/' segment to match backend
      const dictModule = await import('../src/api/dict.js')

      dictModule.getDictValueDetail(42)

      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/dict/value/get/42')
    })
  })

  describe('user.js', () => {
    it('batchDeleteUsers should send plain array', async () => {
      // FIXED: batchDeleteUsers now sends plain array, not wrapped object
      const userModule = await import('../src/api/user.js')

      const ids = [1, 2, 3]
      userModule.batchDeleteUsers(ids)

      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/user')
      expect(config.method).toBe('delete')
      expect(config.data).toEqual([1, 2, 3])
      expect(Array.isArray(config.data)).toBe(true)
    })
  })

  describe('duplicate getOwnerList', () => {
    it('getOwnerList should only exist in activity.js', async () => {
      // FIXED: getOwnerList removed from clue.js
      const activityModule = await import('../src/api/activity.js')
      const clueModule = await import('../src/api/clue.js')

      expect(typeof activityModule.getOwnerList).toBe('function')
      expect(clueModule.getOwnerList).toBeUndefined()
    })
  })

  describe('API path consistency', () => {
    it('all API modules should export functions', async () => {
      const modules = [
        await import('../src/api/activity.js'),
        await import('../src/api/clue.js'),
        await import('../src/api/user.js'),
        await import('../src/api/dict.js'),
        await import('../src/api/product.js'),
        await import('../src/api/tran.js'),
        await import('../src/api/customer.js'),
        await import('../src/api/system.js'),
      ]

      modules.forEach(mod => {
        expect(Object.keys(mod).length).toBeGreaterThan(0)
        Object.values(mod).forEach(fn => {
          expect(typeof fn).toBe('function')
        })
      })
    })
  })
})
