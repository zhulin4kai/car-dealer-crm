import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

interface OperatorSummary {
  id?: EntityId
  name?: string
}

export type ActivityStatus =
  | 'DRAFT'
  | 'PLANNED'
  | 'ONGOING'
  | 'ENDED'
  | 'REVIEWED'
  | 'CLOSED'
  | 'CANCELED'

export const activityStatusOptions: Array<{ value: ActivityStatus; label: string; tone: string }> = [
  { value: 'DRAFT', label: '草稿', tone: 'bg-slate-100 text-slate-700' },
  { value: 'PLANNED', label: '待开始', tone: 'bg-sky-100 text-sky-700' },
  { value: 'ONGOING', label: '进行中', tone: 'bg-emerald-100 text-emerald-700' },
  { value: 'ENDED', label: '已结束', tone: 'bg-amber-100 text-amber-700' },
  { value: 'REVIEWED', label: '已复盘', tone: 'bg-indigo-100 text-indigo-700' },
  { value: 'CLOSED', label: '已关闭', tone: 'bg-zinc-200 text-zinc-700' },
  { value: 'CANCELED', label: '已取消', tone: 'bg-rose-100 text-rose-700' },
]

export function activityStatusLabel(status?: string): string {
  return activityStatusOptions.find(item => item.value === status)?.label ?? '--'
}

export function activityStatusTone(status?: string): string {
  return activityStatusOptions.find(item => item.value === status)?.tone ?? 'bg-muted text-muted-foreground'
}

export function isActivityCoreLocked(status?: string): boolean {
  return status === 'ENDED' || status === 'REVIEWED' || status === 'CLOSED' || status === 'CANCELED'
}

export interface Activity {
  id?: EntityId
  name?: string
  status?: ActivityStatus
  ownerId?: EntityId
  ownerDO?: OperatorSummary
  createByDO?: OperatorSummary
  editByDO?: OperatorSummary
  reviewedByDO?: OperatorSummary
  channel?: string
  targetModel?: string
  cost?: number | string
  actualCost?: number | string
  startTime?: string
  endTime?: string
  description?: string
  resultSummary?: string
  reviewConclusion?: string
  reviewedBy?: EntityId
  reviewedTime?: string
  closedReason?: string
  canceledReason?: string
  createTime?: string
  editTime?: string
}

export interface ActivityQuery extends Partial<PageQuery> {
  ownerId?: EntityId | ''
  name?: string
  status?: ActivityStatus | ''
  channel?: string
  startTime?: string
  endTime?: string
  cost?: string
  createTime?: string
}

export interface ActivityForm {
  id?: EntityId
  name: string
  channel: string
  targetModel?: string
  startTime: string
  endTime: string
  cost: string
  description?: string
}

export interface ReviewActivityForm {
  actualCost: string
  resultSummary: string
  reviewConclusion: string
}

export interface ActivityRoi {
  activityId?: EntityId
  activityName?: string
  status?: ActivityStatus
  plannedCost?: number | string
  actualCost?: number | string
  clueCount?: number
  validClueCount?: number
  customerCount?: number
  opportunityCount?: number
  testDriveCount?: number
  quoteCount?: number
  orderCount?: number
  dealAmount?: number | string
  roi?: number | string | null
}

export interface ActivityRemark {
  id: EntityId
  activityId?: EntityId
  noteContent?: string
  createTime?: string
  editTime?: string
  createByDO?: OperatorSummary
  editByDO?: OperatorSummary
}
