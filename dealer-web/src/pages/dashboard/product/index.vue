<template>
  <div class="min-h-full space-y-5 p-6">
    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <section
        v-for="card in summaryCards"
        :key="card.label"
        class="rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-5 shadow-[var(--crm-shadow-card)]"
      >
        <div class="flex items-center gap-4">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl" :class="card.iconClass">
            <component :is="card.icon" class="h-5 w-5" />
          </div>
          <div class="min-w-0">
            <div class="text-sm text-[var(--crm-text-tertiary)]">{{ card.label }}</div>
            <div class="mt-1 flex items-end gap-2">
              <span class="text-2xl font-semibold leading-none">{{ card.value }}</span>
              <span class="pb-0.5 text-sm text-[var(--crm-text-tertiary)]">{{ card.description }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="overflow-hidden rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] shadow-[var(--crm-shadow-card)]">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="flex items-center gap-3">
          <h2 class="text-lg font-semibold">产品列表</h2>
          <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
            {{ formatNumber(total) }} 条
          </span>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Button v-has-permission="PERMISSIONS.product.add" class="gap-2 bg-[var(--crm-primary)] hover:bg-[var(--crm-primary-hover)]" @click="handleAdd">
            <Plus class="h-4 w-4" />
            新增产品
          </Button>
          <Button variant="outline" class="gap-2" @click="handleCategory">
            <Tags class="h-4 w-4" />
            分类管理
          </Button>
          <Button variant="outline" class="gap-2" @click="handlePromotion">
            <Percent class="h-4 w-4" />
            促销设置
          </Button>
          <Button variant="outline" class="gap-2" @click="handleStockAlert">
            <TriangleAlert class="h-4 w-4" />
            库存预警
            <span
              v-if="lowStockPageCount > 0"
              class="-mr-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-[var(--crm-danger)] px-1.5 text-xs text-white"
            >
              {{ lowStockPageCount }}
            </span>
          </Button>
        </div>
      </div>

      <div class="flex flex-col gap-4 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="relative w-full max-w-[360px]">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--crm-text-tertiary)]" />
            <Input
              v-model="searchKeyword"
              class="h-10 pl-9"
              placeholder="搜索产品名称..."
              @keyup.enter="applyFilters"
            />
          </div>
          <Button variant="outline" class="gap-2" @click="applyFilters">
            <Search class="h-4 w-4" />
            搜索
          </Button>
        </div>

        <div class="flex min-w-0 gap-2 overflow-x-auto pb-1">
          <button
            class="h-9 shrink-0 rounded-full px-4 text-sm font-medium transition-colors"
            :class="activeCategoryId === 'all' ? 'bg-[var(--crm-primary)] text-white' : 'text-[var(--crm-text-secondary)] hover:bg-[var(--crm-bg-hover)]'"
            type="button"
            @click="selectCategory('all')"
          >
            全部
          </button>
          <button
            v-for="category in categoryOptions"
            :key="category.id ?? category.name"
            class="h-9 shrink-0 rounded-full px-4 text-sm font-medium transition-colors"
            :class="String(activeCategoryId) === String(category.id) ? 'bg-[var(--crm-primary)] text-white' : 'text-[var(--crm-text-secondary)] hover:bg-[var(--crm-bg-hover)]'"
            type="button"
            @click="selectCategory(category.id)"
          >
            {{ category.name || '--' }}
          </button>
        </div>
      </div>

      <div class="overflow-x-auto">
        <Table class="min-w-[1120px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[56px]">
                <Checkbox :checked="allSelected" @update:checked="(checked) => toggleSelectAll(checked === true)" />
              </TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>产品名称</TableHead>
              <TableHead>分类</TableHead>
              <TableHead>规格</TableHead>
              <TableHead>价格</TableHead>
              <TableHead>库存</TableHead>
              <TableHead>最低库存</TableHead>
              <TableHead>状态</TableHead>
              <TableHead class="w-[132px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="10" class="h-32 text-center text-[var(--crm-text-tertiary)]">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="productList.length === 0">
              <TableCell colspan="10" class="h-32 text-center text-[var(--crm-text-tertiary)]">暂无产品数据</TableCell>
            </TableRow>
            <template v-else>
              <TableRow
                v-for="(row, index) in productList"
                :key="row.id ?? row.sku ?? index"
                class="hover:bg-[var(--crm-bg-hover)]"
              >
                <TableCell>
                  <Checkbox
                    :checked="isSelected(row.id)"
                    :disabled="row.id == null"
                    @update:checked="(checked) => handleRowSelect(row.id, checked === true)"
                  />
                </TableCell>
                <TableCell>
                  <span class="inline-flex rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 font-mono text-xs text-[var(--crm-text-secondary)]">
                    {{ row.sku || '--' }}
                  </span>
                </TableCell>
                <TableCell>
                  <div class="flex min-w-[220px] items-center gap-3">
                    <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[var(--crm-bg-muted)] text-[var(--crm-text-tertiary)]">
                      <component :is="resolveProductIcon(row.categoryName)" class="h-4 w-4" />
                    </span>
                    <span class="truncate font-semibold text-[var(--crm-text-primary)]">{{ row.name || '--' }}</span>
                  </div>
                </TableCell>
                <TableCell class="text-[var(--crm-text-secondary)]">{{ row.categoryName || '--' }}</TableCell>
                <TableCell class="text-[var(--crm-text-secondary)]">{{ row.specification || '--' }}</TableCell>
                <TableCell class="font-semibold text-[var(--crm-text-primary)]">{{ formatCurrency(row.price) }}</TableCell>
                <TableCell>
                  <div class="font-semibold" :class="isLowStock(row) ? 'text-[var(--crm-warning)]' : 'text-[var(--crm-text-primary)]'">
                    {{ formatNumber(row.stock) }}
                  </div>
                  <div v-if="isLowStock(row)" class="mt-1 text-xs text-[var(--crm-warning)]">低于最低库存</div>
                </TableCell>
                <TableCell class="text-[var(--crm-text-secondary)]">{{ formatNumber(row.minStock) }}</TableCell>
                <TableCell>
                  <StatusBadge :label="row.status" :tone="getProductStatusTone(row.status)" />
                </TableCell>
                <TableCell>
                  <div class="flex items-center gap-1">
                    <RowActionButton v-has-permission="PERMISSIONS.product.view" label="查看" @click="handleView(row)">
                      <Eye class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton v-has-permission="PERMISSIONS.product.edit" label="编辑" @click="handleEdit(row)">
                      <Pencil class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton v-has-permission="PERMISSIONS.product.delete" label="删除" danger @click="handleDelete(row)">
                      <Trash2 class="h-4 w-4" />
                    </RowActionButton>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </div>

      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--crm-border-light)] px-5 py-4">
        <div class="text-sm text-[var(--crm-text-tertiary)]">共 {{ formatNumber(total) }} 条记录</div>
        <DataTablePagination
          :page="currentPage"
          :page-size="pageSize"
          :total="total"
          @change="handleCurrentChange"
        />
      </div>
    </section>

    <Dialog v-model:open="viewDialogVisible">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>产品详情</DialogTitle>
        </DialogHeader>
        <div v-if="viewLoading" class="py-12 text-center text-[var(--crm-text-tertiary)]">加载中...</div>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <div v-for="item in viewItems" :key="item.label" class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-muted)] p-3">
            <div class="text-xs text-[var(--crm-text-tertiary)]">{{ item.label }}</div>
            <div class="mt-1 break-words text-sm font-semibold text-[var(--crm-text-primary)]">{{ item.value }}</div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>{{ dialogType === 'add' ? '新增产品' : '编辑产品' }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="onSubmit">
          <div class="space-y-2">
            <Label for="product-sku">SKU</Label>
            <Input id="product-sku" v-model="productForm.sku" />
            <p v-if="formErrors.sku" class="text-sm text-destructive">{{ formErrors.sku }}</p>
          </div>
          <div class="space-y-2">
            <Label for="product-name">产品名称</Label>
            <Input id="product-name" v-model="productForm.name" />
            <p v-if="formErrors.name" class="text-sm text-destructive">{{ formErrors.name }}</p>
          </div>
          <div class="space-y-2">
            <Label>分类</Label>
            <Select v-model="productForm.categoryId">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择分类" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :value="String(item.id)"
                >
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <p v-if="formErrors.categoryId" class="text-sm text-destructive">{{ formErrors.categoryId }}</p>
          </div>
          <div class="space-y-2">
            <Label for="product-specification">规格</Label>
            <Input id="product-specification" v-model="productForm.specification" />
          </div>
          <div class="space-y-2">
            <Label>价格</Label>
            <NumberField v-model="productForm.price" :min="0.01" :step="0.1">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="formErrors.price" class="text-sm text-destructive">{{ formErrors.price }}</p>
          </div>
          <div class="space-y-2">
            <Label>库存</Label>
            <NumberField v-model="productForm.stock" :min="0">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="formErrors.stock" class="text-sm text-destructive">{{ formErrors.stock }}</p>
          </div>
          <div class="space-y-2">
            <Label>最低库存</Label>
            <NumberField v-model="productForm.minStock" :min="0">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="formErrors.minStock" class="text-sm text-destructive">{{ formErrors.minStock }}</p>
          </div>
          <div class="space-y-2">
            <Label>状态</Label>
            <Select v-model="productForm.status">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="上架">上架</SelectItem>
                <SelectItem value="下架">下架</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="formErrors.status" class="text-sm text-destructive">{{ formErrors.status }}</p>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button class="bg-[var(--crm-primary)] hover:bg-[var(--crm-primary-hover)]" :disabled="submitting" @click="onSubmit">
            确定
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
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
import {
  createProduct,
  deleteProduct,
  fetchCategoryPage,
  fetchProductDetail,
  fetchProductPage,
  updateProduct,
} from '@/modules/product/api/product-api'
import type { Product, ProductCategory, ProductForm, ProductQuery } from '@/modules/product/model/product.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import type { EntityId } from '@/shared/types/id'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import {
  formatCurrency,
  formatNumber,
  toNumber,
} from '@/shared/utils/display-format'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import {
  Box,
  Car,
  Eye,
  Package,
  Pencil,
  Percent,
  Plus,
  Search,
  Shield,
  Tags,
  Trash2,
  TriangleAlert,
  Wrench,
} from '@lucide/vue'

defineOptions({
  name: 'ProductListView',
})

type ProductFormState = {
  sku: string
  name: string
  categoryId: string
  specification: string
  price: number
  stock: number
  minStock: number
  status: string
}

const router = useRouter()
const productList = ref<Product[]>([])
const categoryOptions = ref<ProductCategory[]>([])
const categoryTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const activeCategoryId = ref<'all' | EntityId>('all')
const selectedIds = ref<EntityId[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const editingProductId = ref<EntityId | null>(null)
const viewDialogVisible = ref(false)
const viewLoading = ref(false)
const viewProduct = ref<Product | null>(null)

const productForm = reactive<ProductFormState>({
  sku: '',
  name: '',
  categoryId: '',
  specification: '',
  price: 0,
  stock: 0,
  minStock: 0,
  status: '上架',
})

const formErrors = reactive<Record<keyof ProductFormState, string>>({
  sku: '',
  name: '',
  categoryId: '',
  specification: '',
  price: '',
  stock: '',
  minStock: '',
  status: '',
})

const listedPageCount = computed(() => productList.value.filter((item) => item.status === '上架').length)
const lowStockPageCount = computed(() => productList.value.filter(isLowStock).length)
const allSelected = computed(() => {
  const selectableIds = productList.value
    .map((item) => item.id)
    .filter((id): id is EntityId => id != null)
  return selectableIds.length > 0 && selectableIds.every((id) => selectedIds.value.includes(id))
})

const summaryCards = computed<Array<{
  label: string
  value: string
  description: string
  icon: Component
  iconClass: string
}>>(() => [
  {
    label: '产品总数',
    value: formatNumber(total.value),
    description: `${formatNumber(categoryTotal.value)} 个分类`,
    icon: Package,
    iconClass: 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]',
  },
  {
    label: '当前页上架',
    value: formatNumber(listedPageCount.value),
    description: `共 ${formatNumber(productList.value.length)} 个`,
    icon: Box,
    iconClass: 'bg-[var(--crm-success-bg)] text-[var(--crm-success)]',
  },
  {
    label: '当前页库存预警',
    value: formatNumber(lowStockPageCount.value),
    description: '低于最低库存',
    icon: TriangleAlert,
    iconClass: 'bg-[var(--crm-danger-bg)] text-[var(--crm-danger)]',
  },
  {
    label: '产品分类',
    value: formatNumber(categoryTotal.value),
    description: '分类分页总数',
    icon: Tags,
    iconClass: 'bg-[var(--crm-warning-bg)] text-[var(--crm-warning)]',
  },
])

const viewItems = computed(() => {
  const item = viewProduct.value
  return [
    { label: 'SKU', value: item?.sku || '--' },
    { label: '产品名称', value: item?.name || '--' },
    { label: '分类', value: item?.categoryName || '--' },
    { label: '规格', value: item?.specification || '--' },
    { label: '价格', value: formatCurrency(item?.price) },
    { label: '库存', value: formatNumber(item?.stock) },
    { label: '最低库存', value: formatNumber(item?.minStock) },
    { label: '状态', value: item?.status || '--' },
  ]
})

async function loadProducts(): Promise<void> {
  loading.value = true
  try {
    const params: ProductQuery = {
      page: currentPage.value,
      size: pageSize.value,
    }
    const keyword = searchKeyword.value.trim()
    if (keyword) {
      params.name = keyword
    }
    if (activeCategoryId.value !== 'all') {
      params.categoryId = activeCategoryId.value
    }

    const result = await fetchProductPage(params)
    productList.value = result.list ?? []
    total.value = result.total ?? 0
    selectedIds.value = []
  } catch {
    messageTip('加载产品列表失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadCategories(): Promise<void> {
  try {
    const result = await fetchCategoryPage({ page: 1, size: 100 })
    categoryOptions.value = result.list ?? []
    categoryTotal.value = result.total ?? categoryOptions.value.length
  } catch {
    messageTip('加载分类列表失败', 'error')
  }
}

function applyFilters(): void {
  currentPage.value = 1
  void loadProducts()
}

function selectCategory(categoryId?: EntityId | 'all'): void {
  activeCategoryId.value = categoryId ?? 'all'
  currentPage.value = 1
  void loadProducts()
}

function isSelected(id?: EntityId): boolean {
  return id != null && selectedIds.value.includes(id)
}

function toggleSelectAll(checked: boolean): void {
  selectedIds.value = checked
    ? productList.value
      .map((item) => item.id)
      .filter((id): id is EntityId => id != null)
    : []
}

function handleRowSelect(id: EntityId | undefined, checked: boolean): void {
  if (id == null) {
    return
  }
  if (checked && !selectedIds.value.includes(id)) {
    selectedIds.value = [...selectedIds.value, id]
    return
  }
  if (!checked) {
    selectedIds.value = selectedIds.value.filter((value) => value !== id)
  }
}

function isLowStock(row: Product): boolean {
  const stock = row.stock ?? 0
  const minStock = row.minStock ?? 0
  return minStock > 0 && stock < minStock
}

function getProductStatusTone(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === '上架') {
    return 'success'
  }
  if (status === '下架') {
    return 'muted'
  }
  return 'warning'
}

function resolveProductIcon(categoryName?: string): Component {
  if (categoryName?.includes('整车')) {
    return Car
  }
  if (categoryName?.includes('维修') || categoryName?.includes('配件')) {
    return Wrench
  }
  if (categoryName?.includes('用品')) {
    return Shield
  }
  return Package
}

function resetProductForm(row?: Product): void {
  Object.assign(productForm, {
    sku: row?.sku ?? '',
    name: row?.name ?? '',
    categoryId: row?.categoryId == null ? '' : String(row.categoryId),
    specification: row?.specification ?? '',
    price: toNumber(row?.price) ?? 0,
    stock: row?.stock ?? 0,
    minStock: row?.minStock ?? 0,
    status: row?.status ?? '上架',
  })
  clearFormErrors()
}

function clearFormErrors(): void {
  Object.keys(formErrors).forEach((key) => {
    formErrors[key as keyof ProductFormState] = ''
  })
}

function validateProductForm(): boolean {
  clearFormErrors()
  if (!productForm.sku.trim()) {
    formErrors.sku = '请输入SKU'
  }
  if (!productForm.name.trim()) {
    formErrors.name = '请输入产品名称'
  }
  if (!productForm.categoryId) {
    formErrors.categoryId = '请选择分类'
  }
  if (productForm.price <= 0) {
    formErrors.price = '请输入价格'
  }
  if (productForm.stock < 0) {
    formErrors.stock = '请输入库存'
  }
  if (productForm.minStock < 0) {
    formErrors.minStock = '最低库存不能小于0'
  }
  if (!productForm.status) {
    formErrors.status = '请选择状态'
  }

  return Object.values(formErrors).every((message) => !message)
}

function normalizeEntityId(value: string): EntityId {
  return /^\d+$/.test(value) ? Number(value) : value
}

function toProductPayload(): ProductForm {
  return {
    sku: productForm.sku.trim(),
    name: productForm.name.trim(),
    categoryId: normalizeEntityId(productForm.categoryId),
    specification: productForm.specification.trim(),
    price: productForm.price,
    stock: productForm.stock,
    minStock: productForm.minStock,
    status: productForm.status,
  }
}

async function handleView(row: Product): Promise<void> {
  viewProduct.value = row
  viewDialogVisible.value = true
  if (!row.id) {
    return
  }

  viewLoading.value = true
  try {
    viewProduct.value = await fetchProductDetail(row.id)
  } catch {
    messageTip('加载产品详情失败', 'error')
  } finally {
    viewLoading.value = false
  }
}

function handleAdd(): void {
  dialogType.value = 'add'
  editingProductId.value = null
  resetProductForm()
  dialogVisible.value = true
}

function handleEdit(row: Product): void {
  if (!row.id) {
    messageTip('产品ID为空，无法编辑', 'warning')
    return
  }
  dialogType.value = 'edit'
  editingProductId.value = row.id
  resetProductForm(row)
  dialogVisible.value = true
}

async function handleDelete(row: Product): Promise<void> {
  if (!row.id) {
    messageTip('产品ID为空，无法删除', 'warning')
    return
  }

  try {
    await messageConfirm('确认删除该产品？')
  } catch {
    return
  }

  try {
    await deleteProduct(row.id)
    messageTip('删除成功', 'success')
    await loadProducts()
  } catch {
    messageTip('删除失败', 'error')
  }
}

async function onSubmit(): Promise<void> {
  if (!validateProductForm()) {
    return
  }

  submitting.value = true
  try {
    const payload = toProductPayload()
    if (dialogType.value === 'add') {
      await createProduct(payload)
      messageTip('新增成功', 'success')
    } else if (editingProductId.value != null) {
      await updateProduct(editingProductId.value, payload)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadProducts()
  } catch {
    messageTip('操作失败', 'error')
  } finally {
    submitting.value = false
  }
}

function handleCategory(): void {
  void router.push('/dashboard/product/category')
}

function handlePromotion(): void {
  void router.push('/dashboard/product/promotion')
}

function handleStockAlert(): void {
  void router.push('/dashboard/product/stock')
}

function handleCurrentChange(page: number): void {
  currentPage.value = page
  void loadProducts()
}

onMounted(() => {
  void loadCategories()
  void loadProducts()
})
</script>
