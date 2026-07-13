import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

export type CredentialPageAction = 'activate' | 'reset'

export function getCredentialCommandErrorMessage(
  error: unknown,
  action: CredentialPageAction,
): string {
  const noun = action === 'activate' ? '邀请凭证' : '重置凭证'
  const retry = action === 'activate' ? '请重新获取邀请' : '请重新发起找回'
  if (!(error instanceof ApiError)) return '系统暂时不可用，请稍后重试。'
  if (error.code === API_ERROR_CODE.CREDENTIAL_INVALID) return `${noun}无效，${retry}。`
  if (error.code === API_ERROR_CODE.CREDENTIAL_EXPIRED) return `${noun}已过期，${retry}。`
  if (error.code === API_ERROR_CODE.CREDENTIAL_ALREADY_USED) return `${noun}已使用，${retry}。`
  if (error.code === API_ERROR_CODE.CREDENTIAL_RATE_LIMITED) {
    return '请求过于频繁，请稍后再试。'
  }
  if (error.code === API_ERROR_CODE.PASSWORD_POLICY_VIOLATION) {
    return '新密码不符合服务端密码策略，请调整后重试。'
  }
  if (error.code === API_ERROR_CODE.PASSWORD_HISTORY_REUSED) {
    return '新密码与近期使用过的密码重复，请更换后重试。'
  }
  return '系统暂时不可用，请稍后重试。'
}

export function isTerminalCredentialError(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    new Set<number>([
      API_ERROR_CODE.CREDENTIAL_INVALID,
      API_ERROR_CODE.CREDENTIAL_EXPIRED,
      API_ERROR_CODE.CREDENTIAL_ALREADY_USED,
    ]).has(error.code)
  )
}
