import type { EntityId } from '@/shared/types/id'

export const PERMISSION_SENSITIVITY = {
  NORMAL: 'NORMAL',
  SENSITIVE: 'SENSITIVE',
  PROTECTED: 'PROTECTED',
} as const

export type PermissionSensitivity =
  (typeof PERMISSION_SENSITIVITY)[keyof typeof PERMISSION_SENSITIVITY]

export const PERMISSION_SENSITIVITY_LABEL: Record<PermissionSensitivity, string> = {
  NORMAL: '普通',
  SENSITIVE: '敏感',
  PROTECTED: '受保护',
}

export const DATA_SCOPE_CODE = {
  SELF: 'SELF',
  DIRECT_REPORTS: 'DIRECT_REPORTS',
  REPORTING_TREE: 'REPORTING_TREE',
  PRIMARY_ORG: 'PRIMARY_ORG',
  ORG_TREE: 'ORG_TREE',
  CUSTOM_ORGS: 'CUSTOM_ORGS',
  GLOBAL: 'GLOBAL',
} as const

export type DataScopeCode = (typeof DATA_SCOPE_CODE)[keyof typeof DATA_SCOPE_CODE]

export const DATA_SCOPE_LABEL: Record<DataScopeCode, string> = {
  SELF: '本人',
  DIRECT_REPORTS: '直属下属',
  REPORTING_TREE: '汇报树',
  PRIMARY_ORG: '主要组织',
  ORG_TREE: '组织及下级',
  CUSTOM_ORGS: '指定组织',
  GLOBAL: '全局',
}

export type RoleScopeType = 'GLOBAL' | 'ORGANIZATION'

export const ROLE_ACTION = {
  EDIT: 'EDIT',
  COPY: 'COPY',
  STATUS_CHANGE: 'STATUS_CHANGE',
} as const

export type RoleAction = (typeof ROLE_ACTION)[keyof typeof ROLE_ACTION]

export interface AccessOrganizationOption {
  id: EntityId
  name: string
  pathName?: string
}

export interface RoleSummary {
  id: EntityId
  code: string
  name: string
  description?: string | null
  protectedRole: boolean
  protectedReason?: string | null
  authorizationLevel: number
  defaultDataScope: DataScopeCode
  scopeType: RoleScopeType
  applicableOrganizations: AccessOrganizationOption[]
  memberCount: number
  enabled: boolean
  version: number
  editable: boolean
  disabledReason?: string | null
  allowedActions: RoleAction[]
  unavailableReasons: Partial<Record<RoleAction, string>>
}

export type RoleDetail = RoleSummary

export interface PermissionCatalogItem {
  id: EntityId
  name: string
  code: string
  module: string
  type: 'menu' | 'button'
  description?: string | null
  sensitivityLevel: PermissionSensitivity
  delegable: boolean
  enabled: boolean
  orderNo?: number | null
  parentId?: EntityId | null
  assignable: boolean
  restrictionReason?: string | null
  children: PermissionCatalogItem[]
}

export interface RolePermissionMatrix {
  roleId: EntityId
  roleName: string
  expectedVersion: number
  selectedPermissionIds: EntityId[]
  editable: boolean
  disabledReason?: string | null
  permissionScopes: RolePermissionScopeAssignment[]
  permissionScopeOptions: RolePermissionScopeOption[]
}

export interface RolePermissionScopeAssignment {
  permissionId: EntityId
  dataScopeCode: DataScopeCode
  organizationUnitIds: EntityId[]
  organizationNames?: string[]
}

export interface RolePermissionDataScopeCandidate {
  code: DataScopeCode
  label: string
  organizationOptions: AccessOrganizationOption[]
}

export interface RolePermissionScopeOption {
  permissionId: EntityId
  editable: boolean
  unavailableReason?: string | null
  dataScopeCandidates: RolePermissionDataScopeCandidate[]
}

export interface PermissionDifferenceItem {
  permissionId: EntityId
  code: string
  name: string
  sensitivityLevel: PermissionSensitivity
}

export interface PermissionScopeDifferenceItem {
  permissionId: EntityId
  permissionCode: string
  permissionName: string
  beforeDataScopeCode?: DataScopeCode | null
  beforeOrganizationNames: string[]
  afterDataScopeCode?: DataScopeCode | null
  afterOrganizationNames: string[]
}

export interface RolePermissionPreview {
  roleId: EntityId
  expectedVersion: number
  addedPermissions: PermissionDifferenceItem[]
  removedPermissions: PermissionDifferenceItem[]
  affectedUserCount: number
  affectedOrganizationCount: number
  sessionRevocationCount: number
  warnings: string[]
  scopeDifferences: PermissionScopeDifferenceItem[]
}

export interface CreateRoleRequest {
  code: string
  name: string
  description?: string
  authorizationLevel: number
  defaultDataScope: DataScopeCode
  scopeType: RoleScopeType
  organizationUnitIds: EntityId[]
}

export interface UpdateRoleRequest extends Omit<CreateRoleRequest, 'code'> {
  expectedVersion: number
}

export interface CopyRoleRequest extends CreateRoleRequest {
  reason: string
}

export interface ChangeRoleStatusRequest {
  expectedVersion: number
  reason: string
}

export interface PreviewRolePermissionRequest {
  expectedVersion: number
  permissionIds: EntityId[]
  permissionScopes: RolePermissionScopeAssignment[]
}

export interface UpdateRolePermissionRequest extends PreviewRolePermissionRequest {
  reason: string
}

export interface UpdateRolePermissionResponse {
  roleId: EntityId
  version: number
  permissionIds: EntityId[]
  affectedUserCount: number
  securityVersionUpdatedCount: number
  sessionCleanupWarningCount: number | null
  permissionScopes: RolePermissionScopeAssignment[]
}

export interface RoleListQuery {
  page?: number
  size?: number
  keyword?: string
  enabled?: boolean
}

export type RoleFormSubmission =
  | { mode: 'create'; request: CreateRoleRequest }
  | { mode: 'update'; id: EntityId; request: UpdateRoleRequest }
  | { mode: 'copy'; sourceRoleId: EntityId; request: CopyRoleRequest }

export function flattenPermissionCatalog(nodes: PermissionCatalogItem[]): PermissionCatalogItem[] {
  return nodes.flatMap((node) => [node, ...flattenPermissionCatalog(node.children)])
}
