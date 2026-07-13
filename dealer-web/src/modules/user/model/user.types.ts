import type { EntityId } from '@/shared/types/id'
import type { ManagedCredentialDeliveryResult } from '@/modules/user/model/credential.types'

export interface LoginForm {
  loginAct: string
  loginPwd: string
  rememberMe: boolean
}

export const USER_MANAGEMENT_GATE_STATE = {
  UNINITIALIZED: 'UNINITIALIZED',
  PENDING_FIRST_CHANGE: 'PENDING_FIRST_CHANGE',
  READY: 'READY',
  DEGRADED: 'DEGRADED',
} as const

export type UserManagementGateState =
  (typeof USER_MANAGEMENT_GATE_STATE)[keyof typeof USER_MANAGEMENT_GATE_STATE]

export interface Permission {
  id?: number | string
  name?: string
  code: string
  url?: string
  type?: 'menu' | 'button'
  parentId?: number | string
  orderNo?: number
  icon?: string
  enabled?: number
  subPermissionList?: Permission[]
}

export interface User {
  id?: number | string
  loginAct?: string
  name?: string
  phone?: string
  email?: string
  avatarUrl?: string
  accountNoExpired?: number
  credentialsNoExpired?: number
  accountNoLocked?: number
  accountEnabled?: number
  createTime?: string
  createBy?: number
  editTime?: string
  editBy?: number
  lastLoginTime?: string
  mustChangePassword?: boolean
  protectedRecoveryAccount?: boolean
  userManagementGateState?: UserManagementGateState
  roleList?: string[]
  permissionList?: string[]
  menuPermissionList?: Permission[]
  createByDO?: { id?: number; name?: string }
  editByDO?: { id?: number; name?: string }
}

export interface CreateUserRequest {
  loginAct: string
  loginPwd: string
  name: string
  phone: string
  email: string
}

export interface UpdateUserRequest {
  id: number
  loginAct: string
  name: string
  phone: string
  email: string
}

export interface ChangePasswordRequest {
  userId: number
  newPassword: string
}

export interface UserListQuery {
  page?: number
  size?: number
  keyword?: string
  organizationUnitId?: EntityId
  positionId?: EntityId
  managerEmployeeId?: EntityId
  roleId?: EntityId
  employmentStatus?: string
  accountStatus?: string
  lockStatus?: string
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
}

export const MANAGED_USER_ACTION = {
  VIEW: 'VIEW',
  PROFILE_UPDATE: 'PROFILE_UPDATE',
  ASSIGNMENT_UPDATE: 'ASSIGNMENT_UPDATE',
  AUTHORIZATION_VIEW: 'AUTHORIZATION_VIEW',
  AUTHORIZATION_UPDATE: 'AUTHORIZATION_UPDATE',
  ACCOUNT_IDENTITY_UPDATE: 'ACCOUNT_IDENTITY_UPDATE',
  SECURITY_EXPIRATION_UPDATE: 'SECURITY_EXPIRATION_UPDATE',
  STATUS_UPDATE: 'STATUS_UPDATE',
  PASSWORD_RESET: 'PASSWORD_RESET',
  SESSION_VIEW: 'SESSION_VIEW',
  SESSION_REVOKE: 'SESSION_REVOKE',
  HISTORY_VIEW: 'HISTORY_VIEW',
  TRANSFER: 'TRANSFER',
  DEPARTURE: 'DEPARTURE',
  REHIRE: 'REHIRE',
  REINVITE: 'REINVITE',
  HANDOVER: 'HANDOVER',
} as const

export type ManagedUserAction =
  (typeof MANAGED_USER_ACTION)[keyof typeof MANAGED_USER_ACTION]

export interface UserListSummary {
  id: EntityId
  employeeId?: EntityId | null
  employeeNo?: string | null
  name: string
  loginAct: string
  organizationName?: string | null
  positionName?: string | null
  managerName?: string | null
  roleNames: string[]
  employmentStatus: string
  accountStatus: string
  lockStatus: string
  lastLoginTime?: string | null
  allowedActions: ManagedUserAction[]
  unavailableReasons: Partial<Record<ManagedUserAction, string>>
}

export interface UserFilterOption {
  id: EntityId
  label: string
}

export interface UserFilterOptions {
  organizations: UserFilterOption[]
  positions: UserFilterOption[]
  managers: UserFilterOption[]
  roles: UserFilterOption[]
  assignableRoles: UserFilterOption[]
  employmentStatuses: UserFilterOption[]
  accountStatuses: UserFilterOption[]
  lockStatuses: UserFilterOption[]
  bootstrapRequired: boolean
  bootstrapAllowed: boolean
  bootstrapRootOrganizationId?: EntityId | null
  bootstrapRootOrganizationVersion?: number | null
}

export interface ManagedUserDetail {
  id: EntityId
  employeeId?: EntityId | null
  employeeNo?: string | null
  loginAct: string
  name: string
  phone?: string | null
  email?: string | null
  organizationName?: string | null
  positionName?: string | null
  managerName?: string | null
  employmentStatus: string
  accountStatus: string
  lockStatus: string
  lockReason?: string | null
  accountExpired: boolean
  credentialExpired: boolean
  accountExpiresAt?: string | null
  credentialExpiresAt?: string | null
  lastLoginTime?: string | null
  profileVersion: number
  accountVersion: number
  employeeVersion: number
  authorizationVersion: number
  sessionRevision: number
  roleNames: string[]
  statusCommands: UserStatusCommandOption[]
  allowedActions: ManagedUserAction[]
  unavailableReasons: Partial<Record<ManagedUserAction, string>>
}

export interface CreateManagedUserRequest {
  loginAct: string
  name: string
  phone: string | null
  email: string | null
  employeeNo: string
  organizationUnitId: EntityId
  positionId: EntityId
  managerEmployeeId?: EntityId | null
  roleIds: EntityId[]
  bootstrapRootLeader: boolean
  expectedRootOrganizationVersion?: number | null
}

export interface CreateManagedUserResult {
  user: ManagedUserDetail
  credentialDelivery: ManagedCredentialDeliveryResult
}

export interface UpdateManagedUserProfileRequest {
  profileVersion: number
  name: string
  phone: string | null
  email: string | null
}

export type UserStatusCommand = 'ENABLE' | 'DISABLE' | 'LOCK' | 'UNLOCK'

export interface UserStatusCommandOption {
  command: UserStatusCommand
  label: string
  destructive: boolean
  disabledReason?: string | null
}

export interface ChangeManagedUserStatusRequest {
  accountVersion: number
  command: UserStatusCommand
  reason: string
}

export interface ChangeManagedUserLoginAccountRequest {
  accountVersion: number
  loginAct: string
  reason: string
}

export interface ChangeManagedUserSecurityExpirationRequest {
  accountVersion: number
  accountExpiresAt: string | null
  credentialExpiresAt: string | null
  reason: string
}

export interface ResetManagedUserPasswordRequest {
  accountVersion: number
  reason: string
}

export interface PasswordResetDeliveryResult {
  accepted: true
  deliveryStatus: 'QUEUED'
  mustChangePassword: true
}

export interface UserFormValues {
  loginAct: string
  loginPwd: string
  name: string
  phone: string
  email: string
}

export function toCreateUserRequest(values: UserFormValues): CreateUserRequest {
  return {
    loginAct: values.loginAct,
    loginPwd: values.loginPwd,
    name: values.name,
    phone: values.phone,
    email: values.email,
  }
}

export function toUpdateUserRequest(values: UserFormValues, id: number): UpdateUserRequest {
  return {
    id,
    loginAct: values.loginAct,
    name: values.name,
    phone: values.phone,
    email: values.email,
  }
}
