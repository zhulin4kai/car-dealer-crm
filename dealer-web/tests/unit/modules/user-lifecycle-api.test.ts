import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  completeDeparture,
  confirmDepartureHandover,
  fetchUserLifecycleContext,
  precheckDeparture,
  rehireEmployee,
  startDeparture,
  transferEmployee,
} from '@/modules/user/api/user-lifecycle-api'

const mockedAxios = vi.mocked(axios)

describe('user lifecycle api', () => {
  beforeEach(() => mockedAxios.request.mockClear())

  it.each([
    [200, 'RECOVER', 'NOT_REQUIRED'],
    [202, 'INVITE', 'QUEUED'],
  ])(
    'unwraps the same rehire result envelope when HTTP status is %i',
    async (httpStatus, accountActivationMode, credentialDeliveryStatus) => {
      const responseData = {
        context: {
          userId: 21,
          employeeId: 31,
          employmentStatus: 'ACTIVE',
          employeeVersion: 6,
        },
        restoredLegacyAuthorizationCount: 0,
        credentialDeliveryStatus,
      }
      mockedAxios.request.mockResolvedValueOnce({
        status: httpStatus,
        data: { code: 200, msg: 'OK', data: responseData },
      })

      await expect(
        rehireEmployee(21, {
          employeeVersion: 5,
          organizationUnitId: 2,
          positionId: 3,
          managerEmployeeId: 4,
          effectiveFrom: '2026-08-01T01:00:00.000Z',
          accountActivationMode,
          reason: '重新入职',
        }),
      ).resolves.toEqual(responseData)
    },
  )

  it('uses plural target-scoped lifecycle commands with version, reason and memory-only snapshots', async () => {
    const signal = new AbortController().signal
    await fetchUserLifecycleContext(21, signal)
    await transferEmployee(21, {
      employeeVersion: 1,
      organizationUnitId: 2,
      positionId: 3,
      managerEmployeeId: 4,
      effectiveFrom: '2026-07-12T01:00:00.000Z',
      reason: '跨部门调岗',
    })
    await precheckDeparture(21, { employeeVersion: 2, reason: '员工离职' })
    await startDeparture(21, {
      employeeVersion: 2,
      snapshotToken: 'opaque-snapshot',
      reason: '员工离职',
    })
    await confirmDepartureHandover(21, {
      employeeVersion: 3,
      snapshotToken: 'opaque-snapshot-2',
      transfers: [{ resourceType: 'CLUE', targetEmployeeId: 8 }],
      reason: '离职交接',
    })
    await completeDeparture(21, {
      employeeVersion: 4,
      snapshotToken: 'opaque-snapshot-3',
      reason: '交接核对完成',
    })
    await rehireEmployee(21, {
      employeeVersion: 5,
      organizationUnitId: 2,
      positionId: 3,
      managerEmployeeId: 4,
      effectiveFrom: '2026-08-01T01:00:00.000Z',
      accountActivationMode: 'INVITE',
      reason: '重新入职',
    })

    expect(
      mockedAxios.request.mock.calls.map(([config]) => `${config.method} ${config.url}`),
    ).toEqual([
      'get /api/users/21/lifecycle',
      'post /api/users/21/lifecycle/transfer',
      'post /api/users/21/lifecycle/departure/precheck',
      'post /api/users/21/lifecycle/departure/start',
      'post /api/users/21/lifecycle/departure/handover',
      'post /api/users/21/lifecycle/departure/complete',
      'post /api/users/21/lifecycle/rehire',
    ])
    expect(mockedAxios.request.mock.calls[4]?.[0]?.data).toEqual({
      employeeVersion: 3,
      snapshotToken: 'opaque-snapshot-2',
      transfers: [{ resourceType: 'CLUE', targetEmployeeId: 8 }],
      reason: '离职交接',
    })
    expect(mockedAxios.request.mock.calls[4]?.[0]?.data.transfers).not.toEqual(
      expect.arrayContaining([
        expect.objectContaining({ resourceType: 'QUOTE' }),
        expect.objectContaining({ resourceType: 'TRAN' }),
      ]),
    )
    for (const call of mockedAxios.request.mock.calls.slice(1)) {
      expect(call[0].data).toHaveProperty('employeeVersion')
      expect(call[0].data).not.toHaveProperty('profileVersion')
      expect(call[0].data).not.toHaveProperty('accountVersion')
      expect(call[0].data).not.toHaveProperty('authorizationVersion')
    }
    expect(mockedAxios.request.mock.calls[3]?.[0]?.url).not.toContain('opaque-snapshot')
    expect(mockedAxios.request.mock.calls[4]?.[0]?.url).not.toContain('opaque-snapshot')
    expect(mockedAxios.request.mock.calls[5]?.[0]?.url).not.toContain('opaque-snapshot')
    expect(mockedAxios.request.mock.calls[6]?.[0]?.data).not.toHaveProperty('password')
    expect(mockedAxios.request.mock.calls[6]?.[0]?.data).not.toHaveProperty('roleIds')
    expect(mockedAxios.request.mock.calls[6]?.[0]?.data).not.toHaveProperty('permissionIds')
  })
})
