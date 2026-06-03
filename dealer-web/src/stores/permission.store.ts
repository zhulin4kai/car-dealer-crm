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

  function restorePermissions(): void {
    permissionList.value = readPermissionCodes() ?? []
  }

  async function loadPermissions(): Promise<void> {
    const user = await fetchLoginInfo()
    permissionList.value = user.permissionList ?? []
    menuPermissionList.value = user.menuPermissionList ?? []
    writePermissionCodes(permissionList.value)
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
    restorePermissions,
    loadPermissions,
    hasPermission,
    clearPermissions,
  }
})
