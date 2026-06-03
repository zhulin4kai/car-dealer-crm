import type { App, DirectiveBinding } from 'vue'

import { usePermissionStore } from '@/stores/permission.store'

function removeElement(el: HTMLElement): void {
  el.parentNode?.removeChild(el)
}

export function installPermissionDirective(app: App): void {
  app.directive('has-permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
      const permissionStore = usePermissionStore()
      if (!permissionStore.hasPermission(binding.value)) {
        removeElement(el)
      }
    },
    updated(el: HTMLElement, binding: DirectiveBinding<string>) {
      const permissionStore = usePermissionStore()
      if (!permissionStore.hasPermission(binding.value)) {
        removeElement(el)
      }
    },
  })
}
