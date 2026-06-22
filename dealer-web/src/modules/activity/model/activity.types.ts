import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

interface OperatorSummary {
  id?: EntityId
  name?: string
}

export interface Activity {
  id?: EntityId
  name?: string
  ownerId?: EntityId
  ownerDO?: OperatorSummary
  createByDO?: OperatorSummary
  editByDO?: OperatorSummary
  cost?: number | string
  startTime?: string
  endTime?: string
  description?: string
  createTime?: string
  editTime?: string
}

export interface ActivityQuery extends Partial<PageQuery> {
  ownerId?: EntityId | ''
  name?: string
  startTime?: string
  endTime?: string
  cost?: string
  createTime?: string
}

export interface ActivityForm {
  id?: EntityId
  ownerId?: EntityId | string
  name?: string
  startTime?: string
  endTime?: string
  cost?: string
  description?: string
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
