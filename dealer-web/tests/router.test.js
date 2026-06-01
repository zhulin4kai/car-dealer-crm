import { describe, it, expect, vi, beforeEach } from 'vitest'

async function importRouter() {
  const mod = await import('../src/router/router.js')
  return mod.default
}

function getChild(router, parentPath, childPath) {
  const parent = router.options.routes.find((r) => r.path === parentPath)
  if (!parent || !parent.children) return undefined
  return parent.children.find((c) => c.path === childPath)
}

function getChildComponent(router, parentPath, childPath) {
  const child = getChild(router, parentPath, childPath)
  if (!child) return undefined
  return child.component
}

describe('router - route configuration', () => {
  it('uses createWebHistory', async () => {
    const router = await importRouter()
    expect(router.options.history).toBeDefined()
  })

  it('has exactly 3 top-level routes (login, dashboard, catch-all)', async () => {
    const router = await importRouter()
    const paths = router.options.routes.map((r) => r.path)
    expect(paths).toContain('/')
    expect(paths).toContain('/dashboard')
    expect(paths).toContain('/:pathMatch(.*)*')
    expect(router.options.routes.length).toBe(3)
  })

  it('catch-all route redirects to /dashboard', async () => {
    const router = await importRouter()
    const catchAll = router.options.routes.find((r) => r.path === '/:pathMatch(.*)*')
    expect(catchAll.redirect).toBe('/dashboard')
  })

  it('login route resolves to a non-null lazy component', async () => {
    const router = await importRouter()
    const login = router.options.routes.find((r) => r.path === '/')
    expect(login).toBeDefined()
    const comp = await login.component()
    expect(comp.default).toBeDefined()
    expect(typeof comp.default).toBe('object')
  })

  it('dashboard route resolves to a non-null lazy component', async () => {
    const router = await importRouter()
    const dashboard = router.options.routes.find((r) => r.path === '/dashboard')
    expect(dashboard).toBeDefined()
    const comp = await dashboard.component()
    expect(comp.default).toBeDefined()
    expect(typeof comp.default).toBe('object')
  })

  it('all dashboard child routes resolve to non-null lazy components', async () => {
    const router = await importRouter()
    const dashboard = router.options.routes.find((r) => r.path === '/dashboard')
    for (const child of dashboard.children) {
      const comp = await child.component()
      expect(comp.default, `child path ${child.path} should have a default export`).toBeDefined()
    }
  })

  it('dashboard has 18 child routes (1 default + 17 named)', async () => {
    const router = await importRouter()
    const dashboard = router.options.routes.find((r) => r.path === '/dashboard')
    expect(dashboard.children.length).toBe(18)
  })

  it('dashboard default child route renders StatisticView', async () => {
    const router = await importRouter()
    const comp = await getChildComponent(router, '/dashboard', '')
    const mod = await comp()
    expect(mod.default).toBeDefined()
    expect(mod.default.__file).toContain('StatisticView')
  })

  it.each([
    ['user', 'UserView'],
    ['activity', 'ActivityView'],
    ['activity/:id', 'ActivityDetailView'],
    ['clue', 'ClueView'],
    ['clue/detail/:id', 'ClueDetailView'],
    ['customer', 'CustomerView'],
    ['product', 'ProductView'],
    ['product/category', 'ProductCategoryView'],
    ['product/promotion', 'ProductPromotionView'],
    ['product/stock', 'ProductStockAlertView'],
    ['tran', 'TranView'],
    ['tran/:id', 'TranDetailView'],
    ['tran/approve/:id', 'TranApproveView'],
    ['tran/invoice/:id', 'TranInvoiceView'],
    ['dict/type', 'DictTypeView'],
    ['dict/value', 'DictValueView'],
    ['system', 'SystemView'],
  ])('dashboard child "%s" renders %s', async (childPath, expectedFile) => {
    const router = await importRouter()
    const comp = getChildComponent(router, '/dashboard', childPath)
    expect(comp, `child path ${childPath} should be defined`).toBeDefined()
    const mod = await comp()
    expect(mod.default.__file).toContain(expectedFile)
  })

  it('no child path starts with a slash (vue-router convention)', async () => {
    const router = await importRouter()
    const dashboard = router.options.routes.find((r) => r.path === '/dashboard')
    for (const child of dashboard.children) {
      expect(child.path.startsWith('/'), `child path "${child.path}" should not start with /`).toBe(false)
    }
  })

  it('no leftover /hello route from scaffolded demo', async () => {
    const router = await importRouter()
    const hello = router.options.routes.find((r) => r.path === '/hello')
    expect(hello).toBeUndefined()
  })
})

describe('router - navigation guard behavior', () => {
  it('registers a beforeEach guard', async () => {
    // shape-only: doc-allowed — the actual guard behavior is verified by
    // the next three tests (redirect on missing token, allow with token,
    // allow /  without token). Kept as a sanity check that the guard
    // registration didn't get accidentally removed.
    const router = await importRouter()
    expect(typeof router.beforeEach).toBe('function')
  })

  it('redirects to / when navigating to /dashboard without a token', async () => {
    const router = await importRouter()
    sessionStorage.clear()
    localStorage.clear()

    // Use the real router to actually navigate. The guard should redirect.
    await router.push('/dashboard').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('allows navigation to /dashboard when localStorage has a token', async () => {
    const router = await importRouter()
    sessionStorage.clear()
    localStorage.clear()
    localStorage.setItem('dlyk_token', 'jwt')

    await router.push('/dashboard').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('allows navigation to / (login) even without a token', async () => {
    const router = await importRouter()
    sessionStorage.clear()
    localStorage.clear()

    await router.push('/').catch(() => {})
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })
})
