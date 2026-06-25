import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export const PRODUCT_STATUS = {
  ON_SALE: 'ON_SALE',
  OFF_SALE: 'OFF_SALE',
} as const

export type ProductStatus = (typeof PRODUCT_STATUS)[keyof typeof PRODUCT_STATUS]

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
  status?: ProductStatus
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
  status?: ProductStatus
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
  status?: ProductStatus
  code?: string
  description?: string
  sort?: number
}

export interface ProductFormValues {
  sku: string
  name: string
  categoryId: EntityId | string
  specification: string
  price: number
  stock: number
  minStock: number
  status: ProductStatus
}

export interface CreateProductRequest {
  sku: string
  name: string
  categoryId?: EntityId
  specification: string
  price: number
  stock: number
  minStock: number
  status: ProductStatus
}

export type UpdateProductRequest = Omit<CreateProductRequest, 'stock'>

export function toCreateProductRequest(values: ProductFormValues): CreateProductRequest {
  return {
    sku: values.sku.trim(),
    name: values.name.trim(),
    categoryId: normalizeProductCategoryId(values.categoryId),
    specification: values.specification.trim(),
    price: values.price,
    stock: values.stock,
    minStock: values.minStock,
    status: values.status,
  }
}

export function toUpdateProductRequest(values: ProductFormValues): UpdateProductRequest {
  return {
    sku: values.sku.trim(),
    name: values.name.trim(),
    categoryId: normalizeProductCategoryId(values.categoryId),
    specification: values.specification.trim(),
    price: values.price,
    minStock: values.minStock,
    status: values.status,
  }
}

export function getProductMutationErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError && error.code === API_ERROR_CODE.RESOURCE_IN_USE) {
    return '该产品已被客户、线索、交易、库存流水或促销引用，不能直接删除'
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export function getProductCategoryMutationErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError && error.code === API_ERROR_CODE.RESOURCE_IN_USE) {
    return '该分类已被商品或历史记录引用，不能直接删除'
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

function normalizeProductCategoryId(value: EntityId | string): EntityId | undefined {
  if (value === '') {
    return undefined
  }
  return typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value
}
