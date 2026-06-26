import axios from 'axios'
import fs from 'node:fs'
import path from 'node:path'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchLoginLogPage, fetchOperationLogPage } from '@/modules/audit/api/audit-api'
import { fetchActivityPage } from '@/modules/activity/api/activity-api'
import { batchDeleteCluesByIds } from '@/modules/clue/api/clue-api'
import { fetchCustomerOptions } from '@/modules/customer/api/customer-api'
import { clearCache } from '@/modules/dict/api/dict-api'
import { fetchProductPage } from '@/modules/product/api/product-api'
import { createTran, fetchSettlementPreview, fetchTranPage, settleTran } from '@/modules/tran/api/tran-api'
import { fetchUserPage } from '@/modules/user/api/user-api'

const mockedAxios = vi.mocked(axios)
const srcDir = path.resolve(__dirname, '../../../src')

function collectSourceFiles(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      return collectSourceFiles(fullPath)
    }
    return /\.(ts|vue)$/.test(entry.name) ? [fullPath] : []
  })
}

describe('module api endpoints', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
  })

  it('uses typed module APIs instead of legacy api files', async () => {
    await fetchActivityPage({ page: 1, size: 10 })
    await fetchLoginLogPage({ page: 1, size: 10 })
    await fetchOperationLogPage({ page: 1, size: 10 })
    await batchDeleteCluesByIds([1, 2])
    await fetchCustomerOptions()
    await clearCache()
    await fetchProductPage({ page: 1, size: 10 })
    await fetchTranPage({ page: 1, size: 10 })
    await fetchUserPage({ page: 1, size: 10 })

    const calls = mockedAxios.request.mock.calls.map(([config]) => config)
    expect(calls.map((config) => config.url)).toEqual([
      '/api/activities',
      '/api/audit/login-logs',
      '/api/audit/operation-logs',
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

  it('uses the current transaction create endpoint', async () => {
    await createTran({
      customerId: 1,
      products: [{ productId: 2, quantity: 1 }],
      description: '购车意向',
    })

    expect(mockedAxios.request).toHaveBeenCalledWith({
      data: {
        customerId: 1,
        products: [{ productId: 2, quantity: 1 }],
        description: '购车意向',
      },
      method: 'post',
      url: '/api/transactions',
    })
  })

  it('does not introduce legacy endpoint calls in business source files', () => {
    const legacyEndpointPatterns = [
      /['"`]\/api\/activitys['"`]/,
      /['"`]\/api\/tran\/create['"`]/,
    ]
    const offenders = collectSourceFiles(srcDir).filter((file) => {
      const content = fs.readFileSync(file, 'utf8')
      return legacyEndpointPatterns.some((pattern) => pattern.test(content))
    })

    expect(offenders.map((file) => path.relative(srcDir, file))).toEqual([])
  })

  it('keeps legacy do* helpers out of business source files', () => {
    const offenders = collectSourceFiles(srcDir).filter((file) => {
      const content = fs.readFileSync(file, 'utf8')
      return /import\s*\{[^}]*\bdo(?:Get|Post|Put|Delete)\b/.test(content)
    })

    expect(offenders.map((file) => path.relative(srcDir, file))).toEqual([])
  })
})
