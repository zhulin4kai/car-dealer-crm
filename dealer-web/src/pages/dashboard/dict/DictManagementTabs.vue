<template>
  <section class="crm-panel">
    <div
      class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
    >
      <div class="min-w-0">
        <h2 class="text-lg font-semibold">字典管理</h2>
        <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
          维护系统字典类型和字典值，保证下拉选项与业务编码一致。
        </p>
      </div>
      <div
        class="flex shrink-0 rounded-lg bg-[var(--crm-bg-muted)] p-1"
        role="tablist"
        aria-label="字典管理视图切换"
      >
        <router-link
          v-for="tab in visibleTabs"
          :key="tab.path"
          :to="tab.path"
          class="rounded-md px-3 py-1.5 text-sm font-medium transition-colors"
          :class="
            route.path === tab.path
              ? 'bg-[var(--crm-bg-surface)] text-[var(--crm-primary)] shadow-sm'
              : 'text-[var(--crm-text-secondary)] hover:text-[var(--crm-primary)]'
          "
          role="tab"
          :aria-selected="route.path === tab.path"
        >
          {{ tab.label }}
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { PERMISSIONS } from '@/shared/constants/permissions'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'DictManagementTabs',
})

const route = useRoute()
const permissionStore = usePermissionStore()

const tabs = [
  {
    label: '字典类型',
    path: '/dashboard/dict/type',
    permission: PERMISSIONS.dict.type.list,
  },
  {
    label: '字典数据',
    path: '/dashboard/dict/value',
    permission: PERMISSIONS.dict.value.list,
  },
]

const visibleTabs = computed(() =>
  tabs.filter((tab) => permissionStore.hasPermission(tab.permission)),
)
</script>
