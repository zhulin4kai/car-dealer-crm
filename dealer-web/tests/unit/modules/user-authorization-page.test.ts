import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import UserAuthorizationPage from '@/pages/dashboard/user/[id].vue'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { usePermissionStore } from '@/stores/permission.store'
import { useAuthStore } from '@/stores/auth.store'
import { PERMISSIONS } from '@/shared/constants/permissions'

const apiMocks = vi.hoisted(() => ({
  fetchUserAuthorizationDetail: vi.fn(),
  updateUserPermissions: vi.fn(),
  updateUserRoleAssignments: vi.fn(),
}))
const sessionMocks = vi.hoisted(() => ({
  fetchManagedUserSessions: vi.fn(),
  revokeAllManagedUserSessions: vi.fn(),
  revokeManagedUserSession: vi.fn(),
}))
const managedUserMocks = vi.hoisted(() => ({
  fetchManagedUserDetail: vi.fn(),
  updateManagedUserProfile: vi.fn(),
  changeManagedUserStatus: vi.fn(),
  changeManagedUserLoginAccount: vi.fn(),
  changeManagedUserSecurityExpiration: vi.fn(),
  resetManagedUserPassword: vi.fn(),
}))
const credentialMocks = vi.hoisted(() => ({ reinviteManagedUser: vi.fn() }))
const historyMocks = vi.hoisted(() => ({ fetchUserHistory: vi.fn() }))
const lifecycleMocks = vi.hoisted(() => ({
  fetchUserLifecycleContext: vi.fn(),
  transferEmployee: vi.fn(),
  precheckDeparture: vi.fn(),
  startDeparture: vi.fn(),
  confirmDepartureHandover: vi.fn(),
  completeDeparture: vi.fn(),
  rehireEmployee: vi.fn(),
}))
const feedbackMocks = vi.hoisted(() => ({ messageConfirm: vi.fn(), messageTip: vi.fn() }))
const routeMock = vi.hoisted(() => ({ params: { id: '21' } }))
const routerMock = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }))

vi.mock('@/modules/access/api/user-authorization-api', () => apiMocks)
vi.mock('@/modules/user/api/user-session-api', () => sessionMocks)
vi.mock('@/modules/user/api/user-api', () => managedUserMocks)
vi.mock('@/modules/user/api/credential-api', () => credentialMocks)
vi.mock('@/modules/user/api/user-history-api', () => historyMocks)
vi.mock('@/modules/user/api/user-lifecycle-api', () => lifecycleMocks)
vi.mock('@/shared/utils/feedback', () => feedbackMocks)
vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => routerMock,
}))

const authorization = {
  user: {
    id: 21,
    loginAct: 'sales01',
    name: '李销售',
    employeeNo: 'E00021',
    organizationName: '上海门店',
    positionName: '销售顾问',
    accountEnabled: true,
    protectedAccount: false,
  },
  authorizationVersion: 5,
  allowedActions: ['ROLE_UPDATE', 'PERMISSION_UPDATE'],
  unavailableReasons: {},
  roleAssignments: [
    {
      roleId: 8,
      roleCode: 'sales',
      roleName: '销售人员',
      source: 'DIRECT',
    },
  ],
  roleCandidates: [
    {
      roleId: 8,
      roleCode: 'sales',
      roleName: '销售人员',
      authorizationLevel: 10,
      defaultDataScope: 'SELF',
      selected: true,
      editable: true,
    },
  ],
  permissions: [],
}

const sessions = {
  targetUserId: 21,
  sessionRevision: 7,
  allowedActions: ['REVOKE_ALL'],
  unavailableReasons: {},
  sessions: [
    {
      id: 'managed-session',
      deviceSummary: '下属移动设备',
      clientSummary: 'Safari · iOS',
      networkSummary: '杭州 · 已脱敏网络',
      loginTime: '2026-07-10T01:00:00Z',
      lastActivityTime: '2026-07-10T02:00:00Z',
      expiresAt: '2026-07-17T01:00:00Z',
      current: false,
      rememberMe: true,
      allowedActions: ['REVOKE'],
      unavailableReasons: {},
    },
  ],
}

const managedUser = {
  id: 21,
  employeeId: 121,
  employeeNo: 'E00021',
  loginAct: 'sales01',
  name: '李销售',
  phone: '13800138000',
  email: 'sales@example.com',
  organizationName: '上海门店',
  positionName: '销售顾问',
  managerName: '销售经理',
  employmentStatus: '在职',
  accountStatus: '启用',
  lockStatus: '未锁定',
  lastLoginTime: null,
  profileVersion: 4,
  accountVersion: 5,
  employeeVersion: 6,
  roleNames: ['销售人员'],
  statusCommands: [],
  allowedActions: ['AUTHORIZATION_VIEW', 'AUTHORIZATION_UPDATE', 'SESSION_VIEW', 'SESSION_REVOKE'],
  unavailableReasons: {},
}

const lifecycleContext = {
  userId: 21,
  employeeId: 121,
  employmentStatus: 'ACTIVE',
  employeeVersion: 6,
  currentAssignment: {
    organizationName: '上海门店',
    positionName: '销售顾问',
    managerName: '销售经理',
  },
  activeRoleCount: 1,
  activePersonalPermissionCount: 0,
  activeSessionCount: 1,
  additionalAssignmentCount: 0,
  reportingRelationCount: 1,
  organizationCandidates: [{ id: 2, label: '杭州门店' }],
  positionCandidates: [{ id: 3, label: '销售主管' }],
  managerCandidates: [{ id: 4, label: '赵经理' }],
  handoverCandidates: [],
  allowedActions: ['TRANSFER'],
  unavailableReasons: {},
  statusTransitions: [
    { action: 'TRANSFER', fromStatus: 'ACTIVE', toStatus: 'ACTIVE', label: '调岗' },
  ],
}

describe('user authorization page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(apiMocks).forEach((mock) => mock.mockReset())
    Object.values(sessionMocks).forEach((mock) => mock.mockReset())
    Object.values(managedUserMocks).forEach((mock) => mock.mockReset())
    credentialMocks.reinviteManagedUser.mockReset()
    historyMocks.fetchUserHistory.mockReset()
    Object.values(lifecycleMocks).forEach((mock) => mock.mockReset())
    Object.values(feedbackMocks).forEach((mock) => mock.mockReset())
    routerMock.push.mockReset()
    routerMock.replace.mockReset()
    routeMock.params.id = '21'
    apiMocks.fetchUserAuthorizationDetail.mockResolvedValue(authorization)
    managedUserMocks.fetchManagedUserDetail.mockResolvedValue(managedUser)
    sessionMocks.fetchManagedUserSessions.mockResolvedValue(sessions)
    feedbackMocks.messageConfirm.mockResolvedValue(undefined)
    historyMocks.fetchUserHistory.mockResolvedValue({
      list: [],
      total: 0,
      pageSize: 10,
      pageNum: 1,
      pages: 0,
      size: 0,
      actionOptions: [],
      allowedActions: ['VIEW'],
      unavailableReasons: {},
    })
    lifecycleMocks.fetchUserLifecycleContext.mockResolvedValue(lifecycleContext)
  })

  it('renders server denial reasons and no self-authorization controls', async () => {
    apiMocks.fetchUserAuthorizationDetail.mockResolvedValueOnce({
      ...authorization,
      allowedActions: [],
      unavailableReasons: {
        ROLE_UPDATE: '任何用户都不能修改自己的角色',
        PERMISSION_UPDATE: '任何用户都不能修改自己的权限',
      },
    })

    render(UserAuthorizationPage)

    expect(await screen.findByText('任何用户都不能修改自己的角色')).toBeTruthy()
    expect(screen.getByText('任何用户都不能修改自己的权限')).toBeTruthy()
    expect(
      (screen.getByRole('checkbox', { name: '分配角色销售人员' }) as HTMLButtonElement).disabled,
    ).toBe(true)
    expect(screen.queryByRole('button', { name: /保存角色调整|保存个人权限/ })).toBeNull()
    expect(sessionMocks.fetchManagedUserSessions).toHaveBeenCalledWith(21, expect.any(AbortSignal))
  })

  it('shows a stable access-denied message for HTTP 403 business code', async () => {
    apiMocks.fetchUserAuthorizationDetail.mockRejectedValueOnce(
      new ApiError(520, 'access denied', null),
    )

    render(UserAuthorizationPage)

    expect(
      await screen.findByText('不能调整本人、同级、上级、范围外人员或受保护账号的授权'),
    ).toBeTruthy()
  })

  it('rejects an illegal route id without requesting either detail resource', async () => {
    routeMock.params.id = '../21'
    render(UserAuthorizationPage)
    expect(await screen.findByText('用户 ID 无效，无法加载详情。')).toBeTruthy()
    expect(managedUserMocks.fetchManagedUserDetail).not.toHaveBeenCalled()
    expect(apiMocks.fetchUserAuthorizationDetail).not.toHaveBeenCalled()
  })

  it('redirects the current user to personal center instead of opening managed-user detail', async () => {
    useAuthStore().$patch({ currentUser: { id: 21, name: '本人' } })

    render(UserAuthorizationPage)

    await waitFor(() => expect(routerMock.replace).toHaveBeenCalledWith({ name: 'profile' }))
    expect(managedUserMocks.fetchManagedUserDetail).not.toHaveBeenCalled()
    expect(apiMocks.fetchUserAuthorizationDetail).not.toHaveBeenCalled()
  })

  it('shows a distinct not-found state', async () => {
    managedUserMocks.fetchManagedUserDetail.mockRejectedValueOnce(
      new ApiError(404, 'not found', null),
    )
    render(UserAuthorizationPage)
    expect(await screen.findByText('用户不存在或已被删除')).toBeTruthy()
  })

  it('loads history only when audit permission and target allowedActions both permit it', async () => {
    usePermissionStore().setPermissionsFromUser({
      permissionList: [PERMISSIONS.audit.operation.detail],
    })
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'HISTORY_VIEW'],
    })
    render(UserAuthorizationPage)
    expect(await screen.findByText('用户变更历史')).toBeTruthy()
    await waitFor(() =>
      expect(historyMocks.fetchUserHistory).toHaveBeenCalledWith(
        21,
        { page: 1, size: 10 },
        expect.any(AbortSignal),
      ),
    )
  })

  it('does not call the history endpoint when either local audit or target gate is absent', async () => {
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'HISTORY_VIEW'],
    })
    render(UserAuthorizationPage)
    await screen.findByText('李销售')
    expect(historyMocks.fetchUserHistory).not.toHaveBeenCalled()
  })

  it('does not call history when local audit permission exists but target HISTORY_VIEW is denied', async () => {
    usePermissionStore().setPermissionsFromUser({
      permissionList: [PERMISSIONS.audit.operation.detail],
    })
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      unavailableReasons: { HISTORY_VIEW: '目标用户超出当前管理范围' },
    })
    render(UserAuthorizationPage)

    expect(await screen.findByText('变更历史：目标用户超出当前管理范围')).toBeTruthy()
    expect(historyMocks.fetchUserHistory).not.toHaveBeenCalled()
  })

  it('keeps the user facts visible when the history projection fails independently', async () => {
    usePermissionStore().setPermissionsFromUser({
      permissionList: [PERMISSIONS.audit.operation.detail],
    })
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'HISTORY_VIEW'],
    })
    historyMocks.fetchUserHistory.mockImplementationOnce(() => {
      throw new ApiError(520, 'denied', null)
    })
    render(UserAuthorizationPage)
    expect(await screen.findByText('无审计权限或目标用户超出可管理范围')).toBeTruthy()
    expect(screen.getByText('李销售')).toBeTruthy()
    expect(screen.getByText(/组织：上海门店/)).toBeTruthy()
  })

  it('exposes lifecycle entries only from managed-detail allowed actions', async () => {
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'TRANSFER', 'DEPARTURE', 'REHIRE'],
    })
    render(UserAuthorizationPage)
    await screen.findByText('李销售')
    expect((screen.getByRole('button', { name: '调岗' }) as HTMLButtonElement).disabled).toBe(false)
    expect((screen.getByRole('button', { name: '离职闭环' }) as HTMLButtonElement).disabled).toBe(
      false,
    )
    expect((screen.getByRole('button', { name: '返聘' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('does not expose lifecycle actions for self, protected, or out-of-range targets denied by the server', async () => {
    managedUserMocks.fetchManagedUserDetail.mockResolvedValueOnce({
      ...managedUser,
      unavailableReasons: {
        TRANSFER: '目标用户不可管理',
        DEPARTURE: '目标用户不可管理',
        REHIRE: '目标用户不可管理',
      },
    })
    render(UserAuthorizationPage)
    await screen.findByText('李销售')

    expect(screen.queryByRole('button', { name: '调岗' })).toBeNull()
    expect(screen.queryByRole('button', { name: '离职闭环' })).toBeNull()
    expect(screen.queryByRole('button', { name: '返聘' })).toBeNull()
    expect(lifecycleMocks.fetchUserLifecycleContext).not.toHaveBeenCalled()
  })

  it('runs a lifecycle command with employeeVersion and refreshes managed detail after completion', async () => {
    managedUserMocks.fetchManagedUserDetail.mockResolvedValue({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'TRANSFER'],
    })
    lifecycleMocks.transferEmployee.mockResolvedValue({
      ...lifecycleContext,
      employeeVersion: 7,
      currentAssignment: {
        organizationName: '杭州门店',
        positionName: '销售主管',
        managerName: '赵经理',
      },
    })
    render(UserAuthorizationPage)
    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('button', { name: '调岗' }))
    await screen.findByText(/员工版本 6/)
    await fireEvent.update(screen.getByLabelText('目标组织'), '2')
    await fireEvent.update(screen.getByLabelText('目标岗位'), '3')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '4')
    await fireEvent.update(screen.getByLabelText('生效时间'), '2026-07-12T09:00')
    await fireEvent.update(screen.getByLabelText('调岗原因'), '跨部门调岗')
    await fireEvent.click(screen.getByRole('button', { name: '确认调岗' }))

    await waitFor(() => expect(managedUserMocks.fetchManagedUserDetail).toHaveBeenCalledTimes(2))
    expect(lifecycleMocks.transferEmployee).toHaveBeenCalledWith(21, {
      employeeVersion: 6,
      organizationUnitId: '2',
      positionId: '3',
      managerEmployeeId: '4',
      effectiveFrom: new Date('2026-07-12T09:00').toISOString(),
      reason: '跨部门调岗',
    })
    expect(lifecycleMocks.transferEmployee.mock.calls[0]?.[1]).not.toHaveProperty('accountVersion')
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith('调岗已完成，原任职历史已保留', 'success')
  })

  it('exposes re-invite only from managed-detail allowedActions and displays delivery status', async () => {
    managedUserMocks.fetchManagedUserDetail.mockResolvedValue({
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'REINVITE'],
    })
    credentialMocks.reinviteManagedUser.mockResolvedValue({
      accepted: true,
      deliveryStatus: 'QUEUED',
    })
    render(UserAuthorizationPage)

    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('button', { name: '重新邀请' }))
    await fireEvent.update(screen.getByLabelText('重新邀请原因'), '首次邀请已过期')
    await fireEvent.click(screen.getByRole('button', { name: '提交重新邀请' }))

    await waitFor(() =>
      expect(credentialMocks.reinviteManagedUser).toHaveBeenCalledWith(21, {
        accountVersion: 5,
        reason: '首次邀请已过期',
      }),
    )
    expect(await screen.findByText('重新邀请凭证已排队')).toBeTruthy()
    expect(screen.queryByText(/QUEUED/)).toBeNull()
    expect(screen.queryByText('raw-invitation-secret')).toBeNull()
  })

  it('refreshes server facts after an expected-version conflict', async () => {
    apiMocks.updateUserRoleAssignments.mockRejectedValueOnce(new ApiError(409, 'conflict', null))
    render(UserAuthorizationPage)

    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('checkbox', { name: '分配角色销售人员' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '移除过期角色')
    await fireEvent.click(screen.getByRole('button', { name: '保存角色调整' }))

    await waitFor(() => expect(apiMocks.fetchUserAuthorizationDetail).toHaveBeenCalledTimes(2))
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      '用户授权已被其他管理者更新，页面将刷新为最新版本',
      'error',
    )
  })

  it('refreshes managed profile facts after the dedicated profile-version conflict', async () => {
    const editable = {
      ...managedUser,
      allowedActions: [...managedUser.allowedActions, 'PROFILE_UPDATE'],
    }
    managedUserMocks.fetchManagedUserDetail
      .mockResolvedValueOnce(editable)
      .mockResolvedValueOnce({ ...editable, name: '服务端最新姓名', profileVersion: 5 })
    managedUserMocks.updateManagedUserProfile.mockRejectedValueOnce(
      new ApiError(API_ERROR_CODE.PROFILE_VERSION_CONFLICT, 'conflict', null, false, 409),
    )
    render(UserAuthorizationPage)

    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('button', { name: '编辑资料' }))
    await fireEvent.update(screen.getByLabelText('姓名'), '本地过期姓名')
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => expect(managedUserMocks.fetchManagedUserDetail).toHaveBeenCalledTimes(2))
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith('用户资料已变化，已刷新最新信息', 'error')
    expect(await screen.findByText('服务端最新姓名')).toBeTruthy()
  })

  it('uses server allowedActions and an audit reason to revoke a managed-user session', async () => {
    apiMocks.fetchUserAuthorizationDetail.mockResolvedValueOnce({
      ...authorization,
      allowedActions: [...authorization.allowedActions, 'SESSION_VIEW', 'SESSION_REVOKE'],
    })
    sessionMocks.revokeManagedUserSession.mockResolvedValue({
      ...sessions,
      sessionRevision: 8,
      sessions: [],
    })
    render(UserAuthorizationPage)

    expect(await screen.findByText('下属移动设备')).toBeTruthy()
    expect(sessionMocks.fetchManagedUserSessions).toHaveBeenCalledWith(21, expect.any(AbortSignal))
    expect((screen.getByRole('button', { name: '撤销会话' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    await fireEvent.update(screen.getByLabelText('撤销原因'), '下属报告设备遗失')
    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))

    await waitFor(() =>
      expect(sessionMocks.revokeManagedUserSession).toHaveBeenCalledWith(21, 'managed-session', {
        sessionRevision: 7,
        reason: '下属报告设备遗失',
      }),
    )
    expect(screen.queryByText('下属移动设备')).toBeNull()
  })

  it('shows 403 and refreshes after a managed-session version conflict', async () => {
    apiMocks.fetchUserAuthorizationDetail.mockResolvedValue({
      ...authorization,
      allowedActions: [...authorization.allowedActions, 'SESSION_VIEW', 'SESSION_REVOKE'],
    })
    sessionMocks.revokeAllManagedUserSessions.mockRejectedValueOnce(
      new ApiError(409, 'conflict', null),
    )
    render(UserAuthorizationPage)

    await screen.findByText('下属移动设备')
    await fireEvent.update(screen.getByLabelText('撤销原因'), '账号安全风险')
    await fireEvent.click(screen.getByRole('button', { name: '撤销全部会话' }))

    await waitFor(() => expect(sessionMocks.fetchManagedUserSessions).toHaveBeenCalledTimes(2))
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith('会话状态已变化，列表将刷新', 'error')

    sessionMocks.fetchManagedUserSessions.mockRejectedValueOnce(
      new ApiError(520, 'access denied', null),
    )
    await waitFor(() =>
      expect((screen.getByRole('button', { name: '刷新' }) as HTMLButtonElement).disabled).toBe(
        false,
      ),
    )
    await fireEvent.click(screen.getByRole('button', { name: '刷新' }))
    expect(
      await screen.findByText('不能查看或撤销本人、同级、上级、范围外或受保护账号的会话'),
    ).toBeTruthy()
  })

  it('shows the exact expired-session message and refreshes the managed list', async () => {
    sessionMocks.revokeManagedUserSession.mockRejectedValueOnce(
      new ApiError(API_ERROR_CODE.SESSION_EXPIRED, 'expired', null, false, 410),
    )
    render(UserAuthorizationPage)

    await screen.findByText('下属移动设备')
    await fireEvent.update(screen.getByLabelText('撤销原因'), '处理过期会话')
    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))

    await waitFor(() => expect(sessionMocks.fetchManagedUserSessions).toHaveBeenCalledTimes(2))
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith('会话已过期，列表将刷新', 'error')
  })
})
