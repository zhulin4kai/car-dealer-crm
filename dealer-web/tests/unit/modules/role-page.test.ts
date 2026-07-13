import { createPinia, setActivePinia } from 'pinia'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type {
  PermissionCatalogItem,
  RoleDetail,
  RolePermissionMatrix,
} from '@/modules/access/model/access.types'
import RolePage from '@/pages/dashboard/role.vue'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { usePermissionStore } from '@/stores/permission.store'

const apiMocks = vi.hoisted(() => ({
  copyRole: vi.fn(),
  createRole: vi.fn(),
  disableRole: vi.fn(),
  enableRole: vi.fn(),
  fetchPermissionCatalog: vi.fn(),
  fetchRoleDetail: vi.fn(),
  fetchRoleOrganizationOptions: vi.fn(),
  fetchRolePage: vi.fn(),
  fetchRolePermissionMatrix: vi.fn(),
  previewRolePermissionMatrix: vi.fn(),
  updateRole: vi.fn(),
  updateRolePermissionMatrix: vi.fn(),
}))
const feedbackMocks = vi.hoisted(() => ({ messageTip: vi.fn() }))

vi.mock('@/modules/access/api/access-api', () => apiMocks)
vi.mock('@/shared/utils/feedback', () => feedbackMocks)
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

function role(id: number, name: string): RoleDetail {
  return {
    id,
    code: `role_${id}`,
    name,
    protectedRole: false,
    authorizationLevel: 10,
    defaultDataScope: 'SELF',
    scopeType: 'GLOBAL',
    applicableOrganizations: [],
    memberCount: 0,
    enabled: true,
    version: 1,
    editable: true,
    allowedActions: ['EDIT', 'COPY', 'STATUS_CHANGE'],
    unavailableReasons: {},
  }
}

function matrix(id: number): RolePermissionMatrix {
  return {
    roleId: id,
    roleName: `角色 ${id}`,
    expectedVersion: 1,
    selectedPermissionIds: [],
    permissionScopes: [],
    permissionScopeOptions: [],
    editable: true,
  }
}

describe('role page', () => {
  beforeEach(() => {
    Object.values(apiMocks).forEach(mock => mock.mockReset())
    feedbackMocks.messageTip.mockReset()
    setActivePinia(createPinia())
    apiMocks.fetchPermissionCatalog.mockResolvedValue([])
    apiMocks.fetchRoleOrganizationOptions.mockResolvedValue([])
    apiMocks.fetchRolePage.mockImplementation(({ page }: { page: number }) => {
      const current = page === 2 ? role(2, '第二页角色') : role(1, '第一页角色')
      return Promise.resolve({
        list: [current],
        total: 11,
        pageSize: 10,
        pageNum: page,
        pages: 2,
        size: 1,
      })
    })
    apiMocks.fetchRoleDetail.mockImplementation((id: number) =>
      Promise.resolve(id === 2 ? role(2, '第二页角色') : role(1, '第一页角色')),
    )
    apiMocks.fetchRolePermissionMatrix.mockImplementation((id: number) =>
      Promise.resolve(matrix(id)),
    )
  })

  it('requests the selected page from the server instead of loading a fixed 200-row page', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    usePermissionStore().setPermissionsFromUser({
      permissionList: [
        PERMISSIONS.role.list,
        PERMISSIONS.role.view,
        PERMISSIONS.permission.list,
      ],
    })
    render(RolePage, {
      global: {
        plugins: [pinia],
        directives: { hasPermission: {} },
      },
    })

    await screen.findAllByText('第一页角色')
    expect(apiMocks.fetchRolePage).toHaveBeenCalledWith({ page: 1, size: 10 })

    await fireEvent.click(screen.getByRole('button', { name: '下一页' }))

    await waitFor(() =>
      expect(apiMocks.fetchRolePage).toHaveBeenLastCalledWith({ page: 2, size: 10 }),
    )
    expect(await screen.findAllByText('第二页角色')).not.toHaveLength(0)
  })

  it('keeps role:list usable without silently requiring role:view or permission:list', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    usePermissionStore().setPermissionsFromUser({ permissionList: [PERMISSIONS.role.list] })
    render(RolePage, {
      global: {
        plugins: [pinia],
        directives: { hasPermission: {} },
      },
    })

    await screen.findAllByText('第一页角色')
    expect(screen.getByText('当前账号只有角色列表权限，不能查看角色详情和权限矩阵')).toBeTruthy()
    expect(apiMocks.fetchRoleDetail).not.toHaveBeenCalled()
    expect(apiMocks.fetchRolePermissionMatrix).not.toHaveBeenCalled()
    expect(apiMocks.fetchPermissionCatalog).not.toHaveBeenCalled()
    expect(apiMocks.fetchRoleOrganizationOptions).not.toHaveBeenCalled()
  })

  it('shows role detail but does not fetch an ID-only matrix without permission:list', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    usePermissionStore().setPermissionsFromUser({
      permissionList: [PERMISSIONS.role.list, PERMISSIONS.role.view],
    })
    render(RolePage, {
      global: { plugins: [pinia], directives: { hasPermission: {} } },
    })

    expect(await screen.findByText('当前账号可以查看角色资料，但没有权限目录读取权限，不能查看权限矩阵')).toBeTruthy()
    expect(apiMocks.fetchRoleDetail).toHaveBeenCalledWith(1)
    expect(apiMocks.fetchRolePermissionMatrix).not.toHaveBeenCalled()
    expect(apiMocks.fetchPermissionCatalog).not.toHaveBeenCalled()
  })

  it('reloads the role matrix from the server after a save CAS conflict', async () => {
    const permission: PermissionCatalogItem = {
      id: 101,
      name: '查看用户',
      code: 'user:view',
      module: 'user',
      type: 'button',
      sensitivityLevel: 'NORMAL',
      delegable: true,
      enabled: true,
      assignable: true,
      parentId: null,
      children: [],
    }
    apiMocks.fetchPermissionCatalog.mockResolvedValue([permission])
    apiMocks.fetchRolePermissionMatrix
      .mockResolvedValueOnce(matrix(1))
      .mockResolvedValueOnce({ ...matrix(1), expectedVersion: 2 })
    apiMocks.previewRolePermissionMatrix.mockResolvedValue({
      roleId: 1,
      expectedVersion: 1,
      addedPermissions: [
        {
          permissionId: 101,
          code: 'user:view',
          name: '查看用户',
          sensitivityLevel: 'NORMAL',
        },
      ],
      removedPermissions: [],
      affectedUserCount: 2,
      affectedOrganizationCount: 1,
      sessionRevocationCount: 0,
      warnings: [],
      scopeDifferences: [],
    })
    apiMocks.updateRolePermissionMatrix.mockRejectedValue(
      new ApiError(API_ERROR_CODE.ROLE_VERSION_CONFLICT, '角色版本冲突', null, false, 409),
    )
    const pinia = createPinia()
    setActivePinia(pinia)
    usePermissionStore().setPermissionsFromUser({
      permissionList: [
        PERMISSIONS.role.list,
        PERMISSIONS.role.view,
        PERMISSIONS.role.permissionManage,
        PERMISSIONS.permission.list,
      ],
    })

    render(RolePage, {
      global: { plugins: [pinia], directives: { hasPermission: {} } },
    })
    await screen.findByRole('checkbox', { name: '选择权限查看用户' })
    await fireEvent.click(screen.getByRole('checkbox', { name: '选择权限查看用户' }))
    await fireEvent.click(screen.getByRole('button', { name: '预览影响' }))
    expect(await screen.findByText('影响 2 名用户')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('变更原因'), '补充用户查看权限')
    await fireEvent.click(screen.getByRole('button', { name: '保存矩阵' }))

    await waitFor(() => expect(apiMocks.fetchRolePermissionMatrix).toHaveBeenCalledTimes(2))
    expect(apiMocks.fetchRolePage).toHaveBeenCalledTimes(2)
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      '角色或权限矩阵已被其他人更新，页面将刷新最新内容，请重新预览',
      'error',
    )
  })
})
