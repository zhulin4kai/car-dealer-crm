<template>
  <div class="tran-detail-container">
    <!-- 基本信息 -->
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>交易基本信息</span>
          <el-tag :type="getStatusType(tranDetail.stage)">{{ getStatusText(tranDetail.stage) }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="交易编号">{{ tranDetail.tranNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ tranDetail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="交易金额">
          <span v-if="tranDetail.stage === 'QUOTATION'">?</span>
          <span v-else>¥{{ tranDetail.amount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ tranDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="预计交付日期">{{ tranDetail.expectedDeliveryDate }}</el-descriptions-item>
        <el-descriptions-item label="最后更新时间">{{ tranDetail.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="交易描述" :span="2">{{ tranDetail.description }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 产品信息 -->
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>产品信息</span>
        </div>
      </template>

      <el-table :data="tranDetail.products" style="width: 100%">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="productName" label="产品名称" min-width="300" />
        <el-table-column prop="quantity" label="数量" width="120" />
        <el-table-column prop="price" label="单价" width="140">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="total" label="小计" width="140">
          <template #default="scope">
            ¥{{ (scope.row.price * scope.row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 促销选择 - 仅在待报价状态显示 -->
      <div v-if="tranDetail.stage === 'QUOTATION'" class="promotion-section">
        <div class="section-title">促销选择</div>
        <el-form :model="promotionForm" label-width="120px">
          <el-form-item label="促销活动">
            <el-select 
              v-model="promotionForm.selectedPromotion" 
              placeholder="请选择促销活动（可选）"
              clearable
              style="width: 100%"
              @change="onPromotionChange"
            >
              <el-option
                v-for="promotion in availablePromotions"
                :key="promotion.id"
                :label="`${promotion.name} (${getPromotionText(promotion)})`"
                :value="promotion.id"
              />
            </el-select>
          </el-form-item>
          
          <!-- 促销效果预览 -->
          <el-form-item v-if="selectedPromotionInfo" label="促销效果">
            <div class="promotion-preview">
              <div class="price-calculation">
                <span class="original-price">原价：¥{{ originalTotalAmount.toFixed(2) }}</span>
                <span class="discount-info">{{ getDiscountDescription() }}</span>
                <span class="final-price">结算价：¥{{ finalAmount.toFixed(2) }}</span>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 发票信息 -->
    <el-card class="detail-card" v-if="invoiceList.length > 0">
      <template #header>
        <div class="card-header">
          <span>发票信息</span>
        </div>
      </template>

      <el-table :data="invoiceList" style="width: 100%">
        <el-table-column prop="invoiceNo" label="发票号码" width="180" />
        <el-table-column prop="type" label="发票类型" width="140">
          <template #default="scope">
            {{ getInvoiceTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="发票抬头" min-width="200" />
        <el-table-column prop="amount" label="发票金额" width="120">
          <template #default="scope">
            ¥{{ scope.row.amount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getInvoiceStatusType(scope.row.status)">
              {{ getInvoiceStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="issueTime" label="开票时间" width="160">
          <template #default="scope">
            {{ scope.row.issueTime || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button @click="goBack">返回</el-button>
      <!-- <el-button 
        type="primary" 
        @click="handleEdit" 
        v-if="tranDetail.stage === 'QUOTATION'"
      >编辑</el-button> -->
      <el-button 
        type="success" 
        @click="handleSettle" 
        v-if="tranDetail.stage === 'QUOTATION'"
      >结算</el-button>
      <el-button 
        type="success" 
        @click="handleApprove" 
        v-if="tranDetail.stage === 'PENDING'"
      >审批</el-button>
      <el-button 
        type="warning" 
        @click="handleInvoice" 
        v-if="tranDetail.stage === 'APPROVED'"
      >开票</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTranDetail, getTranInvoiceList, getTranProducts, settleTran } from '../api/tran'
import { getPromotionList } from '../api/product'

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

// 促销相关数据
const promotionList = ref([])
const promotionForm = ref({
  selectedPromotion: null
})

// 计算属性
const availablePromotions = computed(() => {
  // 只显示进行中的促销活动
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
  console.log('计算原价总额：', total)
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
      discountRate = discountRate / 100  // 92 -> 0.92
    }
    discountedAmount = original * discountRate
  } else if (promotionType === 'amount' || promotionType === '满减' || promotionType === '直降') {
    discountedAmount = original - promotion.discount
  } else {
    console.log('未知促销类型：', promotion.type)
    discountedAmount = original
  }
  // 确保最低为0，不能出现负数
  const result = Math.max(0, Number(discountedAmount.toFixed(2)))
  console.log('最终结算价：', result)
  return result
})

// 状态映射
const statusMap = {
  'QUOTATION': { type: 'info', text: '待报价' },
  'PENDING': { type: 'warning', text: '待审批' },
  'APPROVED': { type: 'success', text: '已审批' },
  'PAYMENT': { type: 'warning', text: '待收款' },
  'COMPLETED': { type: 'success', text: '已完成' }
}

const invoiceStatusMap = {
  'PENDING': { type: 'warning', text: '待开具' },
  'ISSUED': { type: 'success', text: '已开具' },
  'VOID': { type: 'danger', text: '已作废' }
}

const getStatusType = (status) => statusMap[status]?.type || ''
const getStatusText = (status) => statusMap[status]?.text || status

const getInvoiceStatusType = (status) => invoiceStatusMap[status]?.type || ''
const getInvoiceStatusText = (status) => invoiceStatusMap[status]?.text || status

// 发票类型映射
const getInvoiceTypeText = (type) => {
  const typeMap = {
    'VAT_NORMAL': '增值税普通发票',
    'VAT_SPECIAL': '增值税专用发票'
  }
  return typeMap[type] || type
}

// 促销相关方法
const getPromotionText = (promotion) => {
  const promotionType = promotion.type?.toLowerCase() || ''
  
  if (promotionType === 'percentage' || promotionType === '折扣') {
    // 处理折扣显示
    let discountRate = promotion.discount
    if (discountRate > 1) {
      // 大于1表示百分比，如92表示92折
      return `${discountRate}折`
    } else {
      // 小数表示保留比例，如0.92表示保留92%，即92折
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
  // 促销选择变化时的处理逻辑
  console.log('选择的促销：', selectedPromotionInfo.value)
  console.log('原价：', originalTotalAmount.value)
  console.log('折扣后价格：', finalAmount.value)
  console.log('优惠金额：', originalTotalAmount.value - finalAmount.value)
}

// 获取促销列表
const fetchPromotionList = async () => {
  try {
    const res = await getPromotionList({
      page: 1,
      size: 1000 // 获取所有促销活动
    })
    console.log('促销列表响应:', res)
    if (res.data.code === 200) {
      promotionList.value = res.data.data.list || []
      console.log('促销列表:', promotionList.value)
    }
  } catch (error) {
    console.error('获取促销列表失败:', error)
  }
}

// 获取交易详情
const fetchTranDetail = async () => {
  try {
    console.log('获取交易详情，ID:', route.params.id) // 添加调试日志
    const res = await getTranDetail(route.params.id)
    console.log('交易详情响应:', res) // 添加调试日志
    if (res.data.code === 200) {
      const data = res.data.data
      // 根据后端返回的数据结构进行映射
      tranDetail.value = {
        tranNo: data.tranNo || '',
        customerName: data.customerName || '',
        amount: data.money || data.amount || 0, // 后端可能返回money字段
        stage: getStageStatus(data.stage), // 转换stage状态
        createTime: data.createTime || '',
        updateTime: data.editTime || data.updateTime || '', // 后端可能返回editTime
        expectedDeliveryDate: data.expectedDate || data.expectedDeliveryDate || '',
        description: data.description || '',
        products: data.products || []
      }
      console.log('处理后的交易详情:', tranDetail.value) // 添加调试日志
    } else {
      ElMessage.error(res.data.msg || '获取交易详情失败')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    ElMessage.error('获取交易详情失败')
  }
}

// 根据阶段获取状态 - 添加这个方法，因为TranDetailView中缺少了
const getStageStatus = (stage) => {
  const stageMap = {
    41: 'QUOTATION', // 待报价
    42: 'PENDING',   // 待审批
    43: 'APPROVED',  // 已审批
    45: 'PAYMENT',   // 待收款
    46: 'COMPLETED'  // 已完成
  }
  return stageMap[stage] || 'QUOTATION'
}

// 获取交易产品详情
const fetchProducts = async () => {
  try {
    const res = await getTranProducts(route.params.id)
    console.log('交易产品详情:', res)
    if (res.data.code === 200) {
      tranDetail.value.products = res.data.data
    }
  } catch (error) {
    console.error('获取产品详情失败:', error)
  }
}

// 获取发票信息
const fetchInvoiceInfo = async () => {
  try {
    const res = await getTranInvoiceList(route.params.id)
    if (res.data.code === 200) {
      invoiceList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取发票信息失败:', error)
  }
}

// 返回列表页
const goBack = () => {
  router.push('/dashboard/tran')
}

// 编辑交易
const handleEdit = () => {
  router.push(`/dashboard/tran/edit/${route.params.id}`)
}

// 结算交易
const handleSettle = async () => {
  try {
    // 检查是否有产品信息
    if (!tranDetail.value.products || tranDetail.value.products.length === 0) {
      ElMessage.error('该交易没有产品信息，无法结算')
      return
    }
    
    // 使用折扣后的金额
    const settlementAmount = finalAmount.value
    const originalAmount = originalTotalAmount.value
    
    let confirmMessage = `确认结算该交易吗？`
    
    if (selectedPromotionInfo.value) {
      confirmMessage += `\n原价：¥${originalAmount.toFixed(2)}\n${getDiscountDescription()}\n最终结算金额：¥${settlementAmount.toFixed(2)}`
    } else {
      confirmMessage += `\n结算金额：¥${settlementAmount.toFixed(2)}`
    }
    
    // 显示确认对话框
    const confirmResult = await ElMessageBox.confirm(
      confirmMessage,
      '确认结算',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).catch(() => false)
    
    if (!confirmResult) {
      return
    }
    
    try {
      const res = await settleTran(route.params.id, settlementAmount)
      if (res.data.code === 200) {
        ElMessage.success('结算成功，交易状态已更新为待审批')
        await fetchTranDetail()
        goBack()
      } else {
        console.error('结算失败响应：', res.data)
        ElMessage.error(res.data.msg || '结算失败')
      }
    } catch (error) {
      console.error('结算API调用失败：', error)
      ElMessage.error('结算失败：' + (error.message || '网络错误'))
    }
  } catch (error) {
    console.error('结算失败:', error)
    ElMessage.error('结算失败')
  }
}

// 审批交易
const handleApprove = () => {
  router.push(`/dashboard/tran/approve/${route.params.id}`)
}

// 开具发票
const handleInvoice = () => {
  router.push(`/dashboard/tran/invoice/${route.params.id}`)
}

// 监听路由参数变化
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchTranDetail()
    await fetchProducts()
    await fetchInvoiceInfo()
    await fetchPromotionList()
  }
})

onMounted(async () => {
  console.log('TranDetailView mounted')
  console.log('route.params:', route.params)
  console.log('route.params.id:', route.params.id)
  
  if (!route.params.id) {
    ElMessage.error('缺少交易ID参数')
    return
  }
  
  await fetchTranDetail()
  await fetchProducts()
  await fetchInvoiceInfo()
  await fetchPromotionList() // 加载促销列表
})
</script>

<style scoped>
.tran-detail-container {
  padding: 20px;
}

.detail-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.promotion-section {
  margin-top: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #303133;
}

.promotion-preview {
  padding: 15px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.price-calculation {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.original-price {
  color: #909399;
  text-decoration: line-through;
}

.discount-info {
  color: #67c23a;
  font-weight: bold;
}

.final-price {
  color: #e6a23c;
  font-size: 18px;
  font-weight: bold;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

:deep(.el-timeline) {
  margin: 0 20px;
}

:deep(.el-table) {
  margin-bottom: 20px;
}
</style> 