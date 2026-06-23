<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="crm-panel-body">
        <div class="crm-toolbar">
          <div class="crm-field">
            <Label class="crm-field-label">交易编号</Label>
            <Input
              v-model="searchForm.tranNo"
              @keyup.enter="handleSearch"
              placeholder="请输入交易编号"
              class="w-[200px]"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">客户名称</Label>
            <Input
              v-model="searchForm.customerName"
              @keyup.enter="handleSearch"
              placeholder="请输入客户名称"
              class="w-[200px]"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">交易状态</Label>
            <Select v-model="searchForm.stage">
              <SelectTrigger class="w-[200px]" @keyup.enter="handleSearch">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="option in TRAN_STAGE_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="crm-toolbar-actions">
            <Button class="gap-2" @click="handleSearch">
              <Search class="h-4 w-4" />
              查询
            </Button>
            <Button
              v-has-permission="PERMISSIONS.tran.create"
              variant="outline"
              class="gap-2"
              @click="handleAdd"
            >
              <Plus class="h-4 w-4" />
              新增交易
            </Button>
            <Button
              v-has-permission="PERMISSIONS.tran.delete"
              variant="destructive"
              class="gap-2"
              @click="handleBatchDelete"
              :disabled="selectedIds.length === 0"
            >
              <Trash2 class="h-4 w-4" />
              批量删除
            </Button>
          </div>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <Table class="min-w-[1080px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox :checked="isAllSelected" @update:checked="toggleSelectAll" />
              </TableHead>
              <TableHead
                class="w-[80px]"
                sortable
                sort-key="index"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >序号</TableHead
              >
              <TableHead
                sortable
                sort-key="tranNo"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >交易编号</TableHead
              >
              <TableHead
                sortable
                sort-key="customerName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >客户名称</TableHead
              >
              <TableHead
                sortable
                sort-key="amount"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >交易金额</TableHead
              >
              <TableHead
                sortable
                sort-key="stage"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >状态</TableHead
              >
              <TableHead
                sortable
                sort-key="createTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >创建时间</TableHead
              >
              <TableHead class="w-[180px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <template v-if="loading">
              <TableRow v-for="i in 5" :key="'skel-' + i">
                <TableCell v-for="j in 8" :key="'skel-c-' + j"
                  ><Skeleton class="h-4 w-full"
                /></TableCell>
              </TableRow>
            </template>
            <template v-else>
              <TableRow v-if="displayTranList.length === 0">
                <TableCell colspan="8" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                  >暂无交易数据</TableCell
                >
              </TableRow>
              <TableRow v-for="(row, idx) in displayTranList" :key="row.id">
                <TableCell>
                  <Checkbox
                    :checked="selectedIds.includes(row.id)"
                    @update:checked="(v) => toggleRowSelection(row.id, v)"
                  />
                </TableCell>
                <TableCell class="text-[var(--crm-text-tertiary)]">{{ startIndex(idx) }}</TableCell>
                <TableCell
                  class="max-w-[200px] truncate font-mono text-xs text-[var(--crm-text-secondary)]"
                  >{{ row.tranNo }}</TableCell
                >
                <TableCell
                  class="max-w-[200px] truncate font-semibold text-[var(--crm-text-primary)]"
                  >{{ row.customerName }}</TableCell
                >
                <TableCell class="max-w-[200px] truncate">
                  <span
                    v-if="row.stage === TRAN_STAGE.QUOTATION"
                    class="text-[var(--crm-text-tertiary)]"
                    >待报价</span
                  >
                  <span v-else class="font-semibold text-[var(--crm-text-primary)]">{{
                    formatCurrency(row.amount, { fractionDigits: 0 })
                  }}</span>
                </TableCell>
                <TableCell>
                  <StatusBadge :label="getStatusText(row.stage)" :tone="getTranTone(row.stage)" />
                </TableCell>
                <TableCell class="max-w-[200px] truncate">{{ row.createTime }}</TableCell>
                <TableCell>
                  <div class="flex items-center gap-1">
                    <RowActionButton
                      v-has-permission="PERMISSIONS.tran.view"
                      label="查看"
                      @click="handleView(row)"
                    >
                      <Eye class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton
                      v-if="row.stage === TRAN_STAGE.QUOTATION"
                      v-has-permission="PERMISSIONS.tran.edit"
                      label="编辑"
                      @click="handleEdit(row)"
                    >
                      <Pencil class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton
                      v-if="row.stage === TRAN_STAGE.PENDING"
                      v-has-permission="PERMISSIONS.tran.approve"
                      label="审批"
                      @click="handleApprove(row)"
                    >
                      <BadgeCheck class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton
                      v-if="row.stage === TRAN_STAGE.APPROVED"
                      v-has-permission="PERMISSIONS.tran.invoice"
                      label="开票"
                      @click="handleInvoice(row)"
                    >
                      <FileText class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton
                      v-if="row.stage === TRAN_STAGE.QUOTATION"
                      v-has-permission="PERMISSIONS.tran.delete"
                      label="删除"
                      danger
                      @click="handleDelete(row.id)"
                    >
                      <Trash2 class="h-4 w-4" />
                    </RowActionButton>
                    <RowActionButton
                      v-if="row.stage === TRAN_STAGE.LOST"
                      v-has-permission="PERMISSIONS.tran.resubmit"
                      label="重新提交"
                      @click="handleResubmit(row)"
                    >
                      <RotateCcw class="h-4 w-4" />
                    </RowActionButton>
                  </div>
                </TableCell>
              </TableRow>
            </template>
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

    <!-- Transaction Edit Dialog -->
    <Dialog v-model:open="dialogOpen">
      <DialogContent
        class="max-w-[800px]"
        @update:open="
          (v) => {
            if (!v) handleDialogClose()
          }
        "
      >
        <DialogHeader>
          <DialogTitle>{{ isEdit ? '编辑交易' : '新增交易' }}</DialogTitle>
        </DialogHeader>

        <form class="space-y-4 mt-4" @submit.prevent="onSubmit">
          <!-- Customer -->
          <div class="space-y-2">
            <Label>客户名称</Label>
            <Select v-model="values.customerId" @update:model-value="onCustomerChange">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择客户" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="customer in customerOptions"
                  :key="customer.customerId"
                  :value="customer.customerId"
                >
                  {{ customer.customerName }}
                </SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.customerId" class="text-sm text-destructive">{{ errors.customerId }}</p>
          </div>

          <!-- Products -->
          <div class="border rounded-md p-4 bg-muted/30 space-y-3">
            <div v-for="(product, index) in values.products" :key="index" class="space-y-2">
              <Label v-if="index === 0">产品选择</Label>
              <div class="flex items-center gap-2">
                <Select
                  v-model="product.productId"
                  @update:model-value="(v) => onProductChange(index, v)"
                  class="flex-1"
                >
                  <SelectTrigger class="w-[60%]">
                    <SelectValue placeholder="请选择产品" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="item in productOptions.list" :key="item.id" :value="item.id">
                      {{ item.name }} (&yen;{{ item.price }})
                    </SelectItem>
                  </SelectContent>
                </Select>

                <NumberField v-model="product.quantity" :min="1" :max="999" class="w-[25%]">
                  <NumberFieldContent>
                    <NumberFieldDecrement />
                    <NumberFieldInput placeholder="数量" />
                    <NumberFieldIncrement />
                  </NumberFieldContent>
                </NumberField>

                <Button
                  v-if="values.products.length > 1"
                  type="button"
                  variant="destructive"
                  size="sm"
                  @click="removeProduct(index)"
                  class="w-[10%]"
                  >删除</Button
                >
              </div>
              <p v-if="errors[`products.${index}.productId`]" class="text-sm text-destructive">
                {{ errors[`products.${index}.productId`] }}
              </p>
            </div>

            <Button type="button" variant="outline" @click="addProduct">追加商品</Button>
          </div>

          <!-- Description -->
          <div class="space-y-2">
            <Label>交易描述</Label>
            <Textarea v-model="values.description" :rows="4" placeholder="请输入交易描述" />
          </div>

          <!-- Expected Delivery Date -->
          <div class="space-y-2">
            <Label>预计交付日期</Label>
            <Input type="date" v-model="values.expectedDeliveryDate" class="w-full" />
            <p v-if="errors.expectedDeliveryDate" class="text-sm text-destructive">
              {{ errors.expectedDeliveryDate }}
            </p>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" type="button" @click="handleDialogClose">取消</Button>
          <Button @click="onSubmit" :disabled="isSubmitting">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, reactive, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import {
  getTranList,
  updateTran,
  createTran,
  getTranDetail,
  getTranProducts,
  deleteTran,
  batchDeleteTran,
  resubmitTran,
} from '@/modules/tran/api/tran-api'
import { getProductList } from '@/modules/product/api/product-api'
import { getCustomerOptions } from '@/modules/customer/api/customer-api'
import {
  TRAN_STAGE,
  TRAN_STAGE_OPTIONS,
  getTranStageText,
  getTranStageType,
  normalizeTranStage,
} from '@/modules/tran/model/tran-stage'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import { normalizePage } from '@/shared/utils/pagination'
import { toLocalDateInput } from '@/shared/datetime/local-date'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { Tran } from '@/modules/tran/model/tran.types'
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import {
  NumberField,
  NumberFieldContent,
  NumberFieldInput,
  NumberFieldIncrement,
  NumberFieldDecrement,
} from '@/components/ui/number-field'
import { Skeleton } from '@/components/ui/skeleton'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { formatCurrency } from '@/shared/utils/display-format'
import { useClientSort } from '@/shared/utils/table-sort'
import { BadgeCheck, Eye, FileText, Pencil, Plus, RotateCcw, Search, Trash2 } from '@lucide/vue'

const router = useRouter()
interface TranListRow {
  id: EntityId
  tranNo: string
  customerName: string
  amount: number
  stage: string
  createTime: string
}

const tableData = ref<TranListRow[]>([])
const {
  sortBy,
  sortDirection,
  sortedRows: displayTranList,
  toggleSort,
} = useClientSort<TranListRow>(tableData, {
  index: 'id',
  tranNo: 'tranNo',
  customerName: 'customerName',
  amount: 'amount',
  stage: (row) => getStatusText(row.stage),
  createTime: 'createTime',
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref<EntityId[]>([])
const { loading, run: runTranPage } = useLatestRequest<PageResult<Tran>>()

// Dialog state
const dialogOpen = ref(false)
const isEdit = ref(false)
const productOptions = reactive({
  list: [],
})
const customerOptions = ref([])

const searchForm = reactive({
  tranNo: '',
  customerId: '',
  customerName: '',
  stage: '',
})

// Form schema
const tranSchema = toTypedSchema(
  z.object({
    customerId: z.string().min(1, '请选择客户'),
    description: z.string().optional().default(''),
    expectedDeliveryDate: z.string().min(1, '请选择预计交付日期'),
    products: z
      .array(
        z.object({
          productId: z.string().min(1, '请选择产品'),
          quantity: z.number().min(1, '数量至少为1').max(999),
          price: z.number().min(0),
        }),
      )
      .min(1),
  }),
)

const { handleSubmit, errors, values, isSubmitting, setFieldValue, resetForm, setValues } = useForm(
  {
    validationSchema: tranSchema,
    initialValues: {
      customerId: '',
      description: '',
      expectedDeliveryDate: '',
      products: [{ productId: '', quantity: 1, price: 0 }],
    },
  },
)

// Internal edit ID (not in form schema)
let editId = null

const getStatusType = getTranStageType
const getStatusText = getTranStageText

function getTranTone(
  stage: unknown,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  const type = getStatusType(stage)
  switch (type) {
    case 'success':
      return 'success'
    case 'warning':
      return 'warning'
    case 'danger':
      return 'danger'
    case 'info':
      return 'info'
    case 'primary':
      return 'purple'
    default:
      return 'muted'
  }
}

// Selection helpers
const isAllSelected = computed(() => {
  return displayTranList.value.length > 0 && selectedIds.value.length === displayTranList.value.length
})

const toggleSelectAll = (checked) => {
  if (checked) {
    selectedIds.value = displayTranList.value.map((item) => item.id)
  } else {
    selectedIds.value = []
  }
}

const toggleRowSelection = (id, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter((i) => i !== id)
  }
}

// Fetch transaction list
const fetchData = async () => {
  const params: TranQuery = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (searchForm.tranNo.trim()) {
    params.tranNo = searchForm.tranNo.trim()
  }
  if (searchForm.customerName.trim()) {
    params.customerName = searchForm.customerName.trim()
  }
  if (searchForm.stage) {
    params.stage = searchForm.stage
  }
  const res = await runTranPage((signal) => getTranList(params, signal))
  if (res) {
    tableData.value = res.list.map((item) => ({
      id: item.id,
      tranNo: item.tranNo ?? '',
      customerName: item.customerName ?? '',
      amount: item.money ?? 0,
      stage: normalizeTranStage(item.stage),
      createTime: item.createTime ?? '',
    }))
    total.value = res.total
  }
}

async function reloadAfterDelete(deletedCount: number): Promise<void> {
  const estimatedTotal = Math.max(total.value - deletedCount, 0)
  currentPage.value = normalizePage(currentPage.value, estimatedTotal, pageSize.value)
  await fetchData()
  if (tableData.value.length === 0 && currentPage.value > 1) {
    currentPage.value -= 1
    await fetchData()
  }
}

// Search
const handleSearch = () => {
  currentPage.value = 1
  void fetchData()
}

// Single delete
const handleDelete = async (id: number | string) => {
  try {
    await messageConfirm('您确定要删除该交易吗？')
  } catch {
    messageTip('取消删除', 'warning')
    return
  }
  try {
    await deleteTran(id)
    messageTip('删除成功', 'success')
    try {
      await reloadAfterDelete(1)
    } catch {
      messageTip('删除已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip('删除失败', 'error')
  }
}

// Batch delete
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请选择要删除的交易', 'warning')
    return
  }
  const deletedCount = selectedIds.value.length
  try {
    await messageConfirm(`您确定要删除选中的 ${deletedCount} 条交易吗？`)
  } catch {
    messageTip('取消批量删除', 'warning')
    return
  }
  try {
    await batchDeleteTran(selectedIds.value)
    messageTip(`成功删除 ${deletedCount} 条交易`, 'success')
    selectedIds.value = []
    try {
      await reloadAfterDelete(deletedCount)
    } catch {
      messageTip('删除已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip('批量删除失败', 'error')
  }
}

// Resubmit rejected transaction
const handleResubmit = async (row: Record<string, unknown>) => {
  try {
    await messageConfirm(
      '确定要重新提交该交易吗？将重新占用库存并清除旧审批记录，如不改动请先进入详情编辑。',
    )
  } catch {
    messageTip('取消重新提交', 'warning')
    return
  }
  try {
    await resubmitTran(row.id)
    messageTip('重新提交成功，交易已回待报价，可重新编辑结算', 'success')
    try {
      await fetchData()
    } catch {
      messageTip('重新提交已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip('重新提交失败，可能是库存不足或状态已变更', 'error')
  }
}

// Add transaction
const handleAdd = () => {
  isEdit.value = false
  editId = null
  resetFormState()
  dialogOpen.value = true
  loadCustomers()
  loadProducts()
}

// Edit transaction
const handleEdit = async (row: TranListRow) => {
  isEdit.value = true
  resetFormState()
  editId = row.id
  dialogOpen.value = true
  await Promise.all([loadCustomers(), loadProducts()])
  await fetchTranDetail(row.id)
}

// Reset form state
const resetFormState = () => {
  editId = null
  resetForm()
}

// Close Dialog
const handleDialogClose = () => {
  dialogOpen.value = false
  resetFormState()
}

// Add product row
const addProduct = () => {
  values.products.push({
    productId: '',
    quantity: 1,
    price: 0,
  })
}

// Remove product row
const removeProduct = (index) => {
  if (values.products.length > 1) {
    values.products.splice(index, 1)
  }
}

// Customer selection change - update customerName
const onCustomerChange = (customerId) => {
  const selectedCustomer = customerOptions.value.find((c) => c.customerId === customerId)
  if (selectedCustomer) {
    setFieldValue('customerName', selectedCustomer.customerName)
  }
}

// Product selection change - update price
const onProductChange = (index, productId) => {
  const selectedProduct = productOptions.list.find((p) => p.id === productId)
  if (selectedProduct) {
    setFieldValue(`products.${index}.price`, selectedProduct.price)
  }
}

// Load customer options
const loadCustomers = async () => {
  try {
    const res = await getCustomerOptions()
    customerOptions.value = res
  } catch {
    messageTip('加载客户列表失败', 'error')
  }
}

// Load product list
const loadProducts = async () => {
  try {
    const res = await getProductList({
      page: 1,
      size: 1000,
    })
    productOptions.list = res.list
  } catch {
    messageTip('加载产品列表失败', 'error')
  }
}

// Fetch transaction detail for editing
const fetchTranDetail = async (id) => {
  try {
    const res = await getTranDetail(id)
    const data = res
    editId = data.id || id
    setValues({
      customerId: String(data.customerId ?? ''),
      description: data.description || '',
      expectedDeliveryDate: toLocalDateInput(data.expectedDate ?? null),
      products: [],
    })
    setFieldValue('customerName', data.customerName || '')

    const productRes = await getTranProducts(id)
    if (productRes.length > 0) {
      setFieldValue(
        'products',
        productRes.map((item) => ({
          productId: String(item.productId),
          quantity: item.quantity,
          price: item.price,
        })),
      )
    }
  } catch {
    messageTip('获取交易详情失败', 'error')
  }
}

// Submit form
const onSubmit = handleSubmit(async () => {
  try {
    // Format data for API
    const baseRequest = {
      customerId: values.customerId,
      products: values.products.map((p) => ({
        productId: p.productId,
        quantity: p.quantity,
      })),
      description: values.description,
      expectedDeliveryDate: values.expectedDeliveryDate
        ? values.expectedDeliveryDate + ' 00:00:00'
        : null,
    }
    if (isEdit.value) {
      if (editId === null) throw new Error('缺少编辑交易ID')
      await updateTran({ ...baseRequest, id: editId })
    } else {
      await createTran(baseRequest)
    }
    messageTip('保存成功', 'success')
    dialogOpen.value = false
    resetFormState()
    await fetchData()
  } catch {
    messageTip('保存失败', 'error')
  }
})

// View detail
const handleView = (row) => {
  router.push(`/dashboard/tran/${row.id}`)
}

// Approve transaction
const handleApprove = (row) => {
  router.push(`/dashboard/tran/approve/${row.id}`)
}

// Invoice transaction
const handleInvoice = (row) => {
  router.push(`/dashboard/tran/invoice/${row.id}`)
}

const handleCurrentChange = (val) => {
  if (val < 1 || val > Math.ceil(total.value / pageSize.value)) return
  currentPage.value = val
  fetchData()
}

const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  fetchData()
})
</script>
