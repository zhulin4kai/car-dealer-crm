import type { LooseRecord } from '@/shared/types/common'

export interface LoginForm {
  loginAct: string
  loginPwd: string
  rememberMe: boolean
}

export interface Permission extends LooseRecord {
  id?: number | string
  name?: string
  code?: string
  url?: string
  icon?: string
  subPermissionList?: Permission[]
}

export interface User extends LooseRecord {
  id?: number | string
  name?: string
  loginAct?: string
  permissionList?: string[]
  menuPermissionList?: Permission[]
}

export type UserQuery = LooseRecord
export type UserForm = LooseRecord
