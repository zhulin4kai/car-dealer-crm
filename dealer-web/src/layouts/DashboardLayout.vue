<template>
  <div class="flex h-screen overflow-hidden bg-[var(--crm-bg-page)] text-[var(--crm-text-primary)]">
    <aside
      class="flex shrink-0 flex-col border-r border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] transition-[width] duration-200"
      :class="
        isCollapse ? 'w-[var(--crm-sidebar-collapsed-width)]' : 'w-[var(--crm-sidebar-width)]'
      "
    >
      <button
        class="flex h-[var(--crm-header-height)] items-center gap-3 border-b border-[var(--crm-border-light)] px-4 text-left"
        :class="isCollapse ? 'justify-center px-0' : ''"
        type="button"
        @click="backToHome"
      >
        <span
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[var(--crm-primary)] text-white shadow-[0_8px_18px_rgba(51,112,255,0.28)]"
        >
          <Car class="h-5 w-5" />
        </span>
        <span v-if="!isCollapse" class="truncate text-base font-semibold">汽车销售管理系统</span>
      </button>

      <nav class="min-h-0 flex-1 space-y-5 overflow-y-auto px-3 py-4">
        <section
          v-for="section in navigationSections"
          :key="section.key"
          class="space-y-1"
          :aria-label="section.label"
        >
          <div
            v-if="!isCollapse"
            class="px-3 pb-1 text-xs font-semibold tracking-wide text-[var(--crm-text-tertiary)]"
          >
            {{ section.label }}
          </div>
          <router-link
            v-for="menuItem in section.items"
            :key="getMenuKey(menuItem)"
            :to="menuItem.url"
            class="flex h-10 w-full items-center rounded-lg px-3 text-sm font-medium text-[var(--crm-text-secondary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)] focus-visible:outline-none focus-visible:ring-0"
            :class="[
              isCollapse ? 'justify-center px-0' : '',
              isNavigationItemActive(menuItem)
                ? 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]'
                : '',
              menuItem.code === AI_MENU_CODE && !isNavigationItemActive(menuItem)
                ? 'text-[var(--crm-primary)]'
                : '',
            ]"
            :title="menuItem.name"
          >
            <component :is="resolveIcon(menuItem.icon)" class="h-5 w-5 shrink-0" />
            <span v-if="!isCollapse" class="ml-3 truncate">{{ menuItem.name }}</span>
          </router-link>
        </section>
      </nav>

      <div class="border-t border-[var(--crm-border-light)] p-3">
        <button
          v-if="isCollapse"
          class="flex w-full items-center justify-center rounded-lg px-0 py-3 text-[var(--crm-primary)] transition-colors hover:bg-[var(--crm-bg-hover)]"
          type="button"
          title="退出登录"
          aria-label="退出登录"
          @click="logout"
        >
          <div
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-sm font-semibold text-[var(--crm-primary)]"
          >
            {{ getUserFirstChar }}
          </div>
        </button>
        <div v-else class="flex items-center gap-3 rounded-lg px-2 py-3">
          <div
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-sm font-semibold text-[var(--crm-primary)]"
          >
            {{ getUserFirstChar }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm font-semibold">{{ user.name || '管理员' }}</div>
            <div class="mt-0.5 truncate text-xs text-[var(--crm-text-tertiary)]">当前用户</div>
          </div>
          <button
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-[var(--crm-text-tertiary)] transition-colors hover:bg-[var(--crm-danger-bg)] hover:text-[var(--crm-danger)]"
            type="button"
            title="退出登录"
            aria-label="退出登录"
            @click="logout"
          >
            <LogOut class="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1">
      <div class="flex min-w-0 flex-1 flex-col">
        <header
          class="flex h-[var(--crm-header-height)] shrink-0 items-center justify-between border-b border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-6"
        >
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
                <span
                  class="rounded-md bg-[var(--crm-bg-muted)] px-2.5 py-1 text-sm text-[var(--crm-text-tertiary)]"
                >
                  {{ currentDateLabel }}
                </span>
              </div>
            </div>
          </div>

          <Button
            v-if="canUseAi && !isAiPage"
            variant="outline"
            class="shrink-0 border-[#B9CCFF] bg-[#F4F7FF] text-[var(--crm-primary)] hover:bg-[#E8F1FF]"
            type="button"
            @click="openAiPanel"
          >
            <Sparkles class="mr-2 h-4 w-4" />
            AI 助手
          </Button>
          <div v-else aria-hidden="true" />
        </header>

        <main data-testid="dashboard-main" class="min-h-0 flex-1 overflow-auto">
          <router-view v-slot="{ Component, route: viewRoute }">
            <div :key="viewRoute.fullPath" class="min-h-full">
              <component :is="Component" v-if="Component" />
            </div>
          </router-view>
        </main>
      </div>

      <AiSidePanel
        :open="aiAssistantStore.isPanelOpen && !isAiPage"
        :context="aiPageContext"
        @close="aiAssistantStore.closePanel"
        @expand="expandAiPanel"
      />
    </div>

    <AiFloatingButton
      v-if="canUseAi && !isAiPage && !aiAssistantStore.isPanelOpen"
      @click="openAiPanel"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Car, LogOut, PanelLeftClose, PanelLeftOpen, Sparkles } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import AiFloatingButton from '@/modules/ai/components/AiFloatingButton.vue'
import AiSidePanel from '@/modules/ai/components/AiSidePanel.vue'
import type { AiPageContext } from '@/modules/ai/model/ai.types'
import type { Permission, User } from '@/modules/user/model/user.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import { resolveIcon } from '@/shared/utils/icon-mapper'
import { useAppStore } from '@/stores/app.store'
import { useAiAssistantStore } from '@/stores/ai-assistant.store'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'DashboardLayout',
})

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const aiAssistantStore = useAiAssistantStore()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

const user = computed<User>(() => authStore.currentUser ?? {})
const isCollapse = computed(() => appStore.sidebarCollapsed)
const canUseAi = computed(() => permissionStore.hasPermission(PERMISSIONS.ai.assistantUse))
const isAiPage = computed(() => route.name === 'ai-assistant' || route.name === 'ai-provider-configs')
const currentRouterPath = computed(() => String(route.meta.activeMenu ?? route.path))
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
const routePageContext = computed<AiPageContext>(() => {
  const id = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
  const objectId = typeof id === 'string' ? id : undefined
  switch (route.name) {
    case 'customer-detail':
      return { objectType: 'CUSTOMER', objectId }
    case 'clue-detail':
      return { objectType: 'CLUE', objectId }
    case 'tran-detail':
    case 'tran-approve':
    case 'tran-invoice':
      return { objectType: 'TRANSACTION', objectId }
    default:
      return {}
  }
})
const aiPageContext = computed(() => aiAssistantStore.context)

type NavigationMenuItem = Omit<Permission, 'url' | 'subPermissionList'> & {
  name: string
  url: string
}

type NavigationSection = {
  key: string
  label: string
  items: NavigationMenuItem[]
}

type NavigationSectionDefinition = Omit<NavigationSection, 'items'> & {
  matches: (item: NavigationMenuItem) => boolean
}

const PRODUCT_MENU_CODE = 'menu:product'
const DICT_MENU_CODE = 'menu:dict'
const AUDIT_MENU_CODE = 'menu:audit'
const AI_MENU_CODE = 'menu:ai'
const BUSINESS_MENU_CODES = new Set([
  'menu:dashboard',
  'menu:activity',
  'menu:clue',
  'menu:customer',
  'menu:opportunity',
  'menu:test-drive',
  'menu:quote',
  'menu:delivery',
  'menu:tran',
])
const SYSTEM_MENU_CODES = new Set(['menu:user', DICT_MENU_CODE, AUDIT_MENU_CODE])
const MENU_ITEM_OVERRIDES: Record<string, Partial<Pick<NavigationMenuItem, 'name' | 'icon'>>> = {
  'menu:dashboard': { name: '工作台', icon: 'Gauge' },
  [AI_MENU_CODE]: { name: 'AI 助手', icon: 'Sparkles' },
}
const PRODUCT_ITEM_OVERRIDES: Record<string, Pick<NavigationMenuItem, 'name' | 'icon'>> = {
  'page:product:list': { name: '产品列表', icon: 'Box' },
  'page:product:category': { name: '产品分类', icon: 'Tag' },
  'page:product:promotion': { name: '促销管理', icon: 'BadgePercent' },
  'page:product:stock': { name: '库存管理', icon: 'Warehouse' },
}
const DICT_ITEM_OVERRIDE: Pick<NavigationMenuItem, 'name' | 'icon'> = {
  name: '字典管理',
  icon: 'Grid',
}
const NAVIGATION_SECTION_DEFINITIONS: NavigationSectionDefinition[] = [
  {
    key: 'assistant',
    label: '智能助手',
    matches: (item) => item.code === AI_MENU_CODE,
  },
  {
    key: 'business',
    label: '业务管理',
    matches: (item) => BUSINESS_MENU_CODES.has(item.code),
  },
  {
    key: 'product',
    label: '产品中心',
    matches: (item) => item.code.startsWith('page:product') || item.code === PRODUCT_MENU_CODE,
  },
  {
    key: 'system',
    label: '系统',
    matches: (item) =>
      SYSTEM_MENU_CODES.has(item.code) ||
      item.code.startsWith('page:dict') ||
      item.code.startsWith('page:audit'),
  },
]
const NAVIGATION_ITEM_ORDER: Record<string, number> = {
  [AI_MENU_CODE]: 1,
  'menu:dashboard': 1,
  'menu:activity': 3,
  'menu:clue': 4,
  'menu:customer': 5,
  'menu:opportunity': 6,
  'page:opportunity:list': 5,
  'menu:quote': 7,
  'page:quote:list': 6,
  'menu:tran': 8,
  'page:product:list': 1,
  'page:product:category': 2,
  'page:product:promotion': 3,
  'page:product:stock': 4,
  'menu:user': 1,
  [DICT_MENU_CODE]: 2,
  'page:audit:login': 3,
  'page:audit:operation': 4,
}

const routablePathSet = computed(() => new Set(router.getRoutes().map((item) => item.path)))

const navigationMenuItems = computed(() =>
  permissionStore.menuPermissionList.flatMap(toFlatNavigationItems),
)
const navigationSections = computed<NavigationSection[]>(() => {
  const sections = NAVIGATION_SECTION_DEFINITIONS.map((section) => ({
    key: section.key,
    label: section.label,
    items: [] as NavigationMenuItem[],
  }))
  const fallbackSection: NavigationSection = {
    key: 'other',
    label: '其他',
    items: [],
  }

  navigationMenuItems.value.forEach((item) => {
    const sectionIndex = NAVIGATION_SECTION_DEFINITIONS.findIndex((section) =>
      section.matches(item),
    )
    if (sectionIndex >= 0) {
      sections[sectionIndex].items.push(item)
      return
    }
    fallbackSection.items.push(item)
  })

  return [...sections, fallbackSection]
    .filter((section) => section.items.length > 0)
    .map((section) => ({
      ...section,
      items: sortNavigationItems(section.items),
    }))
})

function sortByOrder<T extends { orderNo?: number }>(items: T[]): T[] {
  return [...items].sort((current, next) => (current.orderNo ?? 0) - (next.orderNo ?? 0))
}

function sortNavigationItems(items: NavigationMenuItem[]): NavigationMenuItem[] {
  return [...items].sort(
    (current, next) =>
      (NAVIGATION_ITEM_ORDER[current.code] ?? current.orderNo ?? 0) -
      (NAVIGATION_ITEM_ORDER[next.code] ?? next.orderNo ?? 0),
  )
}

function createNavigationItem(
  menuPermission: Permission,
  urlOverride?: string,
  overrides: Partial<Pick<NavigationMenuItem, 'name' | 'icon'>> = {},
): NavigationMenuItem | null {
  const url = normalizeMenuUrl(urlOverride ?? menuPermission.url)
  if (!url) {
    return null
  }
  const itemOverrides = {
    ...MENU_ITEM_OVERRIDES[menuPermission.code],
    ...overrides,
  }

  return {
    id: menuPermission.id,
    code: menuPermission.code,
    type: menuPermission.type,
    parentId: menuPermission.parentId,
    orderNo: menuPermission.orderNo,
    enabled: menuPermission.enabled,
    name: itemOverrides.name ?? menuPermission.name ?? menuPermission.code,
    icon: itemOverrides.icon ?? menuPermission.icon,
    url,
  }
}

function toFlatNavigationItems(menuPermission: Permission): NavigationMenuItem[] {
  const subPermissionList = sortByOrder(menuPermission.subPermissionList ?? [])

  if (menuPermission.code === PRODUCT_MENU_CODE) {
    return subPermissionList
      .map((subPermission) =>
        createNavigationItem(
          subPermission,
          subPermission.url,
          PRODUCT_ITEM_OVERRIDES[subPermission.code],
        ),
      )
      .filter((item): item is NavigationMenuItem => item !== null)
  }

  if (menuPermission.code === DICT_MENU_CODE) {
    const firstRoutableSubMenu = subPermissionList
      .map((subPermission) => createNavigationItem(subPermission))
      .find((item): item is NavigationMenuItem => item !== null)

    return firstRoutableSubMenu
      ? [createNavigationItem(menuPermission, firstRoutableSubMenu.url, DICT_ITEM_OVERRIDE)].filter(
          (item): item is NavigationMenuItem => item !== null,
        )
      : []
  }

  const directItem = createNavigationItem(menuPermission)
  if (directItem) {
    return [directItem]
  }

  const routableSubMenuItems = subPermissionList
    .map((subPermission) => createNavigationItem(subPermission))
    .filter((item): item is NavigationMenuItem => item !== null)

  if (routableSubMenuItems.length === 1) {
    return [
      createNavigationItem(menuPermission, routableSubMenuItems[0].url, {
        name: menuPermission.name ?? routableSubMenuItems[0].name,
        icon: menuPermission.icon ?? routableSubMenuItems[0].icon,
      }),
    ].filter((item): item is NavigationMenuItem => item !== null)
  }

  return routableSubMenuItems
}

function isNavigationItemActive(menuItem: NavigationMenuItem): boolean {
  if (menuItem.code === DICT_MENU_CODE && route.path.startsWith('/dashboard/dict')) {
    return true
  }
  return currentRouterPath.value === menuItem.url
}

function normalizeMenuUrl(url?: string): string | null {
  const trimmed = url?.trim()
  if (!trimmed || trimmed === 'undefined' || trimmed === 'null' || !trimmed.startsWith('/')) {
    return null
  }

  return routablePathSet.value.has(trimmed) ? trimmed : null
}

function getMenuKey(menuPermission: Permission): string {
  return String(menuPermission.id ?? menuPermission.code)
}

function showMenu(): void {
  appStore.toggleSidebar()
}

async function logout(): Promise<void> {
  try {
    await authStore.logout()
    permissionStore.clearPermissions()
    aiAssistantStore.reset()
    messageTip('退出成功', 'success')
    await router.push('/')
  } catch {
    messageTip('退出失败，请稍后重试', 'error')
  }
}

function backToHome(): void {
  void router.push('/dashboard')
}

function openAiPanel(): void {
  aiAssistantStore.openPanel(routePageContext.value)
}

function expandAiPanel(payload?: { conversationNo?: string; runNo?: string }): void {
  aiAssistantStore.closePanel()
  void router.push({
    name: 'ai-assistant',
    query: payload?.conversationNo
      ? { conversationNo: payload.conversationNo }
      : payload?.runNo
        ? { runNo: payload.runNo }
        : undefined,
  })
}

watch(
  () => route.fullPath,
  () => {
    if (aiAssistantStore.isPanelOpen) aiAssistantStore.setContext(routePageContext.value)
  },
)

onMounted(async () => {
  if (!authStore.currentUser) {
    await authStore.loadCurrentUser()
  }
  if (!permissionStore.hasMenu) {
    await permissionStore.loadPermissions()
  }
})
</script>
