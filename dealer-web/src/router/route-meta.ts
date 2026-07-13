import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth: boolean
    title?: string
    activeMenu?: string
    permission?: string
    allowDuringPasswordChange?: boolean
    allowDuringUserManagementGate?: boolean
    allowDuringPendingAdminSetup?: boolean
    allowDuringRecoveryBootstrap?: boolean
  }
}
