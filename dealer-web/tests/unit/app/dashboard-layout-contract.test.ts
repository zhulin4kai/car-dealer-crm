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

    expect(source).toContain('navigationSections')
    expect(source).toContain('normalizeMenuUrl')
    expect(source).not.toMatch(/:to="[^"]*\?\?\s*['"]{2}/)
  })

  it('renders the permission tree as grouped flat navigation instead of nested sidebar menus', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('PRODUCT_MENU_CODE')
    expect(source).toContain('DICT_MENU_CODE')
    expect(source).toContain('产品中心')
    expect(source).toContain('字典管理')
    expect(source).not.toContain('Collapsible')
  })

  it('groups organization management under the system navigation section', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain("'menu:organization'")
    expect(source).toContain("'menu:organization': 2")
  })

  it('groups role and permission catalog entries under system navigation', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain("'menu:role'")
    expect(source).toContain("'menu:permission'")
  })

  it('uses an explicit user menu in both sidebar states instead of avatar logout', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('aria-label="打开用户菜单"')
    expect(source).toContain('个人中心')
    expect(source).toContain('修改密码')
    expect(source).toContain('<DropdownMenuItem variant="destructive" @select="logout">')
    expect(source).not.toContain('@click="logout"')
    expect(source).toContain("router.push({ name: 'profile' })")
    expect(source).toContain("hash: '#change-password'")
  })

  it('keeps the protected recovery account out of personal actions and normal navigation', () => {
    const source = readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('v-if="!isProtectedRecoveryAccount" @select="openProfile"')
    expect(source).toContain('v-if="!isProtectedRecoveryAccount" @select="openPasswordChange"')
    expect(source).toContain('USER_MANAGEMENT_GATE_STATE.UNINITIALIZED')
    expect(source).toContain("item.url === '/dashboard/user'")
    expect(source).toContain(
      'user.value.userManagementGateState === USER_MANAGEMENT_GATE_STATE.READY ? items : []',
    )
  })
})
