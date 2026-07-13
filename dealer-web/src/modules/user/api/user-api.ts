import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { OwnerCandidate, OwnerCandidateQuery } from '@/modules/user/model/owner.types'
import type {
  ChangePasswordRequest, CreateUserRequest,
  ChangeManagedUserStatusRequest, CreateManagedUserRequest, CreateManagedUserResult, LoginForm,
  ChangeManagedUserLoginAccountRequest, ChangeManagedUserSecurityExpirationRequest,
  ManagedUserDetail, PasswordResetDeliveryResult, ResetManagedUserPasswordRequest,
  UpdateManagedUserProfileRequest, UpdateUserRequest, User, UserFilterOptions,
  UserListQuery, UserListSummary,
} from '@/modules/user/model/user.types'

export function login(payload: URLSearchParams | LoginForm): Promise<string> {
  return httpClient.post<string>('/api/login', payload, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

export function freeLogin(): Promise<string | boolean> {
  return httpClient.get<string | boolean>('/api/login/free')
}

export function logout(): Promise<unknown> {
  return httpClient.post('/api/logout')
}

export function fetchLoginInfo(): Promise<User> {
  return httpClient.get<User>('/api/login/info')
}

export function fetchUserPage(
  params: UserListQuery,
  signal?: AbortSignal,
): Promise<PageResult<UserListSummary>> {
  return httpClient.get<PageResult<UserListSummary>>('/api/users', { params, signal })
}

export function fetchUserFilterOptions(
  organizationUnitId?: EntityId,
  signal?: AbortSignal,
): Promise<UserFilterOptions> {
  return httpClient.get<UserFilterOptions>('/api/users/filter-options', {
    ...(organizationUnitId !== undefined ? { params: { organizationUnitId } } : {}),
    signal,
  })
}

export function fetchManagedUserDetail(
  id: EntityId,
  signal?: AbortSignal,
): Promise<ManagedUserDetail> {
  return httpClient.get<ManagedUserDetail>(`/api/users/${id}`, { signal })
}

export function createManagedUser(request: CreateManagedUserRequest): Promise<CreateManagedUserResult> {
  return httpClient.post<CreateManagedUserResult>('/api/users', request)
}

export function updateManagedUserProfile(
  id: EntityId,
  request: UpdateManagedUserProfileRequest,
): Promise<ManagedUserDetail> {
  return httpClient.put<ManagedUserDetail>(`/api/users/${id}/profile`, request)
}

export function changeManagedUserStatus(
  id: EntityId,
  request: ChangeManagedUserStatusRequest,
): Promise<ManagedUserDetail> {
  return httpClient.post<ManagedUserDetail>(`/api/users/${id}/status`, request)
}

export function changeManagedUserLoginAccount(
  id: EntityId,
  request: ChangeManagedUserLoginAccountRequest,
): Promise<ManagedUserDetail> {
  return httpClient.put<ManagedUserDetail>(`/api/users/${id}/login-account`, request)
}

export function changeManagedUserSecurityExpiration(
  id: EntityId,
  request: ChangeManagedUserSecurityExpirationRequest,
): Promise<ManagedUserDetail> {
  return httpClient.put<ManagedUserDetail>(`/api/users/${id}/security-expiration`, request)
}

export function resetManagedUserPassword(
  id: EntityId,
  request: ResetManagedUserPasswordRequest,
): Promise<PasswordResetDeliveryResult> {
  return httpClient.post<PasswordResetDeliveryResult>(`/api/users/${id}/password-reset`, request)
}

export function fetchUserDetail(id: EntityId): Promise<User> {
  return httpClient.get<User>(`/api/user/${id}`)
}

export function fetchOwnerList(params: OwnerCandidateQuery): Promise<OwnerCandidate[]> {
  return httpClient.get<OwnerCandidate[]>('/api/owner', { params })
}

export function createUser(data: CreateUserRequest): Promise<User> {
  return httpClient.post<User>('/api/user', data)
}

export function updateUser(data: UpdateUserRequest): Promise<User> {
  return httpClient.put<User>('/api/user', data)
}

export function disableUser(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/disable`)
}

export function enableUser(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/enable`)
}

export function lockUser(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/lock`)
}

export function unlockUser(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/unlock`)
}

export function batchDisableUsers(ids: EntityId[]): Promise<unknown> {
  return httpClient.put('/api/users/batch-disable', { ids })
}

export function changeUserPassword(id: EntityId, data: ChangePasswordRequest): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/password`, data)
}

export const getUserList = fetchUserPage
export const getUserDetail = fetchUserDetail
export const getOwnerList = fetchOwnerList
