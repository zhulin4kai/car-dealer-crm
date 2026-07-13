import { createApp } from 'vue'

import App from '@/App.vue'
import { installPermissionDirective } from '@/app/directives/has-permission'
import { pinia } from '@/app/plugins/pinia'
import { registerSessionInvalidHandler } from '@/shared/auth/session-invalid-handler'
import { registerUserManagementGateHandler } from '@/shared/auth/user-management-gate-handler'
import router from '@/router'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

import '@/assets/index.css'

const app = createApp(App)

app.use(pinia)
installPermissionDirective(app)
app.use(router)

registerSessionInvalidHandler({
  handleSessionInvalid() {
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()
    authStore.forceLogout()
    permissionStore.clearPermissions()
    const current = router.currentRoute.value
    if (current.name !== 'login') {
      return router.replace({ name: 'login', query: { redirect: current.fullPath } })
    }
  },
})

registerUserManagementGateHandler({
  async handleUserManagementGate(context) {
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()
    try {
      const user = await authStore.loadCurrentUser()
      permissionStore.setPermissionsFromUser(user)
    } catch {
      // 门禁页只依赖现有登录态和稳定错误码，刷新登录信息失败时仍需完成阻断跳转。
    }
    await router.replace({
      name: 'user-management-gate',
      query: { code: String(context.code) },
    })
  },
})

app.mount('#app')
