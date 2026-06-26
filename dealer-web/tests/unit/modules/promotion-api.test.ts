import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createPromotion, updatePromotion, fetchPromotionDetail } from '@/modules/product/api/product-api'
import {
  toCreatePromotionRequest,
  toUpdatePromotionRequest,
  type PromotionFormValues,
} from '@/modules/product/model/product.types'

const mockedAxios = vi.mocked(axios)

const formValues: PromotionFormValues = {
  productId: '1',
  code: ' SUMMER-2026 ',
  name: '夏季促销',
  type: 'PERCENTAGE',
  discount: 8.5,
  ruleSummary: ' 夏季限时折扣 ',
  applicableStore: 'ALL',
  customerType: 'ALL',
  applicableChannel: 'ALL',
  inventoryScope: 'ALL',
  stackable: false,
  priority: 10,
  budgetLimit: null,
  usageLimit: null,
  startTime: '2026-06-22 00:00:00',
  endTime: '2026-07-22 23:59:59',
}

describe('promotion request mappers', () => {
  it('toCreatePromotionRequest includes productId', () => {
    const request = toCreatePromotionRequest(formValues)

    expect(request).toEqual({
      productId: '1',
      code: 'SUMMER-2026',
      name: '夏季促销',
      type: 'PERCENTAGE',
      discount: 8.5,
      ruleSummary: '夏季限时折扣',
      applicableStore: 'ALL',
      customerType: 'ALL',
      applicableChannel: 'ALL',
      inventoryScope: 'ALL',
      stackable: false,
      priority: 10,
      budgetLimit: null,
      usageLimit: null,
      startTime: '2026-06-22 00:00:00',
      endTime: '2026-07-22 23:59:59',
    })
    expect(request).toHaveProperty('productId')
  })

  it('toUpdatePromotionRequest has same fields as create', () => {
    const request = toUpdatePromotionRequest(formValues)

    expect(request).toEqual(toCreatePromotionRequest(formValues))
    expect(request).toHaveProperty('productId')
  })
})

describe('promotion api request bodies', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('createPromotion sends JSON with productId', async () => {
    await createPromotion(toCreatePromotionRequest(formValues))

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('post')
    expect(callArgs?.url).toBe('/api/product-promotions')
    expect(callArgs?.data).toEqual({
      productId: '1',
      code: 'SUMMER-2026',
      name: '夏季促销',
      type: 'PERCENTAGE',
      discount: 8.5,
      ruleSummary: '夏季限时折扣',
      applicableStore: 'ALL',
      customerType: 'ALL',
      applicableChannel: 'ALL',
      inventoryScope: 'ALL',
      stackable: false,
      priority: 10,
      budgetLimit: null,
      usageLimit: null,
      startTime: '2026-06-22 00:00:00',
      endTime: '2026-07-22 23:59:59',
    })
  })

  it('updatePromotion sends JSON with productId to /api/product-promotions/{id}', async () => {
    await updatePromotion(5, toUpdatePromotionRequest(formValues))

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('put')
    expect(callArgs?.url).toBe('/api/product-promotions/5')
    expect(callArgs?.data).toHaveProperty('productId', '1')
  })

  it('fetchPromotionDetail sends GET to /api/product-promotions/{id}', async () => {
    await fetchPromotionDetail(3)

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('get')
    expect(callArgs?.url).toBe('/api/product-promotions/3')
  })
})
