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
        <el-descriptions-item label="交易金额">¥{{ tranDetail.amount }}</el-descriptions-item>
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
            ¥{{ scope.row.price * scope.row.quantity }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发票信息 -->
    <el-card class="detail-card" v-if="invoiceInfo">
      <template #header>
        <div class="card-header">
          <span>发票信息</span>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="发票号码">{{ invoiceInfo.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="发票金额">¥{{ invoiceInfo.amount }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ invoiceInfo.issueDate }}</el-descriptions-item>
        <el-descriptions-item label="发票状态">
          <el-tag :type="getInvoiceStatusType(invoiceInfo.status)">
            {{ getInvoiceStatusText(invoiceInfo.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button @click="goBack">返回</el-button>
      <el-button 
        type="primary" 
        @click="handleEdit" 
        v-if="tranDetail.stage === 'QUOTATION'"
      >编辑</el-button>
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTranDetail, getInvoiceInfo, getTranProducts } from '../api/tran'

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

const invoiceInfo = ref(null)

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

const getTimelineItemType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PENDING': return 'warning'
    default: return ''
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
    const res = await getInvoiceInfo(route.params.id)
    if (res.data.code === 200) {
      invoiceInfo.value = res.data.data
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

// 审批交易
const handleApprove = () => {
  router.push(`/dashboard/tran/approve/${route.params.id}`)
}

// 开具发票
const handleInvoice = () => {
  router.push(`/dashboard/tran/invoice/${route.params.id}`)
}

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