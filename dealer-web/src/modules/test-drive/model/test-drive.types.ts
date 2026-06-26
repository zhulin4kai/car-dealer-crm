import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export type TestDriveStatus =
  | 'PENDING_CONFIRM'
  | 'SCHEDULED'
  | 'RESCHEDULED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELED'
  | 'NO_SHOW'
  | 'EXCEPTION_CLOSED'

export interface TestDrive {
  id: EntityId
  testDriveNo: string
  customerId: EntityId
  customerName?: string
  opportunityId?: EntityId
  opportunityNo?: string
  vehicleId: EntityId
  vin?: string
  vehicleName?: string
  ownerId: EntityId
  ownerName?: string
  plannedStartTime: string
  plannedEndTime: string
  actualArriveTime?: string
  actualStartTime?: string
  actualEndTime?: string
  safetyConfirmedAt?: string
  status: TestDriveStatus
  contactName: string
  contactPhone: string
  result?: string
  customerFeedback?: string
  nextAction?: string
  cancelType?: string
  cancelReason?: string
  remark?: string
  rescheduleCount?: number
  createTime?: string
  updateTime?: string
}

export interface TestDriveStatusHistory {
  id: EntityId
  testDriveId: EntityId
  fromStatus?: TestDriveStatus
  toStatus: TestDriveStatus
  actionType: string
  reason?: string
  oldStartTime?: string
  oldEndTime?: string
  newStartTime?: string
  newEndTime?: string
  operateBy: EntityId
  operateTime: string
}

export interface TestDriveQuery extends PageQuery {
  customerId?: EntityId
  opportunityId?: EntityId
  vehicleId?: EntityId
  ownerId?: EntityId
  status?: TestDriveStatus
  keyword?: string
}

export interface CreateTestDriveRequest {
  customerId: EntityId
  opportunityId?: EntityId
  vehicleId: EntityId
  plannedStartTime: string
  plannedEndTime: string
  contactName: string
  contactPhone: string
  remark?: string
}

export interface RescheduleTestDriveRequest {
  vehicleId?: EntityId
  plannedStartTime: string
  plannedEndTime: string
  reason: string
}

export interface CancelTestDriveRequest {
  cancelType: 'CUSTOMER_CANCEL' | 'STORE_CANCEL' | 'VEHICLE_UNAVAILABLE' | 'OTHER' | 'NO_SHOW'
  reason: string
}

export interface CheckInTestDriveRequest {
  arrivedAt?: string
  customerConfirmMethod: string
}

export interface CompleteTestDriveRequest {
  actualStartTime?: string
  actualEndTime?: string
  safetyConfirmed: boolean
  result: string
  customerFeedback: string
  nextAction: string
}

export const TEST_DRIVE_STATUS_OPTIONS: Array<{ value: TestDriveStatus; label: string }> = [
  { value: 'PENDING_CONFIRM', label: '待确认' },
  { value: 'SCHEDULED', label: '已预约' },
  { value: 'RESCHEDULED', label: '已改期' },
  { value: 'CHECKED_IN', label: '已到店' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELED', label: '已取消' },
  { value: 'NO_SHOW', label: '已爽约' },
  { value: 'EXCEPTION_CLOSED', label: '异常关闭' },
]

export function formatTestDriveStatus(status?: string): string {
  return TEST_DRIVE_STATUS_OPTIONS.find(option => option.value === status)?.label ?? status ?? '--'
}

export function getTestDriveStatusTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'SCHEDULED' || status === 'RESCHEDULED') return 'info'
  if (status === 'CHECKED_IN') return 'warning'
  if (status === 'CANCELED' || status === 'NO_SHOW' || status === 'EXCEPTION_CLOSED') return 'danger'
  return 'muted'
}

export function isTestDriveTerminal(status?: string): boolean {
  return status === 'COMPLETED' || status === 'CANCELED' || status === 'NO_SHOW' || status === 'EXCEPTION_CLOSED'
}
