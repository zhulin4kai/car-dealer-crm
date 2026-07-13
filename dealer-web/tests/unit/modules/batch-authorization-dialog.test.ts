import { fireEvent, render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import BatchAuthorizationDialog from '@/modules/access/components/BatchAuthorizationDialog.vue'
import type { UserAuthorizationDetail } from '@/modules/access/model/user-permission.types'

const details: UserAuthorizationDetail[] = [
  authorizationDetail(21, 5, '李销售'),
  authorizationDetail(22, 7, '王销售'),
]

describe('batch authorization dialog', () => {
  it('submits one role command with every target version', async () => {
    const { emitted } = render(BatchAuthorizationDialog, {
      props: { open: true, mode: 'roles', details },
    })

    await fireEvent.click(await screen.findByRole('checkbox', { name: '批量选择角色销售人员' }))
    await fireEvent.update(screen.getByLabelText('调整原因'), '批量补充销售角色')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    expect(emitted()['save-roles']?.[0]).toEqual([
      {
        targets: [
          { userId: 21, authorizationVersion: 5 },
          { userId: 22, authorizationVersion: 7 },
        ],
        operation: 'ASSIGN',
        roleIds: [8],
        reason: '批量补充销售角色',
      },
    ])
  })

  it('only exposes common editable permission and scope candidates', async () => {
    const second = authorizationDetail(22, 7, '王销售')
    second.permissions[0]!.dataScopeCandidates = [
      { candidateKey: 'SELF', code: 'SELF', label: '仅本人' },
    ]
    const { emitted } = render(BatchAuthorizationDialog, {
      props: { open: true, mode: 'permissions', details: [details[0]!, second] },
    })

    await fireEvent.update(await screen.findByLabelText('权限'), '31')
    await fireEvent.update(screen.getByLabelText('个人状态'), 'GRANT')
    expect(screen.getByRole('option', { name: '仅本人' })).toBeTruthy()
    expect(screen.queryByRole('option', { name: '本组织' })).toBeNull()
    await fireEvent.update(screen.getByLabelText('数据范围'), 'SELF')
    await fireEvent.update(screen.getByLabelText('调整原因'), '批量临时授权')
    await fireEvent.click(screen.getByRole('button', { name: '确认批量调整' }))

    expect(emitted()['save-permissions']?.[0]).toEqual([
      {
        targets: [
          { userId: 21, authorizationVersion: 5 },
          { userId: 22, authorizationVersion: 7 },
        ],
        changes: [
          {
            permissionId: 31,
            state: 'GRANT',
            dataScopeCandidateKey: 'SELF',
          },
        ],
        reason: '批量临时授权',
      },
    ])
  })
})

function authorizationDetail(
  userId: number,
  authorizationVersion: number,
  name: string,
): UserAuthorizationDetail {
  return {
    user: {
      id: userId,
      loginAct: `user${userId}`,
      name,
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
        module: 'customer',
        sensitivityLevel: 'NORMAL',
        delegable: true,
        effective: false,
        personalState: 'INHERIT',
        editable: true,
        sources: [],
        dataScopeCandidates: [
          { candidateKey: 'SELF', code: 'SELF', label: '仅本人' },
          { candidateKey: 'PRIMARY_ORG', code: 'PRIMARY_ORG', label: '本组织' },
        ],
      },
    ],
  }
}
