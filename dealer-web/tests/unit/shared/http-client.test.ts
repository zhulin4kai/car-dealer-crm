import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/shared/api/api-error'
import { httpClient } from '@/shared/api/http-client'
import { API_ERROR_CODE, isSessionInvalidCode } from '@/shared/api/error-codes'
import {
  registerSessionInvalidHandler,
  resetSessionInvalidHandler,
} from '@/shared/auth/session-invalid-handler'
import {
  registerUserManagementGateHandler,
  resetUserManagementGateHandler,
} from '@/shared/auth/user-management-gate-handler'
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
    resetUserManagementGateHandler()
  })

  it('unwraps backend envelopes', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: { code: 200, msg: 'OK', data: { ok: true } },
    })

    await expect(httpClient.get('/api/test', { params: { id: 1 } })).resolves.toEqual({ ok: true })
    expect(mockedAxios.request).toHaveBeenCalledWith({
      method: 'get',
      url: '/api/test',
      params: { id: 1 },
    })
  })

  it('supports all HTTP methods', async () => {
    await httpClient.post('/api/post', { name: 'x' })
    await httpClient.put('/api/put', { id: 1 })
    await httpClient.delete('/api/delete', { id: 1 })

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

    const calls = Array.from({ length: 5 }, () => httpClient.get('/api/test'))
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
      await httpClient.get('/api/test')
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

    await expect(httpClient.get('/api/test')).rejects.toThrow('没有访问权限')
    await Promise.resolve()
    expect(handler).not.toHaveBeenCalled()
  })

  it('notifies session-invalid handler for HTTP 401 compatibility errors', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })
    const rejectResponse = mockedAxios.interceptors.response.use.mock.calls[0]?.[1] as
      | ((error: unknown) => Promise<never>)
      | undefined

    const rejection = rejectResponse?.({
      response: {
        status: 401,
        data: { code: 505, msg: 'token已过期', data: null },
      },
    })

    await expect(rejection).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).isSessionInvalid).toBe(true)
      return true
    })
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not clear the session for HTTP 403 even when the response code is inconsistent', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })
    const rejectResponse = mockedAxios.interceptors.response.use.mock.calls[0]?.[1] as
      | ((error: unknown) => Promise<never>)
      | undefined

    const rejection = rejectResponse?.({
      response: {
        status: 403,
        data: { code: API_ERROR_CODE.TOKEN_EXPIRED, msg: '没有访问权限', data: null },
      },
    })

    await expect(rejection).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).isSessionInvalid).toBe(false)
      return true
    })
    expect(handler).not.toHaveBeenCalled()
  })

  it.each([
    API_ERROR_CODE.ADMIN_BOOTSTRAP_REQUIRED,
    API_ERROR_CODE.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN,
  ])('notifies the user-management gate handler for stable code %s', async (code) => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerUserManagementGateHandler({ handleUserManagementGate: handler })
    mockedAxios.request.mockResolvedValueOnce({
      data: { code, msg: '用户管理门禁阻断', data: null },
    })

    await expect(httpClient.get('/api/test')).rejects.toMatchObject({ code })
    await Promise.resolve()

    expect(handler).toHaveBeenCalledWith({ code })
  })

  it('preserves the HTTP status alongside the stable business code', async () => {
    const rejectResponse = mockedAxios.interceptors.response.use.mock.calls[0]?.[1] as
      | ((error: unknown) => Promise<never>)
      | undefined

    const rejection = rejectResponse?.({
      response: {
        status: 409,
        data: { code: API_ERROR_CODE.ROLE_VERSION_CONFLICT, msg: '个人资料版本冲突', data: null },
      },
    })

    await expect(rejection).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).code).toBe(API_ERROR_CODE.ROLE_VERSION_CONFLICT)
      expect((error as ApiError).httpStatus).toBe(409)
      return true
    })
  })

  it('does not treat an HTTP 401 login failure as an invalid existing session', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })
    const rejectResponse = mockedAxios.interceptors.response.use.mock.calls[0]?.[1] as
      | ((error: unknown) => Promise<never>)
      | undefined

    const rejection = rejectResponse?.({
      response: {
        status: 401,
        data: { code: API_ERROR_CODE.AUTH_LOGIN_FAILED, msg: '账号或密码错误', data: null },
      },
    })

    await expect(rejection).rejects.toSatisfy((error: unknown) => {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).isSessionInvalid).toBe(false)
      return true
    })
    expect(handler).not.toHaveBeenCalled()
  })

  it('does not notify session-invalid handler for network errors', async () => {
    const handler = vi.fn().mockResolvedValue(undefined)
    registerSessionInvalidHandler({ handleSessionInvalid: handler })

    mockedAxios.request.mockRejectedValueOnce(new Error('Network Error'))

    await expect(httpClient.get('/api/test')).rejects.toThrow('Network Error')
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
    resetUserManagementGateHandler()
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
