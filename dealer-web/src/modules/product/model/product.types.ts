import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Product extends LooseRecord {
  id?: number | string
  name?: string
}

export interface ProductCategory extends LooseRecord {
  id?: number | string
  name?: string
  code?: string
}

export interface ProductPromotion extends LooseRecord {
  id?: number | string
  name?: string
}

export interface StockAlert extends LooseRecord {
  id?: number | string
}

export type ProductQuery = Partial<PageQuery> & LooseRecord
export type ProductForm = LooseRecord
