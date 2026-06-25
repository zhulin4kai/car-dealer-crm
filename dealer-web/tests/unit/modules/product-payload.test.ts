import { describe, expect, it } from 'vitest'

import { ApiError } from '@/shared/api/api-error'
import {
  getProductCategoryMutationErrorMessage,
  getProductMutationErrorMessage,
  toCreateProductRequest,
  toUpdateProductRequest,
  type ProductFormValues,
} from '@/modules/product/model/product.types'

const formValues: ProductFormValues = {
  sku: 'SKU-001',
  name: '测试车型',
  categoryId: '1',
  specification: '2026款',
  price: 120000,
  stock: 10,
  minStock: 2,
  status: 'ON_SALE',
}

describe('product request mappers', () => {
  it('toCreateProductRequest includes initial stock', () => {
    expect(toCreateProductRequest(formValues)).toEqual({
      sku: 'SKU-001',
      name: '测试车型',
      categoryId: 1,
      specification: '2026款',
      price: 120000,
      stock: 10,
      minStock: 2,
      status: 'ON_SALE',
    })
  })

  it('toUpdateProductRequest excludes stock so product edit cannot change inventory facts', () => {
    const request = toUpdateProductRequest(formValues)

    expect(request).toEqual({
      sku: 'SKU-001',
      name: '测试车型',
      categoryId: 1,
      specification: '2026款',
      price: 120000,
      minStock: 2,
      status: 'ON_SALE',
    })
    expect(request).not.toHaveProperty('stock')
  })

  it('maps product reference deletion errors by stable code', () => {
    const messageA = getProductMutationErrorMessage(new ApiError(422, '引用文案A', null), '删除失败')
    const messageB = getProductMutationErrorMessage(new ApiError(422, 'reference message B', null), '删除失败')

    expect(messageA).toBe('该产品已被客户、线索、交易、库存流水或促销引用，不能直接删除')
    expect(messageB).toBe(messageA)
  })

  it('does not classify product reference deletion by message text', () => {
    const message = getProductMutationErrorMessage(new ApiError(500, '外键约束或引用失败', null), '删除失败')

    expect(message).toBe('外键约束或引用失败')
  })

  it('maps product category reference deletion errors by stable code', () => {
    const message = getProductCategoryMutationErrorMessage(new ApiError(422, 'any locale', null), '删除失败')

    expect(message).toBe('该分类已被商品或历史记录引用，不能直接删除')
  })
})
