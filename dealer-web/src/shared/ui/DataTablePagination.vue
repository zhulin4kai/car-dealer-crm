<template>
  <Pagination
    v-slot="{ page }"
    :page="currentPage"
    :total="total"
    :items-per-page="pageSize"
    :sibling-count="1"
    show-edges
    @update:page="onPageChange"
  >
    <PaginationContent>
      <PaginationPrevious />
      <template v-for="item in page" :key="item.value">
        <PaginationItem v-if="item.type === 'page'">
          <PaginationLink
            :is-active="item.value === currentPage"
            @click="onPageChange(item.value)"
          >
            {{ item.value }}
          </PaginationLink>
        </PaginationItem>
        <PaginationEllipsis v-else />
      </template>
      <PaginationNext />
    </PaginationContent>
  </Pagination>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'

const props = defineProps<{
  page?: number
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()

const currentPage = ref(props.page ?? 1)

function onPageChange(page: number) {
  currentPage.value = page
  emit('change', page)
}

watch(() => props.total, () => {
  if (currentPage.value > Math.ceil(props.total / props.pageSize) && Math.ceil(props.total / props.pageSize) > 0) {
    currentPage.value = 1
    emit('change', 1)
  }
})

watch(() => props.page, (page) => {
  if (typeof page === 'number' && page !== currentPage.value) {
    currentPage.value = page
  }
})
</script>
