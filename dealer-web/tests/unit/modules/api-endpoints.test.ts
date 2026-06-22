import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchActivityPage } from '@/modules/activity/api/activity-api'
import { batchDeleteCluesByIds } from '@/modules/clue/api/clue-api'
import { fetchCustomerOptions } from '@/modules/customer/api/customer-api'
import { clearCache } from '@/modules/dict/api/dict-api'
import { fetchProductPage } from '@/modules/product/api/product-api'
import { fetchSettlementPreview, fetchTranPage, settleTran } from '@/modules/tran/api/tran-api'
import { fetchUserPage } from '@/modules/user/api/user-api'

const mockedAxios = vi.mocked(axios)

describe('module api endpoints', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
  })

  it('uses typed module APIs instead of legacy api files', async () => {
    await fetchActivityPage({ page: 1, size: 10 })
    await batchDeleteCluesByIds([1, 2])
    await fetchCustomerOptions()
    await clearCache()
    await fetchProductPage({ page: 1, size: 10 })
    await fetchTranPage({ page: 1, size: 10 })
    await fetchUserPage({ page: 1, size: 10 })

    const calls = mockedAxios.request.mock.calls.map(([config]) => config)
    expect(calls.map((config) => config.url)).toEqual([
      '/api/activitys',
      '/api/clue/batch',
      '/api/customer/options',
      '/api/dict/clear',
      '/api/products',
      '/api/tran/list',
      '/api/users',
    ])
  })

  it('sends the server preview token when settling a transaction', async () => {
    await fetchSettlementPreview(9, { promotionId: 3 })
    await settleTran(9, {
      promotionId: 3,
      expectedVersion: 4,
      pricingFingerprint: 'sha256-token',
    })

    expect(mockedAxios.request).toHaveBeenNthCalledWith(1, {
      data: { promotionId: 3 },
      method: 'post',
      url: '/api/tran/9/settlement-preview',
    })
    expect(mockedAxios.request).toHaveBeenNthCalledWith(2, {
      data: {
        promotionId: 3,
        expectedVersion: 4,
        pricingFingerprint: 'sha256-token',
      },
      method: 'put',
      url: '/api/tran/9/settle',
    })
  })
})
