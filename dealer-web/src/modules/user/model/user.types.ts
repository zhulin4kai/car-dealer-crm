import type { EntityId } from '@/shared/types/id'

export interface LoginForm {
  loginAct: string
  loginPwd: string
  rememberMe: boolean
}

export interface Permission {
  id?: number | string
  name?: string
  code?: string
  url?: string
  icon?: string
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

export interface UserListQuery {
  current?: number
  pageSize?: number
  loginAct?: string
  name?: string
  phone?: string
  email?: string
}
