<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button variant="outline" @click="goBack">返 回</Button>
        <Button v-has-permission="PERMISSIONS.product.category.add" @click="handleAdd">新增分类</Button>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <div v-if="loading" class="py-10 text-center text-muted-foreground">加载中...</div>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>分类名称</TableHead>
              <TableHead>分类编码</TableHead>
              <TableHead>描述</TableHead>
              <TableHead>排序</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="row in categoryList" :key="row.id">
              <TableCell>{{ row.id }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.name }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.code }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.description }}</TableCell>
              <TableCell>{{ row.sort }}</TableCell>
              <TableCell>
                <Badge :class="row.status === '启用' ? 'bg-green-600 text-white' : ''" :variant="row.status === '启用' ? undefined : 'outline'">
                  {{ row.status }}
                </Badge>
              </TableCell>
              <TableCell class="flex gap-2">
                <Button v-has-permission="PERMISSIONS.product.category.edit" variant="outline" size="sm" @click="handleEdit(row)">编辑</Button>
                <Button v-has-permission="PERMISSIONS.product.category.delete" variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
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
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { onMounted, ref } from 'vue'

import { createCategory, deleteCategory, getCategoryList, updateCategory } from '@/modules/product/api/product-api'
import type { ProductCategory, ProductForm } from '@/modules/product/model/product.types'
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
import { Textarea } from '@/components/ui/textarea'

defineOptions({ name: 'ProductCategoryView' })

const categoryList = ref<ProductCategory[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const categoryForm = ref<ProductForm>({ name: '', code: '', description: '', sort: 0, status: '启用' })

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
  try {
    if (dialogType.value === 'add') {
      await createCategory(categoryForm.value)
      messageTip('新增成功', 'success')
    } else if (categoryForm.value.id !== undefined) {
      await updateCategory(String(categoryForm.value.id), categoryForm.value)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch {
    messageTip('操作失败', 'error')
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadCategories()
}

function goBack(): void {
  window.history.length > 1 ? window.history.back() : (window.location.href = '/dashboard/product')
}

onMounted(() => { void loadCategories() })
</script>
