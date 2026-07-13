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

function operation(path: string, method: 'get' | 'post'): string {
  const item = pathItem(path)
  const marker = `    ${method}:\n`
  const start = item.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI operation missing: ${method.toUpperCase()} ${path}`)
  return item.slice(start + marker.length)
}

describe('Task 20 strict OpenAPI lifecycle contract', () => {
  it('publishes the target-scoped lifecycle context and all explicit commands', () => {
    expect(operation('/api/users/{id}/lifecycle', 'get')).toContain(
      '#/components/responses/UserLifecycleContextOk',
    )
    for (const path of [
      '/api/users/{id}/lifecycle/transfer',
      '/api/users/{id}/lifecycle/departure/precheck',
      '/api/users/{id}/lifecycle/departure/start',
      '/api/users/{id}/lifecycle/departure/handover',
      '/api/users/{id}/lifecycle/departure/complete',
      '/api/users/{id}/lifecycle/rehire',
    ]) {
      const value = operation(path, 'post')
      expect(value).toMatch(/"403"\s*:/)
      expect(value).toMatch(/"404"\s*:/)
      expect(value).toMatch(/"409"\s*:/)
    }
    for (const path of [
      '/api/users/{id}/lifecycle/departure/start',
      '/api/users/{id}/lifecycle/departure/handover',
      '/api/users/{id}/lifecycle/departure/complete',
    ])
      expect(operation(path, 'post')).toMatch(/"410"\s*:/)
  })

  it('keeps allowed actions, transitions, and employeeVersion server-authoritative', () => {
    const context = schema('UserLifecycleContext')
    const required = context.split('\n').find((line) => line.includes('required:')) ?? ''
    expect(required).toContain('employeeVersion')
    expect(required).toContain('allowedActions')
    expect(required).toContain('unavailableReasons')
    expect(required).toContain('statusTransitions')

    for (const name of [
      'TransferEmployeeRequest',
      'DeparturePrecheckRequest',
      'StartDepartureRequest',
      'ConfirmHandoverRequest',
      'CompleteDepartureRequest',
      'RehireEmployeeRequest',
    ]) {
      const request = schema(name)
      expect(request).toContain('employeeVersion:')
      expect(request).not.toMatch(/profileVersion|accountVersion|authorizationVersion/)
    }
  })

  it('defines six direct-owner domains and only Quote/Tran as derived impacts', () => {
    const responsibility = schema('DepartureResponsibilitySummary')
    expect(responsibility).toContain(
      'enum: [ACTIVITY, CLUE, CUSTOMER, OPPORTUNITY, FOLLOW_TASK, TEST_DRIVE, QUOTE, TRAN]',
    )
    expect(responsibility).toContain('enum: [DIRECT_OWNER, DERIVED_IMPACT]')
    expect(responsibility).toContain('blockingReasons:')
    expect(responsibility).toContain('conflicts:')
    expect(responsibility).toContain('#/components/schemas/HandoverCandidate')

    const candidate = schema('HandoverCandidate')
    for (const field of ['eligible', 'qualificationCode', 'qualificationName', 'unavailableReason'])
      expect(candidate).toContain(`${field}:`)

    const selection = schema('HandoverTransferSelection')
    const domainResult = schema('HandoverDomainResult')
    for (const value of [selection, domainResult]) {
      expect(value).toContain(
        'enum: [ACTIVITY, CLUE, CUSTOMER, OPPORTUNITY, FOLLOW_TASK, TEST_DRIVE]',
      )
      expect(value).not.toMatch(/QUOTE|TRAN|PENDING_APPROVAL|COMMUNICATION_RECORD/)
    }
  })

  it('requires memory-only snapshots, atomic six-domain results, and no restored authorization input', () => {
    for (const name of [
      'StartDepartureRequest',
      'ConfirmHandoverRequest',
      'CompleteDepartureRequest',
    ])
      expect(schema(name)).toContain('snapshotToken:')

    const handover = schema('HandoverResult')
    expect(handover).toContain('domainResults:')
    expect(handover).toContain('#/components/schemas/HandoverDomainResult')

    const rehire = schema('RehireEmployeeRequest')
    expect(rehire).not.toMatch(/roleIds|permissionIds|authorization|legacyRole/)
    expect(schema('RehireResult')).toContain('restoredLegacyAuthorizationCount:')
  })
})
