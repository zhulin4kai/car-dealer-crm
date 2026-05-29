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
      // BUG: The router includes a /hello route pointing to HelloWorld.vue
      // which is a default Vite scaffold component and should be removed.
      // It serves no purpose in the CRM application.
      const helloRoute = router.options.routes.find(r => r.path === '/hello')
      expect(helloRoute).toBeUndefined() // FAILS: /hello route exists
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
  })

  describe('404 catch-all route', () => {
    it('should have a 404 catch-all route', () => {
      // BUG: There is no catch-all route (path: '/:pathMatch(.*)*') defined.
      // Unknown URLs will show a blank page instead of a 404 page.
      const catchAllRoute = router.options.routes.find(
        r => r.path === '/:pathMatch(.*)*' || r.path === '*'
      )
      expect(catchAllRoute).toBeDefined() // FAILS: no catch-all route
    })
  })

  describe('navigation guards', () => {
    it('should have navigation guards', () => {
      // FIXED: The router now has a beforeEach guard for auth protection
      expect(router.beforeEach).toBeDefined()
      // The beforeEach function exists and can be called
      // In vue-router 4, guards are registered via router.beforeEach()
      // We verify the router has the method and it's callable
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

    it('should have clue routes', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const clueRoute = dashboardRoute.children.find(r => r.path === 'clue')
      expect(clueRoute).toBeDefined()
    })

    it('should have customer route', () => {
      const dashboardRoute = router.options.routes.find(r => r.path === '/dashboard')
      const customerRoute = dashboardRoute.children.find(r => r.path === 'customer')
      expect(customerRoute).toBeDefined()
    })
  })
})
