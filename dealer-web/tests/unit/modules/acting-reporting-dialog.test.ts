import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ActingReportingDialog from '@/modules/organization/components/ActingReportingDialog.vue'
import type {
  ActingReportingCollection,
  EmployeeSummary,
  ManagerCandidate,
} from '@/modules/organization/model/organization.types'

const employee: EmployeeSummary = {
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
  managerEmployeeName: '王直属主管',
  version: 5,
  allowedActions: ['reporting'],
  unavailableReasons: {},
}

const candidates: ManagerCandidate[] = [
  {
    employeeId: 7,
    employeeNo: 'E00007',
    name: '王代理主管',
    organizationUnitName: '示例汽车集团',
    positionName: '销售主管',
  },
  {
    employeeId: 8,
    employeeNo: 'E00008',
    name: '赵代理主管',
    organizationUnitName: '示例汽车集团',
    positionName: '门店经理',
  },
]

describe('ActingReportingDialog', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-13T00:00:00.000Z'))
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
  })

  it('preserves multiple ACTING relations and second precision in the replace command', async () => {
    const collection = actingCollection([
      {
        id: 101,
        version: 1,
        managerEmployeeId: 7,
        managerEmployeeNo: 'E00007',
        managerEmployeeName: '王代理主管',
        status: 'ACTIVE',
        effectiveFrom: '2026-07-13T00:00:00.000Z',
        effectiveTo: '2026-08-20T10:15:37.000Z',
      },
      {
        id: 102,
        version: 2,
        managerEmployeeId: 8,
        managerEmployeeNo: 'E00008',
        managerEmployeeName: '赵代理主管',
        status: 'ACTIVE',
        effectiveFrom: '2026-07-13T00:00:00.000Z',
        effectiveTo: '2026-09-21T03:04:29.000Z',
      },
    ])
    const { emitted } = renderDialog(collection)

    const firstEnd = (await screen.findByLabelText('代理结束时间 1')) as HTMLInputElement
    const secondEnd = screen.getByLabelText('代理结束时间 2') as HTMLInputElement
    expect(firstEnd.value).toMatch(/:37$/)
    expect(secondEnd.value).toMatch(/:29$/)

    await fireEvent.update(screen.getByLabelText('调整原因'), '主管休假期间代理')
    await fireEvent.click(screen.getByRole('button', { name: '保存代理关系' }))

    expect(emitted().submit?.[0]).toEqual([
      {
        expectedEmployeeVersion: 5,
        relations: [
          { managerEmployeeId: '7', effectiveTo: '2026-08-20T10:15:37.000Z' },
          { managerEmployeeId: '8', effectiveTo: '2026-09-21T03:04:29.000Z' },
        ],
        reason: '主管休假期间代理',
      },
    ])
  })

  it('submits an empty ACTING collection without exposing a DIRECT relation field', async () => {
    const { emitted } = renderDialog(actingCollection([]))

    expect(await screen.findByText('当前没有代理主管')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('调整原因'), '代理期结束')
    await fireEvent.click(screen.getByRole('button', { name: '保存代理关系' }))

    const request = emitted().submit?.[0]?.[0]
    expect(request).toEqual({
      expectedEmployeeVersion: 5,
      relations: [],
      reason: '代理期结束',
    })
    expect(request).not.toHaveProperty('reporting')
    expect(request).not.toHaveProperty('managerEmployeeId')
  })

  it('rejects duplicate ACTING managers before emitting the command', async () => {
    const { emitted } = renderDialog(
      actingCollection([
        {
          id: 101,
          version: 1,
          managerEmployeeId: 7,
          managerEmployeeNo: 'E00007',
          managerEmployeeName: '王代理主管',
          status: 'ACTIVE',
          effectiveFrom: '2026-07-13T00:00:00.000Z',
          effectiveTo: '2026-08-20T10:15:37.000Z',
        },
      ]),
    )

    await fireEvent.click(await screen.findByRole('button', { name: '添加代理主管' }))
    await fireEvent.update(screen.getByLabelText('代理主管 2'), '7')
    await fireEvent.update(screen.getByLabelText('代理结束时间 2'), '2026-08-21T18:16:38')
    await fireEvent.update(screen.getByLabelText('调整原因'), '重复候选校验')
    await fireEvent.click(screen.getByRole('button', { name: '保存代理关系' }))

    expect(await screen.findByText('同一代理管理者不能重复')).toBeTruthy()
    expect(emitted().submit).toBeUndefined()
  })

  it('keeps the command disabled when the server omits the UPDATE action', async () => {
    const collection = actingCollection([])
    collection.allowedActions = []
    renderDialog(collection)

    const save = await screen.findByRole('button', { name: '保存代理关系' })
    expect((save as HTMLButtonElement).disabled).toBe(true)
  })
})

function renderDialog(collection: ActingReportingCollection) {
  return render(ActingReportingDialog, {
    props: {
      open: true,
      employee,
      collection,
      candidates,
      loading: false,
      loadError: '',
      submitting: false,
    },
  })
}

function actingCollection(
  relations: ActingReportingCollection['relations'],
): ActingReportingCollection {
  return {
    employeeId: 21,
    employeeVersion: 5,
    relations,
    allowedActions: ['UPDATE'],
    unavailableReasons: {},
  }
}
