import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

interface NameSummary { id?: EntityId; name?: string }
interface DictSummary { id?: EntityId; typeValue?: string; valueCode?: string }

export interface Clue {
  id?: EntityId
  ownerId?: EntityId
  activityId?: EntityId
  fullName?: string
  phone?: string
  qq?: string | number
  email?: string
  age?: string | number
  yearIncome?: string | number
  appellation?: EntityId
  weixin?: string
  job?: string
  address?: string
  needLoan?: EntityId
  intentionState?: EntityId
  intentionProduct?: EntityId
  state?: number | EntityId
  source?: EntityId
  nextContactTime?: string
  description?: string
  ownerDO?: NameSummary
  activityDO?: NameSummary
  intentionProductDO?: NameSummary
  appellationDO?: DictSummary
  needLoanDO?: DictSummary
  intentionStateDO?: DictSummary
  stateDO?: DictSummary
  sourceDO?: DictSummary
}

export interface ClueRemark {
  id: EntityId
  clueId?: EntityId
  noteContent?: string
  noteWayDO?: DictSummary
  createTime?: string
  editTime?: string
  createByDO?: NameSummary
  editByDO?: NameSummary
}

export interface ClueOwnerHistory {
  id: EntityId
  clueId?: EntityId
  fromOwnerId?: EntityId
  toOwnerId?: EntityId
  assignedBy?: EntityId
  reason?: string
  assignedTime?: string
  fromOwnerName?: string
  toOwnerName?: string
  assignedByName?: string
}

export interface TransferClueOwnerRequest {
  newOwnerId: EntityId
  reason: string
}

export interface ClueLifecycleRequest {
  reason: string
}

export interface ImportRowError {
  row: number
  column?: string
  reason?: string
}

export interface ImportResult {
  totalRows: number
  validRows: number
  failedRows: number
  importedCount: number
  errors?: ImportRowError[]
}

export interface ClueQuery extends Partial<PageQuery> {
  ownerId?: EntityId
  activityId?: EntityId
  phone?: string
  fullName?: string
  state?: EntityId
}

export interface ClueForm {
  id?: EntityId
  ownerId?: EntityId
  activityId?: EntityId
  phone?: string
  fullName?: string
  qq?: string
  email?: string
  age?: string
  yearIncome?: string
  description?: string
  appellation?: EntityId
  weixin?: string
  job?: string
  address?: string
  needLoan?: EntityId
  intentionState?: EntityId
  intentionProduct?: EntityId
  state?: EntityId
  source?: EntityId
  nextContactTime?: string
}
