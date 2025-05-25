import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'

// 产品管理
// 加载产品列表
export function getProductList(params) {
    return doGet('/api/products', params)
}

// 获取产品详情
export function getProductDetail(id) {
    return doGet(`/api/products/${id}`)
}

// 新增产品
export function createProduct(data) {
    return doPost('/api/products', data)
}

// 编辑产品
export function updateProduct(id, data) {
    return doPut(`/api/products/${id}`, data)
}

// 删除产品
export function deleteProduct(id) {
    return doDelete(`/api/products/${id}`)
}

// 库存预警管理
// 加载库存预警列表
export function getStockAlerts(params) {
    return doGet('/api/products/stockalerts', params)
}

// 补货
export function restockProduct(data) {
    return doPost('/api/productstock/restock', data)
}

// 加载库存变动记录
export function getStockRecords(id, params) {
    return doGet(`/api/productstock/records/${id}`, params)
}

// 促销管理
// 加载促销列表
export function getPromotionList(params) {
    return doGet('/api/product-promotions', params)
}

// 新增促销
export function createPromotion(data) {
    return doPost('/api/product-promotions', data)
}

// 编辑促销
export function updatePromotion(id, data) {
    return doPut(`/api/product-promotions/${id}`, data)
}

// 删除促销
export function deletePromotion(id) {
    return doDelete(`/api/product-promotions/${id}`)
}

// 分类管理
// 加载分类列表
export function getCategoryList(params) {
    return doGet('/api/product-categories', params)
}

// 新增分类
export function createCategory(data) {
    return doPost('/api/product-categories', data)
}

// 编辑分类
export function updateCategory(id, data) {
    return doPut(`/api/product-categories/${id}`, data)
}

// 删除分类
export function deleteCategory(id) {
    return doDelete(`/api/product-categories/${id}`)
} 