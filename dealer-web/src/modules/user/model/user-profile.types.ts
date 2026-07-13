import type { EntityId } from '@/shared/types/id'

export interface UserProfileRole {
  id: EntityId
  code: string
  name: string
  sourceDescription?: string | null
}

export interface UserProfilePermissionSource {
  permissionCode: string
  permissionName: string
  /** 兼容旧响应；展示时应优先使用 sources，避免来源与范围失去配对。 */
  sourceNames: string[]
  dataScopeLabel?: string | null
  sources: UserProfilePermissionSourceDetail[]
  effectiveTo?: string | null
}

export interface UserProfilePermissionSourceDetail {
  sourceType: 'ROLE' | 'PERSONAL_GRANT' | 'SYSTEM_EFFECTIVE'
  sourceName: string
  dataScopeCode?: string | null
  dataScopeLabel?: string | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
  organizations: UserProfilePermissionOrganization[]
}

export interface UserProfilePermissionOrganization {
  id: EntityId
  code: string
  name: string
}

export interface UserProfile {
  id: EntityId
  loginAct: string
  name: string
  phone?: string | null
  email?: string | null
  phoneVerified: boolean
  emailVerified: boolean
  avatarUrl?: string | null
  employeeNo?: string | null
  employmentStatus?: string | null
  organizationName?: string | null
  positionName?: string | null
  managerName?: string | null
  roles: UserProfileRole[]
  effectivePermissions: UserProfilePermissionSource[]
  profileVersion: number
}

export interface UpdateOwnProfileRequest {
  profileVersion: number
  name: string
  phone: string | null
  email: string | null
  avatarUrl: string | null
}
