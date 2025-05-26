<template>
  <div class="tran-invoice-container">
    <el-card class="invoice-card">
      <template #header>
        <div class="card-header">
          <span>交易开票</span>
          <el-tag type="success">已审批</el-tag>
        </div>
      </template>

      <!-- 交易基本信息 -->
      <el-descriptions title="交易基本信息" :column="2" border>
        <el-descriptions-item label="交易编号">{{ tranDetail.tranNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ tranDetail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="交易金额">¥{{ tranDetail.amount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ tranDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="预计交付日期">{{ tranDetail.expectedDeliveryDate }}</el-descriptions-item>
        <el-descriptions-item label="最后更新时间">{{ tranDetail.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="交易描述" :span="2">{{ tranDetail.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 产品信息 -->
      <div class="section-title">产品信息</div>
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

      <!-- 发票表单 -->
      <el-form
        ref="formRef"
        :model="invoiceForm"
        :rules="rules"
        label-width="120px"
        class="invoice-form"
      >
        <div class="section-title">发票信息</div>
        
        <el-form-item label="发票类型" prop="type">
          <el-radio-group v-model="invoiceForm.type">
            <el-radio label="VAT_NORMAL">增值税普通发票</el-radio>
            <el-radio label="VAT_SPECIAL">增值税专用发票</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发票抬头" prop="title">
          <el-input
            v-model="invoiceForm.title"
            placeholder="请输入发票抬头"
          />
        </el-form-item>

        <el-form-item label="纳税人识别号" prop="taxNumber">
          <el-input
            v-model="invoiceForm.taxNumber"
            placeholder="请输入纳税人识别号"
          />
        </el-form-item>

        <el-form-item label="开户行" prop="bankName">
          <el-input
            v-model="invoiceForm.bankName"
            placeholder="请输入开户行"
          />
        </el-form-item>

        <el-form-item label="银行账号" prop="bankAccount">
          <el-input
            v-model="invoiceForm.bankAccount"
            placeholder="请输入银行账号"
          />
        </el-form-item>

        <el-form-item label="注册地址" prop="address">
          <el-input
            v-model="invoiceForm.address"
            placeholder="请输入注册地址"
          />
        </el-form-item>

        <el-form-item label="注册电话" prop="phone">
          <el-input
            v-model="invoiceForm.phone"
            placeholder="请输入注册电话"
          />
        </el-form-item>

        <el-form-item label="发票金额" prop="amount">
          <el-input-number
            v-model="invoiceForm.amount"
            :precision="2"
            :min="0"
            style="width: 100%"
            placeholder="发票金额"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="invoiceForm.remark"
            type="textarea"
            rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm(formRef)">开具发票</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>

      <!-- 已有发票列表 -->
      <div v-if="invoiceList.length > 0" class="invoice-list">
        <div class="section-title">发票记录</div>
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
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'PENDING'"
                type="success"
                size="small"
                @click="markAsIssued(scope.row)"
              >
                标记已开具
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTranDetail, getTranProducts, createInvoice, getTranInvoiceList, updateInvoiceStatus } from '../api/tran'

const route = useRoute()
const router = useRouter()
const formRef = ref()

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

const invoiceForm = reactive({
  tranId: null,
  type: 'VAT_NORMAL',
  title: '',
  taxNumber: '',
  bankName: '',
  bankAccount: '',
  address: '',
  phone: '',
  amount: 0,
  remark: ''
})

const rules = reactive({
  type: [
    { required: true, message: '请选择发票类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入发票抬头', trigger: 'blur' }
  ],
  taxNumber: [
    { required: true, message: '请输入纳税人识别号', trigger: 'blur' }
  ],
  amount: [
    { required: true, message: '请输入发票金额', trigger: 'blur' }
  ]
})

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

// 发票类型映射
const getInvoiceTypeText = (type) => {
  const typeMap = {
    'VAT_NORMAL': '增值税普通发票',
    'VAT_SPECIAL': '增值税专用发票'
  }
  return typeMap[type] || type
}

// 发票状态映射
const invoiceStatusMap = {
  'PENDING': { type: 'warning', text: '待开具' },
  'ISSUED': { type: 'success', text: '已开具' },
  'VOID': { type: 'danger', text: '已作废' }
}

const getInvoiceStatusType = (status) => invoiceStatusMap[status]?.type || ''
const getInvoiceStatusText = (status) => invoiceStatusMap[status]?.text || status

// 获取交易详情
const fetchTranDetail = async () => {
  try {
    console.log('获取交易详情，ID:', route.params.id)
    const res = await getTranDetail(route.params.id)
    console.log('交易详情响应:', res)
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
      
      // 设置发票金额为交易金额
      invoiceForm.amount = tranDetail.value.amount
      invoiceForm.tranId = parseInt(route.params.id)
      
      console.log('处理后的交易详情:', tranDetail.value)
    } else {
      ElMessage.error(res.data.msg || '获取交易详情失败')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    ElMessage.error('获取交易详情失败')
  }
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

// 获取发票列表
const fetchInvoiceList = async () => {
  try {
    const res = await getTranInvoiceList(route.params.id)
    if (res.data.code === 200) {
      invoiceList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取发票列表失败:', error)
  }
}

// 提交发票表单
const submitForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const res = await createInvoice(invoiceForm)
        if (res.data.code === 200) {
          ElMessage.success('发票创建成功')
          // 重新获取发票列表
          await fetchInvoiceList()
          // 重置表单
          formEl.resetFields()
          invoiceForm.amount = tranDetail.value.amount
        } else {
          ElMessage.error(res.data.msg || '发票创建失败')
        }
      } catch (error) {
        console.error('发票创建失败:', error)
        ElMessage.error('发票创建失败')
      }
    }
  })
}

// 标记发票为已开具
const markAsIssued = async (invoice) => {
  try {
    const res = await updateInvoiceStatus(invoice.id, 'ISSUED')
    if (res.data.code === 200) {
      ElMessage.success('发票状态更新成功')
      // 重新获取发票列表
      await fetchInvoiceList()
      // 返回交易列表
      goBack()
    } else {
      ElMessage.error(res.data.msg || '发票状态更新失败')
    }
  } catch (error) {
    console.error('发票状态更新失败:', error)
    ElMessage.error('发票状态更新失败')
  }
}

// 返回列表页
const goBack = () => {
  router.push('/dashboard/tran')
}

onMounted(async () => {
  console.log('TranInvoiceView mounted')
  console.log('route.params:', route.params)
  console.log('route.params.id:', route.params.id)
  
  if (!route.params.id) {
    ElMessage.error('缺少交易ID参数')
    return
  }
  
  await fetchTranDetail()
  await fetchProducts()
  await fetchInvoiceList()
})
</script>

<style scoped>
.tran-invoice-container {
  padding: 20px;
}

.invoice-card {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin: 20px 0;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
}

.invoice-form {
  margin-top: 30px;
  max-width: 600px;
}

.invoice-list {
  margin-top: 30px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

:deep(.el-table) {
  margin-bottom: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}
</style> 