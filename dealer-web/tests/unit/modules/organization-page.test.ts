import { createPinia, setActivePinia } from 'pinia'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OrganizationPage from '@/pages/dashboard/organization.vue'
import { ApiError } from '@/shared/api/api-error'
import { usePermissionStore } from '@/stores/permission.store'

const apiMocks = vi.hoisted(() => ({
  createOrganizationUnit: vi.fn(),
  createPosition: vi.fn(),
  disableOrganizationUnit: vi.fn(),
  disablePosition: vi.fn(),
  enableOrganizationUnit: vi.fn(),
  enablePosition: vi.fn(),
  fetchActingManagerCandidates: vi.fn(),
  fetchActingReportings: vi.fn(),
  fetchEmployeeOrganizationHistory: vi.fn(),
  fetchEmployeeOrganizationMembership: vi.fn(),
  fetchManagerCandidates: vi.fn(),
  fetchOrganizationEmployees: vi.fn(),
  fetchOrganizationLeaderCandidates: vi.fn(),
  fetchOrganizationParentCandidates: vi.fn(),
  fetchOrganizationTree: vi.fn(),
  fetchPositions: vi.fn(),
  replaceActingReportings: vi.fn(),
  updateEmployeeOrganizationMembership: vi.fn(),
  updateOrganizationUnit: vi.fn(),
  updatePosition: vi.fn(),
}))

const feedbackMocks = vi.hoisted(() => ({
  messageTip: vi.fn(),
}))

vi.mock('@/modules/organization/api/organization-api', () => apiMocks)
vi.mock('@/shared/utils/feedback', () => feedbackMocks)

const company = {
  id: 1,
  code: 'COMPANY',
  name: '示例汽车集团',
  type: 'COMPANY' as const,
  parentId: null,
  leaderEmployeeId: null,
  leaderEmployeeName: '张总',
  orderNo: 1,
  enabled: true,
  version: 2,
  children: [
    {
      id: 2,
      code: 'STORE_SH',
      name: '上海门店',
      type: 'STORE' as const,
      parentId: 1,
      leaderEmployeeId: null,
      orderNo: 1,
      enabled: true,
      version: 1,
      children: [],
    },
  ],
}

const employee = {
  id: 21,
  userId: 9,
  employeeNo: 'E00021',
  name: '李销售',
  employmentStatus: 'ACTIVE',
  organizationUnitId: 1,
  organizationUnitName: '示例汽车集团',
  positionId: 8,
  positionName: '销售顾问',
  managerEmployeeId: 7,
  managerEmployeeName: '王主管',
  version: 3,
  allowedActions: ['assignment', 'reporting', 'history'],
  unavailableReasons: {},
}

async function renderPage(waitForEmployee = true) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const permissionStore = usePermissionStore()
  permissionStore.setPermissionsFromUser({
    permissionList: [
      'organization:list',
      'organization:view',
      'organization:add',
      'organization:edit',
      'organization:status',
      'position:list',
      'position:add',
      'position:edit',
      'position:status',
      'employee:assignment',
      'employee:reporting',
    ],
  })
  render(OrganizationPage, {
    global: {
      plugins: [pinia],
      directives: { hasPermission: {} },
    },
  })
  await waitFor(() => expect(apiMocks.fetchOrganizationTree).toHaveBeenCalledTimes(1))
  if (waitForEmployee) await screen.findByText('李销售')
}

describe('organization page', () => {
  beforeEach(() => {
    Object.values(apiMocks).forEach((mock) => mock.mockReset())
    feedbackMocks.messageTip.mockReset()
    apiMocks.fetchOrganizationTree.mockResolvedValue([company])
    apiMocks.fetchPositions.mockResolvedValue([
      {
        id: 8,
        code: 'SALES',
        name: '销售顾问',
        positionLevel: 10,
        builtIn: true,
        enabled: true,
        version: 1,
      },
    ])
    apiMocks.fetchOrganizationEmployees.mockResolvedValue([employee])
    apiMocks.fetchEmployeeOrganizationMembership.mockResolvedValue({
      employee,
      primaryAssignment: {
        organizationUnitId: 1,
        positionId: 8,
        assignmentType: 'PRIMARY',
        effectiveFrom: '2026-07-01T00:00:00Z',
      },
      additionalAssignments: [],
      reporting: null,
      version: 3,
      allowedActions: ['assignment', 'reporting', 'history'],
      unavailableReasons: {},
    })
    apiMocks.fetchManagerCandidates.mockResolvedValue([])
    apiMocks.fetchEmployeeOrganizationHistory.mockResolvedValue([])
    apiMocks.fetchActingReportings.mockResolvedValue({
      employeeId: 21,
      employeeVersion: 3,
      relations: [],
      allowedActions: ['UPDATE'],
      unavailableReasons: {},
    })
    apiMocks.fetchActingManagerCandidates.mockResolvedValue([
      {
        employeeId: 7,
        employeeNo: 'E00007',
        name: '王代理主管',
        organizationUnitName: '示例汽车集团',
        positionName: '销售主管',
      },
    ])
    apiMocks.replaceActingReportings.mockResolvedValue({
      employeeId: 21,
      employeeVersion: 4,
      relations: [],
      allowedActions: ['UPDATE'],
      unavailableReasons: {},
    })
    apiMocks.fetchOrganizationLeaderCandidates.mockResolvedValue([])
    apiMocks.fetchOrganizationParentCandidates.mockResolvedValue([])
  })

  it('renders the organization tree and scoped employees', async () => {
    await renderPage()

    expect((await screen.findAllByText('示例汽车集团')).length).toBeGreaterThanOrEqual(2)
    expect(await screen.findByText('李销售')).toBeTruthy()
    expect(screen.getByText('销售顾问')).toBeTruthy()
    expect(screen.getByText('在职')).toBeTruthy()
    expect(screen.queryByText('ACTIVE')).toBeNull()
    expect(apiMocks.fetchOrganizationEmployees).toHaveBeenCalledWith(1, expect.any(AbortSignal))
  })

  it('reloads employees when another organization node is selected', async () => {
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: /上海门店/ }))

    await waitFor(() => {
      expect(apiMocks.fetchOrganizationEmployees).toHaveBeenLastCalledWith(
        2,
        expect.any(AbortSignal),
      )
    })
  })

  it('opens the organization form for a child of the selected node', async () => {
    await renderPage()

    await fireEvent.click(screen.getByRole('button', { name: '新增下级' }))

    expect(await screen.findByText('新增组织节点')).toBeTruthy()
    expect(screen.getByText('在“示例汽车集团”下新增节点。')).toBeTruthy()
  })

  it('keeps the organization code read-only while editing', async () => {
    await renderPage()

    await fireEvent.click(screen.getByRole('button', { name: '编辑' }))

    const codeInput = await screen.findByLabelText('组织编码')
    expect((codeInput as HTMLInputElement).disabled).toBe(true)
  })

  it('loads membership and direct-manager candidates before editing an employee assignment', async () => {
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '调整任职' }))

    await waitFor(() => {
      expect(apiMocks.fetchEmployeeOrganizationMembership).toHaveBeenCalledWith(
        21,
        expect.any(AbortSignal),
      )
    })
    expect(apiMocks.fetchManagerCandidates).toHaveBeenCalledWith(21, expect.any(AbortSignal))
    expect(apiMocks.fetchEmployeeOrganizationHistory).not.toHaveBeenCalled()
    expect(await screen.findByText(/E00021 · 李销售/)).toBeTruthy()
  })

  it('loads manager candidates and history independently for their own actions', async () => {
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '调整汇报' }))
    await waitFor(() =>
      expect(apiMocks.fetchManagerCandidates).toHaveBeenCalledWith(21, expect.any(AbortSignal)),
    )
    expect(apiMocks.fetchEmployeeOrganizationHistory).not.toHaveBeenCalled()

    await fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await fireEvent.click(await screen.findByRole('button', { name: '查看历史' }))
    await waitFor(() =>
      expect(apiMocks.fetchEmployeeOrganizationHistory).toHaveBeenCalledWith(
        21,
        expect.any(AbortSignal),
      ),
    )
  })

  it('loads the independent ACTING collection and candidates for the proxy-manager action', async () => {
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '代理主管' }))

    await waitFor(() => {
      expect(apiMocks.fetchActingReportings).toHaveBeenCalledWith(21, expect.any(AbortSignal))
      expect(apiMocks.fetchActingManagerCandidates).toHaveBeenCalledWith(
        21,
        expect.any(AbortSignal),
      )
    })
    expect(await screen.findByText(/E00021 · 李销售/)).toBeTruthy()
    expect(screen.getByText('当前没有代理主管')).toBeTruthy()
    expect(screen.getByText('王主管')).toBeTruthy()
  })

  it('submits the ACTING collection with employee version and refreshes employees', async () => {
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '代理主管' }))
    await fireEvent.click(await screen.findByRole('button', { name: '添加代理主管' }))
    await fireEvent.update(screen.getByLabelText('代理主管 1'), '7')
    await fireEvent.update(screen.getByLabelText('代理结束时间 1'), '2026-08-20T18:15:37')
    await fireEvent.update(screen.getByLabelText('调整原因'), '主管休假期间代理')
    await fireEvent.click(screen.getByRole('button', { name: '保存代理关系' }))

    await waitFor(() => {
      expect(apiMocks.replaceActingReportings).toHaveBeenCalledWith(21, {
        expectedEmployeeVersion: 3,
        relations: [
          {
            managerEmployeeId: '7',
            effectiveTo: new Date('2026-08-20T18:15:37').toISOString(),
          },
        ],
        reason: '主管休假期间代理',
      })
    })
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith('代理管理关系已更新', 'success')
    expect(apiMocks.fetchOrganizationEmployees).toHaveBeenCalledTimes(2)
  })

  it('reloads ACTING server facts after an employee-version CAS conflict', async () => {
    apiMocks.fetchActingReportings
      .mockResolvedValueOnce({
        employeeId: 21,
        employeeVersion: 3,
        relations: [],
        allowedActions: ['UPDATE'],
        unavailableReasons: {},
      })
      .mockResolvedValueOnce({
        employeeId: 21,
        employeeVersion: 4,
        relations: [],
        allowedActions: ['UPDATE'],
        unavailableReasons: {},
      })
    apiMocks.replaceActingReportings.mockRejectedValueOnce(
      new ApiError(598, '员工版本冲突', null, false, 409),
    )
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '代理主管' }))
    await fireEvent.click(await screen.findByRole('button', { name: '添加代理主管' }))
    await fireEvent.update(screen.getByLabelText('代理主管 1'), '7')
    await fireEvent.update(screen.getByLabelText('代理结束时间 1'), '2026-08-20T18:15:37')
    await fireEvent.update(screen.getByLabelText('调整原因'), '主管休假期间代理')
    await fireEvent.click(screen.getByRole('button', { name: '保存代理关系' }))

    await waitFor(() => expect(apiMocks.fetchActingReportings).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchActingManagerCandidates).toHaveBeenCalledTimes(2)
    expect(await screen.findByText('当前员工版本：4')).toBeTruthy()
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      '代理管理关系或员工版本已变化，页面将刷新最新信息',
      'error',
    )
  })

  it('aborts in-flight ACTING queries when the dialog closes', async () => {
    apiMocks.fetchActingReportings.mockImplementationOnce(() => new Promise(() => undefined))
    apiMocks.fetchActingManagerCandidates.mockImplementationOnce(
      () => new Promise(() => undefined),
    )
    await renderPage()

    await fireEvent.click(await screen.findByRole('button', { name: '代理主管' }))
    expect(await screen.findByText('加载代理管理关系...')).toBeTruthy()
    const collectionSignal = apiMocks.fetchActingReportings.mock.calls[0]?.[1] as AbortSignal
    const candidateSignal = apiMocks.fetchActingManagerCandidates.mock.calls[0]?.[1] as AbortSignal

    await fireEvent.click(screen.getByRole('button', { name: '取消' }))

    expect(collectionSignal.aborted).toBe(true)
    expect(candidateSignal.aborted).toBe(true)
  })

  it('clears employees from the previous organization when the next request fails', async () => {
    await renderPage()
    apiMocks.fetchOrganizationEmployees.mockRejectedValueOnce(new Error('network'))

    await fireEvent.click(await screen.findByRole('button', { name: /上海门店/ }))

    await waitFor(() => expect(screen.queryByText('李销售')).toBeNull())
    expect(await screen.findByText('当前组织暂无员工')).toBeTruthy()
  })

  it('keeps the organization tree available when position loading fails', async () => {
    apiMocks.fetchPositions.mockRejectedValueOnce(new Error('position unavailable'))

    await renderPage()

    expect((await screen.findAllByText('示例汽车集团')).length).toBeGreaterThanOrEqual(2)
    expect(await screen.findByText('李销售')).toBeTruthy()
  })

  it('shows a clear message when organization access is denied', async () => {
    apiMocks.fetchOrganizationTree.mockRejectedValueOnce(new ApiError(520, '没有访问权限', null))

    await renderPage(false)

    await waitFor(() => {
      expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
        '没有权限执行此操作，或目标超出可管理范围',
        'error',
      )
    })
  })
})
