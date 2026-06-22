<template>
  <div class="flex h-screen overflow-hidden bg-[var(--crm-bg-page)] text-[var(--crm-text-primary)]">
    <aside
      class="flex shrink-0 flex-col border-r border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] transition-[width] duration-200"
      :class="isCollapse ? 'w-[var(--crm-sidebar-collapsed-width)]' : 'w-[var(--crm-sidebar-width)]'"
    >
      <button
        class="flex h-[var(--crm-header-height)] items-center gap-3 border-b border-[var(--crm-border-light)] px-4 text-left"
        :class="isCollapse ? 'justify-center px-0' : ''"
        type="button"
        @click="backToHome"
      >
        <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[var(--crm-primary)] text-white shadow-[0_8px_18px_rgba(51,112,255,0.28)]">
          <Car class="h-5 w-5" />
        </span>
        <span v-if="!isCollapse" class="truncate text-base font-semibold">汽车销售管理系统</span>
      </button>

      <nav class="min-h-0 flex-1 overflow-y-auto px-3 py-4">
        <template v-for="menuPermission in visibleMenuPermissionList" :key="getMenuKey(menuPermission)">
          <Collapsible
            v-if="menuPermission.subPermissionList?.length"
            :open="openMenus[getMenuKey(menuPermission)] ?? false"
            @update:open="(open: boolean) => { openMenus[getMenuKey(menuPermission)] = open }"
          >
            <CollapsibleTrigger
              class="mb-1 flex h-10 w-full items-center rounded-lg px-3 text-sm font-medium text-[var(--crm-text-secondary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)]"
              :class="[
                isCollapse ? 'justify-center px-0' : '',
                isMenuActive(menuPermission) ? 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]' : '',
              ]"
            >
              <component :is="resolveIcon(menuPermission.icon)" class="h-5 w-5 shrink-0" />
              <span v-if="!isCollapse" class="ml-3 truncate">{{ menuPermission.name }}</span>
              <ChevronDown
                v-if="!isCollapse"
                class="ml-auto h-4 w-4 transition-transform"
                :class="{ 'rotate-180': openMenus[getMenuKey(menuPermission)] }"
              />
            </CollapsibleTrigger>

            <CollapsibleContent v-if="!isCollapse" class="mb-2 space-y-1">
              <router-link
                v-for="subPermission in menuPermission.subPermissionList"
                :key="getMenuKey(subPermission)"
                :to="subPermission.url"
                class="flex h-10 items-center rounded-lg pl-11 pr-3 text-sm font-medium text-[var(--crm-text-secondary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)]"
                :class="currentRouterPath === subPermission.url ? 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]' : ''"
              >
                <component :is="resolveIcon(subPermission.icon)" class="mr-2 h-4 w-4 shrink-0" />
                <span class="truncate">{{ subPermission.name }}</span>
              </router-link>
            </CollapsibleContent>
          </Collapsible>

          <router-link
            v-else-if="menuPermission.url"
            :to="menuPermission.url"
            class="mb-1 flex h-10 w-full items-center rounded-lg px-3 text-sm font-medium text-[var(--crm-text-secondary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)]"
            :class="[
              isCollapse ? 'justify-center px-0' : '',
              currentRouterPath === menuPermission.url ? 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]' : '',
            ]"
          >
            <component :is="resolveIcon(menuPermission.icon)" class="h-5 w-5 shrink-0" />
            <span v-if="!isCollapse" class="ml-3 truncate">{{ menuPermission.name }}</span>
          </router-link>
        </template>
      </nav>

      <div class="border-t border-[var(--crm-border-light)] p-3">
        <div
          class="flex items-center gap-3 rounded-lg px-2 py-3"
          :class="isCollapse ? 'justify-center px-0' : ''"
        >
          <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-sm font-semibold text-[var(--crm-primary)]">
            {{ getUserFirstChar }}
          </div>
          <div v-if="!isCollapse" class="min-w-0">
            <div class="truncate text-sm font-semibold">{{ user.name || '管理员' }}</div>
            <div class="mt-0.5 truncate text-xs text-[var(--crm-text-tertiary)]">当前用户</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex h-[var(--crm-header-height)] shrink-0 items-center justify-between border-b border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-6">
        <div class="flex min-w-0 items-center gap-4">
          <button
            class="flex h-9 w-9 items-center justify-center rounded-lg text-[var(--crm-text-secondary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)]"
            type="button"
            :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'"
            @click="showMenu"
          >
            <PanelLeftClose v-if="isCollapse" class="h-5 w-5" />
            <PanelLeftOpen v-else class="h-5 w-5" />
          </button>
          <div class="min-w-0">
            <div class="flex items-center gap-3">
              <h1 class="truncate text-lg font-semibold">{{ pageTitle }}</h1>
              <span class="rounded-md bg-[var(--crm-bg-muted)] px-2.5 py-1 text-sm text-[var(--crm-text-tertiary)]">
                {{ currentDateLabel }}
              </span>
            </div>
          </div>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger class="flex items-center gap-2 rounded-lg px-2 py-1.5 outline-none transition-colors hover:bg-[var(--crm-bg-hover)]">
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-sm font-semibold text-[var(--crm-primary)]">
              {{ getUserFirstChar }}
            </div>
            <span class="hidden text-sm font-medium sm:inline">{{ user.name || '管理员' }}</span>
            <ChevronDown class="h-4 w-4 text-[var(--crm-text-tertiary)]" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem @click="logout">退出登录</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>

      <main class="min-h-0 flex-1 overflow-auto">
        <router-view v-slot="{ Component, route: viewRoute }">
          <div :key="viewRoute.fullPath" class="min-h-full">
            <component :is="Component" v-if="Component" />
          </div>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { Permission, User } from '@/modules/user/model/user.types'
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
const currentRouterPath = computed(() => route.meta.activeMenu ?? route.path)
const pageTitle = computed(() => {
  if (route.path === '/dashboard') {
    return '工作台概览'
  }
  return String(route.meta.title ?? '工作台概览')
})
const currentDateLabel = computed(() => {
  const now = new Date()
  return `${now.getMonth() + 1}月${now.getDate()}日`
})
const getUserFirstChar = computed(() => {
  const name = user.value.name?.trim() ?? ''
  return name ? name.charAt(0).toUpperCase() : '管'
})

const openMenus = reactive<Record<string, boolean>>({})

type NavigationMenuItem = Omit<Permission, 'url' | 'subPermissionList'> & {
  url?: string
  subPermissionList?: NavigationMenuItem[]
}

const routablePathSet = computed(() => new Set(router.getRoutes().map((item) => item.path)))

const visibleMenuPermissionList = computed(() =>
  permissionStore.menuPermissionList
    .map(toNavigationMenuItem)
    .filter((item): item is NavigationMenuItem => item !== null)
)

function normalizeMenuUrl(url?: string): string | null {
  const trimmed = url?.trim()
  if (!trimmed || trimmed === 'undefined' || trimmed === 'null' || !trimmed.startsWith('/')) {
    return null
  }

  return routablePathSet.value.has(trimmed) ? trimmed : null
}

function toNavigationMenuItem(menuPermission: Permission): NavigationMenuItem | null {
  const url = normalizeMenuUrl(menuPermission.url) ?? undefined
  const subPermissionList = (menuPermission.subPermissionList ?? [])
    .map(toNavigationMenuItem)
    .filter((item): item is NavigationMenuItem => item !== null)

  if (!url && subPermissionList.length === 0) {
    return null
  }

  return {
    ...menuPermission,
    url,
    subPermissionList,
  }
}

function getMenuKey(menuPermission: Permission): string {
  return String(menuPermission.id ?? menuPermission.code)
}

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
