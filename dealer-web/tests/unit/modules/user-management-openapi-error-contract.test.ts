import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const openapi = readFileSync(resolve(process.cwd(), '../docs/api/openapi.yaml'), 'utf8')

type Method = 'get' | 'post' | 'put'

function pathItem(path: string): string {
  const marker = `  ${path}:\n`
  const start = openapi.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI path missing: ${path}`)
  const rest = openapi.slice(start + marker.length)
  const next = rest.search(/\n {2}\/[^\n]+:\n/)
  return next < 0 ? rest : rest.slice(0, next)
}

function schema(name: string): string {
  const marker = `    ${name}:\n`
  const start = openapi.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI schema missing: ${name}`)
  const rest = openapi.slice(start + marker.length)
  const next = rest.search(/\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
  return next < 0 ? rest : rest.slice(0, next)
}

function operation(path: string, method: Method): string {
  const item = pathItem(path)
  const marker = `    ${method}:\n`
  const start = item.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI operation missing: ${method.toUpperCase()} ${path}`)
  const rest = item.slice(start + marker.length)
  const next = rest.search(/\n {4}(?:get|post|put|patch|delete):\n/)
  return next < 0 ? rest : rest.slice(0, next)
}

function expectStatuses(path: string, method: Method, statuses: number[]): void {
  const value = operation(path, method)
  for (const status of statuses) {
    expect(value, `${method.toUpperCase()} ${path} must document HTTP ${status}`).toMatch(
      new RegExp(`["']?${status}["']?\\s*:`),
    )
  }
}

describe('user-management OpenAPI error contract', () => {
  it('keeps role pagination aligned with the 1-200 backend clamp', () => {
    const roles = operation('/api/roles', 'get')

    expect(roles).toMatch(
      /name: size[^\n]*minimum: 1, maximum: 200, default: 10[^\n]*RoleAccessServiceImpl 的 1—200 限制一致/,
    )
  })

  it('points the fail-close legacy password path to the real replacement endpoints', () => {
    const legacyPassword = operation('/api/user/{id}/password', 'put')

    expect(legacyPassword).toContain('PUT /api/credentials/change-password')
    expect(legacyPassword).toContain('POST /api/users/{id}/password-reset')
    expect(legacyPassword).not.toContain('/api/profile/password')
  })

  it.each([
    ['/api/roles', 'get'],
    ['/api/permissions/tree', 'get'],
    ['/api/users/{id}/authorization', 'get'],
    ['/api/organization-units/tree', 'get'],
    ['/api/positions', 'get'],
    ['/api/employees/{id}/organization-history', 'get'],
  ] as const)('%s %s documents protected read errors', (path, method) => {
    expectStatuses(path, method, [401, 403, 500])
  })

  it.each([
    ['/api/roles/{id}', 'put'],
    ['/api/roles/{id}/permissions', 'put'],
    ['/api/users/{id}/authorization/roles', 'put'],
    ['/api/users/{id}/authorization/permissions', 'put'],
    ['/api/users/authorization/batch/roles', 'put'],
    ['/api/users/authorization/batch/permissions', 'put'],
    ['/api/organization-units/{id}', 'put'],
    ['/api/positions/{id}', 'put'],
    ['/api/employees/{id}/organization-membership', 'put'],
  ] as const)('%s %s documents protected mutation errors', (path, method) => {
    expectStatuses(path, method, [400, 401, 403, 404, 409, 500])
  })

  it('documents owner validation, authentication, authorization and server errors', () => {
    expectStatuses('/api/owner', 'get', [400, 401, 403, 500])
  })

  it('uses the session-specific 503 response for logout', () => {
    const logout = operation('/api/logout', 'post')

    expectStatuses('/api/logout', 'post', [401, 500, 503])
    expect(logout).toMatch(
      /["']?503["']?\s*:\s*\n\s+\$ref: ["']#\/components\/responses\/SessionServiceUnavailable["']/,
    )
  })

  it('documents immediate and scheduled personal-permission effective times', () => {
    const permissions = operation('/api/users/{id}/authorization/permissions', 'put')

    expect(permissions).toContain('effectiveFrom 为空时按服务端当前时间立即生效')
    expect(permissions).toContain('显式未来时间按约束预约')
    expect(permissions).toContain('effectiveTo 必须晚于实际生效时间')
  })

  it('documents the controller batch-authorization endpoints and DTO limits', () => {
    const batchRoles = operation('/api/users/authorization/batch/roles', 'put')
    const batchPermissions = operation('/api/users/authorization/batch/permissions', 'put')
    const roleRequest = schema('BatchUpdateUserRolesRequest')
    const permissionRequest = schema('BatchUpdateUserPermissionsRequest')
    const result = schema('UserAuthorizationBatchResult')

    expect(batchRoles).toContain('#/components/schemas/BatchUpdateUserRolesRequest')
    expect(batchPermissions).toContain('#/components/schemas/BatchUpdateUserPermissionsRequest')
    expect(roleRequest).toContain('enum: [ASSIGN, UNASSIGN]')
    expect(roleRequest).toMatch(/targets:.*minItems: 1, maxItems: 50/)
    expect(roleRequest).toMatch(/roleIds:.*minItems: 1, maxItems: 100/)
    expect(permissionRequest).toMatch(/targets:.*minItems: 1, maxItems: 50/)
    expect(permissionRequest).toMatch(/changes:.*minItems: 1, maxItems: 200/)
    expect(permissionRequest).toContain('#/components/schemas/UserPermissionChangeInput')
    expect(result).toContain('required: [targetCount, changedTargetCount, targets]')
    expect(result).toContain('#/components/schemas/UserAuthorizationBatchTargetResult')
  })
})
