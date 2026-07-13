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

function schema(name: string): string {
  return block(`    ${name}:\n`, /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
}

describe('Task 23 ACTING reporting OpenAPI contract', () => {
  it('publishes a dedicated read and versioned replace collection without conflating DIRECT', () => {
    const item = pathItem('/api/employees/{id}/acting-reporting-relations')

    expect(item).toContain('operationId: getEmployeeActingReportings')
    expect(item).toContain('operationId: replaceEmployeeActingReportings')
    expect(item).toContain('ACTING 是独立的有限期多关系资源')
    expect(item).toContain('不会替代或覆盖唯一 DIRECT 直属管理者')
    expect(item).toContain('空数组仅结束 ACTING')
    expect(item).toContain('#/components/schemas/ReplaceActingReportingsRequest')
    expect(item).toMatch(/put:[\s\S]*"409"\s*:/)
  })

  it('requires employee CAS, bounded finite relations, and an explicit reason', () => {
    const request = schema('ReplaceActingReportingsRequest')
    const relation = schema('ActingReportingInput')

    expect(request).toContain('required: [expectedEmployeeVersion, relations, reason]')
    expect(request).toMatch(/expectedEmployeeVersion:.*minimum:\s*0/)
    expect(request).toMatch(/relations:.*maxItems:\s*20/)
    expect(request).toMatch(/reason:.*minLength:\s*1.*maxLength:\s*500/)
    expect(relation).toContain('required: [managerEmployeeId, effectiveTo]')
    expect(relation).toMatch(/effectiveTo:.*format:\s*date-time/)
    expect(relation).not.toContain('effectiveFrom:')
    expect(relation).not.toContain('relationType:')
  })

  it('returns server-authoritative UPDATE actions, seconds-capable dates, and a read-only candidate path', () => {
    const collection = schema('ActingReportingCollectionResponse')
    const relation = schema('ActingReportingRelationResponse')
    const candidatePath = pathItem(
      '/api/employees/{id}/acting-reporting-relations/manager-candidates',
    )

    expect(collection).toContain(
      'required: [employeeId, employeeVersion, relations, allowedActions, unavailableReasons]',
    )
    expect(collection).toContain('#/components/schemas/ActingReportingRelationResponse')
    expect(collection).toContain('enum: [UPDATE]')
    expect(relation).toMatch(/effectiveTo:.*format:\s*date-time/)
    expect(candidatePath).toContain('operationId: getEmployeeActingManagerCandidates')
    expect(candidatePath).toContain('#/components/responses/ManagerCandidateArrayOk')
    expect(candidatePath).not.toMatch(/\n {4}(?:post|put|patch|delete):\n/)
  })
})
