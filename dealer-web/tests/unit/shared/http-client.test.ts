import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { doDelete, doGet, doPost, doPut } from '@/shared/api/http-client'
import { API_ERROR_CODE, isSessionInvalidCode } from '@/shared/api/error-codes'
import { writeStoredToken } from '@/shared/storage/token-storage'

const mockedAxios = vi.mocked(axios)

describe('http client', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    localStorage.clear()
    sessionStorage.clear()
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

  it('distinguishes login failure from invalid sessions by stable code', () => {
    expect(isSessionInvalidCode(API_ERROR_CODE.AUTH_LOGIN_FAILED)).toBe(false)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_EMPTY)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_INVALID)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_EXPIRED)).toBe(true)
    expect(isSessionInvalidCode(API_ERROR_CODE.TOKEN_MISMATCH)).toBe(true)
  })
})
