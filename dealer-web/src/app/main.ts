import { createApp } from 'vue'

import App from '@/App.vue'
import { installPermissionDirective } from '@/app/directives/has-permission'
import { pinia } from '@/app/plugins/pinia'
import { registerSessionInvalidHandler } from '@/shared/auth/session-invalid-handler'
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

app.mount('#app')
