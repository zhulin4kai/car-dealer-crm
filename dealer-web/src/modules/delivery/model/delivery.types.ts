import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export type DeliveryStatus =
  | 'PENDING_PREPARE'
  | 'PREPARING'
  | 'WAITING_CUSTOMER'
  | 'WAITING_DELIVERY'
  | 'DELIVERING'
  | 'SIGNED'
  | 'COMPLETED'
  | 'EXCEPTION'
  | 'CANCELLED'

export type DeliveryCheckStatus = 'PENDING' | 'COMPLETED' | 'BLOCKED'

export interface Delivery {
  id: EntityId
  tranId: EntityId
  customerId: EntityId
  vehicleId: EntityId
  status: DeliveryStatus
  plannedDeliveryTime: string
  actualDeliveryTime?: string
  responsibleUserId?: EntityId
  signerName?: string
  signedAt?: string
  signMethod?: string
  signEvidence?: string
  exceptionType?: string
  exceptionReason?: string
  createTime?: string
  createBy?: EntityId
  updateTime?: string
  updateBy?: EntityId
}

export interface DeliveryCheckItem {
  id: EntityId
  deliveryId: EntityId
  itemCode: string
  itemName: string
  status: DeliveryCheckStatus
  responsibleUserId?: EntityId
  completedTime?: string
  remark?: string
}

export interface DeliveryCheckItemRequest {
  itemCode: string
  itemName: string
  responsibleUserId?: EntityId
}

export interface CreateDeliveryRequest {
  tranId: EntityId
  vehicleId: EntityId
  plannedDeliveryTime: string
  checkItems?: DeliveryCheckItemRequest[]
}

export interface UpdateDeliveryCheckItemRequest {
  status: DeliveryCheckStatus
  remark?: string
}

export interface SignDeliveryRequest {
  signerName: string
  signedAt: string
  signMethod: string
  signEvidence: string
}

export interface DeliveryExceptionRequest {
  exceptionType: string
  reason: string
}

export interface DeliveryCancelRequest {
  reason: string
}

export interface DeliveryQuery extends PageQuery {
  tranId?: EntityId
  customerId?: EntityId
  vehicleId?: EntityId
  responsibleUserId?: EntityId
  status?: DeliveryStatus
}

export function formatDeliveryStatus(status?: string): string {
  const map: Record<string, string> = {
    PENDING_PREPARE: '待准备',
    PREPARING: '准备中',
    WAITING_CUSTOMER: '待客户确认',
    WAITING_DELIVERY: '待交付',
    DELIVERING: '交付中',
    SIGNED: '已签收',
    COMPLETED: '已完成',
    EXCEPTION: '交付异常',
    CANCELLED: '已取消',
  }
  return map[status ?? ''] ?? status ?? '--'
}

export function getDeliveryStatusTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'COMPLETED' || status === 'SIGNED') return 'success'
  if (status === 'PENDING_PREPARE' || status === 'PREPARING' || status === 'DELIVERING') return 'warning'
  if (status === 'EXCEPTION' || status === 'CANCELLED') return 'danger'
  if (status === 'WAITING_CUSTOMER' || status === 'WAITING_DELIVERY') return 'info'
  return 'muted'
}

export function formatDeliveryCheckStatus(status?: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    COMPLETED: '已完成',
    BLOCKED: '已阻塞',
  }
  return map[status ?? ''] ?? status ?? '--'
}

export function getDeliveryCheckStatusTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'BLOCKED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'muted'
}
