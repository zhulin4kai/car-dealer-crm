import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  AccessOrganizationOption,
  ChangeRoleStatusRequest,
  CopyRoleRequest,
  CreateRoleRequest,
  PermissionCatalogItem,
  PreviewRolePermissionRequest,
  RoleDetail,
  RoleListQuery,
  RolePermissionMatrix,
  RolePermissionPreview,
  RoleSummary,
  UpdateRolePermissionRequest,
  UpdateRolePermissionResponse,
  UpdateRoleRequest,
} from '@/modules/access/model/access.types'

export function fetchRolePage(params: RoleListQuery): Promise<PageResult<RoleSummary>> {
  return httpClient.get<PageResult<RoleSummary>>('/api/roles', { params })
}

export function fetchRoleDetail(id: EntityId): Promise<RoleDetail> {
  return httpClient.get<RoleDetail>(`/api/roles/${id}`)
}

export function createRole(request: CreateRoleRequest): Promise<RoleDetail> {
  return httpClient.post<RoleDetail>('/api/roles', request)
}

export function updateRole(id: EntityId, request: UpdateRoleRequest): Promise<RoleDetail> {
  return httpClient.put<RoleDetail>(`/api/roles/${id}`, request)
}

export function copyRole(id: EntityId, request: CopyRoleRequest): Promise<RoleDetail> {
  return httpClient.post<RoleDetail>(`/api/roles/${id}/copy`, request)
}

export function enableRole(id: EntityId, request: ChangeRoleStatusRequest): Promise<RoleDetail> {
  return httpClient.put<RoleDetail>(`/api/roles/${id}/enable`, request)
}

export function disableRole(id: EntityId, request: ChangeRoleStatusRequest): Promise<RoleDetail> {
  return httpClient.put<RoleDetail>(`/api/roles/${id}/disable`, request)
}

export function fetchRoleOrganizationOptions(): Promise<AccessOrganizationOption[]> {
  return httpClient.get<AccessOrganizationOption[]>('/api/roles/organization-options')
}

export function fetchPermissionCatalog(): Promise<PermissionCatalogItem[]> {
  return httpClient.get<PermissionCatalogItem[]>('/api/permissions/tree')
}

export function fetchRolePermissionMatrix(id: EntityId): Promise<RolePermissionMatrix> {
  return httpClient.get<RolePermissionMatrix>(`/api/roles/${id}/permissions`)
}

export function previewRolePermissionMatrix(
  id: EntityId,
  request: PreviewRolePermissionRequest,
): Promise<RolePermissionPreview> {
  return httpClient.post<RolePermissionPreview>(`/api/roles/${id}/permissions/preview`, request)
}

export function updateRolePermissionMatrix(
  id: EntityId,
  request: UpdateRolePermissionRequest,
): Promise<UpdateRolePermissionResponse> {
  return httpClient.put<UpdateRolePermissionResponse>(`/api/roles/${id}/permissions`, request)
}
