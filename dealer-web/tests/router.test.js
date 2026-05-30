import { describe, it, expect, vi, beforeEach } from 'vitest'

describe('router.js', () => {
  let routerModule
  let router

  beforeEach(async () => {
    vi.clearAllMocks()
    routerModule = await import('../src/router/router.js')
    router = routerModule.default
  })

  describe('route definitions', () => {
    it('should have routes defined', () => {
      expect(router).toBeDefined()
      expect(router.options.routes).toBeDefined()
      expect(router.options.routes.length).toBeGreaterThan(0)
    })

    it('should NOT have a /hello route', () => {
      const helloRoute = router.options.routes.find(r => r.path === '/hello')
      expect(helloRoute).toBeUndefined()
    })

    it('should have a / route for login', () => {
      const loginRoute = router.options.routes.find(r => r.path === '/')
      expect(loginRoute).toBeDefined()
    })

    it('should have a /dashboard route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      expect(dashboardRoute).toBeDefined()
    })

    it('should have child routes under /dashboard', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      expect(dashboardRoute).toBeDefined()
      expect(dashboardRoute.children).toBeDefined()
      expect(dashboardRoute.children.length).toBeGreaterThan(0)
    })

    it('should have a catch-all route', () => {
      const catchAllRoute = router.options.routes.find(
        r => r.path === '/:pathMatch(.*)*'
      )
      expect(catchAllRoute).toBeDefined()
    })
  })

  describe('navigation guards', () => {
    it('should have navigation guards', () => {
      expect(router.beforeEach).toBeDefined()
      expect(typeof router.beforeEach).toBe('function')
    })
  })

  describe('route completeness', () => {
    it('should have user management route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const userRoute = dashboardRoute.children.find(r => r.path === 'user')
      expect(userRoute).toBeDefined()
    })

    it('should have activity routes', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const activityRoute = dashboardRoute.children.find(r => r.path === 'activity')
      expect(activityRoute).toBeDefined()
    })

    it('should have activity detail route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const activityDetailRoute = dashboardRoute.children.find(r => r.path === 'activity/:id')
      expect(activityDetailRoute).toBeDefined()
    })

    it('should have clue routes', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const clueRoute = dashboardRoute.children.find(r => r.path === 'clue')
      expect(clueRoute).toBeDefined()
    })

    it('should have clue detail route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const clueDetailRoute = dashboardRoute.children.find(r => r.path === 'clue/detail/:id')
      expect(clueDetailRoute).toBeDefined()
    })

    it('should have customer route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const customerRoute = dashboardRoute.children.find(r => r.path === 'customer')
      expect(customerRoute).toBeDefined()
    })

    it('should have product route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const productRoute = dashboardRoute.children.find(r => r.path === 'product')
      expect(productRoute).toBeDefined()
    })

    it('should have product category route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const productCategoryRoute = dashboardRoute.children.find(r => r.path === 'product/category')
      expect(productCategoryRoute).toBeDefined()
    })

    it('should have product promotion route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const productPromotionRoute = dashboardRoute.children.find(r => r.path === 'product/promotion')
      expect(productPromotionRoute).toBeDefined()
    })

    it('should have product stock route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const productStockRoute = dashboardRoute.children.find(r => r.path === 'product/stock')
      expect(productStockRoute).toBeDefined()
    })

    it('should have tran route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const tranRoute = dashboardRoute.children.find(r => r.path === 'tran')
      expect(tranRoute).toBeDefined()
    })

    it('should have tran detail route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const tranDetailRoute = dashboardRoute.children.find(r => r.path === 'tran/:id')
      expect(tranDetailRoute).toBeDefined()
    })

    it('should have tran approve route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const tranApproveRoute = dashboardRoute.children.find(r => r.path === 'tran/approve/:id')
      expect(tranApproveRoute).toBeDefined()
    })

    it('should have tran invoice route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const tranInvoiceRoute = dashboardRoute.children.find(r => r.path === 'tran/invoice/:id')
      expect(tranInvoiceRoute).toBeDefined()
    })

    it('should have dict type route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const dictTypeRoute = dashboardRoute.children.find(r => r.path === 'dict/type')
      expect(dictTypeRoute).toBeDefined()
    })

    it('should have dict value route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const dictValueRoute = dashboardRoute.children.find(r => r.path === 'dict/value')
      expect(dictValueRoute).toBeDefined()
    })

    it('should have system route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const systemRoute = dashboardRoute.children.find(r => r.path === 'system')
      expect(systemRoute).toBeDefined()
    })

    it('should have statistic route (default)', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const statisticRoute = dashboardRoute.children.find(r => r.path === '')
      expect(statisticRoute).toBeDefined()
    })
  })

  describe('route configuration', () => {
    it('should use createWebHistory', () => {
      expect(router.options.history).toBeDefined()
    })

    it('should have 3 top-level routes', () => {
      expect(router.options.routes.length).toBe(3)
    })

    it('should have 18 child routes under dashboard', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      expect(dashboardRoute.children.length).toBe(18)
    })
  })

  describe('catch-all route', () => {
    it('should redirect to /dashboard', () => {
      const catchAllRoute = router.options.routes.find(
        r => r.path === '/:pathMatch(.*)*'
      )
      expect(catchAllRoute.redirect).toBe('/dashboard')
    })
  })
})
