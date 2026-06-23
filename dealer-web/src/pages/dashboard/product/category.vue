<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">产品分类</h2>
            <span
              class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]"
            >
              {{ total }} 个
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
            维护产品列表的分类筛选、归类编码和启用状态。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Button
            v-has-permission="PERMISSIONS.product.category.add"
            class="gap-2"
            @click="handleAdd"
          >
            <Plus class="h-4 w-4" />
            新增分类
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="loading" class="py-10 text-center text-[var(--crm-text-tertiary)]">
          加载中...
        </div>
        <Table v-else class="min-w-[900px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead
                class="w-[80px]"
                sortable
                sort-key="id"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >ID</TableHead
              >
              <TableHead
                class="w-[180px]"
                sortable
                sort-key="name"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >分类名称</TableHead
              >
              <TableHead
                class="w-[180px]"
                sortable
                sort-key="code"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >分类编码</TableHead
              >
              <TableHead
                sortable
                sort-key="description"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >描述</TableHead
              >
              <TableHead
                class="w-[100px]"
                sortable
                sort-key="sort"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >排序</TableHead
              >
              <TableHead
                class="w-[100px]"
                sortable
                sort-key="status"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >状态</TableHead
              >
              <TableHead class="w-[100px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="displayCategoryList.length === 0">
              <TableCell colspan="7" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                >暂无分类数据</TableCell
              >
            </TableRow>
            <TableRow v-for="row in displayCategoryList" :key="row.id">
              <TableCell class="text-[var(--crm-text-tertiary)]">{{ row.id }}</TableCell>
              <TableCell
                class="max-w-[200px] truncate font-semibold text-[var(--crm-text-primary)]"
                >{{ row.name || '--' }}</TableCell
              >
              <TableCell class="max-w-[200px] truncate">
                <span
                  class="inline-flex rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 font-mono text-xs text-[var(--crm-text-secondary)]"
                >
                  {{ row.code || '--' }}
                </span>
              </TableCell>
              <TableCell class="max-w-[260px] truncate">{{ row.description || '--' }}</TableCell>
              <TableCell class="text-[var(--crm-text-secondary)]">{{ row.sort ?? '--' }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatEnabledStatus(row.status)"
                  :tone="getEnabledTone(row.status)"
                />
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.category.edit"
                    label="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.category.delete"
                    label="删除"
                    danger
                    @click="handleDelete(row)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </RowActionButton>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="crm-table-footer">
        <DataTablePagination
          :page="currentPage"
          :page-size="pageSize"
          :total="total"
          @change="handleCurrentChange"
        />
      </div>
    </section>

    <!-- 分类表单对话框 -->
    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{{ dialogType === 'add' ? '新增分类' : '编辑分类' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div class="space-y-2">
            <Label>分类名称</Label>
            <Input v-model="categoryForm.name" />
          </div>
          <div class="space-y-2">
            <Label>分类编码</Label>
            <Input v-model="categoryForm.code" />
          </div>
          <div class="space-y-2">
            <Label>描述</Label>
            <Textarea v-model="categoryForm.description" />
          </div>
          <div class="space-y-2">
            <Label>排序</Label>
            <NumberField v-model="categoryForm.sort" :min="0">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="space-y-2">
            <Label>状态</Label>
            <Select v-model="categoryForm.status">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="启用">启用</SelectItem>
                <SelectItem value="禁用">禁用</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false" :disabled="submitting">取消</Button>
          <Button @click="handleSubmit" :disabled="submitting">{{
            submitting ? '提交中...' : '确定'
          }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { onMounted, ref } from 'vue'

import {
  createCategory,
  deleteCategory,
  getCategoryList,
  updateCategory,
} from '@/modules/product/api/product-api'
import type { ProductCategory } from '@/modules/product/model/product.types'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { useClientSort } from '@/shared/utils/table-sort'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  NumberField,
  NumberFieldContent,
  NumberFieldDecrement,
  NumberFieldIncrement,
  NumberFieldInput,
} from '@/components/ui/number-field'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'ProductCategoryView' })

const categoryList = ref<ProductCategory[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const {
  sortBy,
  sortDirection,
  sortedRows: displayCategoryList,
  toggleSort,
} = useClientSort<ProductCategory>(categoryList, {
  id: 'id',
  name: 'name',
  code: 'code',
  description: 'description',
  sort: 'sort',
  status: (row) => formatEnabledStatus(row.status),
})
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
type CategoryFormState = {
  id?: number | string
  name: string
  code: string
  description: string
  sort: number
  status: string
}

const categoryForm = ref<CategoryFormState>({
  name: '',
  code: '',
  description: '',
  sort: 0,
  status: '启用',
})

async function loadCategories(): Promise<void> {
  try {
    loading.value = true
    const res = await getCategoryList({ page: currentPage.value, size: pageSize.value })
    categoryList.value = res.list
    total.value = res.total
  } catch {
    messageTip('加载分类列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleAdd(): void {
  dialogType.value = 'add'
  categoryForm.value = { name: '', code: '', description: '', sort: 0, status: '启用' }
  dialogVisible.value = true
}

function handleEdit(row: ProductCategory): void {
  dialogType.value = 'edit'
  categoryForm.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row: ProductCategory): Promise<void> {
  try {
    await messageConfirm('确认删除该分类？')
    if (row.id === undefined) return
    await deleteCategory(row.id)
    messageTip('删除成功', 'success')
    await loadCategories()
  } catch (error) {
    if (error !== 'cancel') messageTip('删除失败', 'error')
  }
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      name: categoryForm.value.name.trim(),
      code: categoryForm.value.code.trim(),
      description: categoryForm.value.description?.trim() ?? '',
      sort: Number(categoryForm.value.sort ?? 0),
      status: categoryForm.value.status,
    }
    if (dialogType.value === 'add') {
      await createCategory(payload)
      messageTip('新增成功', 'success')
    } else if (categoryForm.value.id !== undefined) {
      await updateCategory(String(categoryForm.value.id), payload)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch {
    messageTip('操作失败', 'error')
  } finally {
    submitting.value = false
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadCategories()
}

function formatEnabledStatus(status?: string): string {
  if (['启用', 'ENABLED', 'enabled', 'ACTIVE', 'active'].includes(status ?? '')) {
    return '启用'
  }
  if (['禁用', 'DISABLED', 'disabled', 'INACTIVE', 'inactive'].includes(status ?? '')) {
    return '禁用'
  }
  return status || '--'
}

function getEnabledTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  return formatEnabledStatus(status) === '启用' ? 'success' : 'muted'
}

onMounted(() => {
  void loadCategories()
})
</script>
