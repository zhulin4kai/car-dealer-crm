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
              <TableCell>&yen;{{ (product.price * product.quantity).toFixed(2) }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
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
      v-if="canShowPaymentRecords"
    >
      <CardHeader>
        <CardTitle>收款记录</CardTitle>
        <span v-if="tranDetail.stage === TRAN_STAGE.CANCELLED" class="text-sm text-muted-foreground">
          交易已取消，已退款: &yen;{{ totalRefunded.toFixed(2) }}
        </span>
        <span v-else class="text-sm text-muted-foreground">
          已确认: &yen;{{ totalPaid.toFixed(2) }} / 应收: &yen;{{ tranDetail.amount }}
          <span v-if="pendingAmount > 0" class="text-yellow-600 ml-2">待确认: &yen;{{ pendingAmount.toFixed(2) }}</span>
          <span v-if="totalRefunded > 0" class="text-red-500 ml-2">已退: &yen;{{ totalRefunded.toFixed(2) }}</span>
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
                  v-if="pay.paymentStatus === 'PENDING'"
                  v-has-permission="PERMISSIONS.tran.paymentConfirm"
                  variant="secondary"
                  size="sm"
                  class="mr-2"
                  @click="handleConfirmPayment(pay)"
                >确认到账</Button>
                <Button
                  v-if="pay.paymentStatus === 'PENDING'"
                  v-has-permission="PERMISSIONS.tran.paymentConfirm"
                  variant="outline"
                  size="sm"
                  class="mr-2"
                  @click="openRejectPaymentDialog(pay)"
                >退回</Button>
                <Button
                  v-if="canRequestRefund(pay)"
                  v-has-permission="PERMISSIONS.tran.refund"
                  variant="outline"
                  size="sm"
                  @click="openRefundRequestDialog(pay)"
                >申请退款</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <div v-else class="text-sm text-muted-foreground py-4 text-center">暂无收款记录</div>
      </CardContent>
    </Card>

    <!-- Refund Requests -->
    <Card
      class="mb-5"
      v-if="refundRequestList.length > 0"
    >
      <CardHeader>
        <CardTitle>退款申请</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>类型</TableHead>
              <TableHead>金额</TableHead>
              <TableHead>原因</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>申请时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="request in refundRequestList" :key="request.id">
              <TableCell>{{ getRefundTypeText(request.refundType) }}</TableCell>
              <TableCell>&yen;{{ request.amount }}</TableCell>
              <TableCell>{{ request.reason }}</TableCell>
              <TableCell>
                <Badge :class="getRefundStatusClass(request.status)">
                  {{ getRefundStatusText(request.status) }}
                </Badge>
              </TableCell>
              <TableCell>{{ request.requestedTime || '-' }}</TableCell>
              <TableCell>
                <Button
                  v-if="request.status === 'PENDING_APPROVAL'"
                  v-has-permission="PERMISSIONS.tran.refundApprove"
                  variant="secondary"
                  size="sm"
                  class="mr-2"
                  @click="openRefundApprovalDialog(request, true)"
                >通过</Button>
                <Button
                  v-if="request.status === 'PENDING_APPROVAL'"
                  v-has-permission="PERMISSIONS.tran.refundApprove"
                  variant="outline"
                  size="sm"
                  class="mr-2"
                  @click="openRefundApprovalDialog(request, false)"
                >驳回</Button>
                <Button
                  v-if="request.status === 'PENDING_EXECUTION'"
                  v-has-permission="PERMISSIONS.tran.refundExecute"
                  variant="outline"
                  size="sm"
                  @click="openRefundExecuteDialog(request)"
                >执行退款</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- Collection Dialog -->
    <Dialog v-model:open="showCollectionDialog">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>登记收款</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <div class="rounded-md border p-3 text-sm">
            <div class="flex justify-between py-1">
              <span class="text-muted-foreground">应收金额</span>
              <span>&yen;{{ tranDetail.amount.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between py-1">
              <span class="text-muted-foreground">已确认金额</span>
              <span>&yen;{{ totalPaid.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between py-1 font-medium">
              <span>本次登记金额</span>
              <span>&yen;{{ balance.toFixed(2) }}</span>
            </div>
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
            <Label>{{ requiresCollectionTransactionRef ? '外部流水号' : '外部流水号（可选）' }}</Label>
            <Input
              v-model="collectionForm.transactionRef"
              :placeholder="requiresCollectionTransactionRef ? '请输入支付参考号' : '银行或第三方支付参考号'"
              :required="requiresCollectionTransactionRef"
            />
          </div>
          <div>
            <Label>备注</Label>
            <Input v-model="collectionForm.remark" placeholder="备注（可选）" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showCollectionDialog = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.payment" @click="submitCollection">登记收款</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="paymentRejectDialogOpen">
      <DialogContent class="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>退回收款</DialogTitle>
        </DialogHeader>
        <div class="space-y-3">
          <Label>退回原因</Label>
          <Textarea v-model="paymentRejectComment" :rows="4" placeholder="请输入退回原因" />
        </div>
        <DialogFooter>
          <Button variant="outline" @click="paymentRejectDialogOpen = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.paymentConfirm" variant="secondary" @click="submitRejectPayment">确认退回</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="refundRequestDialogOpen">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>退款申请</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <div class="rounded-md border p-3 text-sm">
            <div class="flex justify-between py-1">
              <span class="text-muted-foreground">原收款金额</span>
              <span>&yen;{{ selectedPayment?.amount?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="flex justify-between py-1">
              <span class="text-muted-foreground">可申请退款</span>
              <span>&yen;{{ selectedPaymentRefundableAmount.toFixed(2) }}</span>
            </div>
          </div>
          <div>
            <Label>退款类型</Label>
            <Select v-model="refundForm.refundType">
              <SelectTrigger><SelectValue placeholder="选择退款类型" /></SelectTrigger>
              <SelectContent>
                <SelectItem v-for="t in REFUND_TYPES" :key="t.value" :value="t.value">{{ t.label }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>退款金额</Label>
            <Input
              v-model.number="refundForm.amount"
              type="number"
              min="0.01"
              :max="selectedPaymentRefundableAmount"
              step="0.01"
            />
          </div>
          <div>
            <Label>退款原因</Label>
            <Textarea v-model="refundForm.reason" :rows="4" placeholder="请输入退款原因" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="refundRequestDialogOpen = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.refund" @click="submitRefundRequest">提交申请</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="refundApprovalDialogOpen">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>{{ refundApprovalApproved ? '审批通过退款' : '驳回退款申请' }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-3">
          <Label>审批意见</Label>
          <Textarea v-model="refundApprovalComment" :rows="4" :placeholder="refundApprovalApproved ? '审批意见（可选）' : '请输入驳回原因'" />
        </div>
        <DialogFooter>
          <Button variant="outline" @click="refundApprovalDialogOpen = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.refundApprove" @click="submitRefundApproval">
            {{ refundApprovalApproved ? '确认通过' : '确认驳回' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="refundExecuteDialogOpen">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>执行退款</DialogTitle>
        </DialogHeader>
        <div class="space-y-3">
          <div>
            <Label>执行结果</Label>
            <Select v-model="refundExecuteForm.result">
              <SelectTrigger><SelectValue placeholder="选择执行结果" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="SUCCESS">退款成功</SelectItem>
                <SelectItem value="FAILED">退款失败</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>退款参考号</Label>
            <Input v-model="refundExecuteForm.transactionRef" placeholder="银行或第三方退款参考号" />
          </div>
          <div v-if="refundExecuteForm.result === 'FAILED'">
            <Label>失败原因</Label>
            <Textarea v-model="refundExecuteForm.failureReason" :rows="3" placeholder="请输入失败原因" />
          </div>
          <div>
            <Label>执行备注</Label>
            <Textarea v-model="refundExecuteForm.remark" :rows="3" placeholder="备注（可选）" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="refundExecuteDialogOpen = false">取消</Button>
          <Button v-has-permission="PERMISSIONS.tran.refundExecute" @click="submitRefundExecute">执行退款</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Action Buttons -->
    <div class="flex justify-center gap-5 mt-5">
      <Button variant="outline" @click="goBack">返回</Button>
      <Button
        v-has-permission="PERMISSIONS.tran.settle"
        variant="secondary"
        @click="handleSettle"
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
        v-if="canRecordPayment"
      >登记收款</Button>
    </div>

    <SettlementDialog
      v-model:open="settlementDialogOpen"
      :tran-id="route.params.id as string"
      :tran-no="tranDetail.tranNo"
      :customer-name="tranDetail.customerName"
      @settled="handleSettlementDone"
    />
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import {
  approveRefundRequest,
  confirmPayment,
  createRefundRequest,
  executeRefundRequest,
  fetchTranRefundRequests,
  getTranDetail,
  getTranInvoiceList,
  getTranPayments,
  getTranProducts,
  recordPayment,
} from '@/modules/tran/api/tran-api'
import type {
  RefundType,
  TRefundRequest,
  TPayment,
  TranInvoice,
  TranProduct,
} from '@/modules/tran/model/tran.types'
import { TRAN_STAGE, getTranStageText, getTranStageType, normalizeTranStage } from '@/modules/tran/model/tran-stage'
import SettlementDialog from '@/modules/tran/components/SettlementDialog.vue'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

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
const settlementDialogOpen = ref(false)

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
      products: tranDetail.value.products.length > 0 ? tranDetail.value.products : data.products || []
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

const loadTranPageData = async () => {
  await fetchTranDetail()
  await Promise.all([
    fetchProducts(),
    fetchInvoiceInfo(),
    fetchPaymentList(),
    fetchRefundRequestList(),
  ])
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
const refundRequestList = ref<TRefundRequest[]>([])
const showCollectionDialog = ref(false)
const collectionForm = ref({
  paymentMethod: '',
  transactionRef: '',
  remark: ''
})
const paymentRejectDialogOpen = ref(false)
const selectedPayment = ref<TPayment | null>(null)
const paymentRejectComment = ref('')
const refundRequestDialogOpen = ref(false)
const refundForm = ref<{ refundType: RefundType; amount: number; reason: string }>({
  refundType: 'ORDER_CANCEL',
  amount: 0,
  reason: '',
})
const refundApprovalDialogOpen = ref(false)
const selectedRefundRequest = ref<TRefundRequest | null>(null)
const refundApprovalApproved = ref(true)
const refundApprovalComment = ref('')
const refundExecuteDialogOpen = ref(false)
const refundExecuteForm = ref({
  result: 'SUCCESS',
  transactionRef: '',
  remark: '',
  failureReason: '',
})

const PAYMENT_METHODS = [
  { value: 'CASH', label: '现金' },
  { value: 'BANK_TRANSFER', label: '银行转账' },
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'CHECK', label: '支票' },
  { value: 'OTHER', label: '其他' }
]

const EXTERNAL_REF_PAYMENT_METHODS = new Set(['BANK_TRANSFER', 'WECHAT', 'ALIPAY', 'CHECK'])
const requiresCollectionTransactionRef = computed(() =>
  EXTERNAL_REF_PAYMENT_METHODS.has(collectionForm.value.paymentMethod),
)
const canRecordPayment = computed(() =>
  tranDetail.value.stage === TRAN_STAGE.APPROVED || tranDetail.value.stage === TRAN_STAGE.PAYMENT,
)
const canShowPaymentRecords = computed(() =>
  canRecordPayment.value
  || tranDetail.value.stage === TRAN_STAGE.DELIVERY
  || tranDetail.value.stage === TRAN_STAGE.COMPLETED
  || tranDetail.value.stage === TRAN_STAGE.CLOSED
  || tranDetail.value.stage === TRAN_STAGE.CANCELLED,
)

const PAYMENT_TYPES = [
  { value: 'DEPOSIT', label: '定金' },
  { value: 'INSTALLMENT', label: '分期款' },
  { value: 'FULL', label: '全款' },
  { value: 'BALANCE', label: '尾款' },
  { value: 'REFUND', label: '退款' }
]

const REFUND_TYPES: Array<{ value: RefundType; label: string }> = [
  { value: 'ORDER_CANCEL', label: '订单取消退款' },
  { value: 'OVERPAY', label: '多收退款' },
  { value: 'PRICE_ADJUSTMENT', label: '价格调整退款' },
  { value: 'CUSTOMER_BREACH', label: '客户违约部分退款' },
  { value: 'INTERNAL_CORRECTION', label: '内部纠错退款' }
]

const totalPaid = computed(() => {
  return paymentList.value
    .filter(p => p.paymentStatus === 'COMPLETED' && p.paymentType !== 'REFUND')
    .reduce((sum, p) => sum + (p.amount || 0), 0)
    + paymentList.value
      .filter(p => p.paymentStatus === 'COMPLETED' && p.paymentType === 'REFUND')
      .reduce((sum, p) => sum + (p.amount || 0), 0)
})

const pendingAmount = computed(() => {
  return paymentList.value
    .filter(p => p.paymentStatus === 'PENDING' && p.paymentType !== 'REFUND')
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

const REFUND_AMOUNT_HOLDING_STATUSES = new Set([
  'PENDING_APPROVAL',
  'PENDING_EXECUTION',
  'EXECUTING',
  'COMPLETED',
])

const getRefundableAmountForPayment = (payment?: TPayment | null) => {
  if (!payment?.id) {
    return 0
  }
  const originalAmount = Math.max(Number(payment.amount || 0), 0)
  const heldAmount = refundRequestList.value
    .filter(request =>
      Number(request.originalPaymentId) === Number(payment.id)
      && REFUND_AMOUNT_HOLDING_STATUSES.has(request.status),
    )
    .reduce((sum, request) => sum + Number(request.amount || 0), 0)

  return Math.max(Number((originalAmount - heldAmount).toFixed(2)), 0)
}

const selectedPaymentRefundableAmount = computed(() => getRefundableAmountForPayment(selectedPayment.value))

const canRequestRefund = (payment: TPayment) => {
  return (
    tranDetail.value.stage === TRAN_STAGE.PAYMENT
    || tranDetail.value.stage === TRAN_STAGE.DELIVERY
    || tranDetail.value.stage === TRAN_STAGE.CANCELLED
  )
    && payment.paymentStatus === 'COMPLETED'
    && payment.paymentType !== 'REFUND'
    && getRefundableAmountForPayment(payment) > 0
}

const getPaymentMethodText = (m) => PAYMENT_METHODS.find(p => p.value === m)?.label || m
const getPaymentTypeText = (t) => PAYMENT_TYPES.find(p => p.value === t)?.label || t

const getPaymentStatusText = (s) => {
  const map = { PENDING: '待确认', COMPLETED: '已到账', FAILED: '已退回', REVERSED: '已冲正', VOIDED: '已作废' }
  return map[s] || s
}

const getPaymentStatusClass = (s) => {
  const map = { COMPLETED: 'bg-green-600 text-white', PENDING: 'bg-yellow-600 text-white', FAILED: 'bg-red-600 text-white', REVERSED: 'bg-orange-600 text-white', VOIDED: 'bg-slate-600 text-white' }
  return map[s] || ''
}

const getRefundTypeText = (type) => REFUND_TYPES.find(item => item.value === type)?.label || type

const getRefundStatusText = (status) => {
  const map = {
    PENDING_APPROVAL: '待审批',
    PENDING_EXECUTION: '待执行',
    EXECUTING: '执行中',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    FAILED: '执行失败',
    CANCELLED: '已撤销'
  }
  return map[status] || status
}

const getRefundStatusClass = (status) => {
  const map = {
    PENDING_APPROVAL: 'bg-yellow-600 text-white',
    PENDING_EXECUTION: 'bg-blue-600 text-white',
    EXECUTING: 'bg-purple-600 text-white',
    COMPLETED: 'bg-green-600 text-white',
    REJECTED: 'bg-red-600 text-white',
    FAILED: 'bg-red-600 text-white',
    CANCELLED: 'bg-slate-600 text-white'
  }
  return map[status] || ''
}

const fetchPaymentList = async () => {
  try {
    const res = await getTranPayments(route.params.id)
    paymentList.value = res || []
  } catch {
    messageTip('获取收款记录失败', 'error')
  }
}

const fetchRefundRequestList = async () => {
  try {
    const res = await fetchTranRefundRequests(route.params.id)
    refundRequestList.value = res || []
  } catch {
    messageTip('获取退款申请失败', 'error')
  }
}

const submitCollection = async () => {
  if (balance.value <= 0) {
    messageTip('交易已收齐，无需登记收款', 'warning')
    return
  }
  if (!collectionForm.value.paymentMethod) {
    messageTip('请选择支付方式', 'error')
    return
  }
  if (requiresCollectionTransactionRef.value && !collectionForm.value.transactionRef.trim()) {
    messageTip('请输入外部流水号', 'error')
    return
  }
  try {
    await recordPayment({
      tranId: Number(route.params.id),
      paymentMethod: collectionForm.value.paymentMethod,
      transactionRef: collectionForm.value.transactionRef.trim() || undefined,
      remark: collectionForm.value.remark.trim() || undefined
    })
    messageTip('收款已登记，待财务确认', 'success')
    showCollectionDialog.value = false
    collectionForm.value = { paymentMethod: '', transactionRef: '', remark: '' }
    await fetchPaymentList()
    await fetchTranDetail()
  } catch (error) {
    messageTip('收款失败: ' + (error.message || ''), 'error')
  }
}

const handleConfirmPayment = async (payment: TPayment) => {
  const confirmResult = await messageConfirm('确认该笔收款已到账？').catch(() => false)
  if (!confirmResult) {
    return
  }
  try {
    await confirmPayment(payment.id, { approved: true, comment: '确认到账' })
    messageTip('收款已确认', 'success')
    await fetchPaymentList()
    await fetchTranDetail()
  } catch (error) {
    messageTip('确认收款失败: ' + (error.message || ''), 'error')
  }
}

const openRejectPaymentDialog = (payment: TPayment) => {
  selectedPayment.value = payment
  paymentRejectComment.value = ''
  paymentRejectDialogOpen.value = true
}

const submitRejectPayment = async () => {
  if (!selectedPayment.value) return
  if (!paymentRejectComment.value.trim()) {
    messageTip('请输入退回原因', 'error')
    return
  }
  try {
    await confirmPayment(selectedPayment.value.id, {
      approved: false,
      comment: paymentRejectComment.value.trim()
    })
    messageTip('收款已退回', 'success')
    paymentRejectDialogOpen.value = false
    await fetchPaymentList()
  } catch (error) {
    messageTip('退回收款失败: ' + (error.message || ''), 'error')
  }
}

const openRefundRequestDialog = (payment: TPayment) => {
  selectedPayment.value = payment
  refundForm.value = {
    refundType: 'ORDER_CANCEL',
    amount: getRefundableAmountForPayment(payment),
    reason: ''
  }
  refundRequestDialogOpen.value = true
}

const submitRefundRequest = async () => {
  if (!selectedPayment.value) return
  if (!refundForm.value.amount || refundForm.value.amount <= 0) {
    messageTip('请输入有效的退款金额', 'error')
    return
  }
  if (refundForm.value.amount > selectedPaymentRefundableAmount.value) {
    messageTip('退款金额不能超过可申请退款余额', 'error')
    return
  }
  if (!refundForm.value.reason.trim()) {
    messageTip('请输入退款原因', 'error')
    return
  }
  try {
    await createRefundRequest(selectedPayment.value.id, {
      refundType: refundForm.value.refundType,
      amount: refundForm.value.amount,
      reason: refundForm.value.reason.trim()
    })
    messageTip('退款申请已提交', 'success')
    refundRequestDialogOpen.value = false
    await fetchRefundRequestList()
  } catch (error) {
    messageTip('提交退款申请失败: ' + (error.message || ''), 'error')
  }
}

const openRefundApprovalDialog = (request: TRefundRequest, approved: boolean) => {
  selectedRefundRequest.value = request
  refundApprovalApproved.value = approved
  refundApprovalComment.value = ''
  refundApprovalDialogOpen.value = true
}

const submitRefundApproval = async () => {
  if (!selectedRefundRequest.value) return
  if (!refundApprovalApproved.value && !refundApprovalComment.value.trim()) {
    messageTip('请输入驳回原因', 'error')
    return
  }
  try {
    await approveRefundRequest(selectedRefundRequest.value.id, {
      approved: refundApprovalApproved.value,
      comment: refundApprovalComment.value.trim() || undefined
    })
    messageTip(refundApprovalApproved.value ? '退款申请已通过' : '退款申请已驳回', 'success')
    refundApprovalDialogOpen.value = false
    await fetchRefundRequestList()
  } catch (error) {
    messageTip('退款审批失败: ' + (error.message || ''), 'error')
  }
}

const openRefundExecuteDialog = (request: TRefundRequest) => {
  selectedRefundRequest.value = request
  refundExecuteForm.value = { result: 'SUCCESS', transactionRef: '', remark: '', failureReason: '' }
  refundExecuteDialogOpen.value = true
}

const submitRefundExecute = async () => {
  if (!selectedRefundRequest.value) return
  if (refundExecuteForm.value.result === 'FAILED' && !refundExecuteForm.value.failureReason.trim()) {
    messageTip('请输入失败原因', 'warning')
    return
  }
  try {
    await executeRefundRequest(selectedRefundRequest.value.id, {
      transactionRef: refundExecuteForm.value.transactionRef || undefined,
      remark: refundExecuteForm.value.remark || undefined,
      success: refundExecuteForm.value.result === 'SUCCESS',
      failureReason: refundExecuteForm.value.result === 'FAILED'
        ? refundExecuteForm.value.failureReason.trim()
        : undefined
    })
    messageTip(refundExecuteForm.value.result === 'SUCCESS' ? '退款已执行' : '退款执行失败已记录', 'success')
    refundExecuteDialogOpen.value = false
    await Promise.all([fetchPaymentList(), fetchRefundRequestList(), fetchTranDetail()])
  } catch (error) {
    messageTip('执行退款失败: ' + (error.message || ''), 'error')
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
const handleSettle = () => {
  if (!tranDetail.value.products || tranDetail.value.products.length === 0) {
    messageTip('该交易没有产品信息，无法结算', 'error')
    return
  }
  settlementDialogOpen.value = true
}

const handleSettlementDone = async () => {
  try {
    await fetchTranDetail()
    goBack()
  } catch {
    messageTip('结算已成功，但刷新交易详情失败', 'warning')
    goBack()
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

function openCollectionIfRequested(): void {
  if (route.query.collect !== '1') return
  if (canRecordPayment.value) {
    showCollectionDialog.value = true
    return
  }
  messageTip('当前交易状态不允许收款', 'warning')
}

// Watch route params change
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await loadTranPageData()
    openCollectionIfRequested()
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }

  await loadTranPageData()
  openCollectionIfRequested()
})
</script>
