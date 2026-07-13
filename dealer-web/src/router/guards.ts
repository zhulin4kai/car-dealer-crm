import type { Router } from 'vue-router'

import { USER_MANAGEMENT_GATE_STATE } from '@/modules/user/model/user.types'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

export function installRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()

    authStore.restoreSession()
    permissionStore.restorePermissions()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/'
    }

    if (authStore.isAuthenticated && !authStore.currentUser) {
      try {
        const user = await authStore.loadCurrentUser()
        if (to.meta.requiresAuth && !to.meta.allowDuringPasswordChange) {
          permissionStore.setPermissionsFromUser(user)
        }
      } catch (error) {
        if (isUserManagementGateError(error)) {
          permissionStore.clearPermissions()
          if (to.meta.allowDuringUserManagementGate) return true
          return {
            name: 'user-management-gate',
            query: { code: String(error.code) },
          }
        }
        authStore.forceLogout()
        permissionStore.clearPermissions()
        return to.meta.requiresAuth ? '/' : true
      }
    }

    const currentUser = authStore.currentUser
    const gateState = currentUser?.userManagementGateState ?? USER_MANAGEMENT_GATE_STATE.READY
    const isProtectedRecoveryAccount = currentUser?.protectedRecoveryAccount === true
    if (isProtectedRecoveryAccount) {
      if (to.meta.allowDuringUserManagementGate) return true
      if (
        gateState === USER_MANAGEMENT_GATE_STATE.UNINITIALIZED &&
        to.meta.allowDuringRecoveryBootstrap
      ) {
        return true
      }
      return { name: 'user-management-gate' }
    }

    if (authStore.isAuthenticated && currentUser?.mustChangePassword) {
      if (!to.meta.allowDuringPasswordChange) return '/first-password-change'
      return true
    }

    if (gateState !== USER_MANAGEMENT_GATE_STATE.READY) {
      if (
        gateState === USER_MANAGEMENT_GATE_STATE.PENDING_FIRST_CHANGE &&
        to.meta.allowDuringPendingAdminSetup
      ) {
        return true
      }
      if (to.meta.allowDuringUserManagementGate) return true
      return { name: 'user-management-gate' }
    }

    if (to.path === '/first-password-change' && authStore.isAuthenticated) {
      return '/dashboard'
    }

    if (to.path === '/' && authStore.isAuthenticated) {
      return '/dashboard'
    }

    if (to.name === 'user-management-gate') return '/dashboard'

    if (!to.meta.requiresAuth) return true

    if (authStore.isAuthenticated && !permissionStore.hasMenu) {
      try {
        const user = authStore.currentUser ?? (await authStore.loadCurrentUser())
        permissionStore.setPermissionsFromUser(user)
      } catch {
        authStore.forceLogout()
        permissionStore.clearPermissions()
        return '/'
      }
    }

    if (to.meta.permission && !permissionStore.hasPermission(to.meta.permission)) {
      if (to.path === '/dashboard' && permissionStore.firstAccessibleMenuUrl) {
        return permissionStore.firstAccessibleMenuUrl
      }
      return '/403'
    }

    return true
  })
}

function isUserManagementGateError(error: unknown): error is ApiError {
  return (
    error instanceof ApiError &&
    (error.code === API_ERROR_CODE.ADMIN_BOOTSTRAP_REQUIRED ||
      error.code === API_ERROR_CODE.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN)
  )
}
