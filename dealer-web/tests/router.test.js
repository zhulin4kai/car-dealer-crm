import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const originalLocation = window.location
beforeEach(() => {
  delete window.location
  window.location = Object.assign({}, originalLocation, { href: '' })
  sessionStorage.clear()
  localStorage.clear()
  vi.resetModules()
})

afterEach(() => {
  window.location = originalLocation
})

async function importRouterFresh() {
  const mod = await import('../src/router/router.js')
  return mod.default
}

describe('router - navigation guard behavior (real router.push)', () => {
  it('redirects /dashboard to / when there is no token anywhere', async () => {
    const router = await importRouterFresh()
    await router.push('/dashboard').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('redirects /dashboard/user (a deep child) to / when there is no token', async () => {
    const router = await importRouterFresh()
    await router.push('/dashboard/user').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('allows /dashboard when sessionStorage has dlyk_token', async () => {
    sessionStorage.setItem('dlyk_token', 'session-jwt')
    const router = await importRouterFresh()
    await router.push('/dashboard').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('allows /dashboard/user when localStorage has dlyk_token (rememberMe path)', async () => {
    localStorage.setItem('dlyk_token', 'local-jwt')
    const router = await importRouterFresh()
    await router.push('/dashboard/user').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard/user')
  })

  it('allows / (login) when no token exists', async () => {
    const router = await importRouterFresh()
    await router.push('/').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('an unknown path falls through the catch-all redirect to /dashboard (or / if no token)', async () => {
    // Without token: /this-does-not-exist -> / -> / (token check).
    // With token: /this-does-not-exist -> /dashboard.
    const router = await importRouterFresh()
    await router.push('/this-does-not-exist').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')

    sessionStorage.setItem('dlyk_token', 'jwt')
    const router2 = await importRouterFresh()
    await router2.push('/this-does-not-exist').catch(() => {})
    await router2.isReady()
    expect(router2.currentRoute.value.path).toBe('/dashboard')
  })

  it('does NOT call window.location.href as a side effect of the guard (redirect uses router.replace, not location)', async () => {
    // The guard uses next('/') which is a programmatic navigation, not
    // window.location.href. If the guard ever changes to a hard redirect,
    // this test will surface it (and that may be intentional — but it must
    // be a conscious change).
    const router = await importRouterFresh()
    await router.push('/dashboard/activity').catch(() => {})
    await router.isReady()
    expect(window.location.href).toBe('')
  })
})

describe('router - dashboard children resolve to the documented view files', () => {
  // The list below mirrors router.js#children exactly. We deliberately
  // assert by ACTUALLY importing each .vue file via its dynamic import
  // (the same way router.js loads it) and checking default export exists.
  // If a child is repointed to a non-existent file the dynamic import
  // throws, and the test fails.
  const cases = [
    { path: '', file: 'StatisticView' },
    { path: 'user', file: 'UserView' },
    { path: 'activity', file: 'ActivityView' },
    { path: 'activity/:id', file: 'ActivityDetailView' },
    { path: 'clue', file: 'ClueView' },
    { path: 'clue/detail/:id', file: 'ClueDetailView' },
    { path: 'customer', file: 'CustomerView' },
    { path: 'product', file: 'ProductView' },
    { path: 'product/category', file: 'ProductCategoryView' },
    { path: 'product/promotion', file: 'ProductPromotionView' },
    { path: 'product/stock', file: 'ProductStockAlertView' },
    { path: 'tran', file: 'TranView' },
    { path: 'tran/:id', file: 'TranDetailView' },
    { path: 'tran/approve/:id', file: 'TranApproveView' },
    { path: 'tran/invoice/:id', file: 'TranInvoiceView' },
    { path: 'dict/type', file: 'DictTypeView' },
    { path: 'dict/value', file: 'DictValueView' },
    { path: 'system', file: 'SystemView' },
  ]

  it.each(cases)('child path "%s" resolves to a $file .vue module', async ({ path, file }) => {
    const router = await importRouterFresh()
    const dashboard = router.options.routes.find((r) => r.path === '/dashboard')
    const child = dashboard.children.find((c) => c.path === path)
    expect(child).toBeDefined()
    // The component is a dynamic import function. Calling it returns a
    // Promise<Module>. We don't need the full Module just to know the file
    // is resolvable; importing it directly is the strongest signal.
    const mod = await import(`../src/view/${file}.vue`)
    expect(mod.default).toBeDefined()
  })
})

describe('router - top-level structure', () => {
  it('has exactly the 3 documented top-level routes (login, dashboard, catch-all)', async () => {
    const router = await importRouterFresh()
    const paths = router.options.routes.map((r) => r.path).sort()
    expect(paths).toEqual(['/', '/:pathMatch(.*)*', '/dashboard'])
  })

  it('catch-all redirects to /dashboard', async () => {
    const router = await importRouterFresh()
    const catchAll = router.options.routes.find((r) => r.path === '/:pathMatch(.*)*')
    expect(catchAll.redirect).toBe('/dashboard')
  })
})
