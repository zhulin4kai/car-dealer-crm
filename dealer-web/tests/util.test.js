import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  goBack,
  getToken,
  removeToken,
  messageTip,
  getTokenName,
  messageConfirm,
  getUserPermission,
  setUserPermission,
  clearUserPermission,
} from '../src/util/util.js'

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  vi.clearAllMocks()
})

describe('util.js - goBack()', () => {
  it('calls window.history.back exactly once', () => {
    const spy = vi.spyOn(window.history, 'back')
    goBack()
    expect(spy).toHaveBeenCalledTimes(1)
    spy.mockRestore()
  })
})

describe('util.js - getTokenName()', () => {
  it('returns the literal "dlyk_token" (must match util.js getTokenName contract)', () => {
    // Hardcoded because if someone renames the storage key in source without
    // updating httpRequest.js's interceptor, the rest of the system breaks.
    expect(getTokenName()).toBe('dlyk_token')
  })
})

describe('util.js - getToken()', () => {
  it('returns the sessionStorage token when only sessionStorage is set', () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    localStorage.clear()
    expect(getToken()).toBe('session-jwt')
  })

  it('returns the localStorage token when only localStorage is set', () => {
    sessionStorage.clear()
    localStorage.setItem('dlyk_token', 'local-jwt')
    expect(getToken()).toBe('local-jwt')
  })

  it('prefers sessionStorage over localStorage when both are set', () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    localStorage.setItem('dlyk_token', 'local-jwt')
    expect(getToken()).toBe('session-jwt')
  })

  it('returns undefined when neither storage has the token', () => {
    expect(getToken()).toBeUndefined()
  })
})

describe('util.js - removeToken()', () => {
  it('clears sessionStorage.dlyk_token', () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    removeToken()
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
  })

  it('clears localStorage.dlyk_token', () => {
    localStorage.setItem('dlyk_token', 'local-jwt')
    removeToken()
    expect(localStorage.getItem('dlyk_token')).toBeNull()
  })

  it('clears BOTH storages in one call (does not stop after the first hit)', () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    localStorage.setItem('dlyk_token', 'local-jwt')
    removeToken()
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
    expect(localStorage.getItem('dlyk_token')).toBeNull()
  })

  it('is a no-op when no token exists (does not corrupt other keys)', () => {
    sessionStorage.setItem('unrelated', 'keep-me')
    localStorage.setItem('unrelated', 'keep-me')
    removeToken()
    expect(sessionStorage.getItem('unrelated')).toBe('keep-me')
    expect(localStorage.getItem('unrelated')).toBe('keep-me')
  })
})

describe('util.js - messageTip()', () => {
  it('forwards (msg, type) to ElMessage with showClose:true, center:true, duration:3000', () => {
    messageTip('保存成功', 'success')
    expect(ElMessage).toHaveBeenCalledTimes(1)
    const cfg = ElMessage.mock.calls[0][0]
    expect(cfg).toMatchObject({
      message: '保存成功',
      type: 'success',
      showClose: true,
      center: true,
      duration: 3000,
    })
  })

  it('passes the type argument through (error)', () => {
    messageTip('出错了', 'error')
    expect(ElMessage.mock.calls[0][0].type).toBe('error')
  })

  it('passes the type argument through (warning)', () => {
    messageTip('请注意', 'warning')
    expect(ElMessage.mock.calls[0][0].type).toBe('warning')
  })

  it('passes the type argument through (info)', () => {
    messageTip('提示信息', 'info')
    expect(ElMessage.mock.calls[0][0].type).toBe('info')
  })

  it('forwards an empty string message without crashing', () => {
    messageTip('', 'success')
    expect(ElMessage).toHaveBeenCalledTimes(1)
    expect(ElMessage.mock.calls[0][0].message).toBe('')
  })
})

describe('util.js - messageConfirm()', () => {
  it('returns a Promise', () => {
    ElMessageBox.confirm.mockResolvedValue('ok')
    const result = messageConfirm('确认操作？')
    expect(result).toBeInstanceOf(Promise)
  })

  it('forwards message, title "系统提醒" and warning options to ElMessageBox.confirm', async () => {
    ElMessageBox.confirm.mockResolvedValue('ok')
    const p = messageConfirm('是否继续？')
    await p

    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1)
    const [msg, title, opts] = ElMessageBox.confirm.mock.calls[0]
    expect(msg).toBe('是否继续？')
    expect(title).toBe('系统提醒')
    expect(opts).toMatchObject({
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  })

  it('resolves with whatever ElMessageBox.confirm resolves to', async () => {
    ElMessageBox.confirm.mockResolvedValue({ value: 'ok' })
    await expect(messageConfirm('x')).resolves.toEqual({ value: 'ok' })
  })

  it('rejects with whatever ElMessageBox.confirm rejects to', async () => {
    ElMessageBox.confirm.mockRejectedValue(new Error('cancel'))
    await expect(messageConfirm('x')).rejects.toThrow('cancel')
  })
})

describe('util.js - user permission cache', () => {
  it('getUserPermission() returns null when nothing is cached', () => {
    expect(getUserPermission()).toBeNull()
  })

  it('setUserPermission() writes the permissions array to sessionStorage.user_permissions as JSON', () => {
    const perms = ['user:list', 'user:add', 'clue:list']
    setUserPermission(perms)
    const stored = sessionStorage.getItem('user_permissions')
    expect(stored).not.toBeNull()
    expect(JSON.parse(stored)).toEqual(perms)
  })

  it('getUserPermission() round-trips the cached value', () => {
    const perms = ['user:list', 'user:add']
    setUserPermission(perms)
    expect(getUserPermission()).toEqual(perms)
  })

  it('setUserPermission() overwrites a previous value', () => {
    setUserPermission(['old:permission'])
    setUserPermission(['new:permission'])
    expect(getUserPermission()).toEqual(['new:permission'])
  })

  it('getUserPermission() returns null and does not throw on invalid JSON', () => {
    sessionStorage.setItem('user_permissions', 'not-json{{')
    expect(getUserPermission()).toBeNull()
  })

  it('clearUserPermission() removes the key from sessionStorage', () => {
    setUserPermission(['anything'])
    clearUserPermission()
    expect(sessionStorage.getItem('user_permissions')).toBeNull()
  })

  it('clearUserPermission() does not affect other sessionStorage keys', () => {
    sessionStorage.setItem('dlyk_token', 'jwt')
    setUserPermission(['x'])
    clearUserPermission()
    expect(sessionStorage.getItem('dlyk_token')).toBe('jwt')
  })
})
