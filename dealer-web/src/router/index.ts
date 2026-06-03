import { createRouter, createWebHistory } from 'vue-router'

import { installRouterGuards } from '@/router/guards'
import { routes } from '@/router/routes'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

installRouterGuards(router)

export default router
