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

function response(name: string): string {
  return block(`    ${name}:\n`, /\n {4}[A-Za-z][A-Za-z0-9]*:\s*(?:\n|\{)/)
}

function pathItem(path: string): string {
  return block(`  ${path}:\n`, /\n {2}\/[^\n]+:\n/)
}

function operation(path: string, method: 'get'): string {
  const item = pathItem(path)
  const marker = `    ${method}:\n`
  const start = item.indexOf(marker)
  if (start < 0) throw new Error(`OpenAPI operation missing: ${method.toUpperCase()} ${path}`)
  return item.slice(start + marker.length)
}

describe('Task 19 strict OpenAPI history contract', () => {
  it('documents the target-scoped read-only query with all server filters and stable errors', () => {
    const item = pathItem('/api/users/{id}/history')
    const value = operation('/api/users/{id}/history', 'get')
    for (const name of ['page', 'size', 'actionCode', 'startTime', 'endTime']) {
      expect(value).toContain(`name: ${name}`)
    }
    expect(value).toMatch(/name: startTime[\s\S]*format: date-time/)
    expect(value).toMatch(/name: endTime[\s\S]*format: date-time/)
    expect(value).toMatch(/"403"\s*:/)
    expect(value).toMatch(/"404"\s*:/)
    expect(item).not.toMatch(/\n {4}(?:post|put|patch|delete):\n/)
  })

  it('returns a concrete structured collection with server-driven actions and VIEW gate', () => {
    const value = operation('/api/users/{id}/history', 'get')
    const responseName = value.match(
      /["']?200["']?\s*:\s*(?:\{\s*)?\$ref:\s*["']#\/components\/responses\/([^"'}]+)["']\s*}?/,
    )?.[1]
    if (!responseName) throw new Error('OpenAPI history 200 response must use a concrete response')
    expect(response(responseName)).toContain('#/components/schemas/UserHistoryCollection')

    const collection = schema('UserHistoryCollection')
    const required = collection.split('\n').find((line) => line.includes('required:')) ?? ''
    for (const field of [
      'list',
      'total',
      'pageSize',
      'pageNum',
      'pages',
      'size',
      'actionOptions',
      'allowedActions',
      'unavailableReasons',
    ])
      expect(required).toContain(field)
    expect(collection).toContain('#/components/schemas/UserHistoryActionOption')
    expect(collection).toContain('enum: [VIEW]')
  })

  it('publishes only structured display fields and no raw or sensitive history property', () => {
    const item = schema('UserHistoryItem')
    const field = schema('UserHistoryValueField')
    const target = schema('UserHistoryTargetSummary')
    const operator = schema('UserHistoryOperatorSummary')
    const batch = schema('UserHistoryBatchSummary')
    for (const name of [
      'eventId',
      'sourceKey',
      'actionCode',
      'actionName',
      'categoryCode',
      'categoryName',
      'target',
      'operator',
      'beforeValues',
      'afterValues',
      'resultCode',
      'resultName',
      'occurredAt',
    ])
      expect(item).toContain(`${name}:`)
    expect(item).toContain('#/components/schemas/UserHistoryValueField')
    expect(item).toContain(
      'enum: [AUTHORIZATION_HISTORY, OPERATION_LOG, USER_LIFECYCLE_EVENT]',
    )

    expect(`${item}\n${field}\n${target}\n${operator}\n${batch}`).not.toMatch(
      /^\s*(?:raw|rawDetail|detail|payload|password|passwordHash|hash|token|ip|ipAddress|phone|mobile|email|contact):/im,
    )
  })
})
