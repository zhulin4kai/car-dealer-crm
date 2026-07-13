<template>
  <form class="grid gap-3 lg:grid-cols-4" @submit.prevent="submit">
    <Input v-model="form.keyword" aria-label="关键词" placeholder="姓名、账号或员工编号" />
    <select v-model="form.organizationUnitId" aria-label="组织" class="filter-select">
      <option value="">全部组织</option>
      <option v-for="item in options.organizations" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.positionId" aria-label="岗位" class="filter-select">
      <option value="">全部岗位</option>
      <option v-for="item in options.positions" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.managerEmployeeId" aria-label="直属管理者" class="filter-select">
      <option value="">全部直属管理者</option>
      <option v-for="item in options.managers" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.roleId" aria-label="角色" class="filter-select">
      <option value="">全部角色</option>
      <option v-for="item in options.roles" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.employmentStatus" aria-label="任职状态" class="filter-select">
      <option value="">全部任职状态</option>
      <option v-for="item in options.employmentStatuses" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.accountStatus" aria-label="账号状态" class="filter-select">
      <option value="">全部账号状态</option>
      <option v-for="item in options.accountStatuses" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <select v-model="form.lockStatus" aria-label="锁定状态" class="filter-select">
      <option value="">全部锁定状态</option>
      <option v-for="item in options.lockStatuses" :key="item.id" :value="String(item.id)">
        {{ item.label }}
      </option>
    </select>
    <div class="flex gap-2 lg:col-span-4 lg:justify-end">
      <Button type="button" variant="outline" :disabled="loading" @click="reset">重置</Button>
      <Button type="submit" :disabled="loading">{{ loading ? '查询中...' : '查询' }}</Button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { UserFilterOptions } from '@/modules/user/model/user.types'
import type { UserFilterValues } from '@/modules/user/composables/use-user-list'

const props = defineProps<{
  modelValue: UserFilterValues
  options: UserFilterOptions
  loading?: boolean
}>()

const emit = defineEmits<{
  search: [filters: UserFilterValues]
  reset: []
}>()

const form = reactive<UserFilterValues>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (value) => Object.assign(form, value),
  { deep: true },
)

function submit(): void {
  emit('search', { ...form })
}

function reset(): void {
  Object.keys(form).forEach((key) => {
    form[key as keyof UserFilterValues] = ''
  })
  emit('reset')
}
</script>

<style scoped>
.filter-select {
  height: 2.25rem;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
</style>
