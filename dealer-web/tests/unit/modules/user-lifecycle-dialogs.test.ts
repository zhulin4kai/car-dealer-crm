import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserDepartureDialog from '@/modules/user/components/UserDepartureDialog.vue'
import UserRehireDialog from '@/modules/user/components/UserRehireDialog.vue'
import UserTransferDialog from '@/modules/user/components/UserTransferDialog.vue'
import { ApiError } from '@/shared/api/api-error'

const DIRECT_DOMAINS = [
  ['ACTIVITY', '市场活动'],
  ['CLUE', '线索'],
  ['CUSTOMER', '客户'],
  ['OPPORTUNITY', '商机'],
  ['FOLLOW_TASK', '跟进任务'],
  ['TEST_DRIVE', '试驾'],
] as const

const apiMocks = vi.hoisted(() => ({
  fetchUserLifecycleContext: vi.fn(),
  transferEmployee: vi.fn(),
  precheckDeparture: vi.fn(),
  startDeparture: vi.fn(),
  confirmDepartureHandover: vi.fn(),
  completeDeparture: vi.fn(),
  rehireEmployee: vi.fn(),
}))
vi.mock('@/modules/user/api/user-lifecycle-api', () => apiMocks)

function context(overrides: Record<string, unknown> = {}) {
  return {
    userId: 21,
    employeeId: 121,
    employmentStatus: 'ACTIVE',
    employeeVersion: 1,
    currentAssignment: {
      organizationCode: 'STORE_SH',
      organizationName: '上海门店',
      positionCode: 'SALES',
      positionName: '销售顾问',
      managerEmployeeNo: 'E00008',
      managerName: '王经理',
      effectiveFrom: '2026-01-01T00:00:00Z',
    },
    activeRoleCount: 1,
    activePersonalPermissionCount: 2,
    activeSessionCount: 3,
    additionalAssignmentCount: 0,
    reportingRelationCount: 1,
    organizationCandidates: [{ id: 2, label: '杭州门店' }],
    positionCandidates: [{ id: 3, label: '销售主管' }],
    managerCandidates: [{ id: 4, label: '赵经理' }],
    managerRequired: true,
    managerOptionalReason: null,
    handoverCandidates: [],
    allowedActions: ['TRANSFER'],
    unavailableReasons: {},
    statusTransitions: [
      { action: 'TRANSFER', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '调岗' },
    ],
    ...overrides,
  }
}

function precheck(overrides: Record<string, unknown> = {}) {
  return {
    snapshotToken: 'opaque-snapshot',
    generatedAt: '2026-07-12T01:00:00Z',
    expiresAt: '2099-07-12T01:10:00Z',
    userId: 21,
    employmentStatus: 'ACTIVE',
    employeeVersion: 1,
    responsibilities: [],
    activeRoleCount: 1,
    activePersonalPermissionCount: 2,
    activeSessionCount: 3,
    activeAssignmentCount: 1,
    activeReportingCount: 1,
    handoverRequired: true,
    handoverCompleted: false,
    readyToComplete: false,
    blockingReasons: [],
    allowedActions: ['DEPARTURE_START'],
    unavailableReasons: {},
    statusTransitions: [
      {
        action: 'DEPARTURE_START',
        fromStatus: 'ACTIVE',
        toStatus: 'HANDOVER',
        label: '进入待交接',
      },
    ],
    ...overrides,
  }
}

function candidate(id: number, label: string, overrides: Record<string, unknown> = {}) {
  return {
    id,
    label,
    eligible: true,
    qualificationCode: 'QUALIFIED',
    qualificationName: '资格通过',
    unavailableReason: null,
    ...overrides,
  }
}

function directResponsibility(
  resourceType: (typeof DIRECT_DOMAINS)[number][0],
  resourceName: string,
  count: number,
  overrides: Record<string, unknown> = {},
) {
  return {
    resourceType,
    resourceName,
    transferMode: 'DIRECT_OWNER',
    count,
    transferableCount: count,
    blockedCount: 0,
    statusCode: 'READY',
    statusName: '可交接',
    blocking: false,
    blockingReasons: [],
    targetCandidates: count ? [candidate(8, '接收人甲')] : [],
    conflicts: [],
    ...overrides,
  }
}

function derivedImpact(resourceType: 'QUOTE' | 'TRAN', resourceName: string, count: number) {
  return {
    resourceType,
    resourceName,
    transferMode: 'DERIVED_IMPACT',
    count,
    transferableCount: 0,
    blockedCount: 0,
    statusCode: 'IMPACT_ONLY',
    statusName: '仅派生影响',
    blocking: false,
    blockingReasons: [],
    targetCandidates: [],
    conflicts: [],
  }
}

function exactDomainResults(counts: Partial<Record<string, number>> = {}) {
  return DIRECT_DOMAINS.map(([domainCode, domainName]) => ({
    domainCode,
    domainName,
    expectedCount: counts[domainCode] ?? 0,
    transferredCount: counts[domainCode] ?? 0,
    resultCode: 'SUCCESS',
    resultName: '成功',
  }))
}

describe('user lifecycle dialogs', () => {
  beforeEach(() => Object.values(apiMocks).forEach((mock) => mock.mockReset()))

  it('submits a transfer only from server candidates and an allowed transition', async () => {
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(context())
    apiMocks.transferEmployee.mockResolvedValue(
      context({
        employeeVersion: 2,
        currentAssignment: {
          organizationName: '杭州门店',
          positionName: '销售主管',
          managerName: '赵经理',
        },
      }),
    )
    const view = render(UserTransferDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/上海门店/)
    await fireEvent.update(screen.getByLabelText('目标组织'), '2')
    await fireEvent.update(screen.getByLabelText('目标岗位'), '3')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('生效时间'), '2026-07-12T09:00')
    await fireEvent.update(screen.getByLabelText('调岗原因'), '跨部门调岗')
    await fireEvent.click(screen.getByRole('button', { name: '确认调岗' }))
    await waitFor(() =>
      expect(apiMocks.transferEmployee).toHaveBeenCalledWith(21, {
        employeeVersion: 1,
        organizationUnitId: '2',
        positionId: '3',
        managerEmployeeId: '4',
        effectiveFrom: new Date('2026-07-12T09:00').toISOString(),
        reason: '跨部门调岗',
      }),
    )
    expect(view.emitted().completed).toHaveLength(1)
  })

  it.each([409, 600])(
    'refreshes lifecycle facts on conflict code %s and does not auto-resubmit',
    async (code) => {
      apiMocks.fetchUserLifecycleContext
        .mockResolvedValueOnce(context())
        .mockResolvedValueOnce(context())
        .mockResolvedValueOnce(context({ employeeVersion: 2 }))
      apiMocks.transferEmployee.mockImplementationOnce(() => {
        throw new ApiError(code, 'conflict', null)
      })
      render(UserTransferDialog, { props: { open: true, userId: 21 } })
      await screen.findByText(/上海门店/)
      await fireEvent.update(screen.getByLabelText('目标组织'), '2')
      await fireEvent.update(screen.getByLabelText('目标岗位'), '3')
      await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
      await fireEvent.update(screen.getByLabelText('生效时间'), '2026-07-12T09:00')
      await fireEvent.update(screen.getByLabelText('调岗原因'), '跨部门调岗')
      await fireEvent.click(screen.getByRole('button', { name: '确认调岗' }))
      await waitFor(() => expect(apiMocks.fetchUserLifecycleContext).toHaveBeenCalledTimes(3))
      expect(apiMocks.fetchUserLifecycleContext).toHaveBeenNthCalledWith(
        2,
        21,
        expect.any(AbortSignal),
        '2',
      )
      expect(apiMocks.transferEmployee).toHaveBeenCalledTimes(1)
    },
  )

  it('requires both the server action and matching transition and blocks duplicate submit', async () => {
    apiMocks.fetchUserLifecycleContext.mockResolvedValueOnce(
      context({
        allowedActions: ['TRANSFER', 'UNKNOWN_FORCE_ACTION'],
        statusTransitions: [
          {
            action: 'UNKNOWN_FORCE_ACTION',
            fromStatus: 'ACTIVE',
            toStatus: 'LEFT',
            label: '未知动作',
          },
        ],
      }),
    )
    const first = render(UserTransferDialog, { props: { open: true, userId: 21 } })
    expect(
      ((await screen.findByRole('button', { name: '确认调岗' })) as HTMLButtonElement).disabled,
    ).toBe(true)
    first.unmount()

    let resolveTransfer!: (value: ReturnType<typeof context>) => void
    apiMocks.fetchUserLifecycleContext
      .mockResolvedValueOnce(context())
      .mockResolvedValueOnce(context())
    apiMocks.transferEmployee.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveTransfer = resolve
        }),
    )
    render(UserTransferDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/上海门店/)
    await fireEvent.update(screen.getByLabelText('目标组织'), '2')
    await fireEvent.update(screen.getByLabelText('目标岗位'), '3')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('生效时间'), '2026-07-12T09:00')
    await fireEvent.update(screen.getByLabelText('调岗原因'), '跨部门调岗')
    const button = screen.getByRole('button', { name: '确认调岗' })
    await fireEvent.click(button)
    await fireEvent.click(button)
    expect(apiMocks.transferEmployee).toHaveBeenCalledTimes(1)
    resolveTransfer(context({ employeeVersion: 2 }))
    await waitFor(() => expect(screen.queryByRole('button', { name: '确认调岗' })).toBeNull())
  })

  it.each([
    [403, '无权执行该人员流程，可能是本人、同级、上级、范围外或受保护账号'],
    [404, '目标员工或生命周期事实不存在'],
  ])('maps lifecycle HTTP status %s on context load', async (httpStatus, message) => {
    apiMocks.fetchUserLifecycleContext.mockRejectedValueOnce(
      new ApiError(500, 'failed', null, false, httpStatus as number),
    )
    render(UserTransferDialog, { props: { open: true, userId: 21 } })
    expect(await screen.findByText(message as string)).toBeTruthy()
  })

  it('ignores stale context and mutation results after the target user changes', async () => {
    let resolveFirstContext!: (value: ReturnType<typeof context>) => void
    let resolveSecondContext!: (value: ReturnType<typeof context>) => void
    apiMocks.fetchUserLifecycleContext
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveFirstContext = resolve
          }),
      )
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveSecondContext = resolve
          }),
      )
    const view = render(UserTransferDialog, { props: { open: true, userId: 21 } })
    await view.rerender({ open: true, userId: 22 })
    resolveSecondContext(
      context({ userId: 22, currentAssignment: { organizationName: '新目标门店' } }),
    )
    expect(await screen.findByText(/新目标门店/)).toBeTruthy()
    resolveFirstContext(
      context({ userId: 21, currentAssignment: { organizationName: '旧目标门店' } }),
    )
    await Promise.resolve()
    expect(screen.queryByText(/旧目标门店/)).toBeNull()
  })

  it('enforces precheck, pending handover, per-domain confirmation and final completion order', async () => {
    const active = context({
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        { action: 'DEPARTURE_PRECHECK', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '预检' },
      ],
    })
    const handoverContext = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const responsibility = directResponsibility('CLUE', '线索', 2)
    const impacts = [derivedImpact('QUOTE', '报价', 2), derivedImpact('TRAN', '交易', 1)]
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(active)
    apiMocks.precheckDeparture
      .mockResolvedValueOnce(precheck())
      .mockResolvedValueOnce(
        precheck({
          employmentStatus: 'HANDOVER',
          employeeVersion: 2,
          responsibilities: [responsibility, ...impacts],
          allowedActions: ['HANDOVER_CONFIRM'],
          statusTransitions: [
            {
              action: 'HANDOVER_CONFIRM',
              fromStatus: 'HANDOVER',
              toStatus: 'HANDOVER',
              label: '确认交接',
            },
          ],
        }),
      )
      .mockResolvedValueOnce(
        precheck({
          employmentStatus: 'HANDOVER',
          employeeVersion: 3,
          responsibilities: [responsibility],
          handoverCompleted: true,
          readyToComplete: true,
          allowedActions: ['DEPARTURE_COMPLETE'],
          statusTransitions: [
            {
              action: 'DEPARTURE_COMPLETE',
              fromStatus: 'HANDOVER',
              toStatus: 'LEFT',
              label: '完成离职',
            },
          ],
        }),
      )
    apiMocks.startDeparture.mockResolvedValue(handoverContext)
    apiMocks.confirmDepartureHandover.mockResolvedValue({
      operationId: 'op-1',
      success: true,
      resultCode: 'SUCCESS',
      resultName: '成功',
      employeeVersion: 3,
      domainResults: exactDomainResults({ CLUE: 2 }),
    })
    apiMocks.completeDeparture.mockResolvedValue(
      context({
        employmentStatus: 'LEFT',
        employeeVersion: 4,
        allowedActions: ['REHIRE'],
        statusTransitions: [],
      }),
    )
    const view = render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：ACTIVE/)
    expect((screen.getByRole('button', { name: '进入待交接' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect((screen.getByRole('button', { name: '完成离职' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    expect(screen.queryByText('opaque-snapshot')).toBeNull()
    await fireEvent.click(await screen.findByRole('button', { name: '进入待交接' }))
    await fireEvent.click(await screen.findByRole('button', { name: '执行离职预检' }))
    expect(screen.getByText(/报价和交易不具有独立可转移负责人/)).toBeTruthy()
    expect(screen.queryByLabelText('报价接收人')).toBeNull()
    expect(screen.queryByLabelText('交易接收人')).toBeNull()
    await fireEvent.update(await screen.findByLabelText('线索接收人'), '8')
    await fireEvent.click(screen.getByRole('button', { name: '确认责任交接' }))
    await screen.findByText(/责任交接已完成/)
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.click(await screen.findByRole('button', { name: '完成离职' }))
    await waitFor(() =>
      expect(apiMocks.completeDeparture).toHaveBeenCalledWith(21, {
        employeeVersion: 3,
        snapshotToken: 'opaque-snapshot',
        reason: '员工申请离职',
      }),
    )
    expect(apiMocks.confirmDepartureHandover).toHaveBeenCalledWith(21, {
      employeeVersion: 2,
      snapshotToken: 'opaque-snapshot',
      transfers: [{ resourceType: 'CLUE', targetEmployeeId: '8' }],
      reason: '员工申请离职',
    })
    expect(view.emitted().completed).toHaveLength(1)
  })

  it('does not show success or enable completion for a partial handover result', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const responsibility = directResponsibility('CUSTOMER', '客户', 2, {
      targetCandidates: [candidate(9, '接收人乙')],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 2,
        responsibilities: [responsibility],
        allowedActions: ['HANDOVER_CONFIRM'],
        statusTransitions: [
          {
            action: 'HANDOVER_CONFIRM',
            fromStatus: 'HANDOVER',
            toStatus: 'HANDOVER',
            label: '交接',
          },
        ],
      }),
    )
    apiMocks.confirmDepartureHandover.mockResolvedValue({
      operationId: 'op-failed',
      success: false,
      resultCode: 'ROLLED_BACK',
      resultName: '已回滚',
      employeeVersion: 2,
      domainResults: exactDomainResults({ CUSTOMER: 2 }).map((item) =>
        item.domainCode === 'CUSTOMER'
          ? { ...item, transferredCount: 1, resultCode: 'FAILURE', resultName: '失败' }
          : item,
      ),
    })
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.update(await screen.findByLabelText('客户接收人'), '9')
    await fireEvent.click(screen.getByRole('button', { name: '确认责任交接' }))
    expect(
      await screen.findByText('六域交接结果不完整、存在失败或数量不一致，未进入完成离职步骤'),
    ).toBeTruthy()
    expect(screen.queryByText('责任交接已完成，请重新预检确认全部事实后完成离职')).toBeNull()
    expect((screen.getByRole('button', { name: '完成离职' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect(apiMocks.completeDeparture).not.toHaveBeenCalled()
    expect(document.body.textContent).not.toContain('opaque-snapshot')
  })

  it('requires an exact six-domain success result even when the server success flag is true', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const responsibility = directResponsibility('CLUE', '线索', 1)
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 2,
        responsibilities: [responsibility],
        allowedActions: ['HANDOVER_CONFIRM'],
        statusTransitions: [
          {
            action: 'HANDOVER_CONFIRM',
            fromStatus: 'HANDOVER',
            toStatus: 'HANDOVER',
            label: '交接',
          },
        ],
      }),
    )
    apiMocks.confirmDepartureHandover.mockResolvedValue({
      operationId: 'op-incomplete',
      success: true,
      resultCode: 'SUCCESS',
      resultName: '成功',
      employeeVersion: 3,
      domainResults: [
        {
          domainCode: 'CLUE',
          domainName: '线索',
          expectedCount: 1,
          transferredCount: 1,
          resultCode: 'SUCCESS',
          resultName: '成功',
        },
      ],
    })
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.update(await screen.findByLabelText('线索接收人'), '8')
    await fireEvent.click(screen.getByRole('button', { name: '确认责任交接' }))

    expect(
      await screen.findByText('六域交接结果不完整、存在失败或数量不一致，未进入完成离职步骤'),
    ).toBeTruthy()
    expect(apiMocks.completeDeparture).not.toHaveBeenCalled()
  })

  it('clears the snapshot and requires a new precheck after a handover 409 conflict', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const responsibility = directResponsibility('CLUE', '线索', 1)
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 2,
        responsibilities: [responsibility],
        allowedActions: ['HANDOVER_CONFIRM'],
        statusTransitions: [
          {
            action: 'HANDOVER_CONFIRM',
            fromStatus: 'HANDOVER',
            toStatus: 'HANDOVER',
            label: '交接',
          },
        ],
      }),
    )
    apiMocks.confirmDepartureHandover.mockRejectedValue(new ApiError(409, 'conflict', null))
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.update(await screen.findByLabelText('线索接收人'), '8')
    await fireEvent.click(screen.getByRole('button', { name: '确认责任交接' }))

    await waitFor(() => expect(apiMocks.fetchUserLifecycleContext).toHaveBeenCalledTimes(2))
    expect(apiMocks.confirmDepartureHandover).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: '执行离职预检' })).toBeTruthy()
    expect(document.body.textContent).not.toContain('opaque-snapshot')
  })

  it('clears the final snapshot and requires a new precheck after completion 409', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 3,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 3,
        handoverCompleted: true,
        readyToComplete: true,
        allowedActions: ['DEPARTURE_COMPLETE'],
        statusTransitions: [
          {
            action: 'DEPARTURE_COMPLETE',
            fromStatus: 'HANDOVER',
            toStatus: 'LEFT',
            label: '完成离职',
          },
        ],
      }),
    )
    apiMocks.completeDeparture.mockRejectedValue(new ApiError(409, 'conflict', null))
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.click(await screen.findByRole('button', { name: '完成离职' }))

    await waitFor(() => expect(apiMocks.fetchUserLifecycleContext).toHaveBeenCalledTimes(2))
    expect(apiMocks.completeDeparture).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: '执行离职预检' })).toBeTruthy()
    expect(document.body.textContent).not.toContain('opaque-snapshot')
  })

  it('shows test-drive qualification conflicts and refuses ineligible recipients', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const testDrive = directResponsibility('TEST_DRIVE', '试驾', 1, {
      transferableCount: 0,
      blockedCount: 1,
      blocking: true,
      blockingReasons: ['接收人排班冲突'],
      targetCandidates: [
        candidate(10, '冲突接收人', {
          eligible: false,
          qualificationCode: 'SCHEDULE_CONFLICT',
          qualificationName: '排班冲突',
          unavailableReason: '同一时段已有试驾',
        }),
      ],
      conflicts: [
        {
          conflictCode: 'TEST_DRIVE_SCHEDULE_CONFLICT',
          conflictName: '试驾时段冲突',
          count: 1,
          reason: '2026-07-15 10:00 已有试驾安排',
        },
      ],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 2,
        responsibilities: [testDrive],
        allowedActions: ['HANDOVER_CONFIRM'],
        statusTransitions: [
          {
            action: 'HANDOVER_CONFIRM',
            fromStatus: 'HANDOVER',
            toStatus: 'HANDOVER',
            label: '交接',
          },
        ],
      }),
    )
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))

    expect(await screen.findByText(/2026-07-15 10:00 已有试驾安排/)).toBeTruthy()
    expect((screen.getByRole('option', { name: /冲突接收人/ }) as HTMLOptionElement).disabled).toBe(
      true,
    )
    expect(
      (screen.getByRole('button', { name: '确认责任交接' }) as HTMLButtonElement).disabled,
    ).toBe(true)
    expect(apiMocks.confirmDepartureHandover).not.toHaveBeenCalled()
  })

  it('blocks unsupported pending-approval and communication-record responsibility types', async () => {
    const handover = context({
      employmentStatus: 'HANDOVER',
      employeeVersion: 2,
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        {
          action: 'DEPARTURE_PRECHECK',
          fromStatus: 'HANDOVER',
          toStatus: 'HANDOVER',
          label: '预检',
        },
      ],
    })
    const unsupported = {
      ...directResponsibility('CLUE', '待审批事项', 1),
      resourceType: 'PENDING_APPROVAL',
      resourceName: '待审批事项',
    }
    const communication = {
      ...directResponsibility('CLUE', '沟通记录', 1),
      resourceType: 'COMMUNICATION_RECORD',
      resourceName: '沟通记录',
    }
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(handover)
    apiMocks.precheckDeparture.mockResolvedValue(
      precheck({
        employmentStatus: 'HANDOVER',
        employeeVersion: 2,
        responsibilities: [unsupported, communication],
        allowedActions: ['HANDOVER_CONFIRM'],
        statusTransitions: [
          {
            action: 'HANDOVER_CONFIRM',
            fromStatus: 'HANDOVER',
            toStatus: 'HANDOVER',
            label: '交接',
          },
        ],
      }),
    )
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：HANDOVER/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))

    expect(await screen.findByText(/PENDING_APPROVAL、COMMUNICATION_RECORD/)).toBeTruthy()
    expect(screen.queryByLabelText('待审批事项接收人')).toBeNull()
    expect(screen.queryByLabelText('沟通记录接收人')).toBeNull()
    expect(
      (screen.getByRole('button', { name: '确认责任交接' }) as HTMLButtonElement).disabled,
    ).toBe(true)
  })

  it('keeps the snapshot memory-only and blocks an expired precheck token', async () => {
    vi.mocked(localStorage.setItem).mockClear()
    vi.mocked(sessionStorage.setItem).mockClear()
    const active = context({
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        { action: 'DEPARTURE_PRECHECK', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '预检' },
      ],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(active)
    apiMocks.precheckDeparture.mockResolvedValue(precheck({ expiresAt: '2020-01-01T00:00:00Z' }))
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：ACTIVE/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))

    expect((await screen.findByText(/预检有效期至/)).classList.contains('text-destructive')).toBe(
      true,
    )
    expect((screen.getByRole('button', { name: '进入待交接' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect(document.body.textContent).not.toContain('opaque-snapshot')
    expect(localStorage.setItem).not.toHaveBeenCalled()
    expect(sessionStorage.setItem).not.toHaveBeenCalled()
    expect(apiMocks.startDeparture).not.toHaveBeenCalled()
  })

  it('invalidates the in-memory snapshot when the reason changes', async () => {
    const active = context({
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        { action: 'DEPARTURE_PRECHECK', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '预检' },
      ],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(active)
    apiMocks.precheckDeparture.mockResolvedValue(precheck())
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：ACTIVE/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '原因一')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '原因二')

    expect(screen.getByText('离职原因已变化，请重新执行预检。')).toBeTruthy()
    expect((screen.getByRole('button', { name: '进入待交接' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
  })

  it('blocks duplicate precheck submission while the first request is pending', async () => {
    const active = context({
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        { action: 'DEPARTURE_PRECHECK', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '预检' },
      ],
    })
    let resolvePrecheck!: (value: ReturnType<typeof precheck>) => void
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(active)
    apiMocks.precheckDeparture.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolvePrecheck = resolve
        }),
    )
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：ACTIVE/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    const button = screen.getByRole('button', { name: '执行离职预检' })
    await fireEvent.click(button)
    await fireEvent.click(button)
    expect(apiMocks.precheckDeparture).toHaveBeenCalledTimes(1)
    resolvePrecheck(precheck())
    expect(await screen.findByText(/预检有效期至/)).toBeTruthy()
  })

  it('clears a server-rejected snapshot token and requires a new precheck', async () => {
    const active = context({
      allowedActions: ['DEPARTURE_PRECHECK'],
      statusTransitions: [
        { action: 'DEPARTURE_PRECHECK', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '预检' },
      ],
    })
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(active)
    apiMocks.precheckDeparture.mockResolvedValue(precheck())
    apiMocks.startDeparture.mockRejectedValue(new ApiError(500, 'expired', null, false, 410))
    render(UserDepartureDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/任职状态：ACTIVE/)
    await fireEvent.update(screen.getByLabelText('离职及交接原因'), '员工申请离职')
    await fireEvent.click(screen.getByRole('button', { name: '执行离职预检' }))
    await fireEvent.click(await screen.findByRole('button', { name: '进入待交接' }))

    expect(await screen.findByText('离职预检已过期，请重新执行预检')).toBeTruthy()
    expect(screen.getByRole('button', { name: '执行离职预检' })).toBeTruthy()
    expect(document.body.textContent).not.toContain('opaque-snapshot')
  })

  it('creates a new rehire assignment without restoring old authorization', async () => {
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(
      context({
        employmentStatus: 'LEFT',
        employeeVersion: 5,
        allowedActions: ['REHIRE'],
        statusTransitions: [
          { action: 'REHIRE', fromStatus: 'LEFT', toStatus: 'PENDING', label: '返聘' },
        ],
      }),
    )
    apiMocks.rehireEmployee.mockResolvedValue({
      context: context({ employmentStatus: 'PENDING', employeeVersion: 6 }),
      restoredLegacyAuthorizationCount: 0,
      credentialDeliveryStatus: 'INVITATION_SENT',
    })
    const view = render(UserRehireDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/当前任职状态：LEFT/)
    await fireEvent.update(screen.getByLabelText('新组织'), '2')
    await fireEvent.update(screen.getByLabelText('新岗位'), '3')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('生效时间'), '2026-08-01T09:00')
    await fireEvent.update(screen.getByLabelText('返聘原因'), '重新入职')
    await fireEvent.click(screen.getByRole('button', { name: '确认返聘' }))
    await waitFor(() => expect(apiMocks.rehireEmployee).toHaveBeenCalled())
    const request = apiMocks.rehireEmployee.mock.calls[0]?.[1]
    expect(request).not.toHaveProperty('roleIds')
    expect(request).not.toHaveProperty('permissionIds')
    expect(request).not.toHaveProperty('password')
    expect(view.emitted().completed).toHaveLength(1)
  })

  it('fails closed when a rehire response claims that legacy authorization was restored', async () => {
    apiMocks.fetchUserLifecycleContext.mockResolvedValue(
      context({
        employmentStatus: 'LEFT',
        employeeVersion: 5,
        allowedActions: ['REHIRE'],
        statusTransitions: [
          { action: 'REHIRE', fromStatus: 'LEFT', toStatus: 'PENDING', label: '返聘' },
        ],
      }),
    )
    apiMocks.rehireEmployee.mockResolvedValue({
      context: context({ employmentStatus: 'PENDING', employeeVersion: 6 }),
      restoredLegacyAuthorizationCount: 1,
      credentialDeliveryStatus: 'INVITATION_SENT',
    })
    const view = render(UserRehireDialog, { props: { open: true, userId: 21 } })
    await screen.findByText(/当前任职状态：LEFT/)
    await fireEvent.update(screen.getByLabelText('新组织'), '2')
    await fireEvent.update(screen.getByLabelText('新岗位'), '3')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('生效时间'), '2026-08-01T09:00')
    await fireEvent.update(screen.getByLabelText('返聘原因'), '重新入职')
    await fireEvent.click(screen.getByRole('button', { name: '确认返聘' }))

    expect(
      await screen.findByText('服务端返回了不应自动恢复的旧授权，返聘结果需人工复核'),
    ).toBeTruthy()
    expect(view.emitted().completed).toBeUndefined()
  })
})
