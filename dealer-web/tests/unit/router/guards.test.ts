import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router, type RouteRecordRaw } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchLoginInfo } from '@/modules/user/api/user-api'
import { installRouterGuards } from '@/router/guards'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { writeStoredToken } from '@/shared/storage/token-storage'

vi.mock('@/modules/user/api/user-api', () => ({
  fetchLoginInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}))

const DummyPage = { template: '<div />' }
const guardRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DummyPage,
    meta: { requiresAuth: false },
  },
  {
    path: '/forgot-password',
    component: DummyPage,
    meta: { requiresAuth: false },
  },
  {
    path: '/first-password-change',
    component: DummyPage,
    meta: { requiresAuth: true, allowDuringPasswordChange: true },
  },
  {
    path: '/user-management-gate',
    name: 'user-management-gate',
    component: DummyPage,
    meta: { requiresAuth: true, allowDuringUserManagementGate: true },
  },
  {
    path: '/dashboard',
    component: DummyPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard/user',
    name: 'user',
    component: DummyPage,
    meta: { requiresAuth: true, allowDuringRecoveryBootstrap: true },
  },
  {
    path: '/dashboard/profile',
    component: DummyPage,
    meta: { requiresAuth: true, allowDuringPendingAdminSetup: true },
  },
]

describe('router first-password-change guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    vi.mocked(fetchLoginInfo).mockReset()
  })

  it('redirects unauthenticated protected navigation to login', async () => {
    const router = createGuardedRouter()

    await router.push('/dashboard')

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('redirects a first-password-change session away from business pages', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 21,
      mustChangePassword: true,
      permissionList: ['statistic:view'],
      menuPermissionList: [{ code: 'statistic:view', url: '/dashboard' }],
    })
    const router = createGuardedRouter()

    await router.push('/dashboard')

    expect(router.currentRoute.value.path).toBe('/first-password-change')
  })

  it('allows only the password-change route for a restricted session', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({ id: 21, mustChangePassword: true })
    const router = createGuardedRouter()

    await router.push('/first-password-change')
    expect(router.currentRoute.value.path).toBe('/first-password-change')

    await router.push('/forgot-password')
    expect(router.currentRoute.value.path).toBe('/first-password-change')

    await router.push('/dashboard/profile')
    expect(router.currentRoute.value.path).toBe('/first-password-change')
  })

  it('keeps a normal session out of the first-password-change route', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 21,
      mustChangePassword: false,
      permissionList: ['statistic:view'],
      menuPermissionList: [{ code: 'statistic:view', url: '/dashboard' }],
    })
    const router = createGuardedRouter()

    await router.push('/first-password-change')

    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('keeps a ready protected recovery account on the dedicated gate page', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 1,
      mustChangePassword: false,
      protectedRecoveryAccount: true,
      userManagementGateState: 'READY',
      permissionList: ['user:list'],
      menuPermissionList: [{ code: 'menu:user', url: '/dashboard/user' }],
    })
    const router = createGuardedRouter()

    await router.push('/dashboard/profile')

    expect(router.currentRoute.value.path).toBe('/user-management-gate')
  })

  it('never sends a protected recovery account to the personal password-change route', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 1,
      mustChangePassword: true,
      protectedRecoveryAccount: true,
      userManagementGateState: 'READY',
      permissionList: ['user:list'],
      menuPermissionList: [{ code: 'menu:user', url: '/dashboard/user' }],
    })
    const router = createGuardedRouter()

    await router.push('/first-password-change')

    expect(router.currentRoute.value.path).toBe('/user-management-gate')
  })

  it('keeps an ordinary pending administrator on the first-password-change route', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 21,
      mustChangePassword: true,
      protectedRecoveryAccount: false,
      userManagementGateState: 'PENDING_FIRST_CHANGE',
    })
    const router = createGuardedRouter()

    await router.push('/first-password-change')

    expect(router.currentRoute.value.path).toBe('/first-password-change')
  })

  it('allows a pending administrator with completed password change to verify a recovery channel', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 21,
      mustChangePassword: false,
      protectedRecoveryAccount: false,
      userManagementGateState: 'PENDING_FIRST_CHANGE',
    })
    const router = createGuardedRouter()

    await router.push('/dashboard/profile')
    expect(router.currentRoute.value.path).toBe('/dashboard/profile')

    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/user-management-gate')
  })

  it('allows only the user bootstrap workbench for an uninitialized recovery account', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockResolvedValue({
      id: 1,
      mustChangePassword: false,
      protectedRecoveryAccount: true,
      userManagementGateState: 'UNINITIALIZED',
      permissionList: ['user:list'],
      menuPermissionList: [{ code: 'menu:user', url: '/dashboard/user' }],
    })
    const router = createGuardedRouter()

    await router.push('/dashboard/user')
    expect(router.currentRoute.value.path).toBe('/dashboard/user')

    await router.push('/dashboard/profile')
    expect(router.currentRoute.value.path).toBe('/user-management-gate')
  })

  it('preserves the session and shows the gate when login info returns 641', async () => {
    writeStoredToken('session', false)
    vi.mocked(fetchLoginInfo).mockRejectedValue(
      new ApiError(API_ERROR_CODE.ADMIN_BOOTSTRAP_REQUIRED, '初始化未完成', null, false, 403),
    )
    const router = createGuardedRouter()

    await router.push('/dashboard')

    expect(router.currentRoute.value.path).toBe('/user-management-gate')
    expect(localStorage.getItem('dlyk_token')).toBeNull()
    expect(sessionStorage.getItem('dlyk_token')).toBe('session')
  })
})

function createGuardedRouter(): Router {
  const router = createRouter({ history: createMemoryHistory(), routes: guardRoutes })
  installRouterGuards(router)
  return router
}
