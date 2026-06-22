import { readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { describe, expect, it } from 'vitest'

const projectRoot = dirname(dirname(dirname(dirname(fileURLToPath(import.meta.url)))))
const srcDir = join(projectRoot, 'src')
const selectItemComponent = join(srcDir, 'components/ui/select/SelectItem.vue')

function collectVueFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const fullPath = join(dir, entry)
    const stats = statSync(fullPath)
    if (stats.isDirectory()) {
      return collectVueFiles(fullPath)
    }
    return fullPath.endsWith('.vue') ? [fullPath] : []
  })
}

describe('select component contracts', () => {
  it('does not use empty string values for SelectItem', () => {
    const offenders = collectVueFiles(srcDir).filter((file) => {
      const source = readFileSync(file, 'utf8')
      if (!source.includes('<SelectItem')) {
        return false
      }
      return [
        /<SelectItem\b[^>]*(?:\bvalue=(["'])\1|\b:value=(["'])''\2)/,
        /\bvalue\s*:\s*(["'])\1/,
        /\bvalue\s*:\s*String\([^)]*\?\?\s*(["'])\1\)/,
      ].some((pattern) => pattern.test(source))
    })

    expect(offenders).toEqual([])
  })

  it('guards the shared SelectItem wrapper against invalid runtime values', () => {
    const source = readFileSync(selectItemComponent, 'utf8')

    expect(source).toContain("props.value !== ''")
    expect(source).toContain('props.value !== null')
    expect(source).toContain('props.value !== undefined')
  })
})
