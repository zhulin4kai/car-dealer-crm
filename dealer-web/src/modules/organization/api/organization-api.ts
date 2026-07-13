import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'
import type {
  ActingReportingCollection,
  ChangeOrganizationUnitStatusRequest,
  ChangePositionStatusRequest,
  CreateOrganizationUnitRequest,
  CreatePositionRequest,
  EmployeeOrganizationMembership,
  EmployeeSummary,
  ManagerCandidate,
  OrganizationChangeHistory,
  OrganizationParentCandidate,
  OrganizationUnit,
  Position,
  ReplaceActingReportingsRequest,
  UpdateEmployeeOrganizationRequest,
  UpdateOrganizationUnitRequest,
  UpdatePositionRequest,
} from '@/modules/organization/model/organization.types'

export function fetchOrganizationTree(signal?: AbortSignal): Promise<OrganizationUnit[]> {
  return httpClient.get<OrganizationUnit[]>('/api/organization-units/tree', { signal })
}

export function createOrganizationUnit(
  request: CreateOrganizationUnitRequest,
): Promise<OrganizationUnit> {
  return httpClient.post<OrganizationUnit>('/api/organization-units', request)
}

export function updateOrganizationUnit(
  id: EntityId,
  request: UpdateOrganizationUnitRequest,
): Promise<OrganizationUnit> {
  return httpClient.put<OrganizationUnit>(`/api/organization-units/${id}`, request)
}

export function enableOrganizationUnit(
  id: EntityId,
  request: ChangeOrganizationUnitStatusRequest,
): Promise<OrganizationUnit> {
  return httpClient.put<OrganizationUnit>(`/api/organization-units/${id}/enable`, request)
}

export function disableOrganizationUnit(
  id: EntityId,
  request: ChangeOrganizationUnitStatusRequest,
): Promise<OrganizationUnit> {
  return httpClient.put<OrganizationUnit>(`/api/organization-units/${id}/disable`, request)
}

export function fetchPositions(signal?: AbortSignal): Promise<Position[]> {
  return httpClient.get<Position[]>('/api/positions', { signal })
}

export function createPosition(request: CreatePositionRequest): Promise<Position> {
  return httpClient.post<Position>('/api/positions', request)
}

export function updatePosition(id: EntityId, request: UpdatePositionRequest): Promise<Position> {
  return httpClient.put<Position>(`/api/positions/${id}`, request)
}

export function enablePosition(
  id: EntityId,
  request: ChangePositionStatusRequest,
): Promise<Position> {
  return httpClient.put<Position>(`/api/positions/${id}/enable`, request)
}

export function disablePosition(
  id: EntityId,
  request: ChangePositionStatusRequest,
): Promise<Position> {
  return httpClient.put<Position>(`/api/positions/${id}/disable`, request)
}

export function fetchOrganizationEmployees(
  organizationUnitId: EntityId,
  signal?: AbortSignal,
): Promise<EmployeeSummary[]> {
  return httpClient.get<EmployeeSummary[]>(
    `/api/organization-units/${organizationUnitId}/employees`,
    { signal },
  )
}

export function fetchEmployeeOrganizationMembership(
  employeeId: EntityId,
  signal?: AbortSignal,
): Promise<EmployeeOrganizationMembership> {
  return httpClient.get<EmployeeOrganizationMembership>(
    `/api/employees/${employeeId}/organization-membership`,
    { signal },
  )
}

export function fetchManagerCandidates(
  employeeId: EntityId,
  signal?: AbortSignal,
  organizationUnitId?: EntityId,
): Promise<ManagerCandidate[]> {
  return httpClient.get<ManagerCandidate[]>(`/api/employees/${employeeId}/manager-candidates`, {
    signal,
    ...(organizationUnitId !== undefined ? { params: { organizationUnitId } } : {}),
  })
}

export function fetchActingReportings(
  employeeId: EntityId,
  signal?: AbortSignal,
): Promise<ActingReportingCollection> {
  return httpClient.get<ActingReportingCollection>(
    `/api/employees/${employeeId}/acting-reporting-relations`,
    { signal },
  )
}

export function fetchActingManagerCandidates(
  employeeId: EntityId,
  signal?: AbortSignal,
): Promise<ManagerCandidate[]> {
  return httpClient.get<ManagerCandidate[]>(
    `/api/employees/${employeeId}/acting-reporting-relations/manager-candidates`,
    { signal },
  )
}

export function replaceActingReportings(
  employeeId: EntityId,
  request: ReplaceActingReportingsRequest,
): Promise<ActingReportingCollection> {
  return httpClient.put<ActingReportingCollection>(
    `/api/employees/${employeeId}/acting-reporting-relations`,
    request,
  )
}

export function fetchEmployeeOrganizationHistory(
  employeeId: EntityId,
  signal?: AbortSignal,
): Promise<OrganizationChangeHistory[]> {
  return httpClient.get<OrganizationChangeHistory[]>(
    `/api/employees/${employeeId}/organization-history`,
    { signal },
  )
}

export function fetchOrganizationLeaderCandidates(
  params: { organizationUnitId?: EntityId; parentId?: EntityId },
  signal?: AbortSignal,
): Promise<ManagerCandidate[]> {
  return httpClient.get<ManagerCandidate[]>('/api/organization-units/leader-candidates', {
    params,
    signal,
  })
}

export function fetchOrganizationParentCandidates(
  params: { type: string; excludeId?: EntityId },
  signal?: AbortSignal,
): Promise<OrganizationParentCandidate[]> {
  return httpClient.get<OrganizationParentCandidate[]>(
    '/api/organization-units/parent-candidates',
    {
      params,
      signal,
    },
  )
}

export function updateEmployeeOrganizationMembership(
  employeeId: EntityId,
  request: UpdateEmployeeOrganizationRequest,
): Promise<EmployeeOrganizationMembership> {
  return httpClient.put<EmployeeOrganizationMembership>(
    `/api/employees/${employeeId}/organization-membership`,
    request,
  )
}
