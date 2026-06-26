import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/shared/api/api-error'
import { doDelete, doGet, doPost, doPut, httpClient } from '@/shared/api/http-client'
import { API_ERROR_CODE, isSessionInvalidCode } from '@/shared/api/error-codes'
import {
  registerSessionInvalidHandler,
  resetSessionInvalidHandler,
} from '@/shared/auth/session-invalid-handler'
import { writeStoredToken } from '@/shared/storage/token-storage'

const mockedAxios = vi.mocked(axios)

function createDeferred<T = void>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function createJsonBlob(text: string): Blob {
  return new Blob([text], { type: 'application/json' })
}

describe('http client', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
    localStorage.clear()
    sessionStorage.clear()
    resetSessionInvalidHandler()
  })

  it('unwraps backend envelopes', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: { code: 200, msg: 'OK', data: { ok: true } },
    })

    await expect(doGet('/api/test', { id: 1 })).resolves.toEqual({ ok: true })
    expect(mockedAxios.request).toHaveBeenCalledWith({
      method: 'get',
      url: '/api/test',
      params: { id: 1 },
    })
  })

  it('supports all HTTP methods', async () => {
    await doPost('/api/post', { name: 'x' })
    await doPut('/api/put', { id: 1 })
    await doDelete('/api/delete', { id: 1 })

    expect(mockedAxios.request.mock.calls.map(([config]) => config.method)).toEqual([
      'post',
      'put',
      'delete',
    ])
  })

  it('keeps token storage available for request interceptors', () => {
    writeStoredToken('jwt-token', true)
    expect(localStorage.getItem('dlyk_token')).toBe('jwt-token')
  })

  it('adds bearer authorization header from stored token', () => {
    writeStoredToken('jwt-token', true)
    const interceptor = mockedAxios.interceptors.request.use.mock.calls[0]?.[0] as
      | ((config: { headers?: Record<string, unknown> }) => { headers?: Record<string, unknown> })
      | undefined

    const config = interceptor?.({ headers: {} })

    expect(config?.headers?.Authorization).toBe('Bearer jwt-token')
    expect(config?.headers?.rememberMe).toBe(true)
  })

  it('distinguishes login failure from invalid sessions by stable code', () => {
    expect(isSessionInvalidCode(API_ERROR_CODE.AUTH_LOGIN_FAILED)).toBe(false)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_EMPTY)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_INVALID)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_EXPIRED)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_MISMATCH)).toBe(true)
  })

  it('notifies session-invalid handler once for concurrent expired-token responses', async () => {
    const deferred = createDeferred()
    const handler = vi.fn(() => deferred.promise)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockResolvedValue({
      data: { code: API_ERROR_CODE.TOKEN_EXPIRED, msg: 'token已过期', data: null },
    })

    const calls = Array.from({ length: 5 }, () => doGet('/api/test'))
    const results = await Promise.allSettled(calls)

    expect(handler).toHaveBeenCalledTimes(1)
    expect(handler).toHaveBeenCalledWith({ code: API_ERROR_CODE.TOKEN_EXPIRED, msg: 'token已过期' })
    results.forEach((result) => {
      expect(result.status).toBe('rejected')
      if (result.status === 'rejected') {
        expect(result.reason).toBeInstanceOf(ApiError)
        expect(result.reason.isSessionInvalid).toBe(true)
        expect(result.reason.code).toBe(API_ERROR_CODE.TOKEN_EXPIRED)
      }
    })

    deferred.resolve()
    await deferred.promise.catch(() => undefined)
  })

  it('marks ApiError with isSessionInvalid for session-invalid codes', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: { code: API_ERROR_CODE.TOKEN_INVALID, msg: 'token无效', data: null },
    })

    try {
      await doGet('/api/test')
      expect.fail('should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError)
      const apiError = error as ApiError
      expect(apiError.code).toBe(API_ERROR_CODE.TOKEN_INVALID)
      expect(apiError.isSessionInvalid).toBe(true)
    }
  })

  it('does not notify session-invalid handler for access-denied code 520', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockResolvedValueOnce({
      data: { code: 520, msg: '没有访问权限', data: null },
    })

    await expect(doGet('/api/test')).rejects.toThrow('没有访问权限')
    await Promise.resolve()
    expect(handler).not.toHaveBeenCalled()
  })

  it('does not notify session-invalid handler for network errors', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockRejectedValueOnce(new Error('Network Error'))

    await expect(doGet('/api/test')).rejects.toThrow('Network Error')
    await Promise.resolve()
    expect(handler).not.toHaveBeenCalled()
  })
})

describe('httpClient.download', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
    localStorage.clear()
    sessionStorage.clear()
    resetSessionInvalidHandler()
  })

  it('returns blob and filename from Content-Disposition filename*', async () => {
    const blob = new Blob(['fake-excel'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    mockedAxios.request.mockResolvedValueOnce({
      data: blob,
      headers: {
        'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'content-disposition': "attachment; filename=\"fake.xlsx\"; filename*=UTF-8''%E5%AE%A2%E6%88%B7%E4%BF%A1%E6%81%AF%E6%95%B0%E6%8D%AE1.xlsx",
      },
    })

    const result = await httpClient.download('/api/exportExcel')

    expect(result.blob).toBe(blob)
    expect(result.filename).toBe('客户信息数据1.xlsx')
    expect(mockedAxios.request).toHaveBeenCalledWith({
      method: 'get',
      url: '/api/exportExcel',
      responseType: 'blob',
    })
  })

  it('falls back to filename= when filename* is absent', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: new Blob(['x']),
      headers: {
        'content-type': 'application/octet-stream',
        'content-disposition': 'attachment; filename="report.xlsx"',
      },
    })

    const result = await httpClient.download('/api/exportExcel')
    expect(result.filename).toBe('report.xlsx')
  })

  it('uses default filename when Content-Disposition is missing', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: new Blob(['x']),
      headers: { 'content-type': 'application/octet-stream' },
    })

    const result = await httpClient.download('/api/exportExcel')
    expect(result.filename).toBe('download.bin')
  })

  it('converts Blob error envelope to ApiError for session-invalid code', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockResolvedValueOnce({
      data: createJsonBlob(JSON.stringify({ code: API_ERROR_CODE.TOKEN_EXPIRED, msg: 'token已过期', data: null })),
      headers: { 'content-type': 'application/json' },
    })

    await expect(httpClient.download('/api/exportExcel')).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      const apiError = error as ApiError
      expect(apiError.code).toBe(API_ERROR_CODE.TOKEN_EXPIRED)
      expect(apiError.isSessionInvalid).toBe(true)
      return true
    })
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('converts Blob error envelope to ApiError for access-denied code without clearing session', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockResolvedValueOnce({
      data: createJsonBlob(JSON.stringify({ code: 520, msg: '没有访问权限', data: null })),
      headers: { 'content-type': 'application/json' },
    })

    await expect(httpClient.download('/api/exportExcel')).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      const apiError = error as ApiError
      expect(apiError.code).toBe(520)
      expect(apiError.isSessionInvalid).toBe(false)
      return true
    })
    expect(handler).not.toHaveBeenCalled()
  })

  it('passes through network errors without parsing', async () => {
    mockedAxios.request.mockRejectedValueOnce(new Error('Network Error'))

    await expect(httpClient.download('/api/exportExcel')).rejects.toThrow('Network Error')
  })
})
