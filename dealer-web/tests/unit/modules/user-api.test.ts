import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createUser,
  updateUser,
  disableUser,
  batchDisableUsers,
  fetchUserFilterOptions,
  fetchManagedUserDetail,
  createManagedUser,
  updateManagedUserProfile,
  changeManagedUserStatus,
  changeManagedUserLoginAccount,
  changeManagedUserSecurityExpiration,
  resetManagedUserPassword,
  fetchOwnerList,
} from '@/modules/user/api/user-api'
import {
  batchUpdateUserPermissions,
  batchUpdateUserRoleAssignments,
  fetchUserAuthorizationDetail,
  updateUserPermissions,
  updateUserRoleAssignments,
} from '@/modules/access/api/user-authorization-api'
import { fetchOwnProfile, updateOwnProfile } from '@/modules/user/api/user-profile-api'
import {
  toCreateUserRequest,
  toUpdateUserRequest,
  type UserFormValues,
} from '@/modules/user/model/user.types'

const mockedAxios = vi.mocked(axios)

const formValues: UserFormValues = {
  loginAct: 'user001',
  loginPwd: 'pass123456',
  name: '张三',
  phone: '13800138000',
  email: 'zhangsan@example.com',
}

describe('user request mappers', () => {
  it('toCreateUserRequest picks exactly the 5 create fields', () => {
    const request = toCreateUserRequest(formValues)

    expect(request).toEqual({
      loginAct: 'user001',
      loginPwd: 'pass123456',
      name: '张三',
      phone: '13800138000',
      email: 'zhangsan@example.com',
    })
    expect(Object.keys(request).sort()).toEqual(
      ['email', 'loginAct', 'loginPwd', 'name', 'phone'].sort(),
    )
  })

  it('toUpdateUserRequest excludes password and status fields', () => {
    const request = toUpdateUserRequest(formValues, 42)

    expect(request).toEqual({
      id: 42,
      loginAct: 'user001',
      name: '张三',
      phone: '13800138000',
      email: 'zhangsan@example.com',
    })
    expect(Object.keys(request).sort()).toEqual(['email', 'id', 'loginAct', 'name', 'phone'].sort())
    expect(request).not.toHaveProperty('loginPwd')
    expect(request).not.toHaveProperty('accountNoExpired')
    expect(request).not.toHaveProperty('credentialsNoExpired')
    expect(request).not.toHaveProperty('accountNoLocked')
    expect(request).not.toHaveProperty('accountEnabled')
  })
})

describe('user api request bodies', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('createUser sends JSON body with exactly 5 fields', async () => {
    await createUser(toCreateUserRequest(formValues))

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('post')
    expect(callArgs?.url).toBe('/api/user')
    expect(callArgs?.data).toEqual({
      loginAct: 'user001',
      loginPwd: 'pass123456',
      name: '张三',
      phone: '13800138000',
      email: 'zhangsan@example.com',
    })
  })

  it('updateUser sends JSON body with id and 4 fields, no password or status', async () => {
    await updateUser(toUpdateUserRequest(formValues, 7))

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('put')
    expect(callArgs?.url).toBe('/api/user')
    expect(callArgs?.data).toEqual({
      id: 7,
      loginAct: 'user001',
      name: '张三',
      phone: '13800138000',
      email: 'zhangsan@example.com',
    })
    expect(callArgs?.data).not.toHaveProperty('loginPwd')
    expect(callArgs?.data).not.toHaveProperty('accountEnabled')
  })

  it('disableUser sends PUT to /api/user/{id}/disable', async () => {
    await disableUser(5)

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('put')
    expect(callArgs?.url).toBe('/api/user/5/disable')
  })

  it('batchDisableUsers sends ids array in JSON body', async () => {
    await batchDisableUsers([1, 2, 3])

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('put')
    expect(callArgs?.url).toBe('/api/users/batch-disable')
    expect(callArgs?.data).toEqual({ ids: [1, 2, 3] })
  })

  it('uses the Task 18 management resources and never sends a create password', async () => {
    const signal = new AbortController().signal
    await fetchUserFilterOptions(undefined, signal)
    await fetchManagedUserDetail(21, signal)
    await createManagedUser({
      loginAct: 'sales01',
      name: '李销售',
      phone: null,
      email: null,
      employeeNo: 'E00021',
      organizationUnitId: 1,
      positionId: 2,
      managerEmployeeId: 3,
      roleIds: [4],
    })
    await updateManagedUserProfile(21, {
      profileVersion: 5,
      name: '李顾问',
      phone: null,
      email: null,
    })
    await changeManagedUserStatus(21, { accountVersion: 6, command: 'DISABLE', reason: '离职处置' })
    await changeManagedUserLoginAccount(21, {
      accountVersion: 7,
      loginAct: 'sales.renamed',
      reason: '账号规范化',
    })
    await changeManagedUserSecurityExpiration(21, {
      accountVersion: 8,
      accountExpiresAt: null,
      credentialExpiresAt: '2026-08-01T00:00',
      reason: '设置凭证期限',
    })
    await resetManagedUserPassword(21, { accountVersion: 7, reason: '用户忘记密码' })

    const calls = mockedAxios.request.mock.calls.map(([config]) => config)
    expect(calls.map((config) => `${config.method} ${config.url}`)).toEqual([
      'get /api/users/filter-options',
      'get /api/users/21',
      'post /api/users',
      'put /api/users/21/profile',
      'post /api/users/21/status',
      'put /api/users/21/login-account',
      'put /api/users/21/security-expiration',
      'post /api/users/21/password-reset',
    ])
    expect(calls[2]?.data).not.toHaveProperty('password')
    expect(calls[2]?.data).not.toHaveProperty('loginPwd')
    expect(calls[3]?.data).toEqual({ profileVersion: 5, name: '李顾问', phone: null, email: null })
    expect(calls[4]?.data).toEqual({ accountVersion: 6, command: 'DISABLE', reason: '离职处置' })
    expect(calls[5]?.data).toEqual({
      accountVersion: 7,
      loginAct: 'sales.renamed',
      reason: '账号规范化',
    })
    expect(calls[6]?.data).toEqual({
      accountVersion: 8,
      accountExpiresAt: null,
      credentialExpiresAt: '2026-08-01T00:00',
      reason: '设置凭证期限',
    })
    expect(calls[7]?.data).toEqual({ accountVersion: 7, reason: '用户忘记密码' })
  })

  it('requests organization-specific assignable role candidates without changing the resource', async () => {
    const signal = new AbortController().signal
    await fetchUserFilterOptions(12, signal)

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/users/filter-options',
      params: { organizationUnitId: 12 },
      signal,
    })
  })

  it('requests minimal owner candidates for a concrete permission and qualification context', async () => {
    await fetchOwnerList({
      permissionCode: 'customer:owner:transfer',
      qualificationContext: 'CUSTOMER_OWNER',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/owner',
      params: {
        permissionCode: 'customer:owner:transfer',
        qualificationContext: 'CUSTOMER_OWNER',
      },
    })
  })

  it('loads user authorization from the target user resource', async () => {
    await fetchUserAuthorizationDetail(21)

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/users/21/authorization',
      signal: undefined,
    })
  })

  it('replaces user roles with version and reason but no client management scope', async () => {
    await updateUserRoleAssignments(21, {
      authorizationVersion: 5,
      roleIds: [8, 9],
      reason: '岗位调整',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/users/21/authorization/roles',
      data: { authorizationVersion: 5, roleIds: [8, 9], reason: '岗位调整' },
    })
  })

  it('updates personal permission states atomically with validity and data-scope candidate keys', async () => {
    await updateUserPermissions(21, {
      authorizationVersion: 6,
      reason: '临时代理',
      changes: [
        {
          permissionId: 31,
          state: 'GRANT',
          dataScopeCandidateKey: 'store-2',
          effectiveFrom: '2026-07-11T02:00:00.000Z',
          effectiveTo: '2026-07-18T02:00:00.000Z',
        },
      ],
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/users/21/authorization/permissions',
      data: expect.objectContaining({ authorizationVersion: 6, reason: '临时代理' }),
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('operatorId')
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('manageableUserIds')
  })

  it('sends CUSTOM_ORGS as explicit per-source organization ids', async () => {
    await updateUserPermissions(21, {
      authorizationVersion: 7,
      reason: '临时跨店支援',
      changes: [
        {
          permissionId: 31,
          state: 'GRANT',
          dataScopeCandidateKey: 'CUSTOM_ORGS',
          customOrganizationUnitIds: [2, 3],
          effectiveFrom: '2026-07-12T02:00:00.000Z',
        },
      ],
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).toEqual({
      authorizationVersion: 7,
      reason: '临时跨店支援',
      changes: [
        {
          permissionId: 31,
          state: 'GRANT',
          dataScopeCandidateKey: 'CUSTOM_ORGS',
          customOrganizationUnitIds: [2, 3],
          effectiveFrom: '2026-07-12T02:00:00.000Z',
        },
      ],
    })
  })

  it('sends target-specific versions for atomic batch authorization commands', async () => {
    const targets = [
      { userId: 21, authorizationVersion: 5 },
      { userId: 22, authorizationVersion: 7 },
    ]
    await batchUpdateUserRoleAssignments({
      targets,
      operation: 'ASSIGN',
      roleIds: [8],
      reason: '批量补充角色',
    })
    await batchUpdateUserPermissions({
      targets,
      changes: [{ permissionId: 31, state: 'DENY' }],
      reason: '批量限制权限',
    })

    expect(mockedAxios.request.mock.calls.at(-2)?.[0]).toEqual({
      method: 'put',
      url: '/api/users/authorization/batch/roles',
      data: { targets, operation: 'ASSIGN', roleIds: [8], reason: '批量补充角色' },
    })
    expect(mockedAxios.request.mock.calls.at(-1)?.[0]).toEqual({
      method: 'put',
      url: '/api/users/authorization/batch/permissions',
      data: { targets, changes: [{ permissionId: 31, state: 'DENY' }], reason: '批量限制权限' },
    })
  })

  it('uses current-user profile resources without a client user id', async () => {
    await fetchOwnProfile()
    await updateOwnProfile({
      authorizationVersion: 3,
      name: '李顾问',
      phone: '13900139000',
      email: 'new@example.com',
      avatarUrl: null,
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/profile',
      signal: undefined,
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'put',
      url: '/api/profile',
      data: {
        authorizationVersion: 3,
        name: '李顾问',
        phone: '13900139000',
        email: 'new@example.com',
        avatarUrl: null,
      },
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]?.data).not.toHaveProperty('userId')
    expect(mockedAxios.request.mock.calls[1]?.[0]?.data).not.toHaveProperty('loginAct')
    expect(mockedAxios.request.mock.calls[1]?.[0]?.data).not.toHaveProperty('roles')
    expect(mockedAxios.request.mock.calls[1]?.[0]?.data).not.toHaveProperty('accountEnabled')
  })
})
