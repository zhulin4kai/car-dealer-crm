import { httpClient } from '@/shared/api/http-client'
import type { DownloadResult, PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  AuditLoginLog,
  AuditLoginLogQuery,
  AuditOperationLog,
  AuditOperationLogQuery,
} from '@/modules/audit/model/audit.types'

export function fetchLoginLogPage(
  params: AuditLoginLogQuery,
  signal?: AbortSignal,
): Promise<PageResult<AuditLoginLog>> {
  return httpClient.get<PageResult<AuditLoginLog>>('/api/audit/login-logs', { params, signal })
}

export function fetchLoginLogDetail(id: EntityId): Promise<AuditLoginLog> {
  return httpClient.get<AuditLoginLog>(`/api/audit/login-logs/${id}`)
}

export function exportLoginLogs(params: AuditLoginLogQuery): Promise<DownloadResult> {
  return httpClient.download('/api/audit/login-logs/export', { params })
}

export function fetchOperationLogPage(
  params: AuditOperationLogQuery,
  signal?: AbortSignal,
): Promise<PageResult<AuditOperationLog>> {
  return httpClient.get<PageResult<AuditOperationLog>>('/api/audit/operation-logs', {
    params,
    signal,
  })
}

export function fetchOperationLogDetail(id: EntityId): Promise<AuditOperationLog> {
  return httpClient.get<AuditOperationLog>(`/api/audit/operation-logs/${id}`)
}

export function exportOperationLogs(params: AuditOperationLogQuery): Promise<DownloadResult> {
  return httpClient.download('/api/audit/operation-logs/export', { params })
}
