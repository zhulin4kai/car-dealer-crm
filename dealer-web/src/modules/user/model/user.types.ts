import type { EntityId } from '@/shared/types/id'

export interface LoginForm {
  loginAct: string
  loginPwd: string
  rememberMe: boolean
}

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
  accountNoExpired?: number
  credentialsNoExpired?: number
  accountNoLocked?: number
  accountEnabled?: number
  createTime?: string
  createBy?: number
  editTime?: string
  editBy?: number
  lastLoginTime?: string
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

export interface AssignUserRolesRequest {
  userId: number
  roleIds: number[]
}

export interface HandoverUserResponsibilitiesRequest {
  targetUserId: number
  reason: string
}

export interface HandoverUserResponsibilitiesResponse {
  sourceUserId: number
  targetUserId: number
  activityCount: number
  clueCount: number
  customerCount: number
}

export interface UserListQuery {
  page?: number
  size?: number
  loginAct?: string
  name?: string
  phone?: string
  email?: string
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
