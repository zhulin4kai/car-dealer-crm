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
              <Label>促销活动</Label>
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

            <!-- Promotion Effect Preview -->
            <div v-if="selectedPromotionInfo" class="space-y-2">
              <Label>促销效果</Label>
              <div class="p-4 bg-background border rounded-md">
                <div class="flex flex-col gap-2">
                  <span class="text-muted-foreground line-through">原价：&yen;{{ originalTotalAmount.toFixed(2) }}</span>
                  <span class="text-green-600 font-bold">{{ getDiscountDescription() }}</span>
                  <span class="text-lg font-bold text-yellow-600">结算价：&yen;{{ finalAmount.toFixed(2) }}</span>
                </div>
              </div>
            </div>
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

    <!-- Action Buttons -->
    <div class="flex justify-center gap-5 mt-5">
      <Button variant="outline" @click="goBack">返回</Button>
      <Button
        variant="secondary"
        @click="handleSettle"
        v-if="tranDetail.stage === 'QUOTATION'"
      >结算</Button>
      <Button
        variant="secondary"
        @click="handleApprove"
        v-if="tranDetail.stage === TRAN_STAGE.PENDING"
      >审批</Button>
      <Button
        variant="outline"
        @click="handleInvoice"
        v-if="tranDetail.stage === TRAN_STAGE.APPROVED"
      >开票</Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import { getTranDetail, getTranInvoiceList, getTranProducts, settleTran } from '@/modules/tran/api/tran-api'
import { TRAN_STAGE, getTranStageText, getTranStageType, normalizeTranStage } from '@/modules/tran/model/tran-stage'
import { getPromotionList } from '@/modules/product/api/product-api'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
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

// Promotion data
const promotionList = ref([])
const promotionForm = ref({
  selectedPromotion: null
})

// Computed properties
const availablePromotions = computed(() => {
  return promotionList.value.filter(promotion => promotion.status === '进行中')
})

const selectedPromotionInfo = computed(() => {
  if (!promotionForm.value.selectedPromotion) return null
  return promotionList.value.find(p => p.id === promotionForm.value.selectedPromotion)
})

const originalTotalAmount = computed(() => {
  if (!tranDetail.value.products) return 0
  const total = tranDetail.value.products.reduce((total, product) => {
    return total + (product.price * product.quantity)
  }, 0)
  return Number(total.toFixed(2))
})

const finalAmount = computed(() => {
  const original = originalTotalAmount.value
  if (!selectedPromotionInfo.value) return original

  const promotion = selectedPromotionInfo.value
  let discountedAmount = original
  const promotionType = promotion.type?.toLowerCase() || ''

  if (promotionType === 'percentage' || promotionType === '折扣') {
    let discountRate = promotion.discount
    if (discountRate > 1) {
      discountRate = discountRate / 100
    }
    discountedAmount = original * discountRate
  } else if (promotionType === 'amount' || promotionType === '满减' || promotionType === '直降') {
    discountedAmount = original - promotion.discount
  } else {
    discountedAmount = original
  }
  const result = Math.max(0, Number(discountedAmount.toFixed(2)))
  return result
})

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

const getDiscountDescription = () => {
  if (!selectedPromotionInfo.value) return ''

  const promotion = selectedPromotionInfo.value
  const original = originalTotalAmount.value
  const final = finalAmount.value
  const discount = original - final

  const promotionType = promotion.type?.toLowerCase() || ''

  if (promotionType === 'percentage' || promotionType === '折扣') {
    let discountRate = promotion.discount
    if (discountRate > 1) {
      return `${discountRate}折优惠，优惠¥${discount.toFixed(2)}`
    } else {
      return `${(discountRate * 100)}折优惠，优惠¥${discount.toFixed(2)}`
    }
  } else if (promotionType === 'amount' || promotionType === '满减') {
    return `满减优惠，减免¥${discount.toFixed(2)}`
  } else if (promotionType === '直降') {
    return `直降优惠，减免¥${discount.toFixed(2)}`
  } else {
    return `优惠减免¥${discount.toFixed(2)}`
  }
}

const onPromotionChange = () => {
  // Hook for promotion selection change
}

// Fetch promotion list
const fetchPromotionList = async () => {
  try {
    const res = await getPromotionList({
      page: 1,
      size: 1000
    })
    if (true) {
      promotionList.value = res.list || []
    }
  } catch (error) {
    console.error('获取促销列表失败:', error)
  }
}

// Fetch transaction detail
const fetchTranDetail = async () => {
  try {
    const res = await getTranDetail(route.params.id)
    if (true) {
      const data = res
      tranDetail.value = {
        tranNo: data.tranNo || '',
        customerName: data.customerName || '',
        amount: data.money || data.amount || 0,
        stage: normalizeTranStage(data.stage),
        createTime: data.createTime || '',
        updateTime: data.editTime || data.updateTime || '',
        expectedDeliveryDate: data.expectedDate || data.expectedDeliveryDate || '',
        description: data.description || '',
        products: data.products || []
      }
      console.log('处理后的交易详情:', tranDetail.value)
    } else {
      messageTip('请求失败', 'error')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    messageTip('获取交易详情失败', 'error')
  }
}

// Fetch product details
const fetchProducts = async () => {
  try {
    const res = await getTranProducts(route.params.id)
    if (true) {
      tranDetail.value.products = res
    }
  } catch (error) {
    console.error('获取产品详情失败:', error)
  }
}

// Fetch invoice info
const fetchInvoiceInfo = async () => {
  try {
    const res = await getTranInvoiceList(route.params.id)
    if (true) {
      invoiceList.value = res || []
    }
  } catch (error) {
    console.error('获取发票信息失败:', error)
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
  try {
    if (!tranDetail.value.products || tranDetail.value.products.length === 0) {
      messageTip('该交易没有产品信息，无法结算', 'error')
      return
    }

    const settlementAmount = finalAmount.value
    const originalAmount = originalTotalAmount.value

    let confirmMessage = `确认结算该交易吗？`

    if (selectedPromotionInfo.value) {
      confirmMessage += `\n原价：¥${originalAmount.toFixed(2)}\n${getDiscountDescription()}\n最终结算金额：¥${settlementAmount.toFixed(2)}`
    } else {
      confirmMessage += `\n结算金额：¥${settlementAmount.toFixed(2)}`
    }

    const confirmResult = await messageConfirm(confirmMessage).catch(() => false)

    if (!confirmResult) {
      return
    }

    try {
      const res = await settleTran(route.params.id, settlementAmount)
      if (true) {
        messageTip('结算成功，交易状态已更新为待审批', 'success')
        await fetchTranDetail()
        goBack()
      } else {
        console.error('结算失败响应：', res.data)
        messageTip('请求失败', 'error')
      }
    } catch (error) {
      console.error('结算API调用失败：', error)
      messageTip('结算失败：' + (error.message || '网络错误'), 'error')
    }
  } catch (error) {
    console.error('结算失败:', error)
    messageTip('结算失败', 'error')
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
    await fetchTranDetail()
    await fetchProducts()
    await fetchInvoiceInfo()
    await fetchPromotionList()
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }

  await fetchTranDetail()
  await fetchProducts()
  await fetchInvoiceInfo()
  await fetchPromotionList()
})
</script>
