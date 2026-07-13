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
  return block(
    `    ${name}:\n`,
    /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/,
  )
}

function response(name: string): string {
  return block(
    `    ${name}:\n`,
    /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/,
  )
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

function successResponse(
  path: string,
  method: 'get' | 'post' | 'put',
  expectedStatus: 200 | 202,
): string {
  const value = operation(path, method)
  const match = value.match(
    new RegExp(
      `["']?${expectedStatus}["']?\\s*:\\s*(?:\\{\\s*)?\\$ref:\\s*["']#\\/components\\/responses\\/([^"'}]+)["']\\s*}?`,
    ),
  )
  if (!match?.[1]) {
    throw new Error(`OpenAPI success response must use a concrete response schema: ${method.toUpperCase()} ${path}`)
  }
  return response(match[1])
}

describe('Task 15-16 OpenAPI contract', () => {
  it.each([
    ['/api/users', 'post', 202, 'CreateManagedUserResult'],
    ['/api/users/{id}/invitation', 'post', 202, 'ManagedCredentialDeliveryResult'],
    ['/api/users/{id}/password-reset', 'post', 202, 'PasswordResetDeliveryResult'],
    ['/api/users/{id}/profile', 'put', 200, 'ManagedUserDetail'],
    ['/api/profile', 'get', 200, 'UserProfile'],
    ['/api/profile', 'put', 200, 'UserProfile'],
    ['/api/login/info', 'get', 200, 'LoginInfo'],
  ] as const)('%s %s returns HTTP %s with %s', (path, method, status, name) => {
    expect(successResponse(path, method, status)).toContain(`#/components/schemas/${name}`)
  })

  it('models create and delivery results without any raw credential field', () => {
    const createResult = schema('CreateManagedUserResult')
    const delivery = schema('ManagedCredentialDeliveryResult')
    const resetDelivery = schema('PasswordResetDeliveryResult')

    expect(createResult).toContain('required: [user, credentialDelivery]')
    expect(createResult).toContain('#/components/schemas/ManagedUserDetail')
    expect(createResult).toContain('#/components/schemas/ManagedCredentialDeliveryResult')
    expect(delivery).toContain('required: [accepted, deliveryStatus]')
    expect(resetDelivery).toContain('required: [accepted, deliveryStatus, mustChangePassword]')
    expect(delivery).toContain('accepted: {type: boolean, enum: [true]}')
    expect(delivery).toContain('enum: [QUEUED]')
    expect(resetDelivery).toContain('mustChangePassword: {type: boolean, enum: [true]}')
    expect(`${createResult}\n${delivery}\n${resetDelivery}`).not.toMatch(
      /rawCredential|tokenDigest|temporaryPassword|loginPwd/,
    )
  })

  it('keeps independent profile and account versions in the managed user contract', () => {
    const detail = schema('ManagedUserDetail')
    const profileRequest = schema('UpdateManagedUserProfileRequest')

    expect(detail).toContain('profileVersion:')
    expect(detail).toContain('accountVersion:')
    expect(detail).toContain('employeeVersion:')
    expect(profileRequest).toContain('required: [profileVersion, name, phone, email]')
    expect(profileRequest).not.toMatch(/roleIds|permission|organizationUnitId|positionId/)
  })

  it('keeps every profile permission source paired with its validity and concrete custom organizations', () => {
    const sourceDetail = schema('UserProfilePermissionSourceDetail')
    const organization = schema('UserProfilePermissionOrganization')
    const required = sourceDetail.split('\n').find((line) => line.includes('required:')) ?? ''

    expect(required).toContain('sourceType')
    expect(required).toContain('sourceName')
    expect(required).toContain('organizations')
    expect(sourceDetail).toContain('effectiveFrom:')
    expect(sourceDetail).toContain('effectiveTo:')
    expect(sourceDetail).toContain('#/components/schemas/UserProfilePermissionOrganization')
    expect(organization).toContain('required: [id, code, name]')
    expect(organization).toContain('id:')
    expect(organization).toContain('code:')
    expect(organization).toContain('name:')
  })

  it('documents every login-info field required by auth and routing', () => {
    const loginInfo = schema('LoginInfo')
    const required = loginInfo.split('\n').find((line) => line.includes('required:')) ?? ''

    expect(required).toContain('avatarUrl')
    expect(required).toContain('mustChangePassword')
    expect(required).toContain('menuPermissionList')
    expect(loginInfo).toContain('avatarUrl:')
    expect(loginInfo).toContain('mustChangePassword:')
    expect(loginInfo).toContain('protectedRecoveryAccount:')
    expect(loginInfo).toContain('userManagementGateState:')
    expect(loginInfo).toContain('UNINITIALIZED, PENDING_FIRST_CHANGE, READY, DEGRADED')
    expect(loginInfo).toContain('menuPermissionList:')
  })

  it('publishes the stable credential error codes and their real HTTP statuses', () => {
    const errorCode = schema('CredentialErrorCode')
    expect(errorCode).toContain('enum: [620, 621, 622, 623, 624, 625, 626, 628]')

    for (const path of ['/api/credentials/activate', '/api/credentials/reset-password']) {
      const value = operation(path, 'post')
      expect(value).toMatch(/["']?400["']?:/)
      expect(value).toMatch(/["']?409["']?:/)
      expect(value).toMatch(/["']?410["']?:/)
      expect(value).toMatch(/["']?422["']?:/)
      expect(value).toMatch(/["']?429["']?:/)
    }

    for (const path of ['/api/users', '/api/users/{id}/invitation', '/api/users/{id}/password-reset']) {
      expect(operation(path, 'post')).toMatch(/["']?503["']?:/)
    }
  })
})
