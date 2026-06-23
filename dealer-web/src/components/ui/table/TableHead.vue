<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import type { SortDirection } from '@/shared/utils/table-sort'
import { cn } from '@/lib/utils'
import { ArrowDown, ArrowUp, ChevronsUpDown } from '@lucide/vue'

const props = defineProps<{
  class?: HTMLAttributes['class']
  sortable?: boolean
  sortKey?: string
  sortBy?: string
  sortDirection?: SortDirection
}>()

const emit = defineEmits<{
  sort: [key: string]
}>()

function handleSort() {
  if (props.sortable && props.sortKey) {
    emit('sort', props.sortKey)
  }
}
</script>

<template>
  <th
    data-slot="table-head"
    :class="
      cn(
        'h-11 px-4 text-left align-middle text-xs font-semibold text-[var(--crm-text-tertiary)] whitespace-nowrap [&:has([role=checkbox])]:pr-0',
        props.class,
      )
    "
  >
    <button
      v-if="sortable"
      class="inline-flex max-w-full items-center gap-1.5 rounded-sm text-left transition-colors hover:text-[var(--crm-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--crm-primary)]"
      type="button"
      @click="handleSort"
    >
      <span class="truncate"><slot /></span>
      <ArrowUp
        v-if="sortBy === sortKey && sortDirection === 'asc'"
        class="h-3.5 w-3.5 shrink-0 text-[var(--crm-primary)]"
      />
      <ArrowDown
        v-else-if="sortBy === sortKey && sortDirection === 'desc'"
        class="h-3.5 w-3.5 shrink-0 text-[var(--crm-primary)]"
      />
      <ChevronsUpDown v-else class="h-3.5 w-3.5 shrink-0 text-[var(--crm-text-disabled)]" />
    </button>
    <slot v-else />
  </th>
</template>
