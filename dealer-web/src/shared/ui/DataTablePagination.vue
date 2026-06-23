<template>
  <div class="flex w-full flex-wrap items-center justify-between gap-3">
    <div class="text-sm text-[var(--crm-text-tertiary)]">共 {{ formatNumber(total) }} 条记录</div>

    <div class="flex items-center gap-1">
      <Button
        variant="ghost"
        size="sm"
        class="h-8 gap-1 px-2 text-[var(--crm-text-secondary)]"
        type="button"
        :disabled="currentPage <= 1"
        @click="onPageChange(currentPage - 1)"
      >
        <ChevronLeft class="h-4 w-4" />
        <span>上一页</span>
      </Button>

      <template v-for="item in paginationItems" :key="item.key">
        <Button
          v-if="item.type === 'page'"
          variant="outline"
          size="icon-sm"
          class="h-8 w-8 border-[var(--crm-border-light)]"
          :class="
            item.value === currentPage
              ? 'border-[var(--crm-primary)] bg-[var(--crm-primary)] text-white hover:bg-[var(--crm-primary-hover)] hover:text-white'
              : 'text-[var(--crm-text-secondary)] hover:border-[var(--crm-primary)] hover:text-[var(--crm-primary)]'
          "
          type="button"
          @click="onPageChange(item.value)"
        >
          {{ item.value }}
        </Button>
        <span
          v-else
          class="flex h-8 w-8 items-center justify-center text-sm text-[var(--crm-text-tertiary)]"
        >
          ...
        </span>
      </template>

      <Button
        variant="ghost"
        size="sm"
        class="h-8 gap-1 px-2 text-[var(--crm-text-secondary)]"
        type="button"
        :disabled="currentPage >= pageCount"
        @click="onPageChange(currentPage + 1)"
      >
        <span>下一页</span>
        <ChevronRight class="h-4 w-4" />
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { formatNumber } from '@/shared/utils/display-format'
import { ChevronLeft, ChevronRight } from '@lucide/vue'

const props = defineProps<{
  page?: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()

type PaginationItemView =
  | { type: 'page'; value: number; key: string }
  | { type: 'ellipsis'; key: string }

const DEFAULT_PAGE_SIZE = 10

const safePageSize = computed(() => toPositiveInteger(props.pageSize, DEFAULT_PAGE_SIZE))
const safeTotal = computed(() => Math.max(toNonNegativeInteger(props.total, 0), 0))
const pageCount = computed(() => Math.max(Math.ceil(safeTotal.value / safePageSize.value), 1))
const currentPage = ref(normalizePageValue(props.page, 1))
const paginationItems = computed<PaginationItemView[]>(() => buildPaginationItems())

function toPositiveInteger(value: unknown, fallback: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return fallback
  }
  const integer = Math.floor(value)
  return integer > 0 ? integer : fallback
}

function toNonNegativeInteger(value: unknown, fallback: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return fallback
  }
  const integer = Math.floor(value)
  return integer >= 0 ? integer : fallback
}

function normalizePageValue(value: unknown, fallback: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return fallback
  }
  const integer = Math.floor(value)
  if (integer < 1) {
    return 1
  }
  return Math.min(integer, pageCount.value)
}

function buildPaginationItems(): PaginationItemView[] {
  const totalPages = pageCount.value
  const current = currentPage.value
  const pages = new Set<number>([1, totalPages, current - 1, current, current + 1])

  if (current <= 4) {
    pages.add(2)
    pages.add(3)
    pages.add(4)
  }
  if (current >= totalPages - 3) {
    pages.add(totalPages - 3)
    pages.add(totalPages - 2)
    pages.add(totalPages - 1)
  }

  const normalizedPages = Array.from(pages)
    .filter((page) => page >= 1 && page <= totalPages)
    .sort((a, b) => a - b)

  const items: PaginationItemView[] = []
  normalizedPages.forEach((page, index) => {
    const previousPage = normalizedPages[index - 1]
    if (previousPage != null && page - previousPage > 1) {
      items.push({ type: 'ellipsis', key: `ellipsis-${previousPage}-${page}` })
    }
    items.push({ type: 'page', value: page, key: `page-${page}` })
  })

  return items
}

function onPageChange(page: unknown) {
  if (typeof page !== 'number' || !Number.isFinite(page)) {
    return
  }
  const nextPage = normalizePageValue(page, currentPage.value)
  if (nextPage === currentPage.value) {
    return
  }
  currentPage.value = nextPage
  emit('change', nextPage)
}

watch(
  () => [safeTotal.value, safePageSize.value],
  () => {
    if (currentPage.value > pageCount.value) {
      onPageChange(pageCount.value)
    }
  },
)

watch(
  () => props.page,
  (page) => {
    const nextPage = normalizePageValue(page, currentPage.value)
    if (nextPage !== currentPage.value) {
      currentPage.value = nextPage
    }
  },
)
</script>
