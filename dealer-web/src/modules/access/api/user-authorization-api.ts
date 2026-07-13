import type {
  BatchUpdateUserPermissionsRequest,
  BatchUpdateUserRolesRequest,
  UpdateUserPermissionsRequest,
  UpdateUserRoleAssignmentsRequest,
  UserAuthorizationBatchResult,
  UserAuthorizationDetail,
} from '@/modules/access/model/user-permission.types'
import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'

export function fetchUserAuthorizationDetail(
  userId: EntityId,
  signal?: AbortSignal,
): Promise<UserAuthorizationDetail> {
  return httpClient.get<UserAuthorizationDetail>(`/api/users/${userId}/authorization`, { signal })
}

export function updateUserRoleAssignments(
  userId: EntityId,
  request: UpdateUserRoleAssignmentsRequest,
): Promise<UserAuthorizationDetail> {
  return httpClient.put<UserAuthorizationDetail>(
    `/api/users/${userId}/authorization/roles`,
    request,
  )
}

export function updateUserPermissions(
  userId: EntityId,
  request: UpdateUserPermissionsRequest,
): Promise<UserAuthorizationDetail> {
  return httpClient.put<UserAuthorizationDetail>(
    `/api/users/${userId}/authorization/permissions`,
    request,
  )
}

export function batchUpdateUserRoleAssignments(
  request: BatchUpdateUserRolesRequest,
): Promise<UserAuthorizationBatchResult> {
  return httpClient.put<UserAuthorizationBatchResult>(
    '/api/users/authorization/batch/roles',
    request,
  )
}

export function batchUpdateUserPermissions(
  request: BatchUpdateUserPermissionsRequest,
): Promise<UserAuthorizationBatchResult> {
  return httpClient.put<UserAuthorizationBatchResult>(
    '/api/users/authorization/batch/permissions',
    request,
  )
}
