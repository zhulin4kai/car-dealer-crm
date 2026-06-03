import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchActivityPage } from '@/modules/activity/api/activity-api'
import { batchDeleteCluesByIds } from '@/modules/clue/api/clue-api'
import { fetchCustomerOptions } from '@/modules/customer/api/customer-api'
import { clearCache } from '@/modules/dict/api/dict-api'
import { fetchProductPage } from '@/modules/product/api/product-api'
import { getAllMonitorData, toggleSystemStatus } from '@/modules/system/api/system-api'
import { fetchTranPage } from '@/modules/tran/api/tran-api'
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
    await getAllMonitorData()
    await fetchTranPage({ page: 1, size: 10 })
    await fetchUserPage({ page: 1, size: 10 })

    const calls = mockedAxios.request.mock.calls.map(([config]) => config)
    expect(calls.map((config) => config.url)).toEqual([
      '/api/activitys',
      '/api/clue/batch',
      '/api/customer/options',
      '/api/dict/clear',
      '/api/products',
      '/api/monitor/all',
      '/api/tran/list',
      '/api/users',
    ])
  })

  it('sends system status with backend isopen field', async () => {
    await toggleSystemStatus(1, 'false')

    expect(mockedAxios.request).toHaveBeenLastCalledWith({
      data: { isopen: 'false' },
      method: 'put',
      url: '/api/system/1/status',
    })
  })
})
