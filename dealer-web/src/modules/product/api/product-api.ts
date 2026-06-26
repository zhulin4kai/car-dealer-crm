import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  Product,
  ProductCategory,
  ProductForm,
  CreateProductRequest,
  UpdateProductRequest,
  ProductPromotion,
  ProductQuery,
  CreatePromotionRequest,
  UpdatePromotionRequest,
  RestockRequest,
  StockAlert,
  StockAlertQuery,
  StockRecord,
  ProductVehicle,
  ProductVehicleQuery,
  CreateProductVehicleRequest,
  ReserveProductVehicleRequest,
  ReleaseProductVehicleRequest,
} from '@/modules/product/model/product.types'

export function fetchProductPage(params: ProductQuery): Promise<PageResult<Product>> {
  return httpClient.get<PageResult<Product>>('/api/products', { params })
}

export function fetchProductDetail(id: EntityId): Promise<Product> {
  return httpClient.get<Product>(`/api/products/${id}`)
}

export function createProduct(data: CreateProductRequest): Promise<unknown> {
  return httpClient.post('/api/products', data)
}

export function updateProduct(id: EntityId, data: UpdateProductRequest): Promise<unknown> {
  return httpClient.put(`/api/products/${id}`, data)
}

export function deleteProduct(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/products/${id}`)
}

export function fetchStockAlerts(params: StockAlertQuery, signal?: AbortSignal): Promise<PageResult<StockAlert>> {
  return httpClient.get<PageResult<StockAlert>>('/api/products/stockalerts', { params, signal })
}

export function restockProduct(data: RestockRequest): Promise<unknown> {
  return httpClient.post('/api/productstock/restock', data)
}

export function fetchStockRecords(id: EntityId, params: ProductQuery): Promise<PageResult<StockRecord>> {
  return httpClient.get<PageResult<StockRecord>>(`/api/productstock/records/${id}`, { params })
}

export function fetchProductVehicles(params: ProductVehicleQuery): Promise<PageResult<ProductVehicle>> {
  return httpClient.get<PageResult<ProductVehicle>>('/api/productstock/vehicles', { params })
}

export function inboundProductVehicle(data: CreateProductVehicleRequest): Promise<ProductVehicle> {
  return httpClient.post<ProductVehicle>('/api/productstock/vehicles', data)
}

export function reserveProductVehicle(
  vehicleId: EntityId,
  data: ReserveProductVehicleRequest,
): Promise<ProductVehicle> {
  return httpClient.post<ProductVehicle>(`/api/productstock/vehicles/${vehicleId}/reserve`, data)
}

export function releaseProductVehicle(
  vehicleId: EntityId,
  data: ReleaseProductVehicleRequest,
): Promise<ProductVehicle> {
  return httpClient.post<ProductVehicle>(`/api/productstock/vehicles/${vehicleId}/release`, data)
}

export function fetchPromotionPage(params: ProductQuery): Promise<PageResult<ProductPromotion>> {
  return httpClient.get<PageResult<ProductPromotion>>('/api/product-promotions', { params })
}

export function fetchPromotionDetail(id: EntityId): Promise<ProductPromotion> {
  return httpClient.get<ProductPromotion>(`/api/product-promotions/${id}`)
}

export function createPromotion(data: CreatePromotionRequest): Promise<unknown> {
  return httpClient.post('/api/product-promotions', data)
}

export function updatePromotion(id: EntityId, data: UpdatePromotionRequest): Promise<unknown> {
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
