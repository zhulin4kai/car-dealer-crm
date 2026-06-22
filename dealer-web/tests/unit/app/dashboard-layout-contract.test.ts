import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { describe, expect, it } from 'vitest'

const projectRoot = dirname(dirname(dirname(dirname(fileURLToPath(import.meta.url)))))
const dashboardLayout = join(projectRoot, 'src/layouts/DashboardLayout.vue')

describe('dashboard layout route stability contracts', () => {
  it('uses a stable keyed DOM boundary around dashboard route pages', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('v-slot="{ Component, route: viewRoute }"')
    expect(source).toContain(':key="viewRoute.fullPath"')
    expect(source).not.toContain('v-if="isRouterAlive"')
  })

  it('does not render sidebar links from empty fallback URLs', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('visibleMenuPermissionList')
    expect(source).toContain('normalizeMenuUrl')
    expect(source).not.toMatch(/:to="[^"]*\?\?\s*['"]{2}/)
  })
})
