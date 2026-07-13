import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

const REFRESHABLE_SESSION_CODES = new Set<number>([
  API_ERROR_CODE.SESSION_NOT_FOUND,
  API_ERROR_CODE.SESSION_REVOKED,
  API_ERROR_CODE.SESSION_EXPIRED,
  API_ERROR_CODE.SESSION_VERSION_CONFLICT,
  API_ERROR_CODE.CONFLICT,
])

export function isRefreshableSessionError(error: unknown): error is ApiError {
  return error instanceof ApiError && REFRESHABLE_SESSION_CODES.has(error.code)
}

export function getSessionCommandErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  switch (error.code) {
    case API_ERROR_CODE.SESSION_NOT_FOUND:
      return '会话不存在或无权访问，列表将刷新'
    case API_ERROR_CODE.SESSION_REVOKED:
      return '会话已被撤销，列表将刷新'
    case API_ERROR_CODE.SESSION_EXPIRED:
      return '会话已过期，列表将刷新'
    case API_ERROR_CODE.SESSION_VERSION_CONFLICT:
    case API_ERROR_CODE.CONFLICT:
      return '会话状态已变化，列表将刷新'
    case API_ERROR_CODE.SESSION_CACHE_FAILED:
      return '会话服务暂不可用，请稍后重试'
    default:
      return fallback
  }
}
