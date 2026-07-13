import { describe, expect, it } from 'vitest'

import {
  getSessionCommandErrorMessage,
  isRefreshableSessionError,
} from '@/modules/user/model/user-session-error'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

describe('user session error contract', () => {
  it.each([
    [API_ERROR_CODE.SESSION_NOT_FOUND, '会话不存在或无权访问，列表将刷新', true],
    [API_ERROR_CODE.SESSION_REVOKED, '会话已被撤销，列表将刷新', true],
    [API_ERROR_CODE.SESSION_EXPIRED, '会话已过期，列表将刷新', true],
    [API_ERROR_CODE.SESSION_VERSION_CONFLICT, '会话状态已变化，列表将刷新', true],
    [API_ERROR_CODE.SESSION_CACHE_FAILED, '会话服务暂不可用，请稍后重试', false],
  ])('maps stable code %s to an exact action message', (code, message, refreshable) => {
    const error = new ApiError(code, 'server message', null)
    expect(getSessionCommandErrorMessage(error, 'fallback')).toBe(message)
    expect(isRefreshableSessionError(error)).toBe(refreshable)
  })
})
