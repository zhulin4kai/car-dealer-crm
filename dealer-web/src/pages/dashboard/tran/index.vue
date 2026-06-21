<template>
  <div class="p-5">
    <!-- Search Bar -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <div class="flex flex-nowrap items-center gap-4">
          <div class="flex items-center gap-2 shrink-0">
            <Label class="whitespace-nowrap">交易编号</Label>
            <Input v-model="searchForm.tranNo" @keyup.enter="handleSearch" placeholder="请输入交易编号" class="w-[200px]" />
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <Label class="whitespace-nowrap">客户名称</Label>
            <Input v-model="searchForm.customerName" @keyup.enter="handleSearch" placeholder="请输入客户名称" class="w-[200px]" />
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <Label class="whitespace-nowrap">交易状态</Label>
            <Select v-model="searchForm.stage">
              <SelectTrigger class="w-[200px]" @keyup.enter="handleSearch">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in TRAN_STAGE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <Button @click="handleSearch">查询</Button>
            <Button v-has-permission="PERMISSIONS.tran.create" variant="secondary" @click="handleAdd">新增交易</Button>
            <Button v-has-permission="PERMISSIONS.tran.delete" variant="destructive" @click="handleBatchDelete" :disabled="selectedIds.length === 0">批量删除</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Data Table -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox
                  :checked="isAllSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead>交易编号</TableHead>
              <TableHead>客户名称</TableHead>
              <TableHead>交易金额</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>创建时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <template v-if="loading">
              <TableRow v-for="i in 5" :key="'skel-' + i">
                <TableCell v-for="j in 8" :key="'skel-c-' + j"><Skeleton class="h-4 w-full" /></TableCell>
              </TableRow>
            </template>
            <template v-else>
              <TableRow v-for="(row, idx) in tableData" :key="row.id">
                <TableCell>
                  <Checkbox
                    :checked="selectedIds.includes(row.id)"
                    @update:checked="(v) => toggleRowSelection(row.id, v)"
                  />
                </TableCell>
                <TableCell>{{ startIndex(idx) }}</TableCell>
                <TableCell class="truncate max-w-[200px]">{{ row.tranNo }}</TableCell>
                <TableCell class="truncate max-w-[200px]">{{ row.customerName }}</TableCell>
                <TableCell class="truncate max-w-[200px]">
                  <span v-if="row.stage === TRAN_STAGE.QUOTATION">?</span>
                  <span v-else>&yen;{{ row.amount }}</span>
                </TableCell>
                <TableCell>
                  <Badge :class="getBadgeClass(row.stage)">
                    {{ getStatusText(row.stage) }}
                  </Badge>
                </TableCell>
                <TableCell class="truncate max-w-[200px]">{{ row.createTime }}</TableCell>
                <TableCell>
                  <div class="flex gap-1.5 justify-center flex-wrap">
                    <Button v-has-permission="PERMISSIONS.tran.view" variant="outline" size="sm" @click="handleView(row)">查看</Button>
                    <Button
                      v-if="row.stage === TRAN_STAGE.QUOTATION"
                      v-has-permission="PERMISSIONS.tran.edit"
                      size="sm"
                      @click="handleEdit(row)"
                    >编辑</Button>
                    <Button
                      v-if="row.stage === TRAN_STAGE.PENDING"
                      v-has-permission="PERMISSIONS.tran.approve"
                      variant="secondary"
                      size="sm"
                      @click="handleApprove(row)"
                    >审批</Button>
                    <Button
                      v-if="row.stage === TRAN_STAGE.APPROVED"
                      v-has-permission="PERMISSIONS.tran.invoice"
                      variant="outline"
                      size="sm"
                      @click="handleInvoice(row)"
                    >开票</Button>
                    <Button
                      v-if="row.stage === TRAN_STAGE.QUOTATION"
                      v-has-permission="PERMISSIONS.tran.delete"
                      variant="destructive"
                      size="sm"
                      @click="handleDelete(row.id)"
                    >删除</Button>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- Pagination -->
    <div class="flex items-center justify-center gap-2 mt-3">
      <Button variant="outline" size="sm" @click="handleCurrentChange(currentPage - 1)" :disabled="currentPage <= 1">上一页</Button>
      <span class="text-sm text-muted-foreground px-2">{{ currentPage }} / {{ Math.ceil(total / pageSize) || 1 }}</span>
      <Button variant="outline" size="sm" @click="handleCurrentChange(currentPage + 1)" :disabled="currentPage >= Math.ceil(total / pageSize)">下一页</Button>
    </div>

    <!-- Transaction Edit Dialog -->
    <Dialog v-model:open="dialogOpen">
      <DialogContent class="max-w-[800px]" @update:open="(v) => { if (!v) handleDialogClose() }">
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
                <SelectItem v-for="customer in customerOptions" :key="customer.customerId" :value="customer.customerId">
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
                <Select v-model="product.productId" @update:model-value="(v) => onProductChange(index, v)" class="flex-1">
                  <SelectTrigger class="w-[60%]">
                    <SelectValue placeholder="请选择产品" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="item in productOptions.list" :key="item.id" :value="item.id">
                      {{ item.name }} (&yen;{{ item.price }})
                    </SelectItem>
                  </SelectContent>
                </Select>

                <NumberField
                  v-model="product.quantity"
                  :min="1"
                  :max="999"
                  class="w-[25%]"
                >
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
                >删除</Button>
              </div>
              <p v-if="errors[`products.${index}.productId`]" class="text-sm text-destructive">{{ errors[`products.${index}.productId`] }}</p>
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
            <p v-if="errors.expectedDeliveryDate" class="text-sm text-destructive">{{ errors.expectedDeliveryDate }}</p>
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
import { getTranList, updateTran, createTran, getTranDetail, getTranProducts, deleteTran, batchDeleteTran } from '@/modules/tran/api/tran-api'
import { getProductList } from '@/modules/product/api/product-api'
import { getCustomerOptions } from '@/modules/customer/api/customer-api'
import { TRAN_STAGE, TRAN_STAGE_OPTIONS, getTranStageText, getTranStageType, normalizeTranStage } from '@/modules/tran/model/tran-stage'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { NumberField, NumberFieldContent, NumberFieldInput, NumberFieldIncrement, NumberFieldDecrement } from '@/components/ui/number-field'
import { Skeleton } from '@/components/ui/skeleton'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])

// Dialog state
const dialogOpen = ref(false)
const isEdit = ref(false)
const productOptions = reactive({
  list: []
})
const customerOptions = ref([])

const searchForm = reactive({
  tranNo: '',
  customerId: '',
  customerName: '',
  stage: ''
})

// Form schema
const tranSchema = toTypedSchema(z.object({
  customerId: z.string().min(1, '请选择客户'),
  description: z.string().optional().default(''),
  expectedDeliveryDate: z.string().min(1, '请选择预计交付日期'),
  products: z.array(z.object({
    productId: z.string().min(1, '请选择产品'),
    quantity: z.number().min(1, '数量至少为1').max(999),
    price: z.number().min(0),
  })).min(1),
}))

const { handleSubmit, errors, values, isSubmitting, setFieldValue, resetForm, setValues } = useForm({
  validationSchema: tranSchema,
  initialValues: {
    customerId: '',
    description: '',
    expectedDeliveryDate: '',
    products: [{ productId: '', quantity: 1, price: 0 }],
  },
})

// Internal edit ID (not in form schema)
let editId = null

const getStatusType = getTranStageType
const getStatusText = getTranStageText

// Badge class mapping
const getBadgeClass = (stage) => {
  const type = getStatusType(stage)
  switch (type) {
    case 'success': return 'bg-green-600 text-white'
    case 'warning': return 'bg-yellow-600 text-white'
    case 'danger': return 'bg-red-600 text-white'
    case 'info': return ''
    default: return ''
  }
}

// Selection helpers
const isAllSelected = computed(() => {
  return tableData.value.length > 0 && selectedIds.value.length === tableData.value.length
})

const toggleSelectAll = (checked) => {
  if (checked) {
    selectedIds.value = tableData.value.map(item => item.id)
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
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

// Fetch transaction list
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    const res = await getTranList(params)
    if (true) {
      tableData.value = res.list.map(item => ({
        id: item.id,
        tranNo: item.tranNo,
        customerName: `${item.customerName}`,
        amount: item.money,
        stage: normalizeTranStage(item.stage),
        createTime: item.createTime
      }))
      total.value = res.total
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    console.error('获取交易列表失败:', error)
    messageTip('获取数据失败', 'error')
  } finally {
    loading.value = false
  }
}

// Search
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// Single delete
const handleDelete = async (id) => {
  try {
    await messageConfirm('您确定要删除该交易吗？')

    const res = await deleteTran(id)
    if (true) {
      messageTip('删除成功', 'success')
      fetchData()
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      console.error('删除失败:', error)
      messageTip('删除失败', 'error')
    }
  }
}

// Batch delete
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请选择要删除的交易', 'warning')
    return
  }

  try {
    await messageConfirm(`您确定要删除选中的 ${selectedIds.value.length} 条交易吗？`)

    const res = await batchDeleteTran(selectedIds.value)
    if (true) {
      messageTip(`成功删除 ${selectedIds.value.length} 条交易`, 'success')
      selectedIds.value = []
      fetchData()
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      console.error('批量删除失败:', error)
      messageTip('批量删除失败', 'error')
    }
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
const handleEdit = (row) => {
  isEdit.value = true
  editId = row.id
  resetFormState()
  dialogOpen.value = true
  loadCustomers()
  loadProducts()
  setTimeout(() => {
    fetchTranDetail(row.id)
  }, 100)
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
    price: 0
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
  const selectedCustomer = customerOptions.value.find(c => c.customerId === customerId)
  if (selectedCustomer) {
    setFieldValue('customerName', selectedCustomer.customerName)
  }
}

// Product selection change - update price
const onProductChange = (index, productId) => {
  const selectedProduct = productOptions.list.find(p => p.id === productId)
  if (selectedProduct) {
    setFieldValue(`products.${index}.price`, selectedProduct.price)
  }
}

// Load customer options
const loadCustomers = async () => {
  try {
    const res = await getCustomerOptions()
    if (true) {
      customerOptions.value = res
    }
  } catch (error) {
    messageTip('加载客户列表失败', 'error')
  }
}

// Load product list
const loadProducts = async () => {
  try {
    const res = await getProductList({
      page: 1,
      size: 1000
    })
    if (true) {
      productOptions.list = res.list
    }
  } catch (error) {
    messageTip('加载产品列表失败', 'error')
  }
}

// Fetch transaction detail for editing
const fetchTranDetail = async (id) => {
  try {
    const res = await getTranDetail(id)
    if (true) {
      const data = res
      editId = data.id || id
      setValues({
        customerId: data.customerId || '',
        description: data.description || '',
        expectedDeliveryDate: data.expectedDate ? new Date(data.expectedDate).toISOString().split('T')[0] : '',
        products: [],
      })
      // Set customerName via setFieldValue
      setFieldValue('customerName', data.customerName || '')

      // Fetch product details
      const productRes = await getTranProducts(id)
      if (true && productRes.length > 0) {
        setFieldValue('products', productRes.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          price: item.price
        })))
      }
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    messageTip('获取交易详情失败', 'error')
  }
}

// Submit form
const onSubmit = handleSubmit(async () => {
  try {
    // Format data for API
    const customerName = (values as Record<string, unknown>).customerName as string || ''
    const formData = {
      id: isEdit.value ? editId : undefined,
      customerId: values.customerId,
      customerName,
      amount: 0,
      products: values.products.map(p => ({
        productId: p.productId,
        quantity: p.quantity,
        price: p.price
      })),
      description: values.description,
      expectedDeliveryDate: values.expectedDeliveryDate ?
        values.expectedDeliveryDate + ' 00:00:00' : null
    }
    let res
    if (isEdit.value) {
      res = await updateTran(formData)
    } else {
      res = await createTran(formData)
    }
    if (true) {
      messageTip('保存成功', 'success')
      dialogOpen.value = false
      resetFormState()
      fetchData()
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    console.error('保存失败:', error)
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
