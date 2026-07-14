import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import UserPage from '@/pages/dashboard/user.vue'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { usePermissionStore } from '@/stores/permission.store'
import { PERMISSIONS } from '@/shared/constants/permissions'

const apiMocks = vi.hoisted(() => ({
  fetchUserPage: vi.fn(),
  fetchUserFilterOptions: vi.fn(),
  createManagedUser: vi.fn(),
}))
const accessMocks = vi.hoisted(() => ({
  fetchUserAuthorizationDetail: vi.fn(),
  batchUpdateUserRoleAssignments: vi.fn(),
  batchUpdateUserPermissions: vi.fn(),
}))
const routerMock = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/modules/user/api/user-api', () => apiMocks)
vi.mock('@/modules/access/api/user-authorization-api', () => accessMocks)
vi.mock('vue-router', () => ({ useRouter: () => routerMock }))

const options = {
  organizations: [
    { id: 1, label: '上海门店' },
    { id: 2, label: '杭州门店' },
  ],
  positions: [{ id: 2, label: '销售顾问' }],
  managers: [{ id: 3, label: '销售经理' }],
  roles: [
    { id: 1, label: '系统管理员' },
    { id: 4, label: '销售人员' },
  ],
  assignableRoles: [{ id: 4, label: '销售人员' }],
  employmentStatuses: [{ id: 'ACTIVE', label: '在职' }],
  accountStatuses: [{ id: 'ENABLED', label: '启用' }],
  lockStatuses: [{ id: 'UNLOCKED', label: '未锁定' }],
  bootstrapRequired: false,
  bootstrapAllowed: false,
  bootstrapRootOrganizationId: null,
  bootstrapRootOrganizationVersion: null,
}

describe('user management workbench', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    usePermissionStore().setPermissionsFromUser({ permissionList: [PERMISSIONS.user.view] })
    Object.values(apiMocks).forEach((mock) => mock.mockReset())
    Object.values(accessMocks).forEach((mock) => mock.mockReset())
    routerMock.push.mockReset()
    apiMocks.fetchUserFilterOptions.mockResolvedValue(options)
    apiMocks.fetchUserPage.mockResolvedValue({
      list: [],
      total: 0,
      pageSize: 10,
      pageNum: 1,
      pages: 0,
      size: 0,
    })
  })

  it('sends all filters and sorting to the server instead of sorting the current page', async () => {
    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await waitFor(() => expect(apiMocks.fetchUserPage).toHaveBeenCalledTimes(1))

    await fireEvent.update(screen.getByLabelText('关键词'), ' 李销售 ')
    await fireEvent.update(screen.getByLabelText('组织'), '1')
    await fireEvent.update(screen.getByLabelText('岗位'), '2')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '3')
    await fireEvent.update(screen.getByLabelText('角色'), '4')
    await fireEvent.update(screen.getByLabelText('任职状态'), 'ACTIVE')
    await fireEvent.update(screen.getByLabelText('账号状态'), 'ENABLED')
    await fireEvent.update(screen.getByLabelText('锁定状态'), 'UNLOCKED')
    await fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => expect(apiMocks.fetchUserPage).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchUserPage.mock.calls[1]?.[0]).toEqual({
      page: 1,
      size: 10,
      keyword: '李销售',
      organizationUnitId: '1',
      positionId: '2',
      managerEmployeeId: '3',
      roleId: '4',
      employmentStatus: 'ACTIVE',
      accountStatus: 'ENABLED',
      lockStatus: 'UNLOCKED',
      sortBy: 'employeeNo',
      sortDirection: 'asc',
    })

    await fireEvent.click(screen.getByRole('button', { name: /姓名/ }))
    await waitFor(() => expect(apiMocks.fetchUserPage).toHaveBeenCalledTimes(3))
    expect(apiMocks.fetchUserPage.mock.calls[2]?.[0]).toEqual(
      expect.objectContaining({ page: 1, sortBy: 'name', sortDirection: 'asc' }),
    )
  })

  it('renders server empty and error states with a retry', async () => {
    apiMocks.fetchUserPage
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ list: [], total: 0, pageSize: 10, pageNum: 1, pages: 0, size: 0 })
    render(UserPage, { global: { directives: { hasPermission: {} } } })
    expect(await screen.findByText('加载用户列表失败')).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: '重新加载' }))
    expect(await screen.findByText('没有符合条件的用户')).toBeTruthy()
  })

  it('shows a queued invitation without exposing an internal delivery status after create', async () => {
    apiMocks.createManagedUser.mockResolvedValue({
      user: {
        id: 21,
        loginAct: 'sales01',
        name: '李销售',
        employmentStatus: 'ACTIVE',
        accountStatus: 'PENDING_ACTIVATION',
        lockStatus: 'UNLOCKED',
        profileVersion: 1,
        accountVersion: 1,
        employeeVersion: 1,
        roleNames: [],
        statusCommands: [],
        allowedActions: [],
        unavailableReasons: {},
      },
      credentialDelivery: { accepted: true, deliveryStatus: 'QUEUED' },
    })
    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await waitFor(() => expect(apiMocks.fetchUserFilterOptions).toHaveBeenCalled())

    await fireEvent.click(screen.getByRole('button', { name: '新增用户' }))
    await fireEvent.update(screen.getByLabelText('登录账号'), 'sales01')
    await fireEvent.update(screen.getByLabelText('姓名'), '李销售')
    await fireEvent.update(screen.getByLabelText('员工编号'), 'E00021')
    await fireEvent.update(document.getElementById('create-organization')!, '1')
    await fireEvent.update(document.getElementById('create-position')!, '2')
    await fireEvent.update(document.getElementById('create-manager')!, '3')
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText('邀请凭证已排队，等待安全通知服务投递')).toBeTruthy()
    expect(screen.queryByText(/QUEUED/)).toBeNull()
    expect(routerMock.push).not.toHaveBeenCalled()
    await fireEvent.click(screen.getByRole('button', { name: '查看用户详情' }))
    expect(routerMock.push).toHaveBeenCalledWith({ name: 'user-detail', params: { id: '21' } })
    usePermissionStore().setPermissionsFromUser({ permissionList: [] })
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: '查看用户详情' })).toBeNull(),
    )
    expect(screen.getByText('当前账号没有用户详情读取权限')).toBeTruthy()
  })

  it('keeps list-filter roles separate and ignores stale organization candidate responses', async () => {
    const first = deferred<typeof options>()
    const second = deferred<typeof options>()
    apiMocks.fetchUserFilterOptions.mockImplementation((organizationUnitId?: number | string) => {
      if (organizationUnitId === '1') return first.promise
      if (organizationUnitId === '2') return second.promise
      return Promise.resolve(options)
    })
    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await waitFor(() =>
      expect(apiMocks.fetchUserFilterOptions).toHaveBeenCalledWith(
        undefined,
        expect.any(AbortSignal),
      ),
    )

    await fireEvent.click(screen.getByRole('button', { name: '新增用户' }))
    expect(screen.queryByRole('checkbox', { name: '系统管理员' })).toBeNull()
    await fireEvent.update(document.getElementById('create-organization')!, '1')
    await fireEvent.update(document.getElementById('create-organization')!, '2')

    second.resolve({ ...options, assignableRoles: [{ id: 22, label: '杭州销售' }] })
    expect(await screen.findByRole('checkbox', { name: '杭州销售' })).toBeTruthy()
    first.resolve({ ...options, assignableRoles: [{ id: 11, label: '过期上海角色' }] })
    await Promise.resolve()

    expect(screen.queryByRole('checkbox', { name: '过期上海角色' })).toBeNull()
    expect(
      document.querySelector('select[aria-label="角色"] option[value="1"]')?.textContent,
    ).toContain('系统管理员')
  })

  it('clears the previous organization roles when the next candidate load fails', async () => {
    apiMocks.fetchUserFilterOptions.mockImplementation((organizationUnitId?: number | string) => {
      if (organizationUnitId === '1')
        return Promise.resolve({ ...options, assignableRoles: [{ id: 11, label: '上海销售' }] })
      if (organizationUnitId === '2')
        return Promise.reject(new Error('candidate service unavailable'))
      return Promise.resolve(options)
    })
    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await waitFor(() => expect(apiMocks.fetchUserFilterOptions).toHaveBeenCalled())

    await fireEvent.click(screen.getByRole('button', { name: '新增用户' }))
    await fireEvent.update(document.getElementById('create-organization')!, '1')
    expect(await screen.findByRole('checkbox', { name: '上海销售' })).toBeTruthy()
    await fireEvent.update(document.getElementById('create-organization')!, '2')

    expect(await screen.findByText('加载当前组织的直属管理者和可委派角色失败，请重试选择组织')).toBeTruthy()
    expect(screen.queryByRole('checkbox', { name: '上海销售' })).toBeNull()
  })

  it('loads target versions before submitting one atomic batch role command', async () => {
    const users = [userSummary(21, '李销售'), userSummary(22, '王销售')]
    apiMocks.fetchUserPage.mockResolvedValue({
      list: users,
      total: 2,
      pageSize: 10,
      pageNum: 1,
      pages: 1,
      size: 2,
    })
    accessMocks.fetchUserAuthorizationDetail.mockImplementation((userId: number) =>
      Promise.resolve({
        user: {
          id: userId,
          loginAct: `sales${userId}`,
          name: userId === 21 ? '李销售' : '王销售',
          accountEnabled: true,
          protectedAccount: false,
        },
        authorizationVersion: userId === 21 ? 5 : 7,
        allowedActions: ['ROLE_UPDATE', 'PERMISSION_UPDATE'],
        unavailableReasons: {},
        roleAssignments: [],
        roleCandidates: [
          {
            roleId: 8,
            roleCode: 'sales',
            roleName: '销售人员',
            authorizationLevel: 10,
            defaultDataScope: 'SELF',
            selected: false,
            editable: true,
          },
        ],
        permissions: [],
      }),
    )
    accessMocks.batchUpdateUserRoleAssignments.mockResolvedValue({
      targetCount: 2,
      changedTargetCount: 2,
      targets: [],
    })

    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('checkbox', { name: '选择用户李销售' }))
    await fireEvent.click(screen.getByRole('checkbox', { name: '选择用户王销售' }))
    await fireEvent.click(screen.getByRole('button', { name: '批量调整角色' }))
    expect(await screen.findByText(/将对 2 名用户执行同一项调整/)).toBeTruthy()
    await fireEvent.click(screen.getByRole('checkbox', { name: '批量选择角色销售人员' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '批量补充销售角色')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    await waitFor(() =>
      expect(accessMocks.batchUpdateUserRoleAssignments).toHaveBeenCalledWith({
        targets: [
          { userId: 21, authorizationVersion: 5 },
          { userId: 22, authorizationVersion: 7 },
        ],
        operation: 'ASSIGN',
        roleIds: [8],
        reason: '批量补充销售角色',
      }),
    )
  })

  it('submits a batch personal-permission command through the shared batch flow', async () => {
    const user = userSummary(21, '李销售')
    apiMocks.fetchUserPage.mockResolvedValue({
      list: [user],
      total: 1,
      pageSize: 10,
      pageNum: 1,
      pages: 1,
      size: 1,
    })
    accessMocks.fetchUserAuthorizationDetail.mockResolvedValue(authorizationDetail(21, 5))
    accessMocks.batchUpdateUserPermissions.mockResolvedValue({
      targetCount: 1,
      changedTargetCount: 1,
      targets: [],
    })

    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('checkbox', { name: '选择用户李销售' }))
    await fireEvent.click(screen.getByRole('button', { name: '批量调整个人权限' }))
    await fireEvent.update(await screen.findByLabelText('权限'), '31')
    await fireEvent.update(screen.getByLabelText('调整原因'), '统一恢复角色继承')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    await waitFor(() =>
      expect(accessMocks.batchUpdateUserPermissions).toHaveBeenCalledWith({
        targets: [{ userId: 21, authorizationVersion: 5 }],
        changes: [{ permissionId: 31, state: 'INHERIT' }],
        reason: '统一恢复角色继承',
      }),
    )
  })

  it('reloads server authorization facts after a batch role CAS conflict', async () => {
    const user = userSummary(21, '李销售')
    apiMocks.fetchUserPage.mockResolvedValue({
      list: [user],
      total: 1,
      pageSize: 10,
      pageNum: 1,
      pages: 1,
      size: 1,
    })
    accessMocks.fetchUserAuthorizationDetail
      .mockResolvedValueOnce(authorizationDetail(21, 5))
      .mockResolvedValueOnce(authorizationDetail(21, 6))
    accessMocks.batchUpdateUserRoleAssignments
      .mockRejectedValueOnce(
        new ApiError(API_ERROR_CODE.ROLE_VERSION_CONFLICT, '授权版本冲突', null, false, 409),
      )
      .mockResolvedValueOnce({ targetCount: 1, changedTargetCount: 1, targets: [] })

    render(UserPage, { global: { directives: { hasPermission: {} } } })
    await screen.findByText('李销售')
    await fireEvent.click(screen.getByRole('checkbox', { name: '选择用户李销售' }))
    await fireEvent.click(screen.getByRole('button', { name: '批量调整角色' }))
    await fireEvent.click(await screen.findByRole('checkbox', { name: '批量选择角色销售人员' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '首次提交触发冲突')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    await waitFor(() => expect(accessMocks.fetchUserAuthorizationDetail).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchUserPage).toHaveBeenCalledTimes(2)
    await waitFor(() =>
      expect(
        (screen.getByRole('button', { name: '确认批量调整' }) as HTMLButtonElement).disabled,
      ).toBe(false),
    )

    await fireEvent.click(screen.getByRole('checkbox', { name: '批量选择角色销售人员' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '基于最新版本重试')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    await waitFor(() =>
      expect(accessMocks.batchUpdateUserRoleAssignments).toHaveBeenLastCalledWith({
        targets: [{ userId: 21, authorizationVersion: 6 }],
        operation: 'ASSIGN',
        roleIds: [8],
        reason: '基于最新版本重试',
      }),
    )
  })
})

function authorizationDetail(userId: number, authorizationVersion: number) {
  return {
    user: {
      id: userId,
      loginAct: `sales${userId}`,
      name: '李销售',
      accountEnabled: true,
      protectedAccount: false,
    },
    authorizationVersion,
    allowedActions: ['ROLE_UPDATE', 'PERMISSION_UPDATE'],
    unavailableReasons: {},
    roleAssignments: [],
    roleCandidates: [
      {
        roleId: 8,
        roleCode: 'sales',
        roleName: '销售人员',
        authorizationLevel: 10,
        defaultDataScope: 'SELF',
        selected: false,
        editable: true,
      },
    ],
    permissions: [
      {
        permissionId: 31,
        code: 'customer:view',
        name: '客户查看',
        module: '客户',
        sensitivityLevel: 'NORMAL',
        delegable: true,
        effective: true,
        personalState: 'INHERIT',
        editable: true,
        sources: [],
        dataScopeCandidates: [{ candidateKey: 'SELF', code: 'SELF', label: '本人' }],
      },
    ],
  }
}

function userSummary(id: number, name: string) {
  return {
    id,
    employeeId: id + 100,
    employeeNo: `E${id}`,
    loginAct: `sales${id}`,
    name,
    organizationName: '上海门店',
    positionName: '销售顾问',
    managerName: '销售经理',
    roleNames: [],
    employmentStatus: 'ACTIVE',
    accountStatus: 'ACTIVE',
    lockStatus: 'UNLOCKED',
    lastLoginTime: null,
    profileVersion: 0,
    accountVersion: 0,
    employeeVersion: 0,
    statusCommands: [],
    allowedActions: ['VIEW', 'AUTHORIZATION_VIEW'],
    unavailableReasons: {},
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}
