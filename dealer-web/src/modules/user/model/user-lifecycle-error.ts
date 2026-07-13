import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

export function getUserLifecycleErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  if (
    error.httpStatus === 403 ||
    error.code === 403 ||
    error.code === API_ERROR_CODE.ACCESS_DENIED
  ) {
    return '无权执行该人员流程，可能是本人、同级、上级、范围外或受保护账号'
  }
  if (error.httpStatus === 404 || error.code === 404) return '目标员工或生命周期事实不存在'
  if (isUserLifecycleSnapshotExpired(error)) return '离职预检已过期，请重新执行预检'
  if (isUserLifecycleConflict(error)) return '人员状态或版本已变化，已刷新最新事实'
  return fallback
}

export function isUserLifecycleConflict(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.httpStatus === 409 ||
      error.code === API_ERROR_CODE.CONFLICT ||
      error.code === API_ERROR_CODE.ROLE_VERSION_CONFLICT ||
      error.code === API_ERROR_CODE.USER_LIFECYCLE_CONFLICT ||
      error.code === API_ERROR_CODE.USER_HANDOVER_QUALIFICATION_CHANGED ||
      error.code === API_ERROR_CODE.USER_HANDOVER_COUNT_MISMATCH ||
      error.code === API_ERROR_CODE.USER_HANDOVER_SCHEDULE_CONFLICT)
  )
}

export function isUserLifecycleSnapshotExpired(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.httpStatus === 410 || error.code === API_ERROR_CODE.USER_LIFECYCLE_SNAPSHOT_EXPIRED)
  )
}
