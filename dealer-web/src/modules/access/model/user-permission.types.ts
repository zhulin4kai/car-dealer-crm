import type { DataScopeCode, PermissionSensitivity } from '@/modules/access/model/access.types'
import type { EntityId } from '@/shared/types/id'

export const USER_AUTHORIZATION_ACTION = {
  ROLE_UPDATE: 'ROLE_UPDATE',
  PERMISSION_UPDATE: 'PERMISSION_UPDATE',
  SESSION_VIEW: 'SESSION_VIEW',
  SESSION_REVOKE: 'SESSION_REVOKE',
} as const

export type UserAuthorizationAction =
  (typeof USER_AUTHORIZATION_ACTION)[keyof typeof USER_AUTHORIZATION_ACTION]

export const PERSONAL_PERMISSION_STATE = {
  INHERIT: 'INHERIT',
  GRANT: 'GRANT',
  DENY: 'DENY',
} as const

export type PersonalPermissionState =
  (typeof PERSONAL_PERMISSION_STATE)[keyof typeof PERSONAL_PERMISSION_STATE]

export const PERSONAL_PERMISSION_STATE_LABEL: Record<PersonalPermissionState, string> = {
  INHERIT: '继承角色',
  GRANT: '个人增加',
  DENY: '个人拒绝',
}

export const ROLE_ASSIGNMENT_SOURCE = {
  DIRECT: 'DIRECT',
  SYSTEM: 'SYSTEM',
  PROTECTED: 'PROTECTED',
} as const

export type RoleAssignmentSource =
  (typeof ROLE_ASSIGNMENT_SOURCE)[keyof typeof ROLE_ASSIGNMENT_SOURCE]

export const ROLE_ASSIGNMENT_SOURCE_LABEL: Record<RoleAssignmentSource, string> = {
  DIRECT: '直接分配',
  SYSTEM: '系统来源',
  PROTECTED: '受保护来源',
}

export interface AuthorizationTargetUser {
  id: EntityId
  loginAct: string
  name: string
  employeeNo?: string | null
  organizationName?: string | null
  positionName?: string | null
  accountEnabled: boolean
  protectedAccount: boolean
}

export interface UserRoleAssignmentItem {
  roleId: EntityId
  roleCode: string
  roleName: string
  source: RoleAssignmentSource
  sourceDescription?: string | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
}

export interface UserRoleCandidate {
  roleId: EntityId
  roleCode: string
  roleName: string
  authorizationLevel: number
  defaultDataScope: DataScopeCode
  selected: boolean
  editable: boolean
  unavailableReason?: string | null
}

export interface DelegableDataScopeCandidate {
  candidateKey: string
  code: DataScopeCode
  label: string
  description?: string | null
  organizationNames?: string[]
  organizationIds?: EntityId[]
}

export const PERMISSION_SOURCE_TYPE = {
  ROLE: 'ROLE',
  PERSONAL_GRANT: 'PERSONAL_GRANT',
  PERSONAL_DENY: 'PERSONAL_DENY',
} as const

export type PermissionSourceType =
  (typeof PERMISSION_SOURCE_TYPE)[keyof typeof PERMISSION_SOURCE_TYPE]

export const PERMISSION_SOURCE_TYPE_LABEL: Record<PermissionSourceType, string> = {
  ROLE: '角色来源',
  PERSONAL_GRANT: '个人增加',
  PERSONAL_DENY: '个人拒绝',
}

export interface UserPermissionSource {
  type: PermissionSourceType
  sourceId?: EntityId | null
  sourceName: string
  dataScopeLabel?: string | null
  organizationIds?: EntityId[]
  organizationNames?: string[]
  effectiveFrom?: string | null
  effectiveTo?: string | null
  active: boolean
}

export interface UserPermissionAuthorizationItem {
  permissionId: EntityId
  code: string
  name: string
  module: string
  description?: string | null
  sensitivityLevel: PermissionSensitivity
  delegable: boolean
  effective: boolean
  personalState: PersonalPermissionState
  personalDataScopeCandidateKey?: string | null
  personalOrganizationIds?: EntityId[]
  personalEffectiveFrom?: string | null
  personalEffectiveTo?: string | null
  editable: boolean
  unavailableReason?: string | null
  sources: UserPermissionSource[]
  dataScopeCandidates: DelegableDataScopeCandidate[]
}

export interface UserAuthorizationDetail {
  user: AuthorizationTargetUser
  authorizationVersion: number
  allowedActions: UserAuthorizationAction[]
  unavailableReasons: Partial<Record<UserAuthorizationAction, string>>
  roleAssignments: UserRoleAssignmentItem[]
  roleCandidates: UserRoleCandidate[]
  permissions: UserPermissionAuthorizationItem[]
}

export interface UpdateUserRoleAssignmentsRequest {
  authorizationVersion: number
  roleIds: EntityId[]
  reason: string
}

export interface UserPermissionChangeInput {
  permissionId: EntityId
  state: PersonalPermissionState
  dataScopeCandidateKey?: string
  customOrganizationUnitIds?: EntityId[]
  /** GRANT/DENY 省略表示立即生效；显式时间只能预约当前至一年内。 */
  effectiveFrom?: string
  /** 必须晚于本次实际生效时间；INHERIT 不携带有效期。 */
  effectiveTo?: string
}

export interface UpdateUserPermissionsRequest {
  authorizationVersion: number
  changes: UserPermissionChangeInput[]
  reason: string
}

export interface UserAuthorizationBatchTarget {
  userId: EntityId
  authorizationVersion: number
}

export type BatchRoleOperation = 'ASSIGN' | 'UNASSIGN'

export interface BatchUpdateUserRolesRequest {
  targets: UserAuthorizationBatchTarget[]
  operation: BatchRoleOperation
  roleIds: EntityId[]
  reason: string
}

export interface BatchUpdateUserPermissionsRequest {
  targets: UserAuthorizationBatchTarget[]
  changes: UserPermissionChangeInput[]
  reason: string
}

export interface UserAuthorizationBatchTargetResult {
  userId: EntityId
  authorizationVersion: number
  changed: boolean
}

export interface UserAuthorizationBatchResult {
  targetCount: number
  changedTargetCount: number
  targets: UserAuthorizationBatchTargetResult[]
}
