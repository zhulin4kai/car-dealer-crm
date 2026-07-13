import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export const USER_HISTORY_ACTION = {
  VIEW: 'VIEW',
} as const

export type UserHistoryAction = (typeof USER_HISTORY_ACTION)[keyof typeof USER_HISTORY_ACTION]

export interface UserHistoryQuery {
  page: number
  size: number
  actionCode?: string
  startTime?: string
  endTime?: string
}

export interface UserHistoryActionOption {
  code: string
  label: string
}

export interface UserHistoryOperatorSummary {
  id?: EntityId | null
  name: string
  employeeNo?: string | null
}

export interface UserHistoryValueField {
  code: string
  label: string
  valueCode?: string | null
  valueName?: string | null
  displayValue?: string | null
}

export interface UserHistoryTargetSummary {
  typeCode: string
  typeName: string
  id?: EntityId | null
  code?: string | null
  name?: string | null
}

export interface UserHistoryBatchSummary {
  batchId: string
  totalCount: number
  successCount: number
  failureCount: number
  targetResultCode: string
  targetResultName: string
}

export interface UserHistoryItem {
  eventId: string
  sourceKey: string
  actionCode: string
  actionName: string
  categoryCode: string
  categoryName: string
  target: UserHistoryTargetSummary
  operator: UserHistoryOperatorSummary
  beforeValues: UserHistoryValueField[]
  afterValues: UserHistoryValueField[]
  reason?: string | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
  resultCode: string
  resultName: string
  batchSummary?: UserHistoryBatchSummary | null
  occurredAt: string
}

export interface UserHistoryCollection extends PageResult<UserHistoryItem> {
  actionOptions: UserHistoryActionOption[]
  allowedActions: UserHistoryAction[]
  unavailableReasons: Partial<Record<UserHistoryAction, string>>
}
