import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  Product,
  ProductCategory,
  ProductForm,
  ProductPromotion,
  ProductQuery,
  StockAlert,
} from '@/modules/product/model/product.types'

export function fetchProductPage(params: ProductQuery): Promise<PageResult<Product>> {
  return httpClient.get<PageResult<Product>>('/api/products', { params })
}

export function fetchProductDetail(id: EntityId): Promise<Product> {
  return httpClient.get<Product>(`/api/products/${id}`)
}

export function createProduct(data: ProductForm): Promise<unknown> {
  return httpClient.post('/api/products', data)
}

export function updateProduct(id: EntityId, data: ProductForm): Promise<unknown> {
  return httpClient.put(`/api/products/${id}`, data)
}

export function deleteProduct(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/products/${id}`)
}

export function fetchStockAlerts(params: ProductQuery): Promise<PageResult<StockAlert>> {
  return httpClient.get<PageResult<StockAlert>>('/api/products/stockalerts', { params })
}

export function restockProduct(data: ProductForm): Promise<unknown> {
  return httpClient.post('/api/productstock/restock', data)
}

export function fetchStockRecords(id: EntityId, params: ProductQuery): Promise<PageResult<StockAlert>> {
  return httpClient.get<PageResult<StockAlert>>(`/api/productstock/records/${id}`, { params })
}

export function fetchPromotionPage(params: ProductQuery): Promise<PageResult<ProductPromotion>> {
  return httpClient.get<PageResult<ProductPromotion>>('/api/product-promotions', { params })
}

export function createPromotion(data: ProductForm): Promise<unknown> {
  return httpClient.post('/api/product-promotions', data)
}

export function updatePromotion(id: EntityId, data: ProductForm): Promise<unknown> {
  return httpClient.put(`/api/product-promotions/${id}`, data)
}

export function deletePromotion(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/product-promotions/${id}`)
}

export function fetchCategoryPage(params: ProductQuery): Promise<PageResult<ProductCategory>> {
  return httpClient.get<PageResult<ProductCategory>>('/api/product-categories', { params })
}

export function createCategory(data: ProductForm): Promise<unknown> {
  return httpClient.post('/api/product-categories', data)
}

export function updateCategory(id: EntityId, data: ProductForm): Promise<unknown> {
  return httpClient.put(`/api/product-categories/${id}`, data)
}

export function deleteCategory(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/product-categories/${id}`)
}

export const getProductList = fetchProductPage
export const getProductDetail = fetchProductDetail
export const getStockAlerts = fetchStockAlerts
export const getStockRecords = fetchStockRecords
export const getPromotionList = fetchPromotionPage
export const getCategoryList = fetchCategoryPage
