import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export type FollowRelatedObjectType = 'CLUE' | 'CUSTOMER' | 'OPPORTUNITY' | 'TEST_DRIVE' | 'ORDER'

export type FollowTaskStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'POSTPONED'
  | 'OVERDUE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'CLOSED'

export type FollowTaskType =
  | 'FIRST_CONTACT'
  | 'PHONE_FOLLOW_UP'
  | 'STORE_INVITATION'
  | 'TEST_DRIVE_CONFIRM'
  | 'QUOTE_COMMUNICATION'
  | 'PRICE_NEGOTIATION'
  | 'CONTRACT_SIGN_REMINDER'
  | 'PAYMENT_REMINDER'
  | 'DELIVERY_CONFIRM'
  | 'POST_DELIVERY_FOLLOW_UP'
  | 'LONG_TERM_MAINTENANCE'

export type FollowTaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export type CommunicationMethod = 'PHONE' | 'STORE_VISIT' | 'WECHAT' | 'SMS' | 'EMAIL' | 'OTHER'
export type CommunicationRecordStatus = 'ACTIVE' | 'CORRECTED' | 'VOIDED'

export interface FollowTask {
  id: EntityId
  title: string
  taskType: FollowTaskType
  relatedObjectType: FollowRelatedObjectType
  relatedObjectId: EntityId
  relatedObjectName?: string
  ownerId: EntityId
  ownerName?: string
  priority: FollowTaskPriority
  dueTime: string
  remindTime?: string
  status: FollowTaskStatus
  result?: string
  postponeReason?: string
  originalDueTime?: string
  postponeCount?: number
  cancelReason?: string
  communicationRecordId?: EntityId
  completedTime?: string
  completedBy?: EntityId
  createTime?: string
  updateTime?: string
}

export interface CommunicationRecord {
  id: EntityId
  followTaskId?: EntityId
  parentRecordId?: EntityId
  relatedObjectType: FollowRelatedObjectType
  relatedObjectId: EntityId
  relatedObjectName?: string
  ownerId: EntityId
  ownerName?: string
  communicationMethod: CommunicationMethod
  communicationTime: string
  summary: string
  customerFeedback?: string
  nextAction?: string
  nextFollowTime?: string
  status: CommunicationRecordStatus
  correctionReason?: string
  voidReason?: string
  createTime?: string
  updateTime?: string
}

export interface FollowTaskQuery extends PageQuery {
  status?: FollowTaskStatus
  taskType?: FollowTaskType
  relatedObjectType?: FollowRelatedObjectType
  relatedObjectId?: EntityId
  ownerId?: EntityId
  overdueOnly?: boolean
  keyword?: string
}

export interface CommunicationRecordQuery extends PageQuery {
  followTaskId?: EntityId
  relatedObjectType?: FollowRelatedObjectType
  relatedObjectId?: EntityId
  ownerId?: EntityId
  status?: CommunicationRecordStatus
  keyword?: string
}

export interface CreateFollowTaskRequest {
  title: string
  taskType: FollowTaskType
  relatedObjectType: FollowRelatedObjectType
  relatedObjectId: EntityId
  ownerId: EntityId
  priority?: FollowTaskPriority
  dueTime: string
  remindTime?: string
}

export interface PostponeFollowTaskRequest {
  newDueTime: string
  remindTime?: string
  reason: string
}

export interface CancelFollowTaskRequest {
  reason: string
}

export interface CompleteFollowTaskRequest {
  communicationMethod: CommunicationMethod
  communicationTime?: string
  summary: string
  customerFeedback?: string
  result: string
  nextAction?: string
  nextFollowTime?: string
  createNextTask?: boolean
  nextTaskType?: FollowTaskType
  nextTaskTitle?: string
  nextTaskPriority?: FollowTaskPriority
  nextTaskDueTime?: string
  nextTaskRemindTime?: string
}

export interface CreateCommunicationRecordRequest {
  followTaskId?: EntityId
  relatedObjectType: FollowRelatedObjectType
  relatedObjectId: EntityId
  communicationMethod: CommunicationMethod
  communicationTime?: string
  summary: string
  customerFeedback?: string
  nextAction?: string
  nextFollowTime?: string
  createNextTask?: boolean
  nextTaskType?: FollowTaskType
  nextTaskTitle?: string
  nextTaskPriority?: FollowTaskPriority
  nextTaskDueTime?: string
  nextTaskRemindTime?: string
}

export interface CorrectCommunicationRecordRequest {
  communicationMethod: CommunicationMethod
  communicationTime?: string
  summary: string
  customerFeedback?: string
  nextAction?: string
  nextFollowTime?: string
  correctionReason: string
}

export interface VoidCommunicationRecordRequest {
  reason: string
}

export const FOLLOW_OBJECT_OPTIONS: Array<{ value: FollowRelatedObjectType; label: string }> = [
  { value: 'CLUE', label: '线索' },
  { value: 'CUSTOMER', label: '客户' },
  { value: 'OPPORTUNITY', label: '商机' },
  { value: 'TEST_DRIVE', label: '试驾' },
  { value: 'ORDER', label: '订单' },
]

export const FOLLOW_TASK_TYPE_OPTIONS: Array<{ value: FollowTaskType; label: string }> = [
  { value: 'FIRST_CONTACT', label: '首次联系' },
  { value: 'PHONE_FOLLOW_UP', label: '电话回访' },
  { value: 'STORE_INVITATION', label: '到店邀约' },
  { value: 'TEST_DRIVE_CONFIRM', label: '试驾确认' },
  { value: 'QUOTE_COMMUNICATION', label: '报价沟通' },
  { value: 'PRICE_NEGOTIATION', label: '价格协商' },
  { value: 'CONTRACT_SIGN_REMINDER', label: '签约提醒' },
  { value: 'PAYMENT_REMINDER', label: '收款提醒' },
  { value: 'DELIVERY_CONFIRM', label: '交付确认' },
  { value: 'POST_DELIVERY_FOLLOW_UP', label: '交付回访' },
  { value: 'LONG_TERM_MAINTENANCE', label: '长期维护' },
]

export const FOLLOW_STATUS_OPTIONS: Array<{ value: FollowTaskStatus; label: string }> = [
  { value: 'PENDING', label: '待处理' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'POSTPONED', label: '已延期' },
  { value: 'OVERDUE', label: '已逾期' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'CLOSED', label: '已关闭' },
]

export const FOLLOW_PRIORITY_OPTIONS: Array<{ value: FollowTaskPriority; label: string }> = [
  { value: 'LOW', label: '低' },
  { value: 'NORMAL', label: '普通' },
  { value: 'HIGH', label: '高' },
  { value: 'URGENT', label: '紧急' },
]

export const COMMUNICATION_METHOD_OPTIONS: Array<{ value: CommunicationMethod; label: string }> = [
  { value: 'PHONE', label: '电话' },
  { value: 'STORE_VISIT', label: '到店' },
  { value: 'WECHAT', label: '微信' },
  { value: 'SMS', label: '短信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'OTHER', label: '其他' },
]

export const COMMUNICATION_STATUS_OPTIONS: Array<{ value: CommunicationRecordStatus; label: string }> = [
  { value: 'ACTIVE', label: '有效' },
  { value: 'CORRECTED', label: '已更正' },
  { value: 'VOIDED', label: '已作废' },
]

export function formatFollowStatus(status?: string): string {
  return FOLLOW_STATUS_OPTIONS.find(option => option.value === status)?.label ?? status ?? '--'
}

export function formatFollowTaskType(type?: string): string {
  return FOLLOW_TASK_TYPE_OPTIONS.find(option => option.value === type)?.label ?? type ?? '--'
}

export function formatFollowObjectType(type?: string): string {
  return FOLLOW_OBJECT_OPTIONS.find(option => option.value === type)?.label ?? type ?? '--'
}

export function formatCommunicationMethod(method?: string): string {
  return COMMUNICATION_METHOD_OPTIONS.find(option => option.value === method)?.label ?? method ?? '--'
}

export function formatCommunicationStatus(status?: string): string {
  return COMMUNICATION_STATUS_OPTIONS.find(option => option.value === status)?.label ?? status ?? '--'
}

export function getFollowStatusTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'OVERDUE' || status === 'CANCELLED' || status === 'CLOSED') return 'danger'
  if (status === 'POSTPONED') return 'warning'
  if (status === 'IN_PROGRESS') return 'info'
  return 'muted'
}

export function isFollowTaskTerminal(status?: string): boolean {
  return status === 'COMPLETED' || status === 'CANCELLED' || status === 'CLOSED'
}
