<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button variant="outline" @click="goBack">返 回</Button>
        <Button v-has-permission="PERMISSIONS.product.promotion.add" @click="handleAdd">新增促销</Button>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <div v-if="loading" class="py-10 text-center text-muted-foreground">加载中...</div>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>促销名称</TableHead>
              <TableHead>促销类型</TableHead>
              <TableHead>折扣/金额</TableHead>
              <TableHead>开始时间</TableHead>
              <TableHead>结束时间</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="row in promotionList" :key="row.id">
              <TableCell>{{ row.id }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.name }}</TableCell>
              <TableCell>
                <Badge
                  :variant="getPromotionTypeTag(row.type).variant"
                  :class="getPromotionTypeTag(row.type).badgeClass"
                >
                  {{ row.type }}
                </Badge>
              </TableCell>
              <TableCell>
                {{ row.type === '折扣' ? row.discount + '折' : '¥' + row.discount }}
              </TableCell>
              <TableCell>{{ formatDateTime(row.startTime) }}</TableCell>
              <TableCell>{{ formatDateTime(row.endTime) }}</TableCell>
              <TableCell>
                <Badge
                  :variant="getStatusTag(row.status).variant"
                  :class="getStatusTag(row.status).badgeClass"
                >
                  {{ row.status }}
                </Badge>
              </TableCell>
              <TableCell class="flex gap-2">
                <Button v-has-permission="PERMISSIONS.product.promotion.edit" variant="outline" size="sm" @click="handleEdit(row)">编辑</Button>
                <Button v-has-permission="PERMISSIONS.product.promotion.delete" variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <DataTablePagination
      :page-size="pageSize"
      :total="total"
      @change="handleCurrentChange"
    />

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
                <SelectItem v-for="item in productOptions" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
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
          <Button variant="outline" @click="dialogVisible = false" :disabled="submitting">取消</Button>
          <Button @click="handleSubmit" :disabled="submitting">{{ submitting ? '提交中...' : '确定' }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

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
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
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

defineOptions({ name: 'ProductPromotionView' })

const router = useRouter()
const promotionList = ref<ProductPromotion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
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

interface BadgeStyle {
  variant: 'default' | 'secondary' | 'destructive' | 'outline'
  badgeClass: string
}

function getPromotionTypeTag(type: string): BadgeStyle {
  const map: Record<string, BadgeStyle> = {
    折扣: { variant: 'default', badgeClass: 'bg-green-600 text-white' },
    满减: { variant: 'secondary', badgeClass: '' },
    直降: { variant: 'destructive', badgeClass: '' },
  }
  return map[type] ?? { variant: 'outline', badgeClass: '' }
}

function getStatusTag(status: string): BadgeStyle {
  const map: Record<string, BadgeStyle> = {
    未开始: { variant: 'outline', badgeClass: '' },
    进行中: { variant: 'default', badgeClass: 'bg-green-600 text-white' },
    已结束: { variant: 'destructive', badgeClass: '' },
  }
  return map[status] ?? { variant: 'outline', badgeClass: '' }
}

function formatDateTime(dateTimeStr?: string): string {
  if (!dateTimeStr) return ''
  return dateTimeStr.replace('T', ' ').split('.')[0] ?? ''
}

async function loadProductOptions(): Promise<void> {
  try {
    const res = await fetchProductPage({ page: 1, size: 100 })
    productOptions.value = res.list.filter(p => p.id !== null && p.id !== undefined).map(p => ({
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
      const editingId = promotionList.value.find(p => p.productId === promotionForm.value.productId)?.id
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

function goBack(): void {
  if (window.history.length > 1) {
    window.history.back()
  } else {
    router.push('/dashboard/product')
  }
}

onMounted(() => {
  void loadProductOptions()
  void loadPromotions()
})
</script>
