<template>
  <div class="p-5">
    <!-- Basic Info -->
    <Card class="mb-5">
      <CardHeader class="flex flex-row items-center justify-between space-y-0">
        <CardTitle>交易基本信息</CardTitle>
        <Badge :class="getBadgeClass(tranDetail.stage)">
          {{ getStatusText(tranDetail.stage) }}
        </Badge>
      </CardHeader>
      <CardContent>
        <div class="border rounded-md">
          <div class="grid grid-cols-[120px_1fr_120px_1fr]">
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易编号</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ tranDetail.tranNo }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">客户名称</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.customerName }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易金额</div>
            <div class="px-4 py-2 text-sm border-b border-r">
              <span v-if="tranDetail.stage === TRAN_STAGE.QUOTATION">?</span>
              <span v-else>&yen;{{ tranDetail.amount }}</span>
            </div>
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
      </CardContent>
    </Card>

    <!-- Product Info -->
    <Card class="mb-5">
      <CardHeader>
        <CardTitle>产品信息</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
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
              <TableCell>&yen;{{ (product.price * product.quantity).toFixed(2) }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <!-- Promotion Selection - only shown in QUOTATION stage -->
        <div v-if="tranDetail.stage === TRAN_STAGE.QUOTATION" class="mt-5 p-5 bg-muted/30 rounded-md border">
          <div class="text-base font-bold mb-4">促销选择</div>
          <div class="space-y-4">
            <div class="space-y-2">
              <Label>促销活动（可选）</Label>
              <Select v-model="promotionForm.selectedPromotion" @update:model-value="onPromotionChange">
                <SelectTrigger class="w-full">
                  <SelectValue placeholder="请选择促销活动（可选）" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="promotion in availablePromotions" :key="promotion.id" :value="promotion.id">
                    {{ promotion.name }} ({{ getPromotionText(promotion) }})
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div v-if="previewLoading" class="text-sm text-muted-foreground">计算中...</div>
            <div v-else-if="previewError" class="text-sm text-red-500">{{ previewError }}</div>
            <div v-else-if="previewData" class="p-3 bg-background border rounded-md space-y-1 text-sm">
              <div>原价：¥{{ previewData.originalAmount.toFixed(2) }}</div>
              <div v-if="previewData.discountAmount > 0" class="text-green-600">优惠：-¥{{ previewData.discountAmount.toFixed(2) }}</div>
              <div class="text-lg font-bold text-yellow-600">结算价：¥{{ previewData.finalAmount.toFixed(2) }}</div>
            </div>
            <p class="text-xs text-muted-foreground">结算金额由服务端按当前商品与促销规则计算。</p>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Invoice Info -->
    <Card class="mb-5" v-if="invoiceList.length > 0">
      <CardHeader>
        <CardTitle>发票信息</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[180px]">发票号码</TableHead>
              <TableHead class="w-[140px]">发票类型</TableHead>
              <TableHead class="min-w-[200px]">发票抬头</TableHead>
              <TableHead class="w-[120px]">发票金额</TableHead>
              <TableHead class="w-[100px]">状态</TableHead>
              <TableHead class="w-[160px]">创建时间</TableHead>
              <TableHead class="w-[160px]">开票时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(invoice, index) in invoiceList" :key="index">
              <TableCell>{{ invoice.invoiceNo }}</TableCell>
              <TableCell>{{ getInvoiceTypeText(invoice.type) }}</TableCell>
              <TableCell>{{ invoice.title }}</TableCell>
              <TableCell>&yen;{{ invoice.amount }}</TableCell>
              <TableCell>
                <Badge :class="getInvoiceBadgeClass(invoice.status)">
                  {{ getInvoiceStatusText(invoice.status) }}
                </Badge>
              </TableCell>
              <TableCell>{{ invoice.createTime }}</TableCell>
              <TableCell>{{ invoice.issueTime || '-' }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- Payment Records -->
    <Card
      class="mb-5"
      v-if="tranDetail.stage === TRAN_STAGE.PAYMENT
        || tranDetail.stage === TRAN_STAGE.COMPLETED
        || tranDetail.stage === TRAN_STAGE.CANCELLED"
    >
      <CardHeader>
        <CardTitle>收款记录</CardTitle>
        <span v-if="tranDetail.stage === TRAN_STAGE.CANCELLED" class="text-sm text-muted-foreground">
          交易已取消，已退款: &yen;{{ totalRefunded.toFixed(2) }}
        </span>
        <span v-else class="text-sm text-muted-foreground">
          已收: &yen;{{ totalPaid.toFixed(2) }} / 应收: &yen;{{ tranDetail.amount }}
          <span v-if="balance > 0" class="text-red-500 ml-2">待收: &yen;{{ balance.toFixed(2) }}</span>
          <span v-else class="text-green-600 ml-2">已收齐</span>
        </span>
      </CardHeader>
      <CardContent>
        <Table v-if="paymentList.length > 0">
          <TableHeader>
            <TableRow>
              <TableHead>流水号</TableHead>
              <TableHead>金额</TableHead>
              <TableHead>方式</TableHead>
              <TableHead>类型</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(pay, idx) in paymentList" :key="idx">
              <TableCell>{{ pay.paymentNo }}</TableCell>
              <TableCell :class="pay.amount < 0 ? 'text-red-500' : ''">&yen;{{ pay.amount }}</TableCell>
              <TableCell>{{ getPaymentMethodText(pay.paymentMethod) }}</TableCell>
              <TableCell>{{ getPaymentTypeText(pay.paymentType) }}</TableCell>
              <TableCell>
                <Badge :class="getPaymentStatusClass(pay.paymentStatus)">
                  {{ getPaymentStatusText(pay.paymentStatus) }}
                </Badge>
              </TableCell>
              <TableCell>{{ pay.paymentTime || pay.createTime }}</TableCell>
              <TableCell>
                <Button
                  v-if="canRefundPayment(pay)"
                  v-has-permission="PERMISSIONS.tran.refund"
                  variant="destructive"
                  size="sm"
                  @click="handleRefund(pay.id)"
                >退款</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <div v-else class="text-sm text-muted-foreground py-4 text-center">暂无收款记录</div>
      </CardContent>
    </Card>

    <!-- Collection Dialog -->
    <div v-if="showCollectionDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-background p-6 rounded-lg w-[400px] space-y-4">
        <h3 class="text-lg font-bold">记录收款</h3>
        <div class="space-y-3">
          <div>
            <Label>收款金额</Label>
            <Input
              v-model.number="collectionForm.amount"
              type="number"
              :min="0.01"
              :max="balance"
              step="0.01"
              placeholder="请输入收款金额"
            />
          </div>
          <div>
            <Label>支付方式</Label>
            <Select v-model="collectionForm.paymentMethod">
              <SelectTrigger><SelectValue placeholder="选择支付方式" /></SelectTrigger>
              <SelectContent>
                <SelectItem v-for="m in PAYMENT_METHODS" :key="m.value" :value="m.value">{{ m.label }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>收款类型</Label>
            <Select v-model="collectionForm.paymentType">
              <SelectTrigger><SelectValue placeholder="选择收款类型" /></SelectTrigger>
              <SelectContent>
                <SelectItem v-for="t in PAYMENT_TYPES" :key="t.value" :value="t.value">{{ t.label }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>备注</Label>
            <Input v-model="collectionForm.remark" placeholder="备注（可选）" />
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showCollectionDialog = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.payment" @click="submitCollection">确认收款</Button>
        </div>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="flex justify-center gap-5 mt-5">
      <Button variant="outline" @click="goBack">返回</Button>
      <Button
        v-has-permission="PERMISSIONS.tran.settle"
        variant="secondary"
        @click="handleSettle"
        :disabled="previewLoading || !previewData"
        v-if="tranDetail.stage === 'QUOTATION'"
      >结算</Button>
      <Button
        v-has-permission="PERMISSIONS.tran.approve"
        variant="secondary"
        @click="handleApprove"
        v-if="tranDetail.stage === TRAN_STAGE.PENDING"
      >审批</Button>
      <Button
        v-has-permission="PERMISSIONS.tran.invoice"
        variant="outline"
        @click="handleInvoice"
        v-if="tranDetail.stage === TRAN_STAGE.APPROVED"
      >开票</Button>
      <Button
        v-has-permission="PERMISSIONS.tran.payment"
        variant="secondary"
        @click="showCollectionDialog = true"
        v-if="tranDetail.stage === TRAN_STAGE.PAYMENT"
      >收款</Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import { getTranDetail, getTranInvoiceList, getTranProducts, settleTran, fetchSettlementPreview, getTranPayments, recordPayment, refundPayment } from '@/modules/tran/api/tran-api'
import type {
  SettlementPreviewResponse,
  TPayment,
  TranInvoice,
  TranProduct,
} from '@/modules/tran/model/tran.types'
import { TRAN_STAGE, getTranStageText, getTranStageType, normalizeTranStage } from '@/modules/tran/model/tran-stage'
import { getPromotionList } from '@/modules/product/api/product-api'
import type { ProductPromotion } from '@/modules/product/model/product.types'
import type { EntityId } from '@/shared/types/id'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'

const route = useRoute()
const router = useRouter()

interface TranDetailView {
  tranNo: string
  customerName: string
  amount: number
  stage: string
  createTime: string
  updateTime: string
  expectedDeliveryDate: string
  description: string
  products: TranProduct[]
}

const tranDetail = ref<TranDetailView>({
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

// Promotion data
const promotionList = ref<ProductPromotion[]>([])
const promotionForm = ref({
  selectedPromotion: null as EntityId | null,
})

// Computed properties
const availablePromotions = computed(() => {
  return promotionList.value.filter(promotion => promotion.status === '进行中')
})

const selectedPromotionInfo = computed(() => {
  if (!promotionForm.value.selectedPromotion) return null
  return promotionList.value.find(p => p.id === promotionForm.value.selectedPromotion)
})

const previewLoading = ref(false)
const previewData = ref<SettlementPreviewResponse | null>(null)
const previewError = ref<string | null>(null)

async function onPromotionChange(): Promise<void> {
  previewError.value = null
  previewData.value = null
  previewLoading.value = true
  try {
    previewData.value = await fetchSettlementPreview(route.params.id as string, {
      promotionId: promotionForm.value.selectedPromotion ?? undefined,
    })
  } catch {
    previewError.value = '促销预览失败'
    previewData.value = null
  } finally {
    previewLoading.value = false
  }
}

const invoiceStatusMap = {
  'PENDING': { type: 'warning', text: '待开具' },
  'ISSUED': { type: 'success', text: '已开具' },
  'VOID': { type: 'danger', text: '已作废' }
}

const getStatusType = getTranStageType
const getStatusText = getTranStageText

const getInvoiceStatusType = (status) => invoiceStatusMap[status]?.type || ''
const getInvoiceStatusText = (status) => invoiceStatusMap[status]?.text || status

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

const getInvoiceBadgeClass = (status) => {
  const type = getInvoiceStatusType(status)
  switch (type) {
    case 'success': return 'bg-green-600 text-white'
    case 'warning': return 'bg-yellow-600 text-white'
    case 'danger': return 'bg-red-600 text-white'
    default: return ''
  }
}

// Invoice type mapping
const getInvoiceTypeText = (type) => {
  const typeMap = {
    'VAT_NORMAL': '增值税普通发票',
    'VAT_SPECIAL': '增值税专用发票'
  }
  return typeMap[type] || type
}

// Promotion methods
const getPromotionText = (promotion) => {
  const promotionType = promotion.type?.toLowerCase() || ''

  if (promotionType === 'percentage' || promotionType === '折扣') {
    let discountRate = promotion.discount
    if (discountRate > 1) {
      return `${discountRate}折`
    } else {
      return `${(discountRate * 100)}折`
    }
  } else if (promotionType === 'amount' || promotionType === '满减') {
    return `满减¥${promotion.discount}`
  } else if (promotionType === '直降') {
    return `直降¥${promotion.discount}`
  } else {
    return promotion.type || '优惠'
  }
}

// Fetch promotion list
const fetchPromotionList = async () => {
  try {
    const res = await getPromotionList({
      page: 1,
      size: 1000
    })
    promotionList.value = res.list || []
  } catch {
    messageTip('获取促销列表失败', 'error')
  }
}

// Fetch transaction detail
const fetchTranDetail = async () => {
  try {
    const res = await getTranDetail(route.params.id)
    const data = res
    tranDetail.value = {
      tranNo: data.tranNo || '',
      customerName: data.customerName || '',
      amount: data.money || 0,
      stage: normalizeTranStage(data.stage),
      createTime: data.createTime || '',
      updateTime: data.editTime || '',
      expectedDeliveryDate: data.expectedDate || '',
      description: data.description || '',
      products: data.products || []
    }
  } catch {
    messageTip('获取交易详情失败', 'error')
  }
}

// Fetch product details
const fetchProducts = async () => {
  try {
    const res = await getTranProducts(route.params.id)
    tranDetail.value.products = res
  } catch {
    messageTip('获取产品详情失败', 'error')
  }
}

// Fetch invoice info
const fetchInvoiceInfo = async () => {
  try {
    const res = await getTranInvoiceList(route.params.id)
    invoiceList.value = res || []
  } catch {
    messageTip('获取发票信息失败', 'error')
  }
}

// Payment
const paymentList = ref<TPayment[]>([])
const showCollectionDialog = ref(false)
const collectionForm = ref({
  amount: 0,
  paymentMethod: '',
  paymentType: 'FULL',
  remark: ''
})

const PAYMENT_METHODS = [
  { value: 'CASH', label: '现金' },
  { value: 'BANK_TRANSFER', label: '银行转账' },
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'CHECK', label: '支票' },
  { value: 'OTHER', label: '其他' }
]

const PAYMENT_TYPES = [
  { value: 'DEPOSIT', label: '定金' },
  { value: 'INSTALLMENT', label: '分期款' },
  { value: 'FULL', label: '全款' },
  { value: 'BALANCE', label: '尾款' }
]

const totalPaid = computed(() => {
  return paymentList.value
    .filter(p => p.paymentStatus === 'COMPLETED' && p.paymentType !== 'REFUND')
    .reduce((sum, p) => sum + (p.amount || 0), 0)
})

const totalRefunded = computed(() => {
  return Math.abs(paymentList.value
    .filter(p => p.paymentStatus === 'COMPLETED' && p.paymentType === 'REFUND')
    .reduce((sum, p) => sum + (p.amount || 0), 0))
})

const balance = computed(() => {
  return Math.max((tranDetail.value.amount || 0) - totalPaid.value, 0)
})

const completedReceipts = computed(() => paymentList.value.filter(
  p => p.paymentStatus === 'COMPLETED' && p.paymentType !== 'REFUND'
))

const canRefundPayment = (payment) => {
  return tranDetail.value.stage === TRAN_STAGE.COMPLETED
    && completedReceipts.value.length === 1
    && completedReceipts.value[0].id === payment.id
    && Number(payment.amount) === Number(tranDetail.value.amount)
}

const getPaymentMethodText = (m) => PAYMENT_METHODS.find(p => p.value === m)?.label || m
const getPaymentTypeText = (t) => PAYMENT_TYPES.find(p => p.value === t)?.label || t

const getPaymentStatusText = (s) => {
  const map = { PENDING: '待确认', COMPLETED: '已到账', FAILED: '失败', REFUNDED: '已退款' }
  return map[s] || s
}

const getPaymentStatusClass = (s) => {
  const map = { COMPLETED: 'bg-green-600 text-white', REFUNDED: 'bg-red-600 text-white', PENDING: 'bg-yellow-600 text-white', FAILED: 'bg-red-600 text-white' }
  return map[s] || ''
}

const fetchPaymentList = async () => {
  try {
    const res = await getTranPayments(route.params.id)
    paymentList.value = res || []
  } catch {
    messageTip('获取收款记录失败', 'error')
  }
}

const submitCollection = async () => {
  if (!collectionForm.value.amount || collectionForm.value.amount <= 0) {
    messageTip('请输入有效的收款金额', 'error')
    return
  }
  if (collectionForm.value.amount > balance.value) {
    messageTip('收款金额不能超过剩余应收金额', 'error')
    return
  }
  if (!collectionForm.value.paymentMethod) {
    messageTip('请选择支付方式', 'error')
    return
  }
  try {
    await recordPayment({
      tranId: Number(route.params.id),
      amount: collectionForm.value.amount,
      paymentMethod: collectionForm.value.paymentMethod,
      paymentType: collectionForm.value.paymentType,
      remark: collectionForm.value.remark
    })
    messageTip('收款记录成功', 'success')
    showCollectionDialog.value = false
    collectionForm.value = { amount: 0, paymentMethod: '', paymentType: 'FULL', remark: '' }
    await fetchPaymentList()
    await fetchTranDetail()
  } catch (error) {
    messageTip('收款失败: ' + (error.message || ''), 'error')
  }
}

const handleRefund = async (paymentId) => {
  const confirmResult = await messageConfirm('确认退款？退款后将取消该交易并恢复库存').catch(() => false)
  if (!confirmResult) return
  try {
    await refundPayment(paymentId)
    messageTip('退款成功', 'success')
    await fetchPaymentList()
    await fetchTranDetail()
  } catch (error) {
    messageTip('退款失败: ' + (error.message || ''), 'error')
  }
}

// Go back to list
const goBack = () => {
  router.push('/dashboard/tran')
}

// Edit transaction
const handleEdit = () => {
  router.push(`/dashboard/tran/edit/${route.params.id}`)
}

// Settle transaction
const handleSettle = async () => {
  if (!tranDetail.value.products || tranDetail.value.products.length === 0) {
    messageTip('该交易没有产品信息，无法结算', 'error')
    return
  }
  const preview = previewData.value
  if (!preview) {
    messageTip('请先获取有效的服务端结算预览', 'warning')
    return
  }
  const desc = `确认结算该交易吗？\n结算金额：¥${preview.finalAmount.toFixed(2)}`
  try {
    const res = await messageConfirm(desc)
    if (!res) return
  } catch {
    return
  }
  try {
    await settleTran(route.params.id as string, {
      promotionId: preview.promotionId ?? undefined,
      expectedVersion: preview.transactionVersion,
      pricingFingerprint: preview.pricingFingerprint,
    })
    messageTip('结算成功，交易状态已更新为待审批', 'success')
    previewData.value = null
    previewError.value = null
    await fetchTranDetail()
    goBack()
  } catch {
    messageTip('结算失败，请重新获取促销预览', 'error')
    previewData.value = null
  }
}

// Approve transaction
const handleApprove = () => {
  router.push(`/dashboard/tran/approve/${route.params.id}`)
}

// Invoice transaction
const handleInvoice = () => {
  router.push(`/dashboard/tran/invoice/${route.params.id}`)
}

// Watch route params change
watch(() => route.params.id, async (newId) => {
    if (newId) {
    await Promise.all([
      fetchTranDetail(),
      fetchProducts(),
      fetchInvoiceInfo(),
      fetchPaymentList(),
      fetchPromotionList(),
    ])
    await onPromotionChange()
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }

  await Promise.all([
    fetchTranDetail(),
    fetchProducts(),
    fetchInvoiceInfo(),
    fetchPaymentList(),
    fetchPromotionList(),
  ])
  await onPromotionChange()
})
</script>
