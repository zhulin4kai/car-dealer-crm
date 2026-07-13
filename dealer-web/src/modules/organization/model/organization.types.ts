import type { EntityId } from '@/shared/types/id'

export const ORGANIZATION_UNIT_TYPE = {
  COMPANY: 'COMPANY',
  STORE: 'STORE',
  DEPARTMENT: 'DEPARTMENT',
  TEAM: 'TEAM',
} as const

export type OrganizationUnitType =
  (typeof ORGANIZATION_UNIT_TYPE)[keyof typeof ORGANIZATION_UNIT_TYPE]

export const ORGANIZATION_UNIT_TYPE_LABEL: Record<OrganizationUnitType, string> = {
  COMPANY: '公司',
  STORE: '门店',
  DEPARTMENT: '部门',
  TEAM: '团队',
}

export const ASSIGNMENT_TYPE = {
  PRIMARY: 'PRIMARY',
  SECONDARY: 'SECONDARY',
  ACTING: 'ACTING',
} as const

export type AssignmentType = (typeof ASSIGNMENT_TYPE)[keyof typeof ASSIGNMENT_TYPE]

export const ASSIGNMENT_TYPE_LABEL: Record<AssignmentType, string> = {
  PRIMARY: '主要任职',
  SECONDARY: '兼任',
  ACTING: '代理任职',
}

export const REPORTING_TYPE = {
  DIRECT: 'DIRECT',
  ACTING: 'ACTING',
} as const

export type ReportingType = (typeof REPORTING_TYPE)[keyof typeof REPORTING_TYPE]

export const EMPLOYEE_STATUS = {
  PENDING: 'PENDING',
  ACTIVE: 'ACTIVE',
  HANDOVER: 'HANDOVER',
  LEFT: 'LEFT',
} as const

export type EmployeeStatus = (typeof EMPLOYEE_STATUS)[keyof typeof EMPLOYEE_STATUS]

export const EMPLOYEE_STATUS_LABEL: Record<EmployeeStatus, string> = {
  PENDING: '待入职',
  ACTIVE: '在职',
  HANDOVER: '待交接',
  LEFT: '已离职',
}

export const EMPLOYEE_ORGANIZATION_ACTION = {
  ASSIGNMENT_UPDATE: 'assignment',
  REPORTING_UPDATE: 'reporting',
  HISTORY_VIEW: 'history',
} as const

export type EmployeeOrganizationAction =
  (typeof EMPLOYEE_ORGANIZATION_ACTION)[keyof typeof EMPLOYEE_ORGANIZATION_ACTION]

export interface OrganizationUnit {
  id: EntityId
  code: string
  name: string
  type: OrganizationUnitType
  parentId: EntityId | null
  leaderEmployeeId: EntityId | null
  leaderEmployeeName?: string | null
  orderNo: number
  enabled: boolean
  version: number
  employeeCount?: number
  children: OrganizationUnit[]
}

export interface Position {
  id: EntityId
  code: string
  name: string
  description?: string | null
  positionLevel: number
  builtIn: boolean
  enabled: boolean
  version: number
}

export interface EmployeeSummary {
  id: EntityId
  userId?: EntityId | null
  employeeNo: string
  name: string
  employmentStatus: EmployeeStatus
  organizationUnitId?: EntityId | null
  organizationUnitName?: string | null
  positionId?: EntityId | null
  positionName?: string | null
  managerEmployeeId?: EntityId | null
  managerEmployeeName?: string | null
  version: number
  allowedActions: EmployeeOrganizationAction[]
  unavailableReasons: Partial<Record<EmployeeOrganizationAction, string>>
}

export interface EmployeeAssignment {
  id?: EntityId
  organizationUnitId: EntityId
  organizationUnitName?: string
  positionId: EntityId
  positionName?: string
  assignmentType: AssignmentType
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface EmployeeReporting {
  managerEmployeeId: EntityId
  managerEmployeeName?: string
  relationType: ReportingType
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface EmployeeOrganizationMembership {
  employee: EmployeeSummary
  primaryAssignment: EmployeeAssignment | null
  additionalAssignments: EmployeeAssignment[]
  reporting: EmployeeReporting | null
  version: number
  allowedActions: EmployeeOrganizationAction[]
  unavailableReasons: Partial<Record<EmployeeOrganizationAction, string>>
}

export interface ManagerCandidate {
  employeeId: EntityId
  employeeNo: string
  name: string
  organizationUnitName?: string | null
  positionName?: string | null
}

export interface ActingReportingRelation {
  id: EntityId
  version: number
  managerEmployeeId: EntityId
  managerEmployeeNo?: string | null
  managerEmployeeName?: string | null
  status: 'PLANNED' | 'ACTIVE'
  effectiveFrom: string
  effectiveTo: string
}

export interface ActingReportingCollection {
  employeeId: EntityId
  employeeVersion: number
  relations: ActingReportingRelation[]
  allowedActions: Array<'UPDATE'>
  unavailableReasons: Partial<Record<'UPDATE', string>>
}

export interface ReplaceActingReportingsRequest {
  expectedEmployeeVersion: number
  relations: Array<{ managerEmployeeId: EntityId; effectiveTo: string }>
  reason: string
}

export interface OrganizationParentCandidate {
  id: EntityId
  name: string
  type: OrganizationUnitType
  pathName?: string
}

export const ORGANIZATION_HISTORY_TYPE = {
  CREATE: 'CREATE',
  UPDATE: 'UPDATE',
  ENABLE: 'ENABLE',
  DISABLE: 'DISABLE',
  ASSIGN: 'ASSIGN',
  UNASSIGN: 'UNASSIGN',
  GRANT: 'GRANT',
  DENY: 'DENY',
  REVOKE: 'REVOKE',
  EXPIRE: 'EXPIRE',
  ASSIGNMENT_CREATED: 'ASSIGNMENT_CREATED',
  ASSIGNMENT_UPDATED: 'ASSIGNMENT_UPDATED',
  ASSIGNMENT_ENDED: 'ASSIGNMENT_ENDED',
  REPORTING_CREATED: 'REPORTING_CREATED',
  REPORTING_UPDATED: 'REPORTING_UPDATED',
  REPORTING_ENDED: 'REPORTING_ENDED',
} as const

export type OrganizationHistoryType =
  (typeof ORGANIZATION_HISTORY_TYPE)[keyof typeof ORGANIZATION_HISTORY_TYPE]

export const ORGANIZATION_HISTORY_TYPE_LABEL: Record<OrganizationHistoryType, string> = {
  CREATE: '新增组织关系',
  UPDATE: '调整组织关系',
  ENABLE: '启用组织关系',
  DISABLE: '停用组织关系',
  ASSIGN: '分配组织关系',
  UNASSIGN: '取消组织关系',
  GRANT: '授予组织关系',
  DENY: '拒绝组织关系',
  REVOKE: '撤销组织关系',
  EXPIRE: '组织关系到期',
  ASSIGNMENT_CREATED: '新增任职',
  ASSIGNMENT_UPDATED: '调整任职',
  ASSIGNMENT_ENDED: '结束任职',
  REPORTING_CREATED: '新增汇报关系',
  REPORTING_UPDATED: '调整汇报关系',
  REPORTING_ENDED: '结束汇报关系',
}

export interface OrganizationHistorySnapshot {
  organizationUnitName?: string | null
  positionName?: string | null
  assignmentType?: AssignmentType | null
  managerEmployeeName?: string | null
  reportingType?: ReportingType | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
}

export interface OrganizationChangeHistory {
  id: EntityId
  changeType: OrganizationHistoryType
  beforeSummary?: string | OrganizationHistorySnapshot | null
  afterSummary?: string | OrganizationHistorySnapshot | null
  reason: string
  operatorName?: string | null
  createTime: string
}

export interface CreateOrganizationUnitRequest {
  code: string
  name: string
  type: OrganizationUnitType
  parentId?: EntityId | null
  leaderEmployeeId?: EntityId | null
  orderNo: number
}

export interface UpdateOrganizationUnitRequest extends Omit<CreateOrganizationUnitRequest, 'code'> {
  expectedVersion: number
}

export interface ChangeOrganizationUnitStatusRequest {
  expectedVersion: number
  reason: string
}

export interface CreatePositionRequest {
  code: string
  name: string
  description?: string
  positionLevel: number
}

export interface UpdatePositionRequest extends Omit<CreatePositionRequest, 'code'> {
  expectedVersion: number
}

export interface ChangePositionStatusRequest {
  expectedVersion: number
  reason: string
}

export interface AssignmentInput {
  organizationUnitId: EntityId
  positionId: EntityId
  assignmentType: AssignmentType
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface ReportingInput {
  managerEmployeeId: EntityId
  relationType: ReportingType
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface UpdateEmployeeOrganizationRequest {
  expectedVersion: number
  primaryAssignment: AssignmentInput
  additionalAssignments: AssignmentInput[]
  reporting?: ReportingInput | null
  reason: string
}

export type OrganizationFormSubmission =
  | { mode: 'create'; request: CreateOrganizationUnitRequest }
  | { mode: 'update'; id: EntityId; request: UpdateOrganizationUnitRequest }

export type PositionFormSubmission =
  | { mode: 'create'; request: CreatePositionRequest }
  | { mode: 'update'; id: EntityId; request: UpdatePositionRequest }

export function flattenOrganizationTree(nodes: OrganizationUnit[]): OrganizationUnit[] {
  return nodes.flatMap((node) => [node, ...flattenOrganizationTree(node.children)])
}
