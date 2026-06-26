<template>
  <div class="p-5">
    <Card class="max-w-[1000px] mx-auto">
      <CardHeader class="flex flex-row items-center justify-between space-y-0">
        <CardTitle>交易开票</CardTitle>
        <div class="flex items-center gap-2">
          <Button
            v-if="canRecordPayment"
            v-has-permission="PERMISSIONS.tran.payment"
            variant="secondary"
            size="sm"
            @click="handlePayment"
          >去收款</Button>
          <Button variant="outline" size="sm" @click="goBack">返回</Button>
          <Badge :class="getTranBadgeClass(tranDetail.stage)">{{ getTranStageText(tranDetail.stage) }}</Badge>
        </div>
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
              <TableCell>
                <div>{{ product.productName }}</div>
                <div class="text-xs text-muted-foreground">
                  {{ [product.productSku, product.productSpecification].filter(Boolean).join(' / ') }}
                </div>
                <div v-if="product.guidePrice" class="text-xs text-muted-foreground">
                  指导价 &yen;{{ product.guidePrice }}
                </div>
              </TableCell>
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
                  v-if="invoice.status === 'ISSUED' || invoice.status === 'PARTIAL_RED_REVERSED'"
                  v-has-permission="PERMISSIONS.tran.invoice"
                  variant="outline"
                  size="sm"
                  :disabled="invoiceLoading"
                  @click="openRedReverseDialog(invoice)"
                >红冲</Button>
                <Button
                  v-if="invoice.status === 'VOIDED' || invoice.status === 'PARTIAL_RED_REVERSED' || invoice.status === 'RED_REVERSED'"
                  v-has-permission="PERMISSIONS.tran.invoice"
                  variant="outline"
                  size="sm"
                  :disabled="invoiceLoading"
                  @click="startReissue(invoice)"
                >重开</Button>
                <Button
                  v-if="invoice.status === 'PENDING' || invoice.status === 'ISSUED'"
                  v-has-permission="PERMISSIONS.tran.invoice"
                  variant="destructive"
                  size="sm"
                  :disabled="invoiceLoading"
                  @click="openVoidInvoiceDialog(invoice)"
                >作废</Button>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2 text-sm">
              <div>发票编号：{{ invoice.invoiceNo }}</div>
              <div>金额：&yen;{{ invoice.amount }}</div>
              <div>类型：{{ getInvoiceTypeText(invoice.type) }}</div>
              <div v-if="invoice.originalInvoiceId">关联原票：{{ invoice.originalInvoiceId }}</div>
              <div>备注：{{ invoice.remark }}</div>
            </div>
          </div>
        </div>

        <div class="mb-4 text-sm text-muted-foreground">
          可开票金额：&yen;{{ availableInvoiceAmount.toFixed(2) }}
        </div>

        <form v-if="canCreateInvoice" class="mt-8 max-w-[600px] space-y-4" @submit.prevent="onSubmit">
          <div v-if="reissueSourceInvoice" class="rounded-md border border-dashed p-3 text-sm">
            <div>重开发票：{{ reissueSourceInvoice.invoiceNo || reissueSourceInvoice.id }}</div>
            <Button type="button" variant="outline" size="sm" class="mt-2" @click="cancelReissue">取消重开</Button>
          </div>
          <!-- Invoice Type -->
          <div class="space-y-2">
            <Label>发票类型</Label>
            <RadioGroup v-model="type" class="flex items-center gap-6">
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
            <Alert v-if="type === 'VAT_SPECIAL'" class="mt-2">
              <AlertTitle>提示：开具增值税专用发票需要填写完整的开户行、银行账号、注册地址和注册电话信息</AlertTitle>
            </Alert>
          </div>

          <!-- Invoice Title -->
          <div class="space-y-2">
            <Label>发票抬头</Label>
            <Input v-model="title" placeholder="请输入发票抬头（2-100个字符）" maxlength="100" />
            <p v-if="errors.title" class="text-sm text-destructive">{{ errors.title }}</p>
          </div>

          <!-- Tax Number -->
          <div class="space-y-2">
            <Label>纳税人识别号</Label>
            <Input
              v-model="taxNumber"
              placeholder="请输入纳税人识别号（15-20位数字和字母组合）"
              maxlength="20"
              class="uppercase"
              @update:model-value="(v) => taxNumber = (v || '').toUpperCase()"
            />
            <p v-if="errors.taxNumber" class="text-sm text-destructive">{{ errors.taxNumber }}</p>
          </div>

          <!-- Bank Name -->
          <div class="space-y-2">
            <Label>开户行</Label>
            <Input v-model="bankName" placeholder="请输入开户行（专用发票必填）" maxlength="50" />
            <p v-if="errors.bankName" class="text-sm text-destructive">{{ errors.bankName }}</p>
          </div>

          <!-- Bank Account -->
          <div class="space-y-2">
            <Label>银行账号</Label>
            <Input
              v-model="bankAccount"
              placeholder="请输入银行账号（10-30位数字，专用发票必填）"
              maxlength="30"
              @update:model-value="(v) => bankAccount = (v || '').replace(/\D/g, '')"
            />
            <p v-if="errors.bankAccount" class="text-sm text-destructive">{{ errors.bankAccount }}</p>
          </div>

          <!-- Address -->
          <div class="space-y-2">
            <Label>注册地址</Label>
            <Input v-model="address" placeholder="请输入注册地址（专用发票必填）" maxlength="200" />
            <p v-if="errors.address" class="text-sm text-destructive">{{ errors.address }}</p>
          </div>

          <!-- Phone -->
          <div class="space-y-2">
            <Label>注册电话</Label>
            <Input v-model="phone" placeholder="请输入注册电话（固话：0xx-xxxxxxxx，手机：1xxxxxxxxx，专用发票必填）" maxlength="20" />
            <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
          </div>

          <!-- Amount -->
          <div class="space-y-2">
            <Label>发票金额</Label>
            <NumberField v-model="amount" :min="0.01" :max="availableInvoiceAmount" :step="0.01" class="w-full">
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
            <Textarea v-model="remark" :rows="3" placeholder="请输入备注信息（最多500个字符）" maxlength="500" />
            <p v-if="errors.remark" class="text-sm text-destructive">{{ errors.remark }}</p>
          </div>

          <div v-if="reissueSourceInvoice" class="space-y-2">
            <Label>重开原因</Label>
            <Textarea v-model="reissueReason" :rows="3" placeholder="请输入重开原因" maxlength="500" />
          </div>

          <!-- Submit -->
          <div class="pt-2">
            <Button v-has-permission="PERMISSIONS.tran.invoice" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : (reissueSourceInvoice ? '重开发票' : '开具发票') }}
            </Button>
            <Button type="button" variant="outline" class="ml-2" @click="goBack">返回</Button>
          </div>
        </form>
      </CardContent>
    </Card>

    <Dialog v-model:open="voidDialogOpen">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>作废发票</DialogTitle>
        </DialogHeader>
        <div class="space-y-2">
          <Label>作废原因</Label>
          <Textarea v-model="voidReason" :rows="4" placeholder="请输入作废原因" maxlength="500" />
        </div>
        <DialogFooter>
          <Button variant="outline" @click="voidDialogOpen = false">取消</Button>
          <Button variant="destructive" :disabled="invoiceLoading" @click="confirmVoidInvoice">
            {{ invoiceLoading ? '处理中...' : '确认作废' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="redReverseDialogOpen">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>红冲发票</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>红冲金额</Label>
            <NumberField v-model="redReverseAmount" :min="0.01" :max="selectedRedInvoiceRemaining" :step="0.01" class="w-full">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput placeholder="请输入红冲金额" />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <div class="text-xs text-muted-foreground">可红冲金额：&yen;{{ selectedRedInvoiceRemaining.toFixed(2) }}</div>
          </div>
          <div class="space-y-2">
            <Label>红冲原因</Label>
            <Textarea v-model="redReverseReason" :rows="4" placeholder="请输入红冲原因" maxlength="500" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="redReverseDialogOpen = false">取消</Button>
          <Button variant="destructive" :disabled="invoiceLoading" @click="confirmRedReverseInvoice">
            {{ invoiceLoading ? '处理中...' : '确认红冲' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted, computed, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { useRoute, useRouter } from 'vue-router'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import {
  createInvoice,
  getTranDetail,
  getTranInvoiceList,
  getTranProducts,
  redReverseInvoice,
  reissueInvoice,
  updateInvoiceStatus,
} from '@/modules/tran/api/tran-api'
import type { InvoiceStatus, TranInvoice } from '@/modules/tran/model/tran.types'
import { TRAN_STAGE, getTranStageText, getTranStageType, normalizeTranStage } from '@/modules/tran/model/tran-stage'

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
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

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

const invoiceList = ref<TranInvoice[]>([])

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

const { handleSubmit, errors, isSubmitting, resetForm, defineField } = useForm({
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
const [type] = defineField('type')
const [title] = defineField('title')
const [taxNumber] = defineField('taxNumber')
const [bankName] = defineField('bankName')
const [bankAccount] = defineField('bankAccount')
const [address] = defineField('address')
const [phone] = defineField('phone')
const [amount] = defineField('amount')
const [remark] = defineField('remark')

// Internal tranId for API calls
let tranId: number | null = null

const invoiceLoading = ref(false)
const voidDialogOpen = ref(false)
const selectedVoidInvoice = ref<TranInvoice | null>(null)
const voidReason = ref('')
const redReverseDialogOpen = ref(false)
const selectedRedInvoice = ref<TranInvoice | null>(null)
const redReverseAmount = ref(0)
const redReverseReason = ref('')
const reissueSourceInvoice = ref<TranInvoice | null>(null)
const reissueReason = ref('')
const canRecordPayment = computed(() =>
  tranDetail.value.stage === TRAN_STAGE.APPROVED || tranDetail.value.stage === TRAN_STAGE.PAYMENT,
)

const availableInvoiceAmount = computed(() => {
  const usedAmount = invoiceList.value
    .filter(invoice => !['FAILED', 'VOIDED', 'NOT_REQUIRED'].includes(invoice.status))
    .reduce((sum, invoice) => sum + Number(invoice.amount || 0), 0)
  return Math.max(Number((Number(tranDetail.value.amount || 0) - usedAmount).toFixed(2)), 0)
})
const selectedRedInvoiceRemaining = computed(() => {
  if (!selectedRedInvoice.value) {
    return 0
  }
  return getInvoiceRedRemaining(selectedRedInvoice.value)
})
const canCreateInvoice = computed(() =>
  availableInvoiceAmount.value > 0
  && [
    TRAN_STAGE.APPROVED,
    TRAN_STAGE.PAYMENT,
    TRAN_STAGE.DELIVERY,
  ].includes(tranDetail.value.stage),
)

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
  'ISSUING': { type: 'warning', text: '开票中' },
  'ISSUED': { type: 'success', text: '已开具' },
  'FAILED': { type: 'danger', text: '开票失败' },
  'VOIDED': { type: 'danger', text: '已作废' },
  'PARTIAL_RED_REVERSED': { type: 'warning', text: '部分红冲' },
  'RED_REVERSED': { type: 'danger', text: '已红冲' },
  'NOT_REQUIRED': { type: 'info', text: '无需开票' },
}

const getInvoiceStatusType = (status: string) => invoiceStatusMap[status]?.type || ''
const getInvoiceStatusText = (status: string) => invoiceStatusMap[status]?.text || status
const getInvoiceStatusVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  const map: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    ISSUING: 'secondary',
    ISSUED: 'default',
    FAILED: 'destructive',
    VOIDED: 'destructive',
    PARTIAL_RED_REVERSED: 'secondary',
    RED_REVERSED: 'destructive',
    NOT_REQUIRED: 'outline',
  }
  return map[status] || 'outline'
}
const getTranBadgeClass = (stage: string) => {
  const type = getTranStageType(stage)
  switch (type) {
    case 'success': return 'bg-green-600 text-white'
    case 'warning': return 'bg-yellow-600 text-white'
    case 'danger': return 'bg-red-600 text-white'
    case 'info': return ''
    default: return ''
  }
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
      products: tranDetail.value.products.length > 0 ? tranDetail.value.products : data.products || [],
    }
    tranId = Number(id)
    resetInvoiceForm()
  } catch {
    messageTip('获取交易详情失败', 'error')
  }
}

function resetInvoiceForm(): void {
  resetForm({
    values: {
      type: 'VAT_NORMAL',
      title: '',
      taxNumber: '',
      bankName: '',
      bankAccount: '',
      address: '',
      phone: '',
      amount: availableInvoiceAmount.value,
      remark: '',
    },
  })
}

function getInvoiceRedRemaining(invoice: TranInvoice): number {
  const reversedAmount = invoiceList.value
    .filter(item => item.originalInvoiceId === invoice.id && item.status === 'RED_REVERSED')
    .filter(item => Number(item.amount || 0) < 0)
    .reduce((sum, item) => sum + Math.abs(Number(item.amount || 0)), 0)
  return Math.max(Number((Math.abs(Number(invoice.amount || 0)) - reversedAmount).toFixed(2)), 0)
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

async function loadInvoicePageData(): Promise<void> {
  await fetchTranDetail()
  await Promise.all([fetchProducts(), fetchInvoiceList()])
  resetInvoiceForm()
}

// Submit invoice form
const onSubmit = handleSubmit(async (formData) => {
  try {
    if (tranId === null) {
      messageTip('缺少交易ID参数', 'error')
      return
    }
    if (formData.amount > availableInvoiceAmount.value) {
      messageTip(`发票金额不能超过可开票金额 ${availableInvoiceAmount.value.toFixed(2)} 元`, 'warning')
      return
    }
    if (reissueSourceInvoice.value) {
      const reason = reissueReason.value.trim()
      if (!reason) {
        messageTip('请输入重开原因', 'warning')
        return
      }
      await reissueInvoice(reissueSourceInvoice.value.id, { ...formData, reason })
      messageTip('发票重开申请已提交', 'success')
      reissueSourceInvoice.value = null
      reissueReason.value = ''
    } else {
      await createInvoice({ tranId, ...formData })
      messageTip('发票创建成功', 'success')
    }
    try {
      await Promise.all([fetchInvoiceList(), fetchTranDetail()])
      resetInvoiceForm()
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
    await updateInvoiceStatus(invoice.id, { status: 'ISSUED' })
    messageTip('开票完成', 'success')
    await Promise.all([fetchInvoiceList(), fetchTranDetail()])
  } catch {
    messageTip('发票状态更新失败', 'error')
  } finally {
    invoiceLoading.value = false
  }
}

function goBack(): void {
  router.push('/dashboard/tran')
}

function handlePayment(): void {
  router.push(`/dashboard/tran/${route.params.id}?collect=1`)
}

// Void invoice
function openVoidInvoiceDialog(invoice: TranInvoice): void {
  selectedVoidInvoice.value = invoice
  voidReason.value = ''
  voidDialogOpen.value = true
}

function openRedReverseDialog(invoice: TranInvoice): void {
  selectedRedInvoice.value = invoice
  redReverseAmount.value = getInvoiceRedRemaining(invoice)
  redReverseReason.value = ''
  redReverseDialogOpen.value = true
}

const confirmRedReverseInvoice = async () => {
  if (!selectedRedInvoice.value) {
    return
  }
  const reason = redReverseReason.value.trim()
  if (!reason) {
    messageTip('请输入红冲原因', 'warning')
    return
  }
  if (redReverseAmount.value <= 0 || redReverseAmount.value > selectedRedInvoiceRemaining.value) {
    messageTip('红冲金额必须大于0且不能超过可红冲金额', 'warning')
    return
  }
  invoiceLoading.value = true
  try {
    await redReverseInvoice(selectedRedInvoice.value.id, {
      amount: redReverseAmount.value,
      reason,
    })
    messageTip('发票已红冲', 'success')
    redReverseDialogOpen.value = false
    await Promise.all([fetchInvoiceList(), fetchTranDetail()])
    resetInvoiceForm()
  } catch {
    messageTip('发票红冲失败', 'error')
  } finally {
    invoiceLoading.value = false
  }
}

function startReissue(invoice: TranInvoice): void {
  reissueSourceInvoice.value = invoice
  reissueReason.value = ''
  resetForm({
    values: {
      type: invoice.type || 'VAT_NORMAL',
      title: invoice.title || '',
      taxNumber: invoice.taxNumber || '',
      bankName: invoice.bankName || '',
      bankAccount: invoice.bankAccount || '',
      address: invoice.address || '',
      phone: invoice.phone || '',
      amount: Math.min(Math.abs(Number(invoice.amount || 0)), availableInvoiceAmount.value),
      remark: '',
    },
  })
}

function cancelReissue(): void {
  reissueSourceInvoice.value = null
  reissueReason.value = ''
  resetInvoiceForm()
}

const confirmVoidInvoice = async () => {
  if (!selectedVoidInvoice.value) {
    return
  }
  const reason = voidReason.value.trim()
  if (!reason) {
    messageTip('请输入作废原因', 'warning')
    return
  }
  invoiceLoading.value = true
  try {
    await updateInvoiceStatus(selectedVoidInvoice.value.id, { status: 'VOIDED', reason })
    messageTip('发票已作废', 'success')
    voidDialogOpen.value = false
    await Promise.all([fetchInvoiceList(), fetchTranDetail()])
    resetInvoiceForm()
  } catch {
    messageTip('发票作废失败', 'error')
  } finally {
    invoiceLoading.value = false
  }
}

// Watch route params
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await loadInvoicePageData()
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }
  await loadInvoicePageData()
})
</script>
