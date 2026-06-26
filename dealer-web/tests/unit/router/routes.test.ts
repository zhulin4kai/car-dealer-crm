import { describe, expect, it } from 'vitest'

import { routes } from '@/router/routes'

describe('router routes', () => {
  it('keeps the public URL contract', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    expect(dashboardRoute).toBeDefined()
    expect(dashboardRoute?.children).toHaveLength(21)

    const childPaths = dashboardRoute?.children?.map((route) => route.path) ?? []
    expect(childPaths).toEqual([
      '',
      'user',
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
      'quote',
      'delivery',
      'tran',
      'tran/:id',
      'tran/approve/:id',
      'tran/invoice/:id',
      'dict/type',
      'dict/value',
    ])
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
