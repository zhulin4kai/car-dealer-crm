import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'
import type {
  CompleteDepartureRequest,
  ConfirmHandoverRequest,
  DeparturePrecheck,
  DeparturePrecheckRequest,
  HandoverResult,
  RehireEmployeeRequest,
  RehireResult,
  StartDepartureRequest,
  TransferEmployeeRequest,
  UserLifecycleContext,
} from '@/modules/user/model/user-lifecycle.types'

export function fetchUserLifecycleContext(
  userId: EntityId,
  signal?: AbortSignal,
  organizationUnitId?: EntityId,
): Promise<UserLifecycleContext> {
  return httpClient.get<UserLifecycleContext>(`/api/users/${userId}/lifecycle`, {
    signal,
    ...(organizationUnitId !== undefined ? { params: { organizationUnitId } } : {}),
  })
}

export function transferEmployee(
  userId: EntityId,
  request: TransferEmployeeRequest,
): Promise<UserLifecycleContext> {
  return httpClient.post<UserLifecycleContext>(`/api/users/${userId}/lifecycle/transfer`, request)
}

export function precheckDeparture(
  userId: EntityId,
  request: DeparturePrecheckRequest,
): Promise<DeparturePrecheck> {
  return httpClient.post<DeparturePrecheck>(
    `/api/users/${userId}/lifecycle/departure/precheck`,
    request,
  )
}

export function startDeparture(
  userId: EntityId,
  request: StartDepartureRequest,
): Promise<UserLifecycleContext> {
  return httpClient.post<UserLifecycleContext>(
    `/api/users/${userId}/lifecycle/departure/start`,
    request,
  )
}

export function confirmDepartureHandover(
  userId: EntityId,
  request: ConfirmHandoverRequest,
): Promise<HandoverResult> {
  return httpClient.post<HandoverResult>(
    `/api/users/${userId}/lifecycle/departure/handover`,
    request,
  )
}

export function completeDeparture(
  userId: EntityId,
  request: CompleteDepartureRequest,
): Promise<UserLifecycleContext> {
  return httpClient.post<UserLifecycleContext>(
    `/api/users/${userId}/lifecycle/departure/complete`,
    request,
  )
}

export function rehireEmployee(
  userId: EntityId,
  request: RehireEmployeeRequest,
): Promise<RehireResult> {
  return httpClient.post<RehireResult>(`/api/users/${userId}/lifecycle/rehire`, request)
}
