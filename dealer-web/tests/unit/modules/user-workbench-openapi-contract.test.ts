import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const openapi = readFileSync(resolve(process.cwd(), '../docs/api/openapi.yaml'), 'utf8')

function block(marker: string, nextMarker: RegExp): string {
  const start = openapi.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI block missing: ${marker.trim()}`)
  const rest = openapi.slice(start + marker.length)
  const next = rest.search(nextMarker)
  return next < 0 ? rest : rest.slice(0, next)
}

function schema(name: string): string {
  return block(`    ${name}:\n`, /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
}

function pathItem(path: string): string {
  return block(`  ${path}:\n`, /\n {2}\/[^\n]+:\n/)
}

function operation(path: string, method: 'get' | 'post' | 'put'): string {
  const item = pathItem(path)
  const marker = `    ${method}:\n`
  const start = item.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI operation missing: ${method.toUpperCase()} ${path}`)
  const rest = item.slice(start + marker.length)
  const next = rest.search(/\n {4}(?:get|post|put|patch|delete):\n/)
  return next < 0 ? rest : rest.slice(0, next)
}

describe('Task 18 strict OpenAPI contract', () => {
  it('documents all eight list filters and server-side stable sorting', () => {
    const list = operation('/api/users', 'get')
    for (const name of [
      'keyword',
      'organizationUnitId',
      'positionId',
      'managerEmployeeId',
      'roleId',
      'employmentStatus',
      'accountStatus',
      'lockStatus',
    ]) expect(list).toContain(`name: ${name}`)
    expect(list).toContain('name: sortBy')
    expect(list).toContain('name: sortDirection')
    expect(list).toContain('#/components/responses/UserPageOk')
  })

  it('separates list roles from organization-specific assignable roles', () => {
    const endpoint = operation('/api/users/filter-options', 'get')
    const options = schema('ManagedUserFilterOptions')
    expect(endpoint).toContain('name: organizationUnitId')
    expect(endpoint).toContain('#/components/responses/ManagedUserFilterOptionsOk')
    expect(options).toContain('roles:')
    expect(options).toContain('assignableRoles:')
    expect(options).toContain('required: [organizations, positions, managers, roles, assignableRoles')
  })

  it('models versions, status command objects, action reasons, and safe create fields', () => {
    const detail = schema('ManagedUserDetail')
    const statusCommand = schema('ManagedUserStatusCommandOption')
    const create = schema('CreateManagedUserRequest')
    for (const field of ['profileVersion', 'accountVersion', 'employeeVersion', 'authorizationVersion', 'sessionRevision']) {
      expect(detail).toContain(`${field}:`)
    }
    expect(detail).toContain('statusCommands:')
    expect(detail).toContain('allowedActions:')
    expect(detail).toContain('unavailableReasons:')
    expect(statusCommand).toContain('required: [command, label, destructive]')
    expect(create).toContain('required: [loginAct, name, employeeNo, organizationUnitId, positionId, roleIds]')
    expect(create).not.toMatch(/loginPwd|password|accountEnabled|authorizationVersion/)
  })

  it('publishes 403, 404, and 409 boundaries and keeps old mutation entries fail-closed', () => {
    expect(operation('/api/users/{id}', 'get')).toMatch(/"403"[\s\S]*"404"/)
    for (const [path, method] of [
      ['/api/users/{id}/profile', 'put'],
      ['/api/users/{id}/status', 'post'],
      ['/api/users/{id}/login-account', 'put'],
      ['/api/users/{id}/security-expiration', 'put'],
      ['/api/users/{id}/password-reset', 'post'],
    ] as const) {
      const value = operation(path, method)
      expect(value).toMatch(/"403"/)
      expect(value).toMatch(/"409"/)
    }
    for (const method of ['post', 'put'] as const) {
      const legacy = operation('/api/user', method)
      expect(legacy).toContain('deprecated: true')
      expect(legacy).toContain('fail-close')
      expect(legacy).not.toMatch(/"200"\s*:/)
    }
  })

  it('keeps login identity and security expiration as strict independent commands', () => {
    const loginAccount = schema('ManagedUserLoginAccountRequest')
    const securityExpiration = schema('ManagedUserSecurityExpirationRequest')
    const detail = schema('ManagedUserDetail')
    expect(loginAccount).toContain('additionalProperties: false')
    expect(loginAccount).toContain('required: [accountVersion, loginAct, reason]')
    expect(securityExpiration).toContain('required: [accountVersion, reason]')
    expect(securityExpiration).toContain('accountExpiresAt:')
    expect(securityExpiration).toContain('credentialExpiresAt:')
    expect(detail).toContain('accountExpired:')
    expect(detail).toContain('credentialExpiresAt:')
  })
})
