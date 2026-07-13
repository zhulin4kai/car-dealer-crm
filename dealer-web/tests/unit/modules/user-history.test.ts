import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import UserHistoryTimeline from '@/modules/user/components/UserHistoryTimeline.vue'
import { ApiError } from '@/shared/api/api-error'

const apiMocks = vi.hoisted(() => ({ fetchUserHistory: vi.fn() }))
vi.mock('@/modules/user/api/user-history-api', () => apiMocks)

function collection(overrides: Record<string, unknown> = {}) {
  return {
    list: [
      {
        eventId: 'authorization:history-1',
        sourceKey: 'authorization-history:1',
        actionCode: 'USER_ROLE_UPDATED',
        actionName: '调整用户角色',
        categoryCode: 'AUTHORIZATION',
        categoryName: '授权',
        target: {
          typeCode: 'ROLE',
          typeName: '角色',
          id: 8,
          code: 'sales_manager',
          name: '销售主管',
        },
        operator: { id: 8, name: '王经理', employeeNo: 'E00008' },
        beforeValues: [
          { code: 'role', label: '角色', displayValue: '销售人员' },
          { code: 'passwordHash', label: '密码哈希', displayValue: 'NEVER_SHOW_HASH' },
          { code: 'phone', label: '手机号', displayValue: '13800138000' },
        ],
        afterValues: [
          { code: 'role', label: '角色', displayValue: '销售主管' },
          { code: 'token', label: 'Token', displayValue: 'NEVER_SHOW_TOKEN' },
          { code: 'email', label: '邮箱', displayValue: 'full@example.com' },
        ],
        reason: '岗位调整',
        effectiveFrom: '2026-07-11T08:00:00Z',
        effectiveTo: null,
        resultCode: 'SUCCESS',
        resultName: '成功',
        batchSummary: {
          batchId: 'batch-safe-1',
          totalCount: 3,
          successCount: 2,
          failureCount: 1,
          targetResultCode: 'SUCCESS',
          targetResultName: '成功',
        },
        occurredAt: '2026-07-11T08:00:00Z',
      },
    ],
    total: 1,
    pageSize: 10,
    pageNum: 1,
    pages: 1,
    size: 1,
    actionOptions: [{ code: 'USER_ROLE_UPDATED', label: '调整用户角色' }],
    allowedActions: ['VIEW'],
    unavailableReasons: {},
    ...overrides,
  }
}

describe('UserHistoryTimeline', () => {
  beforeEach(() => apiMocks.fetchUserHistory.mockReset())

  it('shows structured stable facts and filters sensitive values without rendering raw audit JSON', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(collection())
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })

    expect(await screen.findByText(/角色：销售人员/)).toBeTruthy()
    expect(screen.getByText(/角色（ROLE） · 销售主管/)).toBeTruthy()
    expect(screen.getAllByText(/USER_ROLE_UPDATED/).length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('王经理 · E00008')).toBeTruthy()
    expect(screen.getByText(/批次 batch-safe-1/)).toBeTruthy()
    expect(screen.queryByText('NEVER_SHOW_HASH')).toBeNull()
    expect(screen.queryByText('NEVER_SHOW_TOKEN')).toBeNull()
    expect(screen.queryByText('13800138000')).toBeNull()
    expect(screen.queryByText('full@example.com')).toBeNull()
    expect(document.querySelector('pre')).toBeNull()
  })

  it('treats frontend masking as a second defense for every rendered history string', async () => {
    const unsafeItem = {
      ...collection().list[0],
      actionName: '来源 192.168.1.20',
      target: {
        ...collection().list[0].target,
        code: '10.0.0.8',
        name: 'owner@example.com',
      },
      operator: { id: 8, name: 'admin@example.com', employeeNo: '13800138000' },
      beforeValues: [
        { code: 'rawDetail', label: '原始明细', displayValue: 'RAW_AUDIT_DETAIL' },
        { code: 'credentialDigest', label: '凭证摘要', displayValue: 'RAW_DIGEST' },
        { code: 'office', label: '办公位置', displayValue: '192.168.1.30' },
      ],
      afterValues: [
        { code: 'department', label: '部门', displayValue: 'full-contact@example.com' },
        { code: 'sessionId', label: '会话编号', displayValue: 'RAW_SESSION_ID' },
        { code: 'recoveryKey', label: '恢复密钥', displayValue: 'RAW_RECOVERY_KEY' },
      ],
      reason: 'password=RAW_PASSWORD 0123456789abcdef0123456789abcdef',
      batchSummary: {
        ...collection().list[0].batchSummary,
        batchId: 'token_abcdefghijklmnop',
      },
      rawDetail: { token: 'RAW_TOKEN' },
    }
    apiMocks.fetchUserHistory.mockResolvedValue(collection({ list: [unsafeItem] }))
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })

    expect((await screen.findAllByText(/\[已脱敏网络\]/)).length).toBeGreaterThan(0)
    expect(document.body.textContent).toContain('[已脱敏邮箱]')
    expect(document.body.textContent).toContain('[已脱敏手机号]')
    expect(document.body.textContent).toContain('[已隐藏敏感内容]')
    for (const secret of [
      '192.168.1.20',
      '10.0.0.8',
      'owner@example.com',
      'admin@example.com',
      '13800138000',
      'RAW_AUDIT_DETAIL',
      'RAW_SESSION_ID',
      'RAW_DIGEST',
      'RAW_RECOVERY_KEY',
      '0123456789abcdef0123456789abcdef',
      'RAW_PASSWORD',
      'RAW_TOKEN',
      'token_abcdefghijklmnop',
    ])
      expect(document.body.textContent).not.toContain(secret)
  })

  it('sends action and time filters with server pagination', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(collection())
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    await screen.findByText(/角色：销售人员/)
    await fireEvent.update(screen.getByLabelText('历史动作'), 'USER_ROLE_UPDATED')
    await fireEvent.update(screen.getByLabelText('历史开始时间'), '2026-07-01T08:00')
    await fireEvent.update(screen.getByLabelText('历史结束时间'), '2026-07-11T18:00')
    await fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => expect(apiMocks.fetchUserHistory).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchUserHistory.mock.calls[1]?.[1]).toEqual({
      page: 1,
      size: 10,
      actionCode: 'USER_ROLE_UPDATED',
      startTime: new Date('2026-07-01T08:00').toISOString(),
      endTime: new Date('2026-07-11T18:00').toISOString(),
    })
    expect(apiMocks.fetchUserHistory.mock.calls[1]?.[2]).toBeInstanceOf(AbortSignal)
  })

  it('does not expose records when the response itself denies history view', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(
      collection({ allowedActions: [], unavailableReasons: { VIEW: '目标用户不在审计管理范围' } }),
    )
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    expect(await screen.findByText('目标用户不在审计管理范围')).toBeTruthy()
    expect(screen.queryByText('销售人员')).toBeNull()
    expect(screen.queryByLabelText('历史动作')).toBeNull()
    expect(screen.queryByRole('button', { name: '刷新' })).toBeNull()
  })

  it('does not request history when the parent audit and target gates are closed', async () => {
    render(UserHistoryTimeline, {
      props: { userId: 21, enabled: false, disabledReason: '缺少审计权限' },
    })
    await Promise.resolve()
    expect(apiMocks.fetchUserHistory).not.toHaveBeenCalled()
    expect(screen.getByText('缺少审计权限')).toBeTruthy()
    expect(screen.queryByLabelText('历史动作')).toBeNull()
  })

  it('shows a stable empty state and consumes action options only from the server', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(
      collection({
        list: [],
        total: 0,
        pages: 0,
        size: 0,
        actionOptions: [{ code: 'SESSION_REVOKED', label: '撤销会话' }],
      }),
    )
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })

    expect(await screen.findByText('暂无符合条件的历史记录')).toBeTruthy()
    const options = Array.from((screen.getByLabelText('历史动作') as HTMLSelectElement).options)
    expect(options.map((option) => option.value)).toEqual(['', 'SESSION_REVOKED'])
    expect(screen.queryByText(/USER_ROLE_UPDATED/)).toBeNull()
  })

  it('rejects a reversed date range without sending a request', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(collection())
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    await screen.findByText(/角色：销售人员/)
    await fireEvent.update(screen.getByLabelText('历史开始时间'), '2026-07-12T08:00')
    await fireEvent.update(screen.getByLabelText('历史结束时间'), '2026-07-11T08:00')
    await fireEvent.click(screen.getByRole('button', { name: '查询' }))

    expect(screen.getByText('开始时间不能晚于结束时间')).toBeTruthy()
    expect(apiMocks.fetchUserHistory).toHaveBeenCalledTimes(1)
  })

  it('uses server pagination and retries a local failure without affecting the parent page', async () => {
    apiMocks.fetchUserHistory
      .mockImplementationOnce(() => {
        throw new Error('offline')
      })
      .mockResolvedValue(collection({ total: 25, pages: 3 }))
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    expect(await screen.findByText('加载用户历史失败')).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: '重新加载' }))
    expect(await screen.findByText(/角色：销售人员/)).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: '2' }))
    await waitFor(() => expect(apiMocks.fetchUserHistory).toHaveBeenCalledTimes(3))
    expect(apiMocks.fetchUserHistory.mock.calls[2]?.[1]).toEqual({ page: 2, size: 10 })
  })

  it.each([
    [520, '无审计权限或目标用户超出可管理范围'],
    [404, '目标用户或历史记录不存在'],
    [409, '历史投影已变化，请刷新后重试'],
    [600, '历史投影已变化，请刷新后重试'],
  ])('maps history error code %s to a stable message', async (code, message) => {
    apiMocks.fetchUserHistory.mockImplementationOnce(() => {
      throw new ApiError(code as number, 'failed', null)
    })
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    expect(await screen.findByText(message as string)).toBeTruthy()
  })

  it.each([
    [403, '无审计权限或目标用户超出可管理范围'],
    [404, '目标用户或历史记录不存在'],
    [409, '历史投影已变化，请刷新后重试'],
  ])('maps HTTP status %s even when the business code is generic', async (httpStatus, message) => {
    apiMocks.fetchUserHistory.mockImplementationOnce(() => {
      throw new ApiError(500, 'failed', null, false, httpStatus as number)
    })
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    expect(await screen.findByText(message as string)).toBeTruthy()
  })

  it('ignores an older response after the target user changes', async () => {
    let resolveFirst!: (value: ReturnType<typeof collection>) => void
    let resolveSecond!: (value: ReturnType<typeof collection>) => void
    apiMocks.fetchUserHistory
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveFirst = resolve
          }),
      )
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveSecond = resolve
          }),
      )
    const view = render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    await view.rerender({ userId: 22, enabled: true })
    resolveSecond(
      collection({
        list: [
          {
            ...collection().list[0],
            eventId: 'new',
            target: { ...collection().list[0].target, name: '新用户角色' },
          },
        ],
      }),
    )
    expect(await screen.findByText(/新用户角色/)).toBeTruthy()
    resolveFirst(
      collection({
        list: [
          {
            ...collection().list[0],
            eventId: 'old',
            target: { ...collection().list[0].target, name: '旧用户角色' },
          },
        ],
      }),
    )
    await Promise.resolve()
    expect(screen.queryByText(/旧用户角色/)).toBeNull()
  })

  it('clears target-specific filters and action options before querying a different user', async () => {
    apiMocks.fetchUserHistory.mockResolvedValue(collection())
    const view = render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    await screen.findByText(/角色：销售人员/)
    await fireEvent.update(screen.getByLabelText('历史动作'), 'USER_ROLE_UPDATED')
    await fireEvent.update(screen.getByLabelText('历史开始时间'), '2026-07-01T08:00')

    await view.rerender({ userId: 22, enabled: true })
    await waitFor(() => expect(apiMocks.fetchUserHistory).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchUserHistory.mock.calls[1]?.[0]).toBe(22)
    expect(apiMocks.fetchUserHistory.mock.calls[1]?.[1]).toEqual({ page: 1, size: 10 })
  })

  it('clears stale action options when a later request fails', async () => {
    apiMocks.fetchUserHistory
      .mockResolvedValueOnce(collection())
      .mockRejectedValueOnce(new Error('offline'))
    render(UserHistoryTimeline, { props: { userId: 21, enabled: true } })
    await screen.findByText(/角色：销售人员/)
    expect(screen.getByRole('option', { name: /调整用户角色/ })).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: '查询' }))
    expect(await screen.findByText('加载用户历史失败')).toBeTruthy()
    expect(screen.queryByRole('option', { name: /调整用户角色/ })).toBeNull()
  })
})
