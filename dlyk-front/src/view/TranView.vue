<template>
  <div class="tran-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="交易编号">
          <el-input v-model="searchForm.tranNo" @keyup.enter="handleSearch" placeholder="请输入交易编号" clearable />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" @keyup.enter="handleSearch" placeholder="请输入客户名称" clearable />
        </el-form-item>
        <el-form-item label="交易状态" style="width: 200px;">
          <el-select v-model="searchForm.stage" @keyup.enter="handleSearch" placeholder="请选择状态" clearable>
            <el-option label="待报价" value="41" />
            <el-option label="待审批" value="42" />
            <el-option label="已审批" value="43" />
            <el-option label="待收款" value="45" />
            <el-option label="已完成" value="46" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <!--<el-button @click="resetForm">重置</el-button>-->
          <el-button type="success" @click="handleAdd">新增交易</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column 
          type="index" 
          label="序号" 
          width="80"
          :index="startIndex"
        />
        <el-table-column prop="tranNo" label="交易编号" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名称" show-overflow-tooltip />
        <el-table-column prop="amount" label="交易金额" show-overflow-tooltip>
          <template #default="scope">
            ¥{{ scope.row.amount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.stage)">
              {{ getStatusText(scope.row.stage) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip />
        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <el-button @click="handleView(scope.row)">查看</el-button>
            <el-button 
              type="primary" 
              @click="handleEdit(scope.row)"
              v-if="scope.row.stage === 'QUOTATION'"
            >编辑</el-button>
            <el-button 
              type="success" 
              @click="handleApprove(scope.row)"
              v-if="scope.row.stage === 'PENDING'"
            >审批</el-button>
            <el-button 
              type="warning" 
              @click="handleInvoice(scope.row)"
              v-if="scope.row.stage === 'APPROVED'"
            >开票</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 分页 -->
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="handleCurrentChange"
        @next-click="handleCurrentChange"
        @current-change="handleCurrentChange"
    />
    
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getTranList } from '../api/tran'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  tranNo: '',
  customerId: '',
  customerName: '',
  stage: ''
})

// 状态映射
const statusMap = {
  'QUOTATION': { type: 'info', text: '待报价' },
  'PENDING': { type: 'warning', text: '待审批' },
  'APPROVED': { type: 'success', text: '已审批' },
  'PAYMENT': { type: 'warning', text: '待收款' },
  'COMPLETED': { type: 'success', text: '已完成' }
}

const getStatusType = (status) => statusMap[status]?.type || ''
const getStatusText = (status) => statusMap[status]?.text || status

// 获取交易列表
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    params.status = parseInt(params.status)
    console.log(params)
    const res = await getTranList(params)
    if (res.data.code === 200) {
      tableData.value = res.data.data.list.map(item => ({
        id: item.id,
        tranNo: item.tranNo,
        customerName: `${item.customerName}`, 
        amount: item.money,
        stage: getStageStatus(item.stage),
        createTime: item.createTime
      }))
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.msg || '获取数据失败')
    }
  } catch (error) {
    console.error('获取交易列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 根据阶段获取状态
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

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// // 重置表单
// const resetForm = () => {
//   Object.keys(searchForm).forEach(key => {
//     searchForm[key] = ''
//   })
//   handleSearch()
// }

// 新增交易
const handleAdd = () => {
  router.push('/dashboard/tran/add')
}

// 查看详情
const handleView = (row) => {
  router.push(`/dashboard/tran/${row.id}`)
}

// 编辑交易
const handleEdit = (row) => {
  router.push(`/dashboard/tran/edit/${row.id}`)
}

// 审批交易
const handleApprove = (row) => {
  router.push(`/dashboard/tran/approve/${row.id}`)
}

// 开具发票
const handleInvoice = (row) => {
  router.push(`/dashboard/tran/invoice/${row.id}`)
}

const handleCurrentChange = (val) => {
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

<style scoped>
.tran-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 12px;
}

.el-pagination {
  margin-top: 12px;
}

:deep(.el-table) {
  width: 100% !important;
}
</style> 