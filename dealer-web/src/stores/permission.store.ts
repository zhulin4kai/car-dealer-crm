import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchLoginInfo } from '@/modules/user/api/user-api'
import type { Permission } from '@/modules/user/model/user.types'
import {
  clearPermissionCodes,
  readPermissionCodes,
  writePermissionCodes,
} from '@/shared/storage/permission-storage'

export const usePermissionStore = defineStore('permission', () => {
  const permissionList = ref<string[]>([])
  const menuPermissionList = ref<Permission[]>([])

  const hasMenu = computed(() => menuPermissionList.value.length > 0)
  const firstAccessibleMenuUrl = computed(() => {
    for (const menu of menuPermissionList.value) {
      if (menu.url) return menu.url
      const child = menu.subPermissionList?.find((item) => item.url)
      if (child?.url) return child.url
    }
    return null
  })

  function restorePermissions(): void {
    permissionList.value = readPermissionCodes() ?? []
  }

  function setPermissionsFromUser(user: { permissionList?: string[]; menuPermissionList?: Permission[] }): void {
    permissionList.value = user.permissionList ?? []
    menuPermissionList.value = user.menuPermissionList ?? []
    writePermissionCodes(permissionList.value)
  }

  async function loadPermissions(): Promise<void> {
    const user = await fetchLoginInfo()
    setPermissionsFromUser(user)
  }

  function hasPermission(code: string): boolean {
    return permissionList.value.includes(code)
  }

  function clearPermissions(): void {
    permissionList.value = []
    menuPermissionList.value = []
    clearPermissionCodes()
  }

  return {
    permissionList,
    menuPermissionList,
    hasMenu,
    firstAccessibleMenuUrl,
    restorePermissions,
    setPermissionsFromUser,
    loadPermissions,
    hasPermission,
    clearPermissions,
  }
})
