import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'

import UserFormDialog from '@/modules/user/components/UserFormDialog.vue'

const options = {
  organizations: [{ id: 1, label: '上海门店' }], positions: [{ id: 2, label: '销售顾问' }], managers: [{ id: 3, label: '销售经理' }], roles: [{ id: 1, label: '系统管理员' }, { id: 4, label: '销售人员' }], assignableRoles: [{ id: 4, label: '销售人员' }], employmentStatuses: [], accountStatuses: [], lockStatuses: [], bootstrapRequired: false, bootstrapAllowed: false, bootstrapRootOrganizationId: null, bootstrapRootOrganizationVersion: null,
}

describe('UserFormDialog', () => {
  afterEach(() => cleanup())
  it('creates a user from server candidates without accepting or emitting a password', async () => {
    const { emitted } = render(UserFormDialog, { props: { open: true, mode: 'create', options } })
    expect(await screen.findByLabelText('登录账号')).toBeTruthy()
    expect(screen.queryByLabelText(/密码/)).toBeNull()
    await fireEvent.update(screen.getByLabelText('登录账号'), ' sales01 ')
    await fireEvent.update(screen.getByLabelText('姓名'), ' 李销售 ')
    await fireEvent.update(screen.getByLabelText('员工编号'), ' E00021 ')
    await fireEvent.update(screen.getByLabelText('组织'), '1')
    await fireEvent.update(screen.getByLabelText('岗位'), '2')
    await fireEvent.update(screen.getByLabelText('直属管理者'), '3')
    await fireEvent.click(screen.getByText('销售人员'))
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))

    const request = emitted().create?.[0]?.[0] as Record<string, unknown>
    expect(request).toEqual(expect.objectContaining({ loginAct: 'sales01', employeeNo: 'E00021', organizationUnitId: '1', positionId: '2', managerEmployeeId: '3', roleIds: ['4'] }))
    expect(request).not.toHaveProperty('password')
    expect(request).not.toHaveProperty('loginPwd')
  })

  it('requires an explicit bootstrap choice and sends the root organization CAS version', async () => {
    const bootstrapOptions = {
      ...options,
      organizations: [{ id: 9, label: '集团公司' }],
      assignableRoles: [{ id: 1, label: '系统管理员' }],
      bootstrapRequired: true,
      bootstrapAllowed: true,
      bootstrapRootOrganizationId: 9,
      bootstrapRootOrganizationVersion: 4,
    }
    const { emitted } = render(UserFormDialog, { props: { open: true, mode: 'create', options: bootstrapOptions } })
    await fireEvent.click(await screen.findByText('初始化首个根公司负责人和普通管理员'))
    await fireEvent.update(screen.getByLabelText('登录账号'), 'leader01')
    await fireEvent.update(screen.getByLabelText('姓名'), '集团负责人')
    await fireEvent.update(screen.getByLabelText('员工编号'), 'E00001')
    await fireEvent.update(screen.getByLabelText('岗位'), '2')
    await fireEvent.click(screen.getByText('系统管理员'))
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(emitted().create?.[0]?.[0]).toEqual(expect.objectContaining({
      organizationUnitId: '9',
      managerEmployeeId: null,
      roleIds: ['1'],
      bootstrapRootLeader: true,
      expectedRootOrganizationVersion: 4,
    }))
  })

  it('uses assignableRoles only and never copies a protected list-filter role into create candidates', async () => {
    render(UserFormDialog, { props: { open: true, mode: 'create', options } })
    await screen.findByLabelText('登录账号')
    await fireEvent.update(screen.getByLabelText('组织'), '1')

    expect(screen.getByRole('checkbox', { name: '销售人员' })).toBeTruthy()
    expect(screen.queryByRole('checkbox', { name: '系统管理员' })).toBeNull()
  })

  it('edits only the profile whitelist with optimistic version', async () => {
    const user = { id: 21, loginAct: 'sales01', name: '李销售', phone: null, email: null, employmentStatus: 'ACTIVE', accountStatus: 'ENABLED', lockStatus: 'UNLOCKED', profileVersion: 7, accountVersion: 8, employeeVersion: 9, roleNames: [], statusCommands: [], allowedActions: ['PROFILE_UPDATE'], unavailableReasons: {} }
    const { emitted } = render(UserFormDialog, { props: { open: true, mode: 'edit', options, user } })
    await fireEvent.update(await screen.findByLabelText('姓名'), '李顾问')
    await fireEvent.update(screen.getByLabelText('手机'), '138 (0013)-8000')
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(emitted().update?.[0]?.[0]).toEqual({ profileVersion: 7, name: '李顾问', phone: '13800138000', email: null })
  })

  it('rejects names over 50 characters and malformed normalized phones', async () => {
    const user = { id: 21, loginAct: 'sales01', name: '李销售', phone: null, email: null, employmentStatus: 'ACTIVE', accountStatus: 'ENABLED', lockStatus: 'UNLOCKED', profileVersion: 7, accountVersion: 8, employeeVersion: 9, roleNames: [], statusCommands: [], allowedActions: ['PROFILE_UPDATE'], unavailableReasons: {} }
    const { emitted } = render(UserFormDialog, { props: { open: true, mode: 'edit', options, user } })

    await fireEvent.update(await screen.findByLabelText('姓名'), '超'.repeat(51))
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(await screen.findByText('姓名最多 50 个字符')).toBeTruthy()

    await fireEvent.update(screen.getByLabelText('姓名'), '李顾问')
    await fireEvent.update(screen.getByLabelText('手机'), '123-456')
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(await screen.findByText('手机号码格式有误')).toBeTruthy()
    expect(emitted().update).toBeUndefined()
  })
})
