import { clearPermissionCodes, readPermissionCodes, writePermissionCodes } from '@/shared/storage/permission-storage'
import {
  clearStoredToken,
  getTokenName,
  readStoredToken,
} from '@/shared/storage/token-storage'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { goBack } from '@/shared/utils/navigation'

export { getTokenName, goBack, messageConfirm, messageTip }

export function removeToken(): void {
  clearStoredToken()
  clearPermissionCodes()
}

export function getToken(): string | undefined {
  const storedToken = readStoredToken()
  if (storedToken) {
    return storedToken.token
  }

  void messageConfirm('请求token为空，是否重新去登录？')
    .then(() => {
      removeToken()
      window.location.href = '/'
    })
    .catch(() => {
      messageTip('取消去登录', 'warning')
    })

  return undefined
}

export function getUserPermission(): string[] | null {
  return readPermissionCodes()
}

export function setUserPermission(permissions: string[]): void {
  writePermissionCodes(permissions)
}

export function clearUserPermission(): void {
  clearPermissionCodes()
}
