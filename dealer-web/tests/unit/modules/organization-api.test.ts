import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createOrganizationUnit,
  createPosition,
  disableOrganizationUnit,
  fetchActingManagerCandidates,
  fetchActingReportings,
  fetchEmployeeOrganizationMembership,
  fetchManagerCandidates,
  fetchOrganizationEmployees,
  fetchOrganizationLeaderCandidates,
  fetchOrganizationParentCandidates,
  fetchOrganizationTree,
  replaceActingReportings,
  updateEmployeeOrganizationMembership,
  updateOrganizationUnit,
  updatePosition,
} from '@/modules/organization/api/organization-api'

const mockedAxios = vi.mocked(axios)

describe('organization api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('queries the organization tree and scoped employees', async () => {
    await fetchOrganizationTree()
    await fetchOrganizationEmployees(12)

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/organization-units/tree',
      signal: undefined,
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'get',
      url: '/api/organization-units/12/employees',
      signal: undefined,
    })
  })

  it('creates and updates organization units with an explicit version', async () => {
    await createOrganizationUnit({
      code: 'STORE_SH',
      name: '上海门店',
      type: 'STORE',
      parentId: 1,
      orderNo: 10,
    })
    await updateOrganizationUnit(12, {
      name: '上海中心门店',
      type: 'STORE',
      parentId: 1,
      orderNo: 10,
      expectedVersion: 3,
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toMatchObject({
      method: 'post',
      url: '/api/organization-units',
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'put',
      url: '/api/organization-units/12',
      data: expect.objectContaining({ expectedVersion: 3 }),
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]?.data).not.toHaveProperty('code')
  })

  it('changes organization status with a reason and expected version', async () => {
    await disableOrganizationUnit(12, { expectedVersion: 4, reason: '门店整合' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/organization-units/12/disable',
      data: { expectedVersion: 4, reason: '门店整合' },
    })
  })

  it('uses separate position resources instead of permission roles', async () => {
    await createPosition({
      code: 'SALES_SUPERVISOR',
      name: '销售主管',
      description: '负责销售团队管理',
      positionLevel: 30,
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/positions',
      data: {
        code: 'SALES_SUPERVISOR',
        name: '销售主管',
        description: '负责销售团队管理',
        positionLevel: 30,
      },
    })
  })

  it('keeps the position code out of update requests', async () => {
    await updatePosition(8, {
      name: '高级销售主管',
      description: '负责多个销售团队',
      positionLevel: 40,
      expectedVersion: 2,
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/positions/8',
      data: {
        name: '高级销售主管',
        description: '负责多个销售团队',
        positionLevel: 40,
        expectedVersion: 2,
      },
    })
  })

  it('loads manager candidates from the target employee scope', async () => {
    await fetchEmployeeOrganizationMembership(21)
    await fetchManagerCandidates(21)

    expect(mockedAxios.request.mock.calls.map(([config]) => config.url)).toEqual([
      '/api/employees/21/organization-membership',
      '/api/employees/21/manager-candidates',
    ])
  })

  it('loads server-filtered leader and parent candidates', async () => {
    await fetchOrganizationLeaderCandidates({ organizationUnitId: 12 })
    await fetchOrganizationParentCandidates({ type: 'STORE', excludeId: 12 })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/organization-units/leader-candidates',
      params: { organizationUnitId: 12 },
      signal: undefined,
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'get',
      url: '/api/organization-units/parent-candidates',
      params: { type: 'STORE', excludeId: 12 },
      signal: undefined,
    })
  })

  it('updates assignments and reporting in one versioned command', async () => {
    await updateEmployeeOrganizationMembership(21, {
      expectedVersion: 5,
      primaryAssignment: {
        organizationUnitId: 12,
        positionId: 8,
        assignmentType: 'PRIMARY',
        effectiveFrom: '2026-07-12T01:00:00.000Z',
      },
      additionalAssignments: [],
      reporting: {
        managerEmployeeId: 7,
        relationType: 'DIRECT',
        effectiveFrom: '2026-07-12T01:00:00.000Z',
      },
      reason: '正式调岗',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/employees/21/organization-membership',
      data: expect.objectContaining({
        expectedVersion: 5,
        reason: '正式调岗',
      }),
    })
  })

  it('uses dedicated ACTING collection, candidate, and versioned replace resources', async () => {
    const firstController = new AbortController()
    const secondController = new AbortController()

    await fetchActingReportings(21, firstController.signal)
    await fetchActingManagerCandidates(21, secondController.signal)
    await replaceActingReportings(21, {
      expectedEmployeeVersion: 5,
      relations: [
        {
          managerEmployeeId: 7,
          effectiveTo: '2026-08-20T10:15:37.000Z',
        },
      ],
      reason: '主管休假期间代理',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/employees/21/acting-reporting-relations',
      signal: firstController.signal,
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'get',
      url: '/api/employees/21/acting-reporting-relations/manager-candidates',
      signal: secondController.signal,
    })
    expect(mockedAxios.request.mock.calls[2]?.[0]).toEqual({
      method: 'put',
      url: '/api/employees/21/acting-reporting-relations',
      data: {
        expectedEmployeeVersion: 5,
        relations: [
          {
            managerEmployeeId: 7,
            effectiveTo: '2026-08-20T10:15:37.000Z',
          },
        ],
        reason: '主管休假期间代理',
      },
    })
  })
})
