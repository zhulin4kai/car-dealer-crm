<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button variant="outline" @click="goBack">返 回</Button>
        <Button @click="handleAdd">新增促销</Button>
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
                <Button variant="outline" size="sm" @click="handleEdit(row)">编辑</Button>
                <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
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
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createPromotion, deletePromotion, getPromotionList, updatePromotion } from '@/modules/product/api/product-api'
import type { ProductForm, ProductPromotion } from '@/modules/product/model/product.types'
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

const promotionList = ref<ProductPromotion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const promotionForm = ref<ProductForm>({ name: '', type: '折扣', discount: 0, startTime: '', endTime: '', status: '未开始' })

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

function formatDateTime(dateTimeStr: string): string {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  const year = String(date.getFullYear())
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds
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
  promotionForm.value = { name: '', type: '折扣', discount: 0, startTime: '', endTime: '', status: '未开始' }
  dialogVisible.value = true
}

function handleEdit(row: ProductPromotion): void {
  dialogType.value = 'edit'
  promotionForm.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row: ProductPromotion): Promise<void> {
  try {
    await messageConfirm('确认删除该促销活动？')
    if (row.id === undefined) return
    await deletePromotion(row.id)
    messageTip('删除成功', 'success')
    await loadPromotions()
  } catch (error) {
    if (error !== 'cancel') messageTip('删除失败', 'error')
  }
}

async function handleSubmit(): Promise<void> {
  try {
    if (dialogType.value === 'add') {
      await createPromotion(promotionForm.value)
      messageTip('新增成功', 'success')
    } else if (promotionForm.value.id !== undefined) {
      await updatePromotion(String(promotionForm.value.id), promotionForm.value)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadPromotions()
  } catch {
    messageTip('操作失败', 'error')
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadPromotions()
}

function goBack(): void {
  window.history.length > 1 ? window.history.back() : (window.location.href = '/dashboard/product')
}

onMounted(() => { void loadPromotions() })
</script>
