<template>
  <el-pagination
    background
    layout="prev, pager, next"
    :page-size="pageSize"
    :total="total"
    @current-change="emit('change', $event)"
    @next-click="emit('change', $event)"
    @prev-click="emit('change', $event)"
  />
</template>

<script setup lang="ts">
defineProps<{
  pageSize: number
  total: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()
</script>
