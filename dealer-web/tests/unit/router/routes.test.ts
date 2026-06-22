import { describe, expect, it } from 'vitest'

import { routes } from '@/router/routes'

describe('router routes', () => {
  it('keeps the public URL contract', () => {
    const dashboardRoute = routes.find((route) => route.path === '/dashboard')
    expect(dashboardRoute).toBeDefined()
    expect(dashboardRoute?.children).toHaveLength(18)

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
      'tran',
      'tran/:id',
      'tran/approve/:id',
      'tran/invoice/:id',
      'dict/type',
      'dict/value',
    ])
  })
})
