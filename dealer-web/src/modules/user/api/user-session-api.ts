import type {
  RevokeSessionRequest,
  UserSessionCollection,
} from '@/modules/user/model/user-session.types'
import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'

export function fetchOwnSessions(signal?: AbortSignal): Promise<UserSessionCollection> {
  return httpClient.get<UserSessionCollection>('/api/me/sessions', { signal })
}

export function revokeOwnSession(
  sessionId: string,
  request: RevokeSessionRequest,
): Promise<UserSessionCollection> {
  return httpClient.post<UserSessionCollection>(
    `/api/me/sessions/${encodeURIComponent(sessionId)}/revoke`,
    request,
  )
}

export function revokeOwnOtherSessions(
  request: RevokeSessionRequest,
): Promise<UserSessionCollection> {
  return httpClient.post<UserSessionCollection>('/api/me/sessions/revoke-others', request)
}

export function revokeAllOwnSessions(
  request: RevokeSessionRequest,
): Promise<UserSessionCollection> {
  return httpClient.post<UserSessionCollection>('/api/me/sessions/revoke-all', request)
}

export function fetchManagedUserSessions(
  userId: EntityId,
  signal?: AbortSignal,
): Promise<UserSessionCollection> {
  return httpClient.get<UserSessionCollection>(`/api/users/${userId}/sessions`, { signal })
}

export function revokeManagedUserSession(
  userId: EntityId,
  sessionId: string,
  request: RevokeSessionRequest,
): Promise<UserSessionCollection> {
  return httpClient.post<UserSessionCollection>(
    `/api/users/${userId}/sessions/${encodeURIComponent(sessionId)}/revoke`,
    request,
  )
}

export function revokeAllManagedUserSessions(
  userId: EntityId,
  request: RevokeSessionRequest,
): Promise<UserSessionCollection> {
  return httpClient.post<UserSessionCollection>(`/api/users/${userId}/sessions/revoke-all`, request)
}
