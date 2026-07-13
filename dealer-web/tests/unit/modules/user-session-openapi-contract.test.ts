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

function pathItem(path: string): string {
  return block(`  ${path}:\n`, /\n {2}\/[^\n]+:\n/)
}

function operation(path: string, method: 'get' | 'post'): string {
  const item = pathItem(path)
  const marker = `    ${method}:\n`
  const start = item.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI operation missing: ${method.toUpperCase()} ${path}`)
  return item.slice(start + marker.length)
}

describe('Task 17 OpenAPI session contract', () => {
  it('publishes all stable session error codes', () => {
    const errorCode = block('    SessionErrorCode:\n', /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
    expect(errorCode).toContain('enum: [631, 632, 633, 634, 635]')
  })

  it.each([
    ['/api/me/sessions/{sessionId}/revoke', ['404', '409', '410', '503']],
    ['/api/users/{id}/sessions/{sessionId}/revoke', ['403', '404', '409', '410', '503']],
    ['/api/me/sessions/revoke-others', ['409', '503']],
    ['/api/me/sessions/revoke-all', ['409', '503']],
    ['/api/users/{id}/sessions/revoke-all', ['403', '404', '409', '503']],
  ] as const)('documents stable errors for %s', (path, statuses) => {
    const value = operation(path, 'post')
    for (const status of statuses) expect(value).toMatch(new RegExp(`"${status}"\\s*:`))
    expect(value).toContain('#/components/schemas/RevokeSessionRequest')
    expect(value).toContain('#/components/responses/UserSessionCollectionOk')
  })

  it('documents managed target absence and the sessionRevision request field', () => {
    expect(operation('/api/users/{id}/sessions', 'get')).toMatch(/"404"\s*:/)
    const request = block('    RevokeSessionRequest:\n', /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
    expect(request).toContain('required: [sessionRevision, reason]')
    expect(request).not.toContain('expectedVersion')
  })
})
