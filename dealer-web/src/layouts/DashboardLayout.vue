<template>
  <div class="flex h-screen">
    <!-- 左侧侧边栏 -->
    <aside
      class="flex flex-col border-r transition-all duration-200 bg-sidebar text-sidebar-foreground"
      :class="isCollapse ? 'w-16' : 'w-[200px]'"
    >
      <!-- 标题 -->
      <div
        v-if="!isCollapse"
        class="h-9 mt-2.5 mb-2.5 text-center text-sm cursor-pointer text-muted-foreground hover:text-foreground transition-colors"
        @click="backToHome()"
      >
        @汽车销售管理系统
      </div>
      <div v-else class="h-9 mt-2.5 mb-2.5 flex items-center justify-center">
        <Car class="w-5 h-5 text-muted-foreground" />
      </div>

      <!-- 菜单 -->
      <nav class="flex-1 overflow-y-auto py-1">
        <template v-for="(menuPermission, index) in permissionStore.menuPermissionList" :key="menuPermission.id">
          <!-- 有子菜单 -->
          <Collapsible
            v-if="menuPermission.subPermissionList?.length"
            v-model:open="openMenus[index]"
          >
            <CollapsibleTrigger
              class="flex items-center w-full px-3 py-2 text-sm transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              :class="{ 'justify-center': isCollapse, 'bg-sidebar-accent/50': isMenuActive(menuPermission) }"
            >
              <component :is="resolveIcon(menuPermission.icon)" class="w-4 h-4 shrink-0" />
              <span v-if="!isCollapse" class="ml-2 truncate">{{ menuPermission.name }}</span>
              <ChevronDown
                v-if="!isCollapse"
                class="w-4 h-4 ml-auto transition-transform"
                :class="{ 'rotate-180': openMenus[index] }"
              />
            </CollapsibleTrigger>
            <CollapsibleContent v-if="!isCollapse">
              <router-link
                v-for="subPermission in menuPermission.subPermissionList"
                :key="subPermission.id"
                :to="subPermission.url ?? ''"
                class="flex items-center pl-9 pr-3 py-2 text-sm transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                :class="{ 'bg-sidebar-accent text-sidebar-accent-foreground font-medium': currentRouterPath === subPermission.url }"
              >
                <component :is="resolveIcon(subPermission.icon)" class="w-4 h-4 mr-2 shrink-0" />
                <span class="truncate">{{ subPermission.name }}</span>
              </router-link>
            </CollapsibleContent>
          </Collapsible>

          <!-- 无子菜单 -->
          <router-link
            v-else
            :to="menuPermission.url ?? ''"
            class="flex items-center w-full px-3 py-2 text-sm transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            :class="{ 'justify-center': isCollapse, 'bg-sidebar-accent text-sidebar-accent-foreground font-medium': currentRouterPath === menuPermission.url }"
          >
            <component :is="resolveIcon(menuPermission.icon)" class="w-4 h-4 shrink-0" />
            <span v-if="!isCollapse" class="ml-2 truncate">{{ menuPermission.name }}</span>
          </router-link>
        </template>
      </nav>
    </aside>

    <!-- 右侧内容区 -->
    <div class="flex flex-col flex-1 min-w-0">
      <!-- 顶栏 -->
      <header class="flex items-center justify-between h-9 px-4 border-b bg-background">
        <button
          class="p-1 rounded hover:bg-accent transition-colors cursor-pointer"
          @click="showMenu"
        >
          <PanelLeftClose v-if="isCollapse" class="w-4 h-4" />
          <PanelLeftOpen v-else class="w-4 h-4" />
        </button>

        <DropdownMenu>
          <DropdownMenuTrigger class="flex items-center gap-2 cursor-pointer outline-none">
            <div class="w-6 h-6 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xs font-bold">
              {{ getUserFirstChar }}
            </div>
            <span class="text-sm">{{ user.name }}</span>
            <ChevronDown class="w-3 h-3 text-muted-foreground" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem @click="logout">退出登录</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>

      <!-- 主内容区 -->
      <main class="flex-1 overflow-auto">
        <router-view v-if="isRouterAlive" />
      </main>

      <!-- 底栏 -->
      <footer class="h-9 flex items-center justify-center text-xs text-muted-foreground border-t bg-background">
        徐州工程学院@信息工程学院（大数据学院）
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { User } from '@/modules/user/model/user.types'
import { messageTip } from '@/shared/utils/feedback'
import { resolveIcon } from '@/shared/utils/icon-mapper'
import { useAppStore } from '@/stores/app.store'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Car, ChevronDown, PanelLeftClose, PanelLeftOpen } from '@lucide/vue'

defineOptions({
  name: 'DashboardLayout',
})

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

const user = computed<User>(() => authStore.currentUser ?? {})
const isCollapse = computed(() => appStore.sidebarCollapsed)
const isRouterAlive = computed(() => true)
const currentRouterPath = computed(() => route.meta.activeMenu ?? route.path)
const getUserFirstChar = computed(() => {
  const name = user.value.name?.trim() ?? ''
  return name ? name.charAt(0).toUpperCase() : ''
})

// Track which sub-menus are open
const openMenus = reactive<Record<number, boolean>>({})

function isMenuActive(menuPermission: { subPermissionList?: { url?: string }[] }): boolean {
  return menuPermission.subPermissionList?.some(
    (sub) => currentRouterPath.value === sub.url
  ) ?? false
}

function showMenu(): void {
  appStore.toggleSidebar()
}

async function logout(): Promise<void> {
  await authStore.logout()
  permissionStore.clearPermissions()
  messageTip('退出成功', 'success')
  await router.push('/')
}

function backToHome(): void {
  void router.push('/dashboard')
}

onMounted(async () => {
  if (!authStore.currentUser) {
    await authStore.loadCurrentUser()
  }
  if (!permissionStore.hasMenu) {
    await permissionStore.loadPermissions()
  }
})
</script>
