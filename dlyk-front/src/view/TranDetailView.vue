<template>
  <div class="tran-detail-container">
    <!-- 基本信息 -->
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>交易基本信息</span>
          <el-tag :type="getStatusType(tranDetail.status)">{{ getStatusText(tranDetail.status) }}</el-tag>
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
        <el-table-column prop="name" label="产品名称" min-width="300" />
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

    <!-- 生产状态 -->
    <el-card class="detail-card" v-if="productionStatus.length > 0">
      <template #header>
        <div class="card-header">
          <span>生产状态</span>
          <el-button type="primary" link @click="refreshProductionStatus">刷新状态</el-button>
        </div>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in productionStatus"
          :key="index"
          :type="getTimelineItemType(item.status)"
          :timestamp="item.updateTime"
        >
          {{ item.description }}
        </el-timeline-item>
      </el-timeline>
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
        v-if="tranDetail.status === 'QUOTATION'"
      >编辑</el-button>
      <el-button 
        type="success" 
        @click="handleApprove" 
        v-if="tranDetail.status === 'PENDING'"
      >审批</el-button>
      <el-button 
        type="warning" 
        @click="handleInvoice" 
        v-if="tranDetail.status === 'APPROVED'"
      >开票</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTranDetail, getProductionStatus, getInvoiceInfo } from '../api/tran'

const route = useRoute()
const router = useRouter()

const tranDetail = ref({
  tranNo: '',
  customerName: '',
  amount: 0,
  status: '',
  createTime: '',
  updateTime: '',
  expectedDeliveryDate: '',
  description: '',
  products: []
})

const productionStatus = ref([])
const invoiceInfo = ref(null)

// 状态映射
const statusMap = {
  'QUOTATION': { type: 'info', text: '待报价' },
  'PENDING': { type: 'warning', text: '待审批' },
  'APPROVED': { type: 'success', text: '已审批' },
  'PRODUCTION': { type: 'primary', text: '生产中' },
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
    case 'IN_PROGRESS': return 'primary'
    case 'PENDING': return 'warning'
    default: return ''
  }
}

// 获取交易详情
const fetchTranDetail = async () => {
  try {
    const res = await getTranDetail(route.params.id)
    if (res.data.code === 200) {
      tranDetail.value = res.data.data
    } else {
      ElMessage.error(res.data.msg || '获取交易详情失败')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    ElMessage.error('获取交易详情失败')
  }
}

// 获取生产状态
const fetchProductionStatus = async () => {
  try {
    const res = await getProductionStatus(route.params.id)
    if (res.data.code === 200) {
      productionStatus.value = res.data.data
    }
  } catch (error) {
    console.error('获取生产状态失败:', error)
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

// 刷新生产状态
const refreshProductionStatus = () => {
  fetchProductionStatus()
  ElMessage.success('刷新成功')
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
  await fetchTranDetail()
  await fetchProductionStatus()
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