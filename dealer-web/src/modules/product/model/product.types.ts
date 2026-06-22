import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export interface Product {
  id?: EntityId
  sku?: string
  name?: string
  categoryId?: EntityId
  categoryName?: string
  specification?: string
  price?: number | string
  stock?: number
  minStock?: number
  status?: string
  createTime?: string
  updateTime?: string
}

export interface ProductCategory {
  id?: EntityId
  name?: string
  code?: string
  description?: string
  sort?: number
  status?: string
  createTime?: string
  updateTime?: string
}

export interface ProductPromotion {
  id: EntityId
  productId: EntityId
  name: string
  type: string
  discount: number | string
  startTime: string
  endTime: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface CreatePromotionRequest {
  productId: EntityId
  name: string
  type: string
  discount: number | string
  startTime: string
  endTime: string
  status: string
}

export type UpdatePromotionRequest = CreatePromotionRequest

export interface PromotionFormValues {
  productId: string
  name: string
  type: string
  discount: number
  startTime: string
  endTime: string
  status: string
}

export function toCreatePromotionRequest(values: PromotionFormValues): CreatePromotionRequest {
  return {
    productId: values.productId,
    name: values.name,
    type: values.type,
    discount: values.discount,
    startTime: values.startTime,
    endTime: values.endTime,
    status: values.status,
  }
}

export function toUpdatePromotionRequest(values: PromotionFormValues): UpdatePromotionRequest {
  return toCreatePromotionRequest(values)
}

export interface StockAlert {
  id: EntityId
  sku: string
  name: string
  categoryId?: number | string
  categoryName?: string
  specification?: string
  price?: number | string
  stock: number
  minStock: number
  status?: string
  createTime?: string
  updateTime?: string
}

export interface StockRecord {
  id: EntityId
  productId: EntityId
  quantity: number
  type: string
  remark?: string
  createTime?: string
}

export interface StockAlertQuery extends PageQuery {
  sku?: string
  name?: string
  categoryId?: number | string
}

export interface RestockRequest {
  productId: EntityId
  quantity: number
  remark?: string
}

export interface ProductQuery extends Partial<PageQuery> {
  sku?: string
  name?: string
  categoryId?: EntityId
  status?: string
}

export interface ProductForm {
  id?: EntityId
  sku?: string
  name?: string
  categoryId?: EntityId | string
  specification?: string
  price?: number
  stock?: number
  minStock?: number
  status?: string
  code?: string
  description?: string
  sort?: number
}
