import type { EntityId } from '@/shared/types/id'

export const OWNER_QUALIFICATION_CONTEXT = {
  ACTIVITY_OWNER: 'ACTIVITY_OWNER',
  CLUE_OWNER: 'CLUE_OWNER',
  CUSTOMER_OWNER: 'CUSTOMER_OWNER',
  TRANSACTION_OWNER: 'TRANSACTION_OWNER',
} as const

export type OwnerQualificationContext =
  (typeof OWNER_QUALIFICATION_CONTEXT)[keyof typeof OWNER_QUALIFICATION_CONTEXT]

export interface OwnerCandidateQuery {
  permissionCode: string
  qualificationContext: OwnerQualificationContext
}

/**
 * 负责人选择只消费必要的展示事实，不接收账号安全字段、角色集合或权限集合。
 */
export interface OwnerCandidate {
  userId: EntityId
  name: string
  employeeId?: EntityId | null
  employeeNo?: string | null
  organizationUnitId?: EntityId | null
  organizationName?: string | null
  positionId?: EntityId | null
  positionName?: string | null
}
