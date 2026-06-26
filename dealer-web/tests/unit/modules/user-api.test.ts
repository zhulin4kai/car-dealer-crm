import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createUser,
  updateUser,
  disableUser,
  batchDisableUsers,
  handoverUserResponsibilities,
} from '@/modules/user/api/user-api'
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
    expect(Object.keys(request).sort()).toEqual(
      ['email', 'id', 'loginAct', 'name', 'phone'].sort(),
    )
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

  it('handoverUserResponsibilities sends target owner and reason only', async () => {
    await handoverUserResponsibilities(5, {
      targetUserId: 8,
      reason: '离职交接',
    })

    const callArgs = mockedAxios.request.mock.calls[0]?.[0] as Record<string, unknown> | undefined
    expect(callArgs?.method).toBe('put')
    expect(callArgs?.url).toBe('/api/user/5/handover')
    expect(callArgs?.data).toEqual({
      targetUserId: 8,
      reason: '离职交接',
    })
  })
})
