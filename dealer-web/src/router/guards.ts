import type { Router } from 'vue-router'

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

    if (to.path === '/' && authStore.isAuthenticated) {
      return '/dashboard'
    }

    if (to.meta.requiresAuth && authStore.isAuthenticated && !permissionStore.hasMenu) {
      try {
        const user = await authStore.loadCurrentUser()
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
