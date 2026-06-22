import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export interface CustomerListItem {
  id: EntityId
  clueId?: number | string
  customerName?: string
  phone?: string
  weixin?: string
  ownerName?: string
  activityName?: string
  appellationName?: string
  needLoanName?: string
  intentionStateName?: string
  stateName?: string
  sourceName?: string
  intentionProductName?: string
  product?: number | string
  description?: string
  nextContactTime?: string
  createTime?: string
}

export interface CustomerDetail {
  id: EntityId
  clueId?: number | string
  customerName?: string
  phone?: string
  weixin?: string
  qq?: string
  email?: string
  age?: number
  job?: string
  yearIncome?: string
  address?: string
  ownerName?: string
  activityName?: string
  appellationName?: string
  needLoanName?: string
  sourceName?: string
  productName?: string
  product?: number | string
  description?: string
  nextContactTime?: string
  createTime?: string
}

export type Customer = CustomerListItem

export interface CustomerQuery extends Partial<PageQuery> {
  current?: number
  customerName?: string
  productId?: number | string
}
