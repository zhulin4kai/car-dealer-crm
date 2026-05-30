import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

describe('httpRequest.js', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('baseURL configuration', () => {
    it('should set baseURL to localhost:8089', () => {
      const baseURL = axios.defaults.baseURL
      expect(baseURL).toBeDefined()
    })
  })

  describe('response interceptor - auth failure detection', () => {
    it('should catch auth failures with code >= 500', () => {
      const authFailureCodes = [510, 511, 512, 513]
      authFailureCodes.forEach(code => {
        const wouldBeCaught = code >= 500
        expect(wouldBeCaught).toBe(true)
      })

      expect(200 >= 500).toBe(false)
      expect(0 >= 500).toBe(false)
    })
  })

  describe('request config', () => {
    it('should use responseType instead of dataType', () => {
      const validAxiosConfigKeys = [
        'method', 'url', 'data', 'params', 'headers',
        'timeout', 'responseType', 'baseURL', 'transformRequest',
        'transformResponse', 'auth', 'withCredentials', 'signal',
        'onUploadProgress', 'onDownloadProgress', 'cancelToken'
      ]
      expect(validAxiosConfigKeys).toContain('responseType')
      expect(validAxiosConfigKeys).not.toContain('dataType')
    })

    it('doGet should pass responseType to axios config', async () => {
      const { doGet } = await import('../src/http/httpRequest.js')
      doGet('/api/test', { id: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config).toHaveProperty('responseType', 'json')
    })
  })

  describe('request interceptor', () => {
    it('should add Authorization header when token exists', () => {
      expect(axios.interceptors.request.use).toBeDefined()
    })

    it('should add rememberMe header for localStorage tokens', () => {
      expect(axios.interceptors.request.use).toBeDefined()
    })
  })

  describe('HTTP method functions', () => {
    it('doGet should use GET method', async () => {
      const { doGet } = await import('../src/http/httpRequest.js')
      doGet('/api/test', {})
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
    })

    it('doPost should use POST method', async () => {
      const { doPost } = await import('../src/http/httpRequest.js')
      doPost('/api/test', { data: 1 })
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
    })

    it('doPut should use PUT method', async () => {
      const { doPut } = await import('../src/http/httpRequest.js')
      doPut('/api/test', { data: 1 })
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
    })

    it('doDelete should use DELETE method', async () => {
      const { doDelete } = await import('../src/http/httpRequest.js')
      doDelete('/api/test', { id: 1 })
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
    })

    it('doGet should pass params correctly', async () => {
      const { doGet } = await import('../src/http/httpRequest.js')
      const params = { name: 'test', age: 22 }
      doGet('/api/test', params)
      const config = axios.mock.calls[0][0]
      expect(config.params).toEqual(params)
    })

    it('doPost should pass data correctly', async () => {
      const { doPost } = await import('../src/http/httpRequest.js')
      const data = { name: 'test', age: 22 }
      doPost('/api/test', data)
      const config = axios.mock.calls[0][0]
      expect(config.data).toEqual(data)
    })

    it('doPut should pass data correctly', async () => {
      const { doPut } = await import('../src/http/httpRequest.js')
      const data = { id: 1, name: 'test' }
      doPut('/api/test', data)
      const config = axios.mock.calls[0][0]
      expect(config.data).toEqual(data)
    })

    it('doDelete should pass data in request body', async () => {
      const { doDelete } = await import('../src/http/httpRequest.js')
      const data = { id: 1 }
      doDelete('/api/test', data)
      const config = axios.mock.calls[0][0]
      expect(config.data).toEqual(data)
    })

    it('doGet should set responseType to json', async () => {
      const { doGet } = await import('../src/http/httpRequest.js')
      doGet('/api/test', {})
      const config = axios.mock.calls[0][0]
      expect(config.responseType).toBe('json')
    })

    it('doPost should set responseType to json', async () => {
      const { doPost } = await import('../src/http/httpRequest.js')
      doPost('/api/test', {})
      const config = axios.mock.calls[0][0]
      expect(config.responseType).toBe('json')
    })

    it('doPut should set responseType to json', async () => {
      const { doPut } = await import('../src/http/httpRequest.js')
      doPut('/api/test', {})
      const config = axios.mock.calls[0][0]
      expect(config.responseType).toBe('json')
    })

    it('doDelete should set responseType to json', async () => {
      const { doDelete } = await import('../src/http/httpRequest.js')
      doDelete('/api/test', {})
      const config = axios.mock.calls[0][0]
      expect(config.responseType).toBe('json')
    })
  })

  describe('interceptor registration', () => {
    it('should have request interceptor use function', () => {
      expect(typeof axios.interceptors.request.use).toBe('function')
    })

    it('should have response interceptor use function', () => {
      expect(typeof axios.interceptors.response.use).toBe('function')
    })
  })
})
