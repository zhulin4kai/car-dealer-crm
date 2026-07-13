import type { EntityId } from '@/shared/types/id'

export const USER_SESSION_ACTION = {
  REVOKE_OTHERS: 'REVOKE_OTHERS',
  REVOKE_ALL: 'REVOKE_ALL',
} as const

export type UserSessionAction = (typeof USER_SESSION_ACTION)[keyof typeof USER_SESSION_ACTION]

export const USER_SESSION_ITEM_ACTION = {
  REVOKE: 'REVOKE',
} as const

export type UserSessionItemAction =
  (typeof USER_SESSION_ITEM_ACTION)[keyof typeof USER_SESSION_ITEM_ACTION]

export interface UserSessionItem {
  id: string
  deviceSummary: string
  clientSummary?: string | null
  networkSummary?: string | null
  loginTime: string
  lastActivityTime: string
  expiresAt: string
  current: boolean
  rememberMe: boolean
  allowedActions: UserSessionItemAction[]
  unavailableReasons: Partial<Record<UserSessionItemAction, string>>
}

export interface UserSessionCollection {
  targetUserId: EntityId
  sessionRevision: number
  allowedActions: UserSessionAction[]
  unavailableReasons: Partial<Record<UserSessionAction, string>>
  sessions: UserSessionItem[]
}

export interface RevokeSessionRequest {
  sessionRevision: number
  reason: string
}
