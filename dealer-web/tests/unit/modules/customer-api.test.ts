import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { exportCustomers, fetchCustomerDetail } from '@/modules/customer/api/customer-api'

const mockedAxios = vi.mocked(axios)

describe('customer api export', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({
      data: new Blob(['fake']),
      headers: {
        'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'content-disposition': 'attachment; filename="customers.xlsx"',
      },
    })
  })

  it('sends ids as comma-separated string and does not put token in URL', async () => {
    await exportCustomers(['1', '2'])

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs).toBeDefined()
    expect(callArgs?.method).toBe('get')
    expect(callArgs?.url).toBe('/api/exportExcel')
    expect(callArgs?.responseType).toBe('blob')
    expect(callArgs?.params).toEqual({ ids: '1,2' })

    const url = String(callArgs?.url ?? '')
    expect(url).not.toContain('Authorization')
    expect(url).not.toContain('token')
  })

  it('omits params entirely for full export', async () => {
    await exportCustomers()

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.params).toBeUndefined()
  })

  it('treats empty ids array as full export', async () => {
    await exportCustomers([])

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.params).toBeUndefined()
  })

  it('returns DownloadResult with blob and filename', async () => {
    const result = await exportCustomers(['1'])

    expect(result.blob).toBeInstanceOf(Blob)
    expect(result.filename).toBe('customers.xlsx')
  })
})

describe('customer api detail', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({
      data: {
        code: 200,
        msg: 'OK',
        data: {
          id: 1,
          customerName: '张三',
          phone: '13800138000',
        },
      },
    })
  })

  it('fetchCustomerDetail sends GET to /api/customer/{id}', async () => {
    const result = await fetchCustomerDetail(42)

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('get')
    expect(callArgs?.url).toBe('/api/customer/42')

    expect(result.id).toBe(1)
    expect(result.customerName).toBe('张三')
    expect(result.phone).toBe('13800138000')
  })
})
