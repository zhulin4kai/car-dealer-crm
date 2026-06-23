<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">促销管理</h2>
            <span
              class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]"
            >
              {{ total }} 条
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
            维护产品促销活动、折扣规则和生效时间。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Button
            v-has-permission="PERMISSIONS.product.promotion.add"
            class="gap-2"
            @click="handleAdd"
          >
            <Plus class="h-4 w-4" />
            新增促销
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="loading" class="py-10 text-center text-[var(--crm-text-tertiary)]">
          加载中...
        </div>
        <Table v-else class="min-w-[1080px]">
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
                class="w-[220px]"
                sortable
                sort-key="name"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >促销名称</TableHead
              >
              <TableHead
                class="w-[120px]"
                sortable
                sort-key="type"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >促销类型</TableHead
              >
              <TableHead
                class="w-[140px]"
                sortable
                sort-key="discount"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >折扣/金额</TableHead
              >
              <TableHead
                sortable
                sort-key="startTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >开始时间</TableHead
              >
              <TableHead
                sortable
                sort-key="endTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >结束时间</TableHead
              >
              <TableHead
                class="w-[120px]"
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
            <TableRow v-if="displayPromotionList.length === 0">
              <TableCell colspan="8" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                >暂无促销数据</TableCell
              >
            </TableRow>
            <TableRow v-for="row in displayPromotionList" :key="row.id">
              <TableCell class="text-[var(--crm-text-tertiary)]">{{ row.id }}</TableCell>
              <TableCell
                class="max-w-[220px] truncate font-semibold text-[var(--crm-text-primary)]"
                >{{ row.name || '--' }}</TableCell
              >
              <TableCell>
                <StatusBadge
                  :label="formatPromotionType(row.type)"
                  :tone="getPromotionTypeTone(row.type)"
                />
              </TableCell>
              <TableCell class="font-semibold text-[var(--crm-text-primary)]">
                {{ formatPromotionDiscount(row) }}
              </TableCell>
              <TableCell>{{ formatDateTime(row.startTime) }}</TableCell>
              <TableCell>{{ formatDateTime(row.endTime) }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatPromotionStatus(row.status)"
                  :tone="getPromotionStatusTone(row.status)"
                />
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.promotion.edit"
                    label="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.promotion.delete"
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

    <!-- 促销表单对话框 -->
    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{{ dialogType === 'add' ? '新增促销' : '编辑促销' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div class="space-y-2">
            <Label>商品</Label>
            <Select v-model="promotionForm.productId">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择商品" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in productOptions" :key="item.value" :value="item.value">{{
                  item.label
                }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>促销名称</Label>
            <Input v-model="promotionForm.name" />
          </div>
          <div class="space-y-2">
            <Label>促销类型</Label>
            <Select v-model="promotionForm.type">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="折扣">折扣</SelectItem>
                <SelectItem value="满减">满减</SelectItem>
                <SelectItem value="直降">直降</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>折扣/金额</Label>
            <NumberField
              v-model="promotionForm.discount"
              :min="0"
              :max="promotionForm.type === '折扣' ? 10 : 999999"
              :step="promotionForm.type === '折扣' ? 0.1 : 1"
            >
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="space-y-2">
            <Label>开始时间</Label>
            <Input type="datetime-local" v-model="promotionForm.startTime" />
          </div>
          <div class="space-y-2">
            <Label>结束时间</Label>
            <Input type="datetime-local" v-model="promotionForm.endTime" />
          </div>
          <div class="space-y-2">
            <Label>状态</Label>
            <Select v-model="promotionForm.status">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="未开始">未开始</SelectItem>
                <SelectItem value="进行中">进行中</SelectItem>
                <SelectItem value="已结束">已结束</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false" :disabled="submitting"
            >取消</Button
          >
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
  createPromotion,
  deletePromotion,
  fetchPromotionDetail,
  fetchProductPage,
  getPromotionList,
  updatePromotion,
} from '@/modules/product/api/product-api'
import type { ProductPromotion } from '@/modules/product/model/product.types'
import {
  toCreatePromotionRequest,
  toUpdatePromotionRequest,
  type PromotionFormValues,
} from '@/modules/product/model/product.types'
import { toLocalDateTimeInput, fromLocalDateTimeInput } from '@/shared/datetime/local-date'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { formatCurrency } from '@/shared/utils/display-format'
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
import { Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'ProductPromotionView' })

const promotionList = ref<ProductPromotion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const {
  sortBy,
  sortDirection,
  sortedRows: displayPromotionList,
  toggleSort,
} = useClientSort<ProductPromotion>(promotionList, {
  id: 'id',
  name: 'name',
  type: (row) => formatPromotionType(row.type),
  discount: 'discount',
  startTime: 'startTime',
  endTime: 'endTime',
  status: (row) => formatPromotionStatus(row.status),
})
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitting = ref(false)
const productOptions = ref<{ value: string; label: string }[]>([])

const promotionForm = ref<PromotionFormValues>({
  productId: '',
  name: '',
  type: '折扣',
  discount: 0,
  startTime: '',
  endTime: '',
  status: '未开始',
})

function getPromotionTypeTone(
  type: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple'> = {
    折扣: 'success',
    DISCOUNT: 'success',
    discount: 'success',
    满减: 'info',
    FULL_REDUCTION: 'info',
    full_reduction: 'info',
    直降: 'warning',
    DIRECT_REDUCTION: 'warning',
    direct_reduction: 'warning',
  }
  return map[type] ?? 'muted'
}

function getPromotionStatusTone(
  status: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple'> = {
    未开始: 'purple',
    NOT_STARTED: 'purple',
    pending: 'purple',
    进行中: 'success',
    ACTIVE: 'success',
    active: 'success',
    已结束: 'muted',
    ENDED: 'muted',
    ended: 'muted',
  }
  return map[status] ?? 'muted'
}

function formatPromotionDiscount(row: ProductPromotion): string {
  if (formatPromotionType(row.type) === '折扣') {
    return `${row.discount ?? '--'}折`
  }
  return formatCurrency(row.discount, { fractionDigits: 0 })
}

function formatPromotionType(type: string): string {
  const map: Record<string, string> = {
    折扣: '折扣',
    DISCOUNT: '折扣',
    discount: '折扣',
    满减: '满减',
    FULL_REDUCTION: '满减',
    full_reduction: '满减',
    直降: '直降',
    DIRECT_REDUCTION: '直降',
    direct_reduction: '直降',
  }
  return map[type] ?? type ?? '--'
}

function formatPromotionStatus(status: string): string {
  const map: Record<string, string> = {
    未开始: '未开始',
    NOT_STARTED: '未开始',
    pending: '未开始',
    进行中: '进行中',
    ACTIVE: '进行中',
    active: '进行中',
    已结束: '已结束',
    ENDED: '已结束',
    ended: '已结束',
  }
  return map[status] ?? status ?? '--'
}

function formatDateTime(dateTimeStr?: string): string {
  if (!dateTimeStr) return ''
  return dateTimeStr.replace('T', ' ').split('.')[0] ?? ''
}

async function loadProductOptions(): Promise<void> {
  try {
    const res = await fetchProductPage({ page: 1, size: 100 })
    productOptions.value = res.list
      .filter((p) => p.id !== null && p.id !== undefined)
      .map((p) => ({
        value: String(p.id),
        label: `${p.sku ?? ''} ${p.name ?? ''}`.trim(),
      }))
  } catch {
    messageTip('加载商品选项失败', 'error')
  }
}

async function loadPromotions(): Promise<void> {
  try {
    loading.value = true
    const res = await getPromotionList({ page: currentPage.value, size: pageSize.value })
    promotionList.value = res.list
    total.value = res.total
  } catch {
    messageTip('加载促销列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleAdd(): void {
  dialogType.value = 'add'
  promotionForm.value = {
    productId: '',
    name: '',
    type: '折扣',
    discount: 0,
    startTime: '',
    endTime: '',
    status: '未开始',
  }
  dialogVisible.value = true
}

async function handleEdit(row: ProductPromotion): Promise<void> {
  dialogType.value = 'edit'
  try {
    const detail = await fetchPromotionDetail(row.id)
    promotionForm.value = {
      productId: String(detail.productId ?? ''),
      name: detail.name ?? '',
      type: detail.type ?? '折扣',
      discount: Number(detail.discount ?? 0),
      startTime: toLocalDateTimeInput(detail.startTime),
      endTime: toLocalDateTimeInput(detail.endTime),
      status: detail.status ?? '未开始',
    }
    dialogVisible.value = true
  } catch {
    messageTip('加载促销详情失败', 'error')
  }
}

async function handleDelete(row: ProductPromotion): Promise<void> {
  try {
    await messageConfirm('确认删除该促销活动？')
  } catch {
    messageTip('取消删除', 'warning')
    return
  }
  try {
    await deletePromotion(row.id)
    messageTip('删除成功', 'success')
    await loadPromotions()
  } catch {
    messageTip('删除失败', 'error')
  }
}

function validateForm(): string | null {
  if (!promotionForm.value.productId) {
    return '请选择商品'
  }
  if (!promotionForm.value.name.trim()) {
    return '请输入促销名称'
  }
  if (!promotionForm.value.startTime || !promotionForm.value.endTime) {
    return '请选择开始和结束时间'
  }
  const start = fromLocalDateTimeInput(promotionForm.value.startTime)
  const end = fromLocalDateTimeInput(promotionForm.value.endTime)
  if (!start || !end) {
    return '时间格式有误'
  }
  if (start >= end) {
    return '结束时间必须晚于开始时间'
  }
  if (promotionForm.value.discount < 0) {
    return '折扣/金额不能为负'
  }
  if (promotionForm.value.type === '折扣' && promotionForm.value.discount > 10) {
    return '折扣不能超过10'
  }
  return null
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) return
  const error = validateForm()
  if (error) {
    messageTip(error, 'warning')
    return
  }
  submitting.value = true
  try {
    const start = fromLocalDateTimeInput(promotionForm.value.startTime)
    const end = fromLocalDateTimeInput(promotionForm.value.endTime)
    if (!start || !end) {
      messageTip('时间格式有误', 'error')
      return
    }
    const valuesWithTime: PromotionFormValues = {
      ...promotionForm.value,
      startTime: start,
      endTime: end,
    }
    if (dialogType.value === 'add') {
      await createPromotion(toCreatePromotionRequest(valuesWithTime))
      messageTip('新增成功', 'success')
    } else {
      const editingId = promotionList.value.find(
        (p) => p.productId === promotionForm.value.productId,
      )?.id
      if (editingId === undefined) {
        messageTip('无法确定编辑目标', 'error')
        return
      }
      await updatePromotion(editingId, toUpdatePromotionRequest(valuesWithTime))
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    try {
      await loadPromotions()
    } catch {
      messageTip('操作已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip('操作失败', 'error')
  } finally {
    submitting.value = false
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadPromotions()
}

onMounted(() => {
  void loadProductOptions()
  void loadPromotions()
})
</script>
