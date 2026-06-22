<template>
  <div class="p-5">
    <Card class="max-w-[1000px] mx-auto">
      <CardHeader class="flex flex-row items-center justify-between space-y-0">
        <CardTitle>交易开票</CardTitle>
        <Badge class="bg-green-600 text-white">已审批</Badge>
      </CardHeader>
      <CardContent>
        <!-- Transaction Basic Info -->
        <div class="border rounded-md mb-5">
          <div class="grid grid-cols-[120px_1fr_120px_1fr]">
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易编号</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ tranDetail.tranNo }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">客户名称</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.customerName }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易金额</div>
            <div class="px-4 py-2 text-sm border-b border-r">&yen;{{ tranDetail.amount }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">创建时间</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.createTime }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">预计交付日期</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ tranDetail.expectedDeliveryDate }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">最后更新时间</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.updateTime }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-r">交易描述</div>
            <div class="px-4 py-2 text-sm col-span-3">{{ tranDetail.description }}</div>
          </div>
        </div>

        <!-- Product Info -->
        <div class="text-base font-bold my-5 pl-2.5 border-l-4 border-primary">产品信息</div>
        <Table class="mb-5">
          <TableHeader>
            <TableRow>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead class="min-w-[300px]">产品名称</TableHead>
              <TableHead class="w-[120px]">数量</TableHead>
              <TableHead class="w-[140px]">单价</TableHead>
              <TableHead class="w-[140px]">小计</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(product, index) in tranDetail.products" :key="index">
              <TableCell>{{ index + 1 }}</TableCell>
              <TableCell>{{ product.productName }}</TableCell>
              <TableCell>{{ product.quantity }}</TableCell>
              <TableCell>&yen;{{ product.price }}</TableCell>
              <TableCell>&yen;{{ product.price * product.quantity }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <!-- Invoice Form -->
        <div class="text-base font-bold my-5 pl-2.5 border-l-4 border-primary">发票信息</div>

        <!-- Existing invoice list -->
        <div v-if="invoiceList.length > 0" class="mb-5 space-y-3">
          <div v-for="invoice in invoiceList" :key="invoice.id" class="p-4 border rounded-md space-y-2">
            <div class="flex justify-between items-center">
              <Badge :variant="getInvoiceStatusVariant(invoice.status)">{{ getInvoiceStatusText(invoice.status) }}</Badge>
              <div class="flex gap-2">
                <Button
                  v-if="invoice.status === 'PENDING'"
                  v-has-permission="PERMISSIONS.tran.invoice"
                  size="sm"
                  :disabled="invoiceLoading"
                  @click="markAsIssued(invoice)"
                >标记已开具</Button>
                <Button
                  v-if="invoice.status === 'PENDING' || invoice.status === 'ISSUED'"
                  v-has-permission="PERMISSIONS.tran.invoice"
                  variant="destructive"
                  size="sm"
                  :disabled="invoiceLoading"
                  @click="voidInvoice(invoice)"
                >作废</Button>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2 text-sm">
              <div>发票编号：{{ invoice.invoiceNo }}</div>
              <div>金额：&yen;{{ invoice.amount }}</div>
              <div>类型：{{ getInvoiceTypeText(invoice.type) }}</div>
              <div>备注：{{ invoice.remark }}</div>
            </div>
          </div>
        </div>

        <!-- Create form (only when no existing invoice) -->
        <form v-if="invoiceList.length === 0" class="mt-8 max-w-[600px] space-y-4" @submit.prevent="onSubmit">
          <!-- Invoice Type -->
          <div class="space-y-2">
            <Label>发票类型</Label>
            <RadioGroup v-model="values.type" class="flex items-center gap-6">
              <div class="flex items-center space-x-2">
                <RadioGroupItem id="vat-normal" value="VAT_NORMAL" />
                <Label for="vat-normal" class="font-normal cursor-pointer">增值税普通发票</Label>
              </div>
              <div class="flex items-center space-x-2">
                <RadioGroupItem id="vat-special" value="VAT_SPECIAL" />
                <Label for="vat-special" class="font-normal cursor-pointer">增值税专用发票</Label>
              </div>
            </RadioGroup>
            <p v-if="errors.type" class="text-sm text-destructive">{{ errors.type }}</p>
            <Alert v-if="values.type === 'VAT_SPECIAL'" class="mt-2">
              <AlertTitle>提示：开具增值税专用发票需要填写完整的开户行、银行账号、注册地址和注册电话信息</AlertTitle>
            </Alert>
          </div>

          <!-- Invoice Title -->
          <div class="space-y-2">
            <Label>发票抬头</Label>
            <Input v-model="values.title" placeholder="请输入发票抬头（2-100个字符）" maxlength="100" />
            <p v-if="errors.title" class="text-sm text-destructive">{{ errors.title }}</p>
          </div>

          <!-- Tax Number -->
          <div class="space-y-2">
            <Label>纳税人识别号</Label>
            <Input
              v-model="values.taxNumber"
              placeholder="请输入纳税人识别号（15-20位数字和字母组合）"
              maxlength="20"
              class="uppercase"
              @update:model-value="(v) => values.taxNumber = (v || '').toUpperCase()"
            />
            <p v-if="errors.taxNumber" class="text-sm text-destructive">{{ errors.taxNumber }}</p>
          </div>

          <!-- Bank Name -->
          <div class="space-y-2">
            <Label>开户行</Label>
            <Input v-model="values.bankName" placeholder="请输入开户行（专用发票必填）" maxlength="50" />
            <p v-if="errors.bankName" class="text-sm text-destructive">{{ errors.bankName }}</p>
          </div>

          <!-- Bank Account -->
          <div class="space-y-2">
            <Label>银行账号</Label>
            <Input
              v-model="values.bankAccount"
              placeholder="请输入银行账号（10-30位数字，专用发票必填）"
              maxlength="30"
              @update:model-value="(v) => values.bankAccount = (v || '').replace(/\D/g, '')"
            />
            <p v-if="errors.bankAccount" class="text-sm text-destructive">{{ errors.bankAccount }}</p>
          </div>

          <!-- Address -->
          <div class="space-y-2">
            <Label>注册地址</Label>
            <Input v-model="values.address" placeholder="请输入注册地址（专用发票必填）" maxlength="200" />
            <p v-if="errors.address" class="text-sm text-destructive">{{ errors.address }}</p>
          </div>

          <!-- Phone -->
          <div class="space-y-2">
            <Label>注册电话</Label>
            <Input v-model="values.phone" placeholder="请输入注册电话（固话：0xx-xxxxxxxx，手机：1xxxxxxxxx，专用发票必填）" maxlength="20" />
            <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
          </div>

          <!-- Amount -->
          <div class="space-y-2">
            <Label>发票金额</Label>
            <NumberField v-model="values.amount" :min="0.01" :max="99999999.99" :step="0.01" class="w-full">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput placeholder="发票金额（0.01-99,999,999.99）" />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="errors.amount" class="text-sm text-destructive">{{ errors.amount }}</p>
          </div>

          <!-- Remark -->
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="values.remark" :rows="3" placeholder="请输入备注信息（最多500个字符）" maxlength="500" />
            <p v-if="errors.remark" class="text-sm text-destructive">{{ errors.remark }}</p>
          </div>

          <!-- Submit -->
          <div class="pt-2">
            <Button v-has-permission="PERMISSIONS.tran.invoice" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : '开具发票' }}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { useRoute, useRouter } from 'vue-router'
import { messageTip } from '@/shared/utils/feedback'
import { getTranDetail, getTranProducts, createInvoice, getTranInvoiceList, updateInvoiceStatus } from '@/modules/tran/api/tran-api'
import { normalizeTranStage } from '@/modules/tran/model/tran-stage'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Alert, AlertTitle } from '@/components/ui/alert'
import { NumberField, NumberFieldContent, NumberFieldInput, NumberFieldIncrement, NumberFieldDecrement } from '@/components/ui/number-field'
import { Label } from '@/components/ui/label'

const route = useRoute()
const router = useRouter()

const tranDetail = ref({
  tranNo: '',
  customerName: '',
  amount: 0,
  stage: '',
  createTime: '',
  updateTime: '',
  expectedDeliveryDate: '',
  description: '',
  products: []
})

const invoiceList = ref([])

// Invoice form schema with dynamic validation via superRefine
const invoiceSchema = toTypedSchema(z.object({
  type: z.string().min(1, '请选择发票类型'),
  title: z.string()
    .min(2, '发票抬头至少2个字符')
    .max(100, '发票抬头不能超过100个字符')
    .regex(/^[\u4e00-\u9fa5a-zA-Z0-9\(\)\（\）\-\_\&\＆\s]+$/, '发票抬头只能包含中文、英文、数字、括号、连字符、下划线和&符号'),
  taxNumber: z.string()
    .min(1, '请输入纳税人识别号')
    .regex(/^[0-9A-Z]{15}$|^[0-9A-Z]{17}$|^[0-9A-Z]{18}$|^[0-9A-Z]{20}$/, '纳税人识别号格式不正确（应为15位、17位、18位或20位数字和大写字母组合）'),
  bankName: z.string().optional()
    .refine(v => !v || v.length >= 2, '开户行名称至少2个字符')
    .refine(v => !v || v.length <= 50, '开户行名称不能超过50个字符')
    .refine(v => !v || /^[\u4e00-\u9fa5a-zA-Z0-9\(\)\（\）\s]+$/.test(v), '开户行名称只能包含中文、英文、数字、括号和空格'),
  bankAccount: z.string().optional()
    .refine(v => !v || /^[0-9]{10,30}$/.test(v), '银行账号应为10-30位数字'),
  address: z.string().optional()
    .refine(v => !v || v.length >= 5, '注册地址至少5个字符')
    .refine(v => !v || v.length <= 200, '注册地址不能超过200个字符')
    .refine(v => !v || /^[\u4e00-\u9fa5a-zA-Z0-9\-\#\s\(\)\（\）\,\，\.\。\号]+$/.test(v), '注册地址格式不正确'),
  phone: z.string().optional()
    .refine(v => !v || /^(0\d{2,3}-?\d{7,8})|(1[3456789]\d{9})$/.test(v), '请输入正确的电话号码格式（固话：0xx-xxxxxxxx，手机：1xxxxxxxxx）'),
  amount: z.number()
    .min(0.01, '发票金额必须大于0')
    .max(99999999.99, '发票金额不能超过99,999,999.99元'),
  remark: z.string().optional()
    .refine(v => !v || v.length <= 500, '备注信息不能超过500个字符'),
}).superRefine((data, ctx) => {
  if (data.type === 'VAT_SPECIAL') {
    if (!data.bankName) ctx.addIssue({ code: 'custom', message: '开具专用发票时开户行为必填项', path: ['bankName'] })
    if (!data.bankAccount) ctx.addIssue({ code: 'custom', message: '开具专用发票时银行账号为必填项', path: ['bankAccount'] })
    if (!data.address) ctx.addIssue({ code: 'custom', message: '开具专用发票时注册地址为必填项', path: ['address'] })
    if (!data.phone) ctx.addIssue({ code: 'custom', message: '开具专用发票时注册电话为必填项', path: ['phone'] })
  }
}))

const { handleSubmit, errors, values, isSubmitting, setValues } = useForm({
  validationSchema: invoiceSchema,
  initialValues: {
    type: 'VAT_NORMAL',
    title: '',
    taxNumber: '',
    bankName: '',
    bankAccount: '',
    address: '',
    phone: '',
    amount: 0,
    remark: '',
  },
})

// Internal tranId for API calls
let tranId = null

// Check if invoice has been issued (one invoice per transaction)
const hasAnyInvoice = computed(() => invoiceList.value.length > 0)
const invoiceLoading = ref(false)

// Invoice type mapping
const getInvoiceTypeText = (type) => {
  const typeMap = {
    'VAT_NORMAL': '增值税普通发票',
    'VAT_SPECIAL': '增值税专用发票'
  }
  return typeMap[type] || type
}

// Invoice status mapping
const invoiceStatusMap = {
  'PENDING': { type: 'warning', text: '待开具' },
  'ISSUED': { type: 'success', text: '已开具' },
  'VOID': { type: 'danger', text: '已作废' }
}

const getInvoiceStatusType = (status: string) => invoiceStatusMap[status]?.type || ''
const getInvoiceStatusText = (status: string) => invoiceStatusMap[status]?.text || status
const getInvoiceStatusVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  const map: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    ISSUED: 'default',
    VOID: 'destructive',
  }
  return map[status] || 'outline'
}

// Fetch transaction detail
const fetchTranDetail = async () => {
  const id = route.params.id as string
  if (!id) return
  try {
    const data = await getTranDetail(id)
    tranDetail.value = {
      tranNo: data.tranNo || '',
      customerName: data.customerName || '',
      amount: data.money || data.amount || 0,
      stage: normalizeTranStage(data.stage),
      createTime: data.createTime || '',
      updateTime: data.editTime || data.updateTime || '',
      expectedDeliveryDate: data.expectedDate || data.expectedDeliveryDate || '',
      description: data.description || '',
      products: data.products || [],
    }
    tranId = Number(id)
    setValues({
      type: 'VAT_NORMAL',
      title: '',
      taxNumber: '',
      bankName: '',
      bankAccount: '',
      address: '',
      phone: '',
      amount: tranDetail.value.amount,
      remark: '',
    })
  } catch {
    messageTip('获取交易详情失败', 'error')
  }
}

// Fetch product details
const fetchProducts = async () => {
  try {
    const res = await getTranProducts(route.params.id as string)
    tranDetail.value.products = res
  } catch {
    // Non-critical, leave existing products
  }
}

// Fetch invoice list
const fetchInvoiceList = async () => {
  try {
    const res = await getTranInvoiceList(route.params.id as string)
    invoiceList.value = res || []
  } catch {
    // Non-critical
  }
}

// Submit invoice form
const onSubmit = handleSubmit(async () => {
  if (hasAnyInvoice.value) {
    messageTip('该交易已有发票，不能重复开票', 'warning')
    return
  }
  try {
    await createInvoice({ tranId, ...values })
    messageTip('发票创建成功', 'success')
    try {
      await Promise.all([fetchInvoiceList(), fetchTranDetail()])
    } catch {
      messageTip('发票已创建，但刷新失败', 'warning')
    }
  } catch {
    messageTip('发票创建失败', 'error')
  }
})

// Mark invoice as issued
const markAsIssued = async (invoice: { id: number | string }) => {
  try {
    await messageConfirm('确认将该发票标记为已开具吗？')
  } catch {
    return
  }
  invoiceLoading.value = true
  try {
    await updateInvoiceStatus(invoice.id, 'ISSUED')
    messageTip('开票完成', 'success')
    await Promise.all([fetchInvoiceList(), fetchTranDetail()])
  } catch {
    messageTip('发票状态更新失败', 'error')
  } finally {
    invoiceLoading.value = false
  }
}

// Void invoice
const voidInvoice = async (invoice: { id: number | string }) => {
  try {
    await messageConfirm('确认作废该发票吗？作废后不可恢复')
  } catch {
    return
  }
  invoiceLoading.value = true
  try {
    await updateInvoiceStatus(invoice.id, 'VOID')
    messageTip('发票已作废', 'success')
    await Promise.all([fetchInvoiceList(), fetchTranDetail()])
  } catch {
    messageTip('发票作废失败', 'error')
  } finally {
    invoiceLoading.value = false
  }
}

// Watch route params
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await Promise.all([fetchTranDetail(), fetchProducts(), fetchInvoiceList()])
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }
  await Promise.all([fetchTranDetail(), fetchProducts(), fetchInvoiceList()])
})
</script>
