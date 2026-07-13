import { describe, expect, it } from 'vitest'

import { routes } from '@/router/routes'

describe('router routes', () => {
  it('declares credential lifecycle routes before the catch-all route', () => {
    const routePaths = routes.map((route) => route.path)

    expect(routePaths).toEqual([
      '/',
      '/activate',
      '/forgot-password',
      '/reset-password',
      '/verify-contact',
      '/first-password-change',
      '/user-management-gate',
      '/dashboard',
      '/403',
      '/:pathMatch(.*)*',
    ])
    expect(routes.find((route) => route.path === '/activate')?.meta?.requiresAuth).toBe(false)
    expect(routes.find((route) => route.path === '/forgot-password')?.meta?.requiresAuth).toBe(
      false,
    )
    expect(routes.find((route) => route.path === '/reset-password')?.meta?.requiresAuth).toBe(false)
    expect(routes.find((route) => route.path === '/verify-contact')?.meta?.requiresAuth).toBe(false)
    expect(
      routes.find((route) => route.path === '/first-password-change')?.meta
        ?.allowDuringPasswordChange,
    ).toBe(true)
    expect(
      routes.find((route) => route.path === '/user-management-gate')?.meta
        ?.allowDuringUserManagementGate,
    ).toBe(true)
  })

  it('keeps the public URL contract', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    expect(dashboardRoute).toBeDefined()
    expect(dashboardRoute?.children).toHaveLength(32)

    const childPaths = dashboardRoute?.children?.map((route) => route.path) ?? []
    expect(childPaths).toEqual([
      '',
      'profile',
      'ai',
      'ai/provider-configs',
      'user',
      'user/:id',
      'organization',
      'role',
      'permission',
      'activity',
      'activity/:id',
      'clue',
      'clue/detail/:id',
      'customer',
      'customer/:id',
      'product',
      'product/category',
      'product/promotion',
      'product/stock',
      'opportunity',
      'test-drive',
      'follow',
      'quote',
      'delivery',
      'tran',
      'tran/:id',
      'tran/approve/:id',
      'tran/invoice/:id',
      'dict/type',
      'dict/value',
      'audit/login',
      'audit/operation',
    ])
  })

  it('protects the organization management route with its list permission', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    const organizationRoute = dashboardRoute?.children?.find(
      (route) => route.path === 'organization',
    )

    expect(organizationRoute?.name).toBe('organization')
    expect(organizationRoute?.meta?.activeMenu).toBe('/dashboard/organization')
    expect(organizationRoute?.meta?.permission).toBe('organization:list')
  })

  it('exposes profile to every authenticated user without a business permission', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    const profileRoute = dashboardRoute?.children?.find((route) => route.path === 'profile')

    expect(profileRoute?.name).toBe('profile')
    expect(profileRoute?.meta?.requiresAuth).toBe(true)
    expect(profileRoute?.meta?.activeMenu).toBe('/dashboard/profile')
    expect(profileRoute?.meta?.permission).toBeUndefined()
    expect(profileRoute?.meta?.allowDuringPendingAdminSetup).toBe(true)
  })

  it('defines the minimal user detail route under the user menu', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    const userDetailRoute = dashboardRoute?.children?.find((route) => route.path === 'user/:id')

    expect(userDetailRoute?.name).toBe('user-detail')
    expect(userDetailRoute?.meta?.activeMenu).toBe('/dashboard/user')
    expect(userDetailRoute?.meta?.permission).toBe('user:view')
  })

  it('defines explicit protected routes for roles and the read-only permission catalog', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    const roleRoute = dashboardRoute?.children?.find((route) => route.path === 'role')
    const permissionRoute = dashboardRoute?.children?.find((route) => route.path === 'permission')

    expect(roleRoute?.name).toBe('role-management')
    expect(roleRoute?.meta?.permission).toBe('role:list')
    expect(permissionRoute?.name).toBe('permission-catalog')
    expect(permissionRoute?.meta?.permission).toBe('permission:list')
  })

  it('keeps dictionary pages highlighted under the single sidebar entry', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    const dictTypeRoute = dashboardRoute?.children?.find((route) => route.path === 'dict/type')
    const dictValueRoute = dashboardRoute?.children?.find((route) => route.path === 'dict/value')

    expect(dictTypeRoute?.meta?.activeMenu).toBe('/dashboard/dict/type')
    expect(dictValueRoute?.meta?.activeMenu).toBe('/dashboard/dict/type')
    expect(dictTypeRoute?.meta?.title).toBe('字典管理')
    expect(dictValueRoute?.meta?.title).toBe('字典管理')
  })
})
