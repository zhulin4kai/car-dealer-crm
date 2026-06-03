import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Tran extends LooseRecord {
  id?: number | string
  name?: string
}

export interface TranProduct extends LooseRecord {
  id?: number | string
  productId?: number | string
}

export interface TranInvoice extends LooseRecord {
  id?: number | string
}

export type TranQuery = Partial<PageQuery> & LooseRecord
export type TranForm = LooseRecord
