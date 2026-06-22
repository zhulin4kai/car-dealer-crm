import type { App, DirectiveBinding } from 'vue'

import { usePermissionStore } from '@/stores/permission.store'

export function setPermissionVisibility(el: HTMLElement, allowed: boolean): void {
  if (allowed) {
    el.style.removeProperty('display')
    el.removeAttribute('aria-hidden')
    return
  }
  el.style.setProperty('display', 'none', 'important')
  el.setAttribute('aria-hidden', 'true')
}

export function installPermissionDirective(app: App): void {
  app.directive('has-permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
      const permissionStore = usePermissionStore()
      setPermissionVisibility(el, permissionStore.hasPermission(binding.value))
    },
    updated(el: HTMLElement, binding: DirectiveBinding<string>) {
      const permissionStore = usePermissionStore()
      setPermissionVisibility(el, permissionStore.hasPermission(binding.value))
    },
  })
}
