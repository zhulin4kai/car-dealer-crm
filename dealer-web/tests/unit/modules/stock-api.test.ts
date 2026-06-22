import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchStockAlerts, fetchCategoryPage, restockProduct } from '@/modules/product/api/product-api'

const mockedAxios = vi.mocked(axios)

describe('stock api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('fetchStockAlerts sends page/size and filter params', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: {
        code: 200,
        msg: 'OK',
        data: {
          list: [
            { id: 1, sku: 'SKU001', name: 'SUV', stock: 3, minStock: 5 },
          ],
          total: 1,
          pageSize: 10,
        },
      },
    })

    const result = await fetchStockAlerts({ page: 1, size: 10, sku: 'SUV', categoryId: 2 })

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('get')
    expect(callArgs?.url).toBe('/api/products/stockalerts')
    expect(callArgs?.params).toEqual({ page: 1, size: 10, sku: 'SUV', categoryId: 2 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0]!.name).toBe('SUV')
    expect(result.total).toBe(1)
    expect(result.pageSize).toBe(10)
  })

  it('fetchStockAlerts returns empty list without checking res.data', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: {
        code: 200,
        msg: 'OK',
        data: { list: [], total: 0, pageSize: 10 },
      },
    })

    const result = await fetchStockAlerts({ page: 1, size: 10 })

    expect(result.list).toEqual([])
    expect(result.total).toBe(0)
  })

  it('fetchCategoryPage returns unwrapped PageResult directly', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: {
        code: 200,
        msg: 'OK',
        data: {
          list: [
            { id: 1, name: 'SUV', code: 'SUV' },
            { id: 2, name: 'Sedan', code: 'SDN' },
          ],
          total: 2,
          pageSize: 100,
        },
      },
    })

    const result = await fetchCategoryPage({ page: 1, size: 100 })

    expect(result.list).toHaveLength(2)
    expect(result.list[0]!.name).toBe('SUV')
    expect(result.total).toBe(2)
  })

  it('restockProduct sends RestockRequest as JSON body', async () => {
    await restockProduct({ productId: 1, quantity: 50, remark: '紧急补货' })

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('post')
    expect(callArgs?.url).toBe('/api/productstock/restock')
    expect(callArgs?.data).toEqual({ productId: 1, quantity: 50, remark: '紧急补货' })
  })
})
