import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import UserPermissionEditor from '@/modules/access/components/UserPermissionEditor.vue'
import UserRoleAssignment from '@/modules/access/components/UserRoleAssignment.vue'
import type {
  UserPermissionAuthorizationItem,
  UserRoleAssignmentItem,
  UserRoleCandidate,
} from '@/modules/access/model/user-permission.types'

const assignments: UserRoleAssignmentItem[] = [
  {
    roleId: 8,
    roleCode: 'sales_manager',
    roleName: '销售主管',
    source: 'DIRECT',
    sourceDescription: '由上级主管分配',
  },
]

const candidates: UserRoleCandidate[] = [
  {
    roleId: 8,
    roleCode: 'sales_manager',
    roleName: '销售主管',
    authorizationLevel: 30,
    defaultDataScope: 'REPORTING_TREE',
    selected: true,
    editable: true,
  },
  {
    roleId: 1,
    roleCode: 'system_admin',
    roleName: '系统管理员',
    authorizationLevel: 100,
    defaultDataScope: 'GLOBAL',
    selected: false,
    editable: false,
    unavailableReason: '超过当前操作者的授权上限',
  },
]

const permissions: UserPermissionAuthorizationItem[] = [
  {
    permissionId: 21,
    code: 'customer:export',
    name: '导出客户',
    module: 'customer',
    description: '导出职责范围内的客户',
    sensitivityLevel: 'SENSITIVE',
    delegable: true,
    effective: true,
    personalState: 'INHERIT',
    editable: true,
    sources: [
      {
        type: 'ROLE',
        sourceId: 8,
        sourceName: '销售主管',
        dataScopeLabel: '完整汇报树',
        active: true,
      },
    ],
    dataScopeCandidates: [
      {
        candidateKey: 'reporting-tree',
        code: 'REPORTING_TREE',
        label: '完整汇报树',
      },
    ],
  },
]

afterEach(() => vi.useRealTimers())

describe('user authorization editors', () => {
  it('shows role sources and only server-provided role candidates', () => {
    render(UserRoleAssignment, {
      props: {
        assignments,
        candidates,
        authorizationVersion: 4,
        editable: true,
      },
    })

    expect(screen.getByText('直接分配')).toBeTruthy()
    expect(screen.getByText('由上级主管分配')).toBeTruthy()
    expect(
      (screen.getByRole('checkbox', { name: '分配角色系统管理员' }) as HTMLButtonElement).disabled,
    ).toBe(true)
    expect(screen.getByText('超过当前操作者的授权上限')).toBeTruthy()
  })

  it('submits an atomic role replacement with difference, reason and expected version', async () => {
    const onSave = vi.fn()
    render(UserRoleAssignment, {
      props: {
        assignments,
        candidates,
        authorizationVersion: 4,
        editable: true,
        onSave,
      },
    })

    await fireEvent.click(screen.getByRole('checkbox', { name: '分配角色销售主管' }))
    expect(screen.getByText('移除：销售主管')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('调整原因'), '下属岗位调整')
    await fireEvent.click(screen.getByRole('button', { name: '保存角色调整' }))

    expect(onSave).toHaveBeenCalledWith({
      authorizationVersion: 4,
      roleIds: [],
      reason: '下属岗位调整',
    })
  })

  it('uses explicit INHERIT, GRANT and DENY radios instead of a binary switch', async () => {
    render(UserPermissionEditor, {
      props: { permissions, authorizationVersion: 7, editable: true },
    })

    expect(screen.getByRole('radio', { name: '继承角色' })).toBeTruthy()
    expect(screen.getByRole('radio', { name: '个人增加' })).toBeTruthy()
    expect(screen.getByRole('radio', { name: '个人拒绝' })).toBeTruthy()
    expect(screen.queryByRole('switch')).toBeNull()
    expect(screen.getByText('角色来源')).toBeTruthy()
    expect(screen.getByText('完整汇报树')).toBeTruthy()

    await fireEvent.click(screen.getByRole('radio', { name: '个人增加' }))
    expect(screen.getByLabelText('数据范围')).toBeTruthy()
    expect(screen.getByRole('checkbox', { name: '立即生效' })).toBeTruthy()
    expect(screen.getByLabelText('预约生效时间')).toBeTruthy()
    expect(screen.getByLabelText('失效时间（可选）')).toBeTruthy()
  })

  it('submits a personal deny with validity, reason and expected version', async () => {
    const onSave = vi.fn()
    render(UserPermissionEditor, {
      props: { permissions, authorizationVersion: 7, editable: true, onSave },
    })

    await fireEvent.click(screen.getByRole('radio', { name: '个人拒绝' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '临时暂停客户导出')
    await waitFor(() =>
      expect(
        (screen.getByRole('button', { name: '保存个人权限' }) as HTMLButtonElement).disabled,
      ).toBe(false),
    )
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))

    expect(onSave).toHaveBeenCalledWith({
      authorizationVersion: 7,
      changes: [expect.objectContaining({ permissionId: 21, state: 'DENY' })],
      reason: '临时暂停客户导出',
    })
    expect(onSave.mock.calls[0]?.[0].changes[0]).not.toHaveProperty('dataScopeCandidateKey')
    expect(onSave.mock.calls[0]?.[0].changes[0]).not.toHaveProperty('customOrganizationUnitIds')
    expect(onSave.mock.calls[0]?.[0].changes[0]).not.toHaveProperty('effectiveFrom')
  })

  it('submits an explicit future schedule and displays a future source as pending', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-12T08:00:00Z'))
    const onSave = vi.fn()
    const scheduledPermission: UserPermissionAuthorizationItem = {
      ...permissions[0]!,
      effective: false,
      sources: [
        {
          type: 'PERSONAL_GRANT',
          sourceId: 91,
          sourceName: '个人增加',
          dataScopeLabel: 'SELF',
          effectiveFrom: '2026-07-20T01:00:00Z',
          effectiveTo: '2026-08-20T01:00:00Z',
          active: false,
        },
      ],
      dataScopeCandidates: [{ candidateKey: 'SELF', code: 'SELF', label: '本人' }],
    }
    render(UserPermissionEditor, {
      props: {
        permissions: [scheduledPermission],
        authorizationVersion: 7,
        editable: true,
        onSave,
      },
    })

    expect(screen.getByText(/计划于/)).toBeTruthy()
    expect(screen.getByText('待生效')).toBeTruthy()
    await fireEvent.click(screen.getByRole('radio', { name: '个人增加' }))
    await fireEvent.update(screen.getByLabelText('数据范围'), 'SELF')
    await fireEvent.click(screen.getByRole('checkbox', { name: '立即生效' }))
    await fireEvent.update(screen.getByLabelText('预约生效时间'), '2026-08-01T09:00')
    await fireEvent.update(screen.getByLabelText('失效时间（可选）'), '2026-08-10T09:00')
    await fireEvent.update(screen.getByLabelText('调整原因'), '预约支援权限')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))

    expect(onSave.mock.calls[0]?.[0].changes[0]).toEqual({
      permissionId: 21,
      state: 'GRANT',
      dataScopeCandidateKey: 'SELF',
      effectiveFrom: new Date('2026-08-01T09:00').toISOString(),
      effectiveTo: new Date('2026-08-10T09:00').toISOString(),
    })
  })

  it('blocks past, over-one-year and non-positive permission schedules', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-12T08:00:00Z'))
    render(UserPermissionEditor, {
      props: {
        permissions: [
          {
            ...permissions[0]!,
            dataScopeCandidates: [{ candidateKey: 'SELF', code: 'SELF', label: '本人' }],
          },
        ],
        authorizationVersion: 7,
        editable: true,
      },
    })
    await fireEvent.click(screen.getByRole('radio', { name: '个人增加' }))
    await fireEvent.update(screen.getByLabelText('数据范围'), 'SELF')
    await fireEvent.click(screen.getByRole('checkbox', { name: '立即生效' }))

    await fireEvent.update(screen.getByLabelText('预约生效时间'), '2026-07-11T09:00')
    expect(screen.getByText('生效时间不能早于当前时间')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('预约生效时间'), '2027-07-13T09:00')
    expect(screen.getByText('预约生效时间不能超过一年')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('预约生效时间'), '2026-08-01T09:00')
    await fireEvent.update(screen.getByLabelText('失效时间（可选）'), '2026-08-01T09:00')
    expect(screen.getByText('失效时间必须晚于生效时间')).toBeTruthy()
  })

  it('requires server-delegable organizations for a CUSTOM_ORGS personal grant', async () => {
    const onSave = vi.fn()
    const customPermission: UserPermissionAuthorizationItem = {
      ...permissions[0]!,
      sources: [
        {
          type: 'ROLE',
          sourceId: 8,
          sourceName: '销售主管',
          dataScopeLabel: '指定组织',
          organizationIds: [2],
          organizationNames: ['上海门店'],
          active: true,
        },
      ],
      dataScopeCandidates: [
        { candidateKey: 'SELF', code: 'SELF', label: '本人' },
        {
          candidateKey: 'CUSTOM_ORGS:2,3',
          code: 'CUSTOM_ORGS',
          label: '指定组织',
          organizationIds: [2, 3],
          organizationNames: ['上海门店', '杭州门店'],
        },
      ],
    }
    render(UserPermissionEditor, {
      props: { permissions: [customPermission], authorizationVersion: 7, editable: true, onSave },
    })

    expect(screen.getByText('组织：上海门店')).toBeTruthy()
    await fireEvent.click(screen.getByRole('radio', { name: '个人增加' }))
    await fireEvent.update(screen.getByLabelText('数据范围'), 'CUSTOM_ORGS')
    expect(screen.getByText('指定组织范围必须至少选择一个可委派组织')).toBeTruthy()
    await fireEvent.click(screen.getByText('上海门店', { selector: 'label' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '临时支援上海门店')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))

    expect(onSave).toHaveBeenCalledWith({
      authorizationVersion: 7,
      changes: [
        expect.objectContaining({
          permissionId: 21,
          state: 'GRANT',
          dataScopeCandidateKey: 'CUSTOM_ORGS',
          customOrganizationUnitIds: [2],
        }),
      ],
      reason: '临时支援上海门店',
    })
  })

  it('clears custom organizations when switching to non-custom, DENY or INHERIT', async () => {
    const onSave = vi.fn()
    const customPermission: UserPermissionAuthorizationItem = {
      ...permissions[0]!,
      personalState: 'GRANT',
      personalDataScopeCandidateKey: 'CUSTOM_ORGS:2',
      personalOrganizationIds: [2],
      personalEffectiveFrom: '2026-07-12T00:00:00Z',
      dataScopeCandidates: [
        { candidateKey: 'SELF', code: 'SELF', label: '本人' },
        {
          candidateKey: 'CUSTOM_ORGS:2',
          code: 'CUSTOM_ORGS',
          label: '指定组织',
          organizationIds: [2],
          organizationNames: ['上海门店'],
        },
      ],
    }
    render(UserPermissionEditor, {
      props: { permissions: [customPermission], authorizationVersion: 8, editable: true, onSave },
    })
    await fireEvent.click(screen.getByRole('radio', { name: '个人拒绝' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '取消原指定组织授权')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))
    const change = onSave.mock.calls[0]?.[0].changes[0]
    expect(change.state).toBe('DENY')
    expect(change).not.toHaveProperty('dataScopeCandidateKey')
    expect(change).not.toHaveProperty('customOrganizationUnitIds')
  })

  it('never carries custom organizations with a non-CUSTOM grant', async () => {
    const onSave = vi.fn()
    const customPermission: UserPermissionAuthorizationItem = {
      ...permissions[0]!,
      personalState: 'GRANT',
      personalDataScopeCandidateKey: 'CUSTOM_ORGS:2',
      personalOrganizationIds: [2],
      personalEffectiveFrom: '2026-07-12T00:00:00Z',
      dataScopeCandidates: [
        { candidateKey: 'SELF', code: 'SELF', label: '本人' },
        {
          candidateKey: 'CUSTOM_ORGS:2',
          code: 'CUSTOM_ORGS',
          label: '指定组织',
          organizationIds: [2],
          organizationNames: ['上海门店'],
        },
      ],
    }
    render(UserPermissionEditor, {
      props: { permissions: [customPermission], authorizationVersion: 9, editable: true, onSave },
    })
    await fireEvent.update(screen.getByLabelText('数据范围'), 'SELF')
    await fireEvent.update(screen.getByLabelText('调整原因'), '收窄为本人范围')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))
    const change = onSave.mock.calls[0]?.[0].changes[0]
    expect(change.dataScopeCandidateKey).toBe('SELF')
    expect(change).not.toHaveProperty('customOrganizationUnitIds')
  })

  it('submits INHERIT as a pure cancellation without old scope, organizations or validity', async () => {
    const onSave = vi.fn()
    const customPermission: UserPermissionAuthorizationItem = {
      ...permissions[0]!,
      personalState: 'GRANT',
      personalDataScopeCandidateKey: 'CUSTOM_ORGS:2',
      personalOrganizationIds: [2],
      personalEffectiveFrom: '2026-07-12T00:00:00Z',
      personalEffectiveTo: '2026-08-12T00:00:00Z',
      dataScopeCandidates: [
        {
          candidateKey: 'CUSTOM_ORGS:2',
          code: 'CUSTOM_ORGS',
          label: '指定组织',
          organizationIds: [2],
          organizationNames: ['上海门店'],
        },
      ],
    }
    render(UserPermissionEditor, {
      props: { permissions: [customPermission], authorizationVersion: 10, editable: true, onSave },
    })
    await fireEvent.click(screen.getByRole('radio', { name: '继承角色' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '取消个人覆盖')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人权限' }))
    expect(onSave.mock.calls[0]?.[0].changes[0]).toEqual({ permissionId: 21, state: 'INHERIT' })
  })

  it('renders no self-authorization action when the server denies editability', () => {
    render(UserPermissionEditor, {
      props: {
        permissions,
        authorizationVersion: 7,
        editable: false,
        disabledReason: '任何用户都不能修改自己的权限',
      },
    })

    expect(screen.getByText('任何用户都不能修改自己的权限')).toBeTruthy()
    expect((screen.getByRole('radio', { name: '个人增加' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect(screen.queryByRole('button', { name: '保存个人权限' })).toBeNull()
  })
})
