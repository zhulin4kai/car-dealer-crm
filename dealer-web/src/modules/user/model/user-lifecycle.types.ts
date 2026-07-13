import type { EntityId } from '@/shared/types/id'

export const USER_LIFECYCLE_ACTION = {
  TRANSFER: 'TRANSFER',
  DEPARTURE_PRECHECK: 'DEPARTURE_PRECHECK',
  DEPARTURE_START: 'DEPARTURE_START',
  HANDOVER_CONFIRM: 'HANDOVER_CONFIRM',
  DEPARTURE_COMPLETE: 'DEPARTURE_COMPLETE',
  REHIRE: 'REHIRE',
} as const

export type UserLifecycleAction = (typeof USER_LIFECYCLE_ACTION)[keyof typeof USER_LIFECYCLE_ACTION]

export const DIRECT_HANDOVER_RESOURCE = {
  ACTIVITY: 'ACTIVITY',
  CLUE: 'CLUE',
  CUSTOMER: 'CUSTOMER',
  OPPORTUNITY: 'OPPORTUNITY',
  FOLLOW_TASK: 'FOLLOW_TASK',
  TEST_DRIVE: 'TEST_DRIVE',
} as const

export type DirectHandoverResourceType =
  (typeof DIRECT_HANDOVER_RESOURCE)[keyof typeof DIRECT_HANDOVER_RESOURCE]

export const DERIVED_HANDOVER_IMPACT = {
  QUOTE: 'QUOTE',
  TRAN: 'TRAN',
} as const

export type DerivedHandoverImpactType =
  (typeof DERIVED_HANDOVER_IMPACT)[keyof typeof DERIVED_HANDOVER_IMPACT]

export type HandoverResourceType = DirectHandoverResourceType | DerivedHandoverImpactType

export const HANDOVER_TRANSFER_MODE = {
  DIRECT_OWNER: 'DIRECT_OWNER',
  DERIVED_IMPACT: 'DERIVED_IMPACT',
} as const

export type HandoverTransferMode =
  (typeof HANDOVER_TRANSFER_MODE)[keyof typeof HANDOVER_TRANSFER_MODE]

export const DIRECT_HANDOVER_RESOURCE_TYPES = Object.values(DIRECT_HANDOVER_RESOURCE)

export function isDirectHandoverResourceType(value: string): value is DirectHandoverResourceType {
  return (DIRECT_HANDOVER_RESOURCE_TYPES as string[]).includes(value)
}

export interface UserLifecycleTransition {
  action: UserLifecycleAction
  fromStatus: string
  toStatus: string
  label: string
  disabledReason?: string | null
}

export interface LifecycleCandidate {
  id: EntityId
  label: string
  secondaryLabel?: string | null
}

export interface HandoverCandidate extends LifecycleCandidate {
  eligible: boolean
  qualificationCode: string
  qualificationName: string
  unavailableReason?: string | null
}

export interface LifecycleAssignmentSummary {
  organizationCode?: string | null
  organizationName?: string | null
  positionCode?: string | null
  positionName?: string | null
  managerEmployeeNo?: string | null
  managerName?: string | null
  effectiveFrom?: string | null
}

export interface UserLifecycleContext {
  userId: EntityId
  employeeId: EntityId
  employmentStatus: string
  employeeVersion: number
  currentAssignment: LifecycleAssignmentSummary | null
  activeRoleCount: number
  activePersonalPermissionCount: number
  activeSessionCount: number
  additionalAssignmentCount: number
  reportingRelationCount: number
  organizationCandidates: LifecycleCandidate[]
  positionCandidates: LifecycleCandidate[]
  managerCandidates: LifecycleCandidate[]
  managerRequired: boolean
  managerOptionalReason?: string | null
  handoverCandidates: LifecycleCandidate[]
  allowedActions: UserLifecycleAction[]
  unavailableReasons: Partial<Record<UserLifecycleAction, string>>
  statusTransitions: UserLifecycleTransition[]
}

export interface AssignmentCommandFields {
  employeeVersion: number
  organizationUnitId: EntityId
  positionId: EntityId
  managerEmployeeId: EntityId | null
  effectiveFrom: string
  reason: string
}

export type TransferEmployeeRequest = AssignmentCommandFields

export interface DeparturePrecheckRequest {
  employeeVersion: number
  reason: string
}

export interface DepartureResponsibilitySummary {
  resourceType: HandoverResourceType
  resourceName: string
  transferMode: HandoverTransferMode
  count: number
  transferableCount: number
  blockedCount: number
  statusCode: string
  statusName: string
  blocking: boolean
  blockingReasons: string[]
  targetCandidates: HandoverCandidate[]
  conflicts: DepartureResponsibilityConflict[]
}

export interface DepartureResponsibilityConflict {
  conflictCode: string
  conflictName: string
  count: number
  reason: string
}

export interface DeparturePrecheck {
  snapshotToken: string
  generatedAt: string
  expiresAt: string
  userId: EntityId
  employmentStatus: string
  employeeVersion: number
  responsibilities: DepartureResponsibilitySummary[]
  activeRoleCount: number
  activePersonalPermissionCount: number
  activeSessionCount: number
  activeAssignmentCount: number
  activeReportingCount: number
  handoverRequired: boolean
  handoverCompleted: boolean
  readyToComplete: boolean
  blockingReasons: string[]
  allowedActions: UserLifecycleAction[]
  unavailableReasons: Partial<Record<UserLifecycleAction, string>>
  statusTransitions: UserLifecycleTransition[]
}

export interface StartDepartureRequest {
  employeeVersion: number
  snapshotToken: string
  reason: string
}

export interface HandoverTransferSelection {
  resourceType: DirectHandoverResourceType
  targetEmployeeId: EntityId
}

export interface ConfirmHandoverRequest {
  employeeVersion: number
  snapshotToken: string
  transfers: HandoverTransferSelection[]
  reason: string
}

export interface HandoverDomainResult {
  domainCode: DirectHandoverResourceType
  domainName: string
  expectedCount: number
  transferredCount: number
  resultCode: string
  resultName: string
}

export interface HandoverResult {
  operationId: string
  success: boolean
  resultCode: string
  resultName: string
  employeeVersion: number
  domainResults: HandoverDomainResult[]
}

export interface CompleteDepartureRequest {
  employeeVersion: number
  snapshotToken: string
  reason: string
}

export interface RehireEmployeeRequest extends AssignmentCommandFields {
  accountActivationMode: string
}

export interface RehireResult {
  context: UserLifecycleContext
  restoredLegacyAuthorizationCount: number
  credentialDeliveryStatus: string
}
