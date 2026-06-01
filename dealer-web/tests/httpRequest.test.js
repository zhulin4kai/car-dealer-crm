import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'

// Capture the interceptor callbacks the source module registers, so each
// test can invoke them directly with a synthetic request/response and
// assert real side effects (storage mutation, dialog calls, redirect).
const requestHandlers = []
const responseSuccessHandlers = []
const responseErrorHandlers = []

vi.mock('axios', () => {
  const mockAxios = vi.fn(() => Promise.resolve({ data: { code: 200 } }))
  mockAxios.create = vi.fn(() => mockAxios)
  mockAxios.defaults = { baseURL: '' }
  mockAxios.get = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.post = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.put = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.delete = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.interceptors = {
    request: {
      use: vi.fn((onFulfilled, onRejected) => {
        requestHandlers.push({ onFulfilled, onRejected })
        return 0
      }),
    },
    response: {
      use: vi.fn((onFulfilled, onRejected) => {
        responseSuccessHandlers.push({ onFulfilled, onRejected })
        responseErrorHandlers.push({ onRejected })
        return 0
      }),
    },
  }
  return { default: mockAxios }
})

// Re-import under a controlled window.location so redirect targets can be
// asserted without actually navigating the test runner.
const originalLocation = window.location
beforeEach(() => {
  delete window.location
  window.location = Object.assign({}, originalLocation, { href: '' })
  sessionStorage.clear()
  localStorage.clear()
  vi.clearAllMocks()
  requestHandlers.length = 0
  responseSuccessHandlers.length = 0
  responseErrorHandlers.length = 0
  // Force a fresh import so interceptors register exactly once per test.
  vi.resetModules()
})

async function importHttp() {
  await import('../src/http/httpRequest.js')
  return {
    reqOnFulfilled: requestHandlers[0]?.onFulfilled,
    reqOnRejected: requestHandlers[0]?.onRejected,
    respOnFulfilled: responseSuccessHandlers[0]?.onFulfilled,
    respOnRejected: responseErrorHandlers[0]?.onRejected,
  }
}

describe('httpRequest.js - request interceptor', () => {
  it('adds Authorization header when sessionStorage has dlyk_token', async () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    const { reqOnFulfilled } = await importHttp()

    const config = { headers: {} }
    const result = reqOnFulfilled(config)

    expect(result.headers.Authorization).toBe('session-jwt')
    expect(result.headers.rememberMe).toBeUndefined()
  })

  it('adds Authorization AND rememberMe:true when only localStorage has dlyk_token', async () => {
    localStorage.setItem('dlyk_token', 'local-jwt')
    const { reqOnFulfilled } = await importHttp()

    const config = { headers: {} }
    const result = reqOnFulfilled(config)

    expect(result.headers.Authorization).toBe('local-jwt')
    expect(result.headers.rememberMe).toBe(true)
  })

  it('prefers sessionStorage token over localStorage token when both are set', async () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    localStorage.setItem('dlyk_token', 'local-jwt')
    const { reqOnFulfilled } = await importHttp()

    const config = { headers: {} }
    const result = reqOnFulfilled(config)

    expect(result.headers.Authorization).toBe('session-jwt')
    // rememberMe must NOT be set when sessionStorage is the source
    expect(result.headers.rememberMe).toBeUndefined()
  })

  it('leaves request headers untouched when neither storage has a token', async () => {
    const { reqOnFulfilled } = await importHttp()

    const config = { headers: { 'X-Caller': 'vitest' } }
    const result = reqOnFulfilled(config)

    expect(result.headers.Authorization).toBeUndefined()
    expect(result.headers.rememberMe).toBeUndefined()
    expect(result.headers['X-Caller']).toBe('vitest')
  })

  it('preserves the same config object reference (mutates, not replaces)', async () => {
    sessionStorage.setItem('dlyk_token', 'jwt')
    const { reqOnFulfilled } = await importHttp()

    const config = { headers: {} }
    const result = reqOnFulfilled(config)
    expect(result).toBe(config)
  })

  it('propagates request errors via rejected promise', async () => {
    const { reqOnRejected } = await importHttp()
    const err = new Error('network down')
    await expect(reqOnRejected(err)).rejects.toBe(err)
  })
})

describe('httpRequest.js - response interceptor (code >= 500)', () => {
  it('triggers messageConfirm with the response msg + "是否重新去登录？" on code >= 500', async () => {
    const { respOnFulfilled } = await importHttp()

    ElMessageBox.confirm.mockResolvedValue('ok')
    const response = { data: { code: 510, msg: 'TOKEN_IS_EMPTY' } }

    await expect(respOnFulfilled(response)).rejects.toThrow('TOKEN_IS_EMPTY')
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1)
    const [msg, title, opts] = ElMessageBox.confirm.mock.calls[0]
    expect(msg).toBe('TOKEN_IS_EMPTY，是否重新去登录？')
    expect(title).toBe('系统提醒')
    expect(opts).toMatchObject({
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  })

  it('when user confirms, calls removeToken() and navigates to /', async () => {
    sessionStorage.setItem('dlyk_token', 'old-jwt')
    localStorage.setItem('dlyk_token', 'old-jwt')
    const { respOnFulfilled } = await importHttp()

    ElMessageBox.confirm.mockResolvedValue('ok')
    // The source rejects with the response msg; we swallow it to assert side effects.
    await respOnFulfilled({ data: { code: 511, msg: 'TOKEN_IS_EXPIRED' } }).catch(() => {})

    // Flush microtasks chained on the .then(...) that called removeToken/redirect.
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()

    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
    expect(localStorage.getItem('dlyk_token')).toBeNull()
    expect(window.location.href).toBe('/')
  })

  it('when user cancels, shows messageTip, waits 1500ms, then removes token and redirects to /', async () => {
    vi.useFakeTimers()
    try {
      sessionStorage.setItem('dlyk_token', 'old-jwt')
      const { respOnFulfilled } = await importHttp()

      ElMessageBox.confirm.mockRejectedValue('cancel')
      await respOnFulfilled({ data: { code: 512, msg: 'TOKEN_INVALID' } }).catch(() => {})

      // Drain microtasks (catch path -> messageTip + setTimeout).
      await vi.runAllTimersAsync()
      // Advance to the inner 1500ms setTimeout.
      await vi.advanceTimersByTimeAsync(1500)

      expect(ElMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'warning',
          message: '登录已过期，即将跳转到登录页',
        })
      )
      expect(sessionStorage.getItem('dlyk_token')).toBeNull()
      expect(window.location.href).toBe('/')
    } finally {
      vi.useRealTimers()
    }
  })

  it('returns the response unchanged and does NOT call messageConfirm or redirect when code is 200', async () => {
    const { respOnFulfilled } = await importHttp()

    const response = { data: { code: 200, msg: 'ok', data: { foo: 1 } } }
    const result = await respOnFulfilled(response)

    expect(result).toBe(response)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(ElMessage).not.toHaveBeenCalled()
    expect(window.location.href).toBe('')
  })

  it('returns the response unchanged when code is in the 2xx-4xx business-failure range', async () => {
    // The source's contract is "code >= 500 => auth failure", so 200/300/4xx
    // must pass through. (Code 502 is intentionally NOT in this range because
    // 502 >= 500 in the current implementation; that broader behavior is
    // verified in the first test above.)
    const { respOnFulfilled } = await importHttp()

    for (const code of [200, 302, 400, 404, 422]) {
      vi.clearAllMocks()
      const response = { data: { code, msg: 'x' } }
      const result = await respOnFulfilled(response)
      expect(result).toBe(response)
      expect(ElMessageBox.confirm).not.toHaveBeenCalled()
      expect(window.location.href).toBe('')
    }
  })
})

describe('httpRequest.js - response interceptor (HTTP errors)', () => {
  it('propagates non-2xx response errors as rejected promises without any UI side effect', async () => {
    const { respOnRejected } = await importHttp()
    const err = new Error('Request failed with status code 500')

    await expect(respOnRejected(err)).rejects.toBe(err)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(window.location.href).toBe('')
  })
})
