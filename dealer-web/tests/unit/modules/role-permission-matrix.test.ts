import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { describe, expect, it, vi } from 'vitest'

import PermissionCatalog from '@/modules/access/components/PermissionCatalog.vue'
import RoleFormDialog from '@/modules/access/components/RoleFormDialog.vue'
import RoleList from '@/modules/access/components/RoleList.vue'
import RolePermissionMatrix from '@/modules/access/components/RolePermissionMatrix.vue'
import type {
  PermissionCatalogItem,
  RoleDetail,
  RolePermissionMatrix as RolePermissionMatrixData,
  RolePermissionPreview,
} from '@/modules/access/model/access.types'

const catalog: PermissionCatalogItem[] = [
  {
    id: 1,
    name: '用户管理',
    code: 'menu:user',
    module: 'user',
    type: 'menu',
    description: '用户管理菜单',
    sensitivityLevel: 'NORMAL',
    delegable: true,
    enabled: true,
    assignable: true,
    children: [
      {
        id: 2,
        parentId: 1,
        name: '编辑用户',
        code: 'user:edit',
        module: 'user',
        type: 'button',
        description: '编辑下属资料',
        sensitivityLevel: 'NORMAL',
        delegable: true,
        enabled: true,
        assignable: true,
        children: [],
      },
      {
        id: 3,
        parentId: 1,
        name: '分配角色',
        code: 'user:role',
        module: 'user',
        type: 'button',
        description: '修改用户角色',
        sensitivityLevel: 'PROTECTED',
        delegable: false,
        enabled: true,
        assignable: false,
        restrictionReason: '受保护权限只能由安全管理员维护',
        children: [],
      },
    ],
  },
]

const matrix: RolePermissionMatrixData = {
  roleId: 8,
  roleName: '销售主管',
  expectedVersion: 4,
  selectedPermissionIds: [1],
  permissionScopes: [{ permissionId: 1, dataScopeCode: 'SELF', organizationUnitIds: [] }],
  permissionScopeOptions: [
    {
      permissionId: 1,
      editable: true,
      dataScopeCandidates: [{ code: 'SELF', label: '本人', organizationOptions: [] }],
    },
    {
      permissionId: 2,
      editable: true,
      dataScopeCandidates: [{ code: 'SELF', label: '本人', organizationOptions: [] }],
    },
  ],
  editable: true,
}

const preview: RolePermissionPreview = {
  roleId: 8,
  expectedVersion: 4,
  addedPermissions: [
    {
      permissionId: 2,
      code: 'user:edit',
      name: '编辑用户',
      sensitivityLevel: 'NORMAL',
    },
  ],
  removedPermissions: [],
  affectedUserCount: 5,
  affectedOrganizationCount: 1,
  sessionRevocationCount: 5,
  warnings: [],
  scopeDifferences: [],
}

describe('role permission matrix', () => {
  it('previews a changed permission set before allowing save', async () => {
    const onPreview = vi.fn()
    const onSave = vi.fn()
    const view = render(RolePermissionMatrix, {
      props: { catalog, matrix, onPreview, onSave },
    })

    await fireEvent.click(screen.getByRole('checkbox', { name: '选择权限编辑用户' }))
    await fireEvent.click(screen.getByRole('button', { name: '预览影响' }))

    expect(onPreview).toHaveBeenCalledWith({
      expectedVersion: 4,
      permissionIds: expect.arrayContaining([1, 2]),
      permissionScopes: [
        { permissionId: 1, dataScopeCode: 'SELF', organizationUnitIds: [] },
        { permissionId: 2, dataScopeCode: 'SELF', organizationUnitIds: [] },
      ],
    })
    expect((screen.getByRole('button', { name: '保存矩阵' }) as HTMLButtonElement).disabled).toBe(
      true,
    )

    await view.rerender({ catalog, matrix, preview, onPreview, onSave })
    expect(screen.getByText('影响 5 名用户')).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('变更原因'), '补充下属资料编辑能力')

    await waitFor(() =>
      expect((screen.getByRole('button', { name: '保存矩阵' }) as HTMLButtonElement).disabled).toBe(
        false,
      ),
    )
    await fireEvent.click(screen.getByRole('button', { name: '保存矩阵' }))

    expect(onSave).toHaveBeenCalledWith({
      expectedVersion: 4,
      permissionIds: expect.arrayContaining([1, 2]),
      permissionScopes: [
        { permissionId: 1, dataScopeCode: 'SELF', organizationUnitIds: [] },
        { permissionId: 2, dataScopeCode: 'SELF', organizationUnitIds: [] },
      ],
      reason: '补充下属资料编辑能力',
    })
  })

  it('edits CUSTOM_ORGS per permission without using role applicable organizations', async () => {
    const scopedMatrix: RolePermissionMatrixData = {
      ...matrix,
      permissionScopes: [
        { permissionId: 1, dataScopeCode: 'SELF', organizationUnitIds: [] },
      ],
      permissionScopeOptions: [
        {
          permissionId: 1,
          editable: true,
          dataScopeCandidates: [
            { code: 'SELF', label: '本人', organizationOptions: [] },
            {
              code: 'CUSTOM_ORGS',
              label: '指定组织',
              organizationOptions: [
                { id: 2, name: '上海门店' },
                { id: 3, name: '杭州门店' },
              ],
            },
          ],
        },
      ],
    }
    const scopedPreview: RolePermissionPreview = {
      ...preview,
      addedPermissions: [],
      scopeDifferences: [
        {
          permissionId: 1,
          permissionCode: 'menu:user',
          permissionName: '用户管理',
          beforeDataScopeCode: 'SELF',
          beforeOrganizationNames: [],
          afterDataScopeCode: 'CUSTOM_ORGS',
          afterOrganizationNames: ['上海门店'],
        },
      ],
    }
    const onPreview = vi.fn()
    const onSave = vi.fn()
    const view = render(RolePermissionMatrix, {
      props: { catalog, matrix: scopedMatrix, onPreview, onSave },
    })

    await fireEvent.update(screen.getByLabelText('用户管理数据范围'), 'CUSTOM_ORGS')
    expect((screen.getByRole('button', { name: '预览影响' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    await fireEvent.click(screen.getByText('上海门店'))
    await fireEvent.click(screen.getByRole('button', { name: '预览影响' }))

    expect(onPreview).toHaveBeenCalledWith({
      expectedVersion: 4,
      permissionIds: [1],
      permissionScopes: [
        { permissionId: 1, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [2] },
      ],
    })
    await view.rerender({
      catalog,
      matrix: scopedMatrix,
      preview: scopedPreview,
      onPreview,
      onSave,
    })
    expect(screen.getByText(/用户管理：本人/)).toBeTruthy()
    await fireEvent.update(screen.getByLabelText('变更原因'), '限定该权限的数据组织')
    await fireEvent.click(screen.getByRole('button', { name: '保存矩阵' }))
    expect(onSave).toHaveBeenCalledWith({
      expectedVersion: 4,
      permissionIds: [1],
      permissionScopes: [
        { permissionId: 1, dataScopeCode: 'CUSTOM_ORGS', organizationUnitIds: [2] },
      ],
      reason: '限定该权限的数据组织',
    })
  })

  it('shows why a protected permission cannot be selected', () => {
    render(RolePermissionMatrix, { props: { catalog, matrix } })

    expect(
      (screen.getByRole('checkbox', { name: '选择权限分配角色' }) as HTMLButtonElement).disabled,
    ).toBe(true)
    expect(screen.getByText('受保护权限只能由安全管理员维护')).toBeTruthy()
  })

  it('keeps a protected role matrix fully read-only', () => {
    render(RolePermissionMatrix, {
      props: {
        catalog,
        matrix: {
          ...matrix,
          editable: false,
          disabledReason: '系统恢复角色不能由普通管理员削弱',
        },
      },
    })

    expect(screen.getByText('系统恢复角色不能由普通管理员削弱')).toBeTruthy()
    expect(
      (screen.getByRole('checkbox', { name: '选择权限编辑用户' }) as HTMLButtonElement).disabled,
    ).toBe(true)
  })

  it('renders the permission catalog without mutation controls', () => {
    render(PermissionCatalog, { props: { nodes: catalog } })

    expect(screen.getByText('用户管理菜单')).toBeTruthy()
    expect(screen.getByText('不可由普通管理者委派')).toBeTruthy()
    expect(screen.queryByRole('checkbox')).toBeNull()
    expect(screen.queryByRole('button', { name: /新增|编辑|删除/ })).toBeNull()
  })

  it('keeps an existing role code disabled in the edit form', async () => {
    const role: RoleDetail = {
      id: 8,
      code: 'sales_manager',
      name: '销售主管',
      protectedRole: false,
      authorizationLevel: 30,
      defaultDataScope: 'REPORTING_TREE',
      scopeType: 'ORGANIZATION',
      applicableOrganizations: [{ id: 2, name: '上海门店' }],
      memberCount: 5,
      enabled: true,
      version: 4,
      editable: true,
      allowedActions: ['EDIT', 'COPY', 'STATUS_CHANGE'],
      unavailableReasons: {},
    }
    render(RoleFormDialog, {
      props: {
        open: true,
        role,
        organizationOptions: [{ id: 2, name: '上海门店' }],
      },
    })

    const codeInput = await screen.findByLabelText('角色编码')
    expect((codeInput as HTMLInputElement).disabled).toBe(true)
  })

  it('keeps role actions read-only when the server does not allow them', () => {
    const baseRole: RoleDetail = {
      id: 8,
      code: 'sales_manager',
      name: '销售主管',
      protectedRole: false,
      authorizationLevel: 30,
      defaultDataScope: 'REPORTING_TREE',
      scopeType: 'ORGANIZATION',
      applicableOrganizations: [],
      memberCount: 5,
      enabled: true,
      version: 4,
      editable: true,
      allowedActions: ['EDIT'],
      unavailableReasons: {
        COPY: '来源角色不能复制',
        STATUS_CHANGE: '角色仍有受保护成员',
      },
    }
    render(RoleList, {
      props: {
        roles: [
          baseRole,
          {
            ...baseRole,
            id: 9,
            code: 'protected_manager',
            name: '不可编辑角色',
            editable: false,
            allowedActions: ['COPY'],
          },
        ],
        canCopy: true,
      },
    })

    const copyButtons = screen.getAllByRole('button', { name: '复制' }) as HTMLButtonElement[]
    expect(copyButtons).toHaveLength(2)
    expect(copyButtons.every((button) => button.disabled)).toBe(true)
    expect(copyButtons[0]?.title).toBe('来源角色不能复制')
  })

  it('explains copy scope semantics and disambiguates same-name organizations by path', async () => {
    const source: RoleDetail = {
      id: 8,
      code: 'sales_manager',
      name: '销售主管',
      protectedRole: false,
      authorizationLevel: 30,
      defaultDataScope: 'REPORTING_TREE',
      scopeType: 'ORGANIZATION',
      applicableOrganizations: [{ id: 2, name: '销售部', pathName: '集团 / 上海 / 销售部' }],
      memberCount: 5,
      enabled: true,
      version: 4,
      editable: true,
      allowedActions: ['COPY'],
      unavailableReasons: {},
    }
    render(RoleFormDialog, {
      props: {
        open: true,
        copySource: source,
        organizationOptions: [
          { id: 2, name: '销售部', pathName: '集团 / 上海 / 销售部' },
          { id: 3, name: '销售部', pathName: '集团 / 杭州 / 销售部' },
        ],
      },
    })

    expect(
      await screen.findByText(/默认数据范围只用于以后新增权限，修改它不会批量改写已复制范围/),
    ).toBeTruthy()
    expect(screen.getByText('集团 / 上海 / 销售部')).toBeTruthy()
    expect(screen.getByText('集团 / 杭州 / 销售部')).toBeTruthy()
  })
})
