<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button @click="handleAdd">新增产品</Button>
        <Button variant="secondary" @click="handleCategory">分类管理</Button>
        <Button variant="outline" @click="handlePromotion">促销设置</Button>
        <Button variant="destructive" @click="handleStockAlert">库存预警</Button>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <div v-if="loading" class="py-10 text-center text-muted-foreground">加载中...</div>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox :checked="allSelected" @update:checked="toggleSelectAll" />
              </TableHead>
              <TableHead class="w-[60px]">序号</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>产品名称</TableHead>
              <TableHead>分类</TableHead>
              <TableHead>规格</TableHead>
              <TableHead>价格</TableHead>
              <TableHead>库存</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, idx) in productList" :key="row.id ?? idx">
              <TableCell>
                <Checkbox
                  :checked="selectedIds.includes(row.id)"
                  @update:checked="(checked: boolean) => handleRowSelect(row.id, checked)"
                />
              </TableCell>
              <TableCell>{{ startIndex(idx) }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.sku }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.name }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.category }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.specification }}</TableCell>
              <TableCell>¥{{ row.price.toFixed(2) }}</TableCell>
              <TableCell>
                <span :class="{ 'text-red-500 font-bold': row.stock < row.minStock }">
                  {{ row.stock }}
                </span>
              </TableCell>
              <TableCell>
                <Badge :class="row.status === '上架' ? 'bg-green-600 text-white' : ''" :variant="row.status === '上架' ? undefined : 'outline'">
                  {{ row.status }}
                </Badge>
              </TableCell>
              <TableCell class="flex gap-2">
                <Button variant="secondary" size="sm" @click="handleEdit(row)">编辑</Button>
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

    <!-- 产品表单对话框 -->
    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{{ dialogType === 'add' ? '新增产品' : '编辑产品' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div class="space-y-2">
            <Label>SKU</Label>
            <Input v-model="values.sku" />
            <p v-if="errors.sku" class="text-sm text-destructive">{{ errors.sku }}</p>
          </div>
          <div class="space-y-2">
            <Label>产品名称</Label>
            <Input v-model="values.name" />
            <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
          </div>
          <div class="space-y-2">
            <Label>分类</Label>
            <Select v-model="values.category">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择分类" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in categoryOptions.list"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.category" class="text-sm text-destructive">{{ errors.category }}</p>
          </div>
          <div class="space-y-2">
            <Label>规格</Label>
            <Input v-model="values.specification" />
          </div>
          <div class="space-y-2">
            <Label>价格</Label>
            <NumberField v-model="values.price" :min="0.01" :step="0.1">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="errors.price" class="text-sm text-destructive">{{ errors.price }}</p>
          </div>
          <div class="space-y-2">
            <Label>库存</Label>
            <NumberField v-model="values.stock" :min="0">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="errors.stock" class="text-sm text-destructive">{{ errors.stock }}</p>
          </div>
          <div class="space-y-2">
            <Label>最低库存</Label>
            <NumberField v-model="values.minStock" :min="0">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="space-y-2">
            <Label>状态</Label>
            <Select v-model="values.status">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="上架">上架</SelectItem>
                <SelectItem value="下架">下架</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.status" class="text-sm text-destructive">{{ errors.status }}</p>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="onSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { messageTip, messageConfirm } from '@/shared/utils/legacy-util'
import { useRouter } from 'vue-router'
import {
  getProductList,
  createProduct,
  updateProduct,
  deleteProduct,
  getCategoryList
} from '@/modules/product/api/product-api'
import type { Product } from '@/modules/product/model/product.types'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
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

const router = useRouter()
const productList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogType = ref('add')
const categoryOptions = reactive({
  list: []
})

const selectedIds = ref<(number | string)[]>([])

const allSelected = computed(() =>
  productList.value.length > 0 && selectedIds.value.length === productList.value.length
)

function toggleSelectAll(checked: boolean) {
  selectedIds.value = checked ? productList.value.map((r: Product) => r.id) : []
  handleSelectionChange(selectedIds.value)
}

function handleRowSelect(id: number | string, checked: boolean) {
  if (checked) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((v: number | string) => v !== id)
  }
  handleSelectionChange(selectedIds.value)
}

// zod schema for form validation
const productSchema = toTypedSchema(z.object({
  sku: z.string().min(1, '请输入SKU'),
  name: z.string().min(1, '请输入产品名称'),
  category: z.string().min(1, '请选择分类'),
  specification: z.string().optional(),
  price: z.number().min(0.01, '请输入价格'),
  stock: z.number().min(0, '请输入库存'),
  minStock: z.number().min(0).optional(),
  status: z.string().min(1, '请选择状态'),
}))

const { handleSubmit, errors, values, setValues, resetForm } = useForm({
  validationSchema: productSchema,
  initialValues: {
    sku: '',
    name: '',
    category: '',
    specification: '',
    price: 0,
    stock: 0,
    minStock: 0,
    status: '上架'
  },
})

const productForm = ref({
  sku: '',
  name: '',
  category: '',
  specification: '',
  price: 0,
  stock: 0,
  minStock: 0,
  status: '上架'
})

const loadCategoryOptions = async () => {
  try {
    const res = await getCategoryList()
    categoryOptions.list = res.list
  } catch (error) {
    messageTip('加载分类列表失败', 'error')
  }
}

// 加载产品列表
const loadProducts = async () => {
  try {
    loading.value = true
    const res = await getProductList({
      page: currentPage.value,
      size: pageSize.value
    })
    productList.value = res.list
    total.value = res.total
    selectedIds.value = []
  } catch (error) {
    messageTip('加载产品列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 处理新增
const handleAdd = async () => {
  await loadCategoryOptions()
  dialogType.value = 'add'
  productForm.value = {
    sku: '',
    name: '',
    category: '',
    specification: '',
    price: 0,
    stock: 0,
    minStock: 0,
    status: '上架'
  }
  resetForm()
  setValues(productForm.value)
  dialogVisible.value = true
}

// 处理编辑
const handleEdit = async (row) => {
  await loadCategoryOptions()
  dialogType.value = 'edit'
  productForm.value = { ...row }
  setValues(productForm.value)
  dialogVisible.value = true
}

// 处理删除
const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该产品？')
    await deleteProduct(row.id)
    messageTip('删除成功', 'success')
    loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('删除失败', 'error')
    }
  }
}

// 处理提交
const onSubmit = handleSubmit(async (formValues) => {
  try {
    if (dialogType.value === 'add') {
      await createProduct(formValues)
      messageTip('新增成功', 'success')
    } else {
      await updateProduct(productForm.value.id, formValues)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    loadProducts()
  } catch (error) {
    messageTip('操作失败', 'error')
  }
})

// 处理分类管理
const handleCategory = () => {
  router.push('/dashboard/product/category')
}

// 处理促销设置
const handlePromotion = () => {
  router.push('/dashboard/product/promotion')
}

// 处理库存预警
const handleStockAlert = () => {
  router.push('/dashboard/product/stock')
}

// 处理分页
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadProducts()
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  // 处理表格选择逻辑
}

const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  loadProducts()
})
</script>
