import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { LoginForm, User, UserForm, UserQuery } from '@/modules/user/model/user.types'

export function login(payload: FormData | LoginForm): Promise<string> {
  return httpClient.post<string>('/api/login', payload)
}

export function freeLogin(): Promise<string | boolean> {
  return httpClient.get<string | boolean>('/api/login/free')
}

export function logout(): Promise<unknown> {
  return httpClient.get('/api/logout')
}

export function fetchLoginInfo(): Promise<User> {
  return httpClient.get<User>('/api/login/info')
}

export function fetchUserPage(params: UserQuery): Promise<PageResult<User>> {
  return httpClient.get<PageResult<User>>('/api/users', { params })
}

export function fetchUserDetail(id: EntityId): Promise<User> {
  return httpClient.get<User>(`/api/user/${id}`)
}

export function createUser(data: UserForm): Promise<unknown> {
  return httpClient.post('/api/user', data)
}

export function updateUser(data: UserForm): Promise<unknown> {
  return httpClient.put('/api/user', data)
}

export function deleteUser(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/user/${id}`)
}

export function batchDeleteUsers(ids: EntityId[]): Promise<unknown> {
  return httpClient.delete('/api/user', ids)
}

export const getUserList = fetchUserPage
export const getUserDetail = fetchUserDetail
