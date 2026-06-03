import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const activeMenu = ref('')
  const globalLoading = ref(false)

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setActiveMenu(path: string): void {
    activeMenu.value = path
  }

  function setGlobalLoading(loading: boolean): void {
    globalLoading.value = loading
  }

  return {
    sidebarCollapsed,
    activeMenu,
    globalLoading,
    toggleSidebar,
    setActiveMenu,
    setGlobalLoading,
  }
})
