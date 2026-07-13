import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const openapi = readFileSync(resolve(process.cwd(), '../docs/api/openapi.yaml'), 'utf8')

function schema(name: string): string {
  const marker = `    ${name}:\n`
  const start = openapi.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI schema missing: ${name}`)
  const rest = openapi.slice(start + marker.length)
  const next = rest.search(/\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
  return next < 0 ? rest : rest.slice(0, next)
}

function pathItem(path: string): string {
  const marker = `  ${path}:\n`
  const start = openapi.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI path missing: ${path}`)
  const rest = openapi.slice(start + marker.length)
  const next = rest.search(/\n {2}\/[^\n]+:\n/)
  return next < 0 ? rest : rest.slice(0, next)
}

describe('Task 11-14 OpenAPI contract', () => {
  it('uses authorizationVersion and explicit CUSTOM_ORGS organization ids', () => {
    const detail = schema('UserAuthorizationDetail')
    const roles = schema('UpdateUserRoleAssignmentsRequest')
    const permissionChange = schema('UserPermissionChangeInput')
    const permissions = schema('UpdateUserPermissionsRequest')

    expect(detail).toContain('authorizationVersion:')
    expect(detail).not.toContain('expectedVersion:')
    expect(roles).toContain('required: [authorizationVersion, roleIds, reason]')
    expect(permissions).toContain('required: [authorizationVersion, changes, reason]')
    expect(permissionChange).toContain('customOrganizationUnitIds:')
  })

  it('defines bounded all-or-nothing batch role and personal permission commands', () => {
    const rolePath = pathItem('/api/users/authorization/batch/roles')
    const permissionPath = pathItem('/api/users/authorization/batch/permissions')
    const target = schema('UserAuthorizationBatchTarget')
    const roles = schema('BatchUpdateUserRolesRequest')
    const permissions = schema('BatchUpdateUserPermissionsRequest')

    expect(rolePath).toContain('BatchUpdateUserRolesRequest')
    expect(rolePath).toContain('任一目标失败则整批回滚')
    expect(permissionPath).toContain('BatchUpdateUserPermissionsRequest')
    expect(permissionPath).toContain('任一目标失败则整批回滚')
    expect(target).toContain('required: [userId, authorizationVersion]')
    expect(roles).toContain('maxItems: 50')
    expect(roles).toContain('enum: [ASSIGN, UNASSIGN]')
    expect(permissions).toContain('maxItems: 50')
  })

  it('returns editable per-permission scope options and nullable scope differences', () => {
    const matrix = schema('RolePermissionMatrix')
    const scopeOption = schema('PermissionScopeOption')
    const scopeCandidate = schema('PermissionDataScopeCandidate')
    const difference = schema('PermissionScopeDifference')

    expect(matrix).toContain('permissionScopeOptions:')
    expect(scopeOption).toContain('dataScopeCandidates:')
    expect(scopeCandidate).toContain('organizationOptions:')
    expect(difference).toMatch(/beforeDataScopeCode:.*nullable:\s*true/)
    expect(difference).toMatch(/afterDataScopeCode:.*nullable:\s*true/)
  })

  it('keeps role actions server-authoritative', () => {
    const role = schema('RoleResponse')
    const required = role.split('\n').find((line) => line.includes('required:')) ?? ''

    expect(role).toContain('allowedActions:')
    expect(role).toContain('unavailableReasons:')
    expect(required).toContain('allowedActions')
    expect(required).toContain('unavailableReasons')
  })

  it('uses the complete employee lifecycle enum and nullable primary assignment', () => {
    const employee = schema('EmployeeSummaryResponse')
    const membership = schema('EmployeeOrganizationMembershipResponse')

    expect(employee).toContain('enum: [PENDING, ACTIVE, HANDOVER, LEFT]')
    expect(membership).toMatch(/primaryAssignment:.*nullable:\s*true/)
  })

  it('requires owner qualification context and returns a narrow candidate schema', () => {
    const ownerPath = pathItem('/api/owner')
    const owner = schema('OwnerCandidate')

    expect(ownerPath).toContain('name: permissionCode')
    expect(ownerPath).toContain('name: qualificationContext')
    expect(ownerPath).toContain(
      'enum: [ACTIVITY_OWNER, CLUE_OWNER, CUSTOMER_OWNER, TRANSACTION_OWNER]',
    )
    expect(ownerPath).toContain('#/components/schemas/OwnerCandidate')
    expect(owner).toContain('required: [userId, name]')
    expect(owner).toContain('organizationUnitId:')
    expect(owner).toContain('positionId:')
    expect(owner).not.toMatch(/loginPwd|permissionList|roleList|accountNoLocked/)
  })
})
