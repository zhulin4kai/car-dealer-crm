import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

export function isAccessVersionConflict(error: unknown): error is ApiError {
  return (
    error instanceof ApiError &&
    (error.code === API_ERROR_CODE.CONFLICT || error.code === API_ERROR_CODE.ROLE_VERSION_CONFLICT)
  )
}

export function getAccessErrorMessage(
  error: unknown,
  fallback: string,
  accessDeniedMessage = '没有权限维护该角色，或角色影响范围超出可管理边界',
): string {
  if (!(error instanceof ApiError)) return fallback
  if (isAccessVersionConflict(error)) {
    return '角色或权限矩阵已被其他人更新，页面将刷新最新内容，请重新预览'
  }
  switch (error.code) {
    case API_ERROR_CODE.ACCESS_DENIED:
      return accessDeniedMessage
    case API_ERROR_CODE.PROTECTED_ROLE_FORBIDDEN:
      return '受保护恢复角色不能编辑、停用、复制或削弱权限'
    case API_ERROR_CODE.ROLE_PERMISSION_INVALID:
      return '权限集合包含未知、停用或不可分配的权限，请刷新权限目录'
    case API_ERROR_CODE.ROLE_PERMISSION_LIMIT:
      return '角色、权限或数据范围超过当前账号的授权上限'
    case API_ERROR_CODE.ROLE_IN_USE:
    case API_ERROR_CODE.RESOURCE_IN_USE:
      return '该角色仍有关联用户，当前操作不能执行'
    default:
      return fallback
  }
}

export function getUserAuthorizationErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  if (isAccessVersionConflict(error)) {
    return '用户授权已被其他管理者更新，页面将刷新为最新版本'
  }
  switch (error.code) {
    case API_ERROR_CODE.ACCESS_DENIED:
      return '不能调整本人、同级、上级、范围外人员或受保护账号的授权'
    case API_ERROR_CODE.LAST_AVAILABLE_ADMIN_REQUIRED:
      return '系统必须保留至少一个可用普通管理员，不能完成本次角色撤销'
    default:
      return fallback
  }
}
