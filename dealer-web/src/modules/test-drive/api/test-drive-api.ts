import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  CancelTestDriveRequest,
  CheckInTestDriveRequest,
  CompleteTestDriveRequest,
  CreateTestDriveRequest,
  RescheduleTestDriveRequest,
  TestDrive,
  TestDriveQuery,
  TestDriveStatusHistory,
} from '@/modules/test-drive/model/test-drive.types'

export function fetchTestDrivePage(params: TestDriveQuery): Promise<PageResult<TestDrive>> {
  return httpClient.get<PageResult<TestDrive>>('/api/test-drives', { params })
}

export function fetchTestDriveDetail(id: EntityId): Promise<TestDrive> {
  return httpClient.get<TestDrive>(`/api/test-drives/${id}`)
}

export function fetchTestDriveHistory(id: EntityId): Promise<TestDriveStatusHistory[]> {
  return httpClient.get<TestDriveStatusHistory[]>(`/api/test-drives/${id}/history`)
}

export function createTestDrive(data: CreateTestDriveRequest): Promise<TestDrive> {
  return httpClient.post<TestDrive>('/api/test-drives', data)
}

export function rescheduleTestDrive(id: EntityId, data: RescheduleTestDriveRequest): Promise<TestDrive> {
  return httpClient.put<TestDrive>(`/api/test-drives/${id}/reschedule`, data)
}

export function cancelTestDrive(id: EntityId, data: CancelTestDriveRequest): Promise<TestDrive> {
  return httpClient.put<TestDrive>(`/api/test-drives/${id}/cancel`, data)
}

export function markTestDriveNoShow(id: EntityId, data: CancelTestDriveRequest): Promise<TestDrive> {
  return httpClient.put<TestDrive>(`/api/test-drives/${id}/no-show`, data)
}

export function checkInTestDrive(id: EntityId, data: CheckInTestDriveRequest): Promise<TestDrive> {
  return httpClient.put<TestDrive>(`/api/test-drives/${id}/check-in`, data)
}

export function completeTestDrive(id: EntityId, data: CompleteTestDriveRequest): Promise<TestDrive> {
  return httpClient.put<TestDrive>(`/api/test-drives/${id}/complete`, data)
}
