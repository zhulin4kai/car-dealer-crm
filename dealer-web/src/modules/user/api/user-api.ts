import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  AssignUserRolesRequest, ChangePasswordRequest, CreateUserRequest,
  LoginForm, UpdateUserRequest, User, UserListQuery,
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

export function fetchUserPage(params: UserListQuery): Promise<PageResult<User>> {
  return httpClient.get<PageResult<User>>('/api/users', { params })
}

export function fetchUserDetail(id: EntityId): Promise<User> {
  return httpClient.get<User>(`/api/user/${id}`)
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

export function assignUserRoles(id: EntityId, data: AssignUserRolesRequest): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/roles`, data)
}

export function changeUserPassword(id: EntityId, data: ChangePasswordRequest): Promise<unknown> {
  return httpClient.put(`/api/user/${id}/password`, data)
}

export const getUserList = fetchUserPage
export const getUserDetail = fetchUserDetail
