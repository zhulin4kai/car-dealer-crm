import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createRole,
  fetchPermissionCatalog,
  fetchRolePermissionMatrix,
  previewRolePermissionMatrix,
  updateRole,
  updateRolePermissionMatrix,
} from '@/modules/access/api/access-api'
import {
  getAccessErrorMessage,
  getUserAuthorizationErrorMessage,
} from '@/modules/access/model/access-error'
import { ApiError } from '@/shared/api/api-error'

const mockedAxios = vi.mocked(axios)

describe('access api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('creates roles with a stable code and explicit organization scope', async () => {
    await createRole({
      code: 'regional_manager',
      name: '区域经理',
      authorizationLevel: 50,
      defaultDataScope: 'ORG_TREE',
      scopeType: 'ORGANIZATION',
      organizationUnitIds: [2, 3],
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/roles',
      data: {
        code: 'regional_manager',
        name: '区域经理',
        authorizationLevel: 50,
        defaultDataScope: 'ORG_TREE',
        scopeType: 'ORGANIZATION',
        organizationUnitIds: [2, 3],
      },
    })
  })

  it('keeps role codes out of update requests', async () => {
    await updateRole(8, {
      name: '区域高级经理',
      authorizationLevel: 60,
      defaultDataScope: 'ORG_TREE',
      scopeType: 'ORGANIZATION',
      organizationUnitIds: [2, 3],
      expectedVersion: 4,
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/roles/8',
      data: expect.objectContaining({ expectedVersion: 4 }),
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('code')
  })

  it('reads the permission catalog without exposing create or update endpoints', async () => {
    await fetchPermissionCatalog()

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/permissions/tree',
    })
  })

  it('loads a role permission matrix separately from the catalog', async () => {
    await fetchRolePermissionMatrix(8)

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/roles/8/permissions',
    })
  })

  it('previews the exact version and permission set before saving', async () => {
    await previewRolePermissionMatrix(8, {
      expectedVersion: 4,
      permissionIds: [1, 2, 3],
      permissionScopes: [
        { permissionId: 2, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [7, 8] },
      ],
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/roles/8/permissions/preview',
      data: {
        expectedVersion: 4,
        permissionIds: [1, 2, 3],
        permissionScopes: [
          { permissionId: 2, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [7, 8] },
        ],
      },
    })
  })

  it('saves a matrix with reason, expected version and the complete permission set', async () => {
    await updateRolePermissionMatrix(8, {
      expectedVersion: 4,
      permissionIds: [1, 2, 3],
      permissionScopes: [
        { permissionId: 2, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [7, 8] },
      ],
      reason: '补充客户查看权限',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/roles/8/permissions',
      data: {
        expectedVersion: 4,
        permissionIds: [1, 2, 3],
        permissionScopes: [
          { permissionId: 2, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [7, 8] },
        ],
        reason: '补充客户查看权限',
      },
    })
  })

  it('maps version conflicts and access denial to actionable messages', () => {
    expect(getAccessErrorMessage(new ApiError(409, '冲突', null), '失败')).toContain('重新预览')
    expect(getAccessErrorMessage(new ApiError(600, '角色版本冲突', null), '失败')).toContain(
      '重新预览',
    )
    expect(getAccessErrorMessage(new ApiError(520, '无权限', null), '失败')).toContain(
      '超出可管理边界',
    )
    expect(getAccessErrorMessage(new ApiError(601, '受保护角色', null), '失败')).toContain(
      '受保护恢复角色',
    )
    expect(getAccessErrorMessage(new ApiError(602, '无效权限', null), '失败')).toContain(
      '未知、停用或不可分配',
    )
    expect(getAccessErrorMessage(new ApiError(603, '超过上限', null), '失败')).toContain('授权上限')
    expect(getAccessErrorMessage(new ApiError(604, '仍有成员', null), '失败')).toContain(
      '仍有关联用户',
    )
  })

  it('explains why the final available ordinary administrator cannot be removed', () => {
    expect(
      getUserAuthorizationErrorMessage(new ApiError(605, '最后管理员', null), '失败'),
    ).toContain('至少一个可用普通管理员')
  })
})
