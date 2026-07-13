import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchUserHistory } from '@/modules/user/api/user-history-api'

const mockedAxios = vi.mocked(axios)

function historyData(overrides: Record<string, unknown> = {}) {
  return {
    list: [
      {
        eventId: 'event-1',
        sourceKey: 'source-1',
        actionCode: 'USER_ROLE_UPDATED',
        actionName: '调整用户角色',
        categoryCode: 'AUTHORIZATION',
        categoryName: '授权',
        target: { typeCode: 'ROLE', typeName: '角色', id: 8, code: 'sales', name: '销售' },
        operator: { id: 7, name: '王经理', employeeNo: 'E00007' },
        beforeValues: [{ code: 'role', label: '角色', displayValue: '销售' }],
        afterValues: [{ code: 'role', label: '角色', displayValue: '主管' }],
        reason: '岗位调整',
        effectiveFrom: null,
        effectiveTo: null,
        resultCode: 'SUCCESS',
        resultName: '成功',
        batchSummary: null,
        occurredAt: '2026-07-11T08:00:00Z',
        rawDetail: { passwordHash: 'must-not-cross-api-boundary' },
      },
    ],
    total: 1,
    pageSize: 10,
    pageNum: 1,
    pages: 1,
    size: 1,
    actionOptions: [{ code: 'USER_ROLE_UPDATED', label: '调整用户角色', raw: 'ignored' }],
    allowedActions: ['VIEW', 'DELETE_HISTORY'],
    unavailableReasons: { VIEW: '可查看', DELETE_HISTORY: 'ignored' },
    rawAuditDetail: { token: 'must-not-cross-api-boundary' },
    ...overrides,
  }
}

describe('user history api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({
      data: { code: 200, msg: 'OK', data: historyData() },
    })
  })

  it('queries the target-scoped structured history resource without raw audit flags', async () => {
    const signal = new AbortController().signal
    const result = await fetchUserHistory(
      21,
      {
        page: 2,
        size: 10,
        actionCode: 'USER_ROLE_UPDATED',
        startTime: '2026-07-01T00:00:00.000Z',
        endTime: '2026-07-11T00:00:00.000Z',
      },
      signal,
    )
    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/users/21/history',
      signal,
      params: {
        page: 2,
        size: 10,
        actionCode: 'USER_ROLE_UPDATED',
        startTime: '2026-07-01T00:00:00.000Z',
        endTime: '2026-07-11T00:00:00.000Z',
      },
    })
    expect(result.allowedActions).toEqual(['VIEW'])
    expect(result.unavailableReasons).toEqual({ VIEW: '可查看' })
    expect(result.actionOptions).toEqual([{ code: 'USER_ROLE_UPDATED', label: '调整用户角色' }])
    expect(result.list[0]).not.toHaveProperty('rawDetail')
    expect(result).not.toHaveProperty('rawAuditDetail')
  })

  it('fails closed when the server returns an unstructured history payload', async () => {
    mockedAxios.request.mockResolvedValueOnce({
      data: {
        code: 200,
        msg: 'OK',
        data: historyData({ list: [{ rawDetail: 'unstructured audit entity' }] }),
      },
    })

    await expect(fetchUserHistory(21, { page: 1, size: 10 })).rejects.toThrow(
      '用户历史响应格式无效',
    )
  })
})
