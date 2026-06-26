import type { PageQuery } from '@/shared/api/api-types'

export type AuditResult = 'SUCCESS' | 'FAILURE'

export interface AuditLoginLog {
  id: number
  loginAct: string
  userId?: number | null
  userName?: string | null
  result: AuditResult
  reasonCode: string
  reasonMessage?: string | null
  ip?: string | null
  browser?: string | null
  os?: string | null
  requestId?: string | null
  createTime: string
}

export interface AuditOperationLog {
  id: number
  userId?: number | null
  userName?: string | null
  actionCode: string
  moduleName?: string | null
  objectType?: string | null
  resourceId?: string | null
  result?: AuditResult | string | null
  detail?: string | null
  ip?: string | null
  requestId?: string | null
  createTime: string
}

export interface AuditLoginLogQuery extends Partial<PageQuery> {
  loginAct?: string
  userName?: string
  result?: AuditResult | ''
  reasonCode?: string
  ip?: string
  requestId?: string
  startTime?: string
  endTime?: string
}

export interface AuditOperationLogQuery extends Partial<PageQuery> {
  userName?: string
  actionCode?: string
  moduleName?: string
  objectType?: string
  resourceId?: string
  result?: AuditResult | ''
  ip?: string
  requestId?: string
  startTime?: string
  endTime?: string
}

export const AUDIT_RESULT_LABEL: Record<AuditResult, string> = {
  SUCCESS: '成功',
  FAILURE: '失败',
}

export const LOGIN_REASON_LABEL: Record<string, string> = {
  SUCCESS: '登录成功',
  BAD_CREDENTIALS: '账号或密码错误',
  ACCOUNT_DISABLED: '账号已停用',
  ACCOUNT_LOCKED: '账号已锁定',
  ACCOUNT_EXPIRED: '账号已过期',
  CREDENTIALS_EXPIRED: '凭证已过期',
  AUTHENTICATION_FAILED: '认证失败',
}
