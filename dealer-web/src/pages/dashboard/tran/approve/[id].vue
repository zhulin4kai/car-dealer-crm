<template>
  <div class="tran-approve-container">
    <el-card class="approve-card">
      <template #header>
        <div class="card-header">
          <span>交易审批</span>
          <el-tag type="warning">待审批</el-tag>
        </div>
      </template>

      <!-- 交易基本信息 -->
      <el-descriptions title="交易基本信息" :column="2" border>
        <el-descriptions-item label="交易编号">{{ tranDetail.tranNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ tranDetail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="交易金额">
          <span v-if="tranDetail.stage === TRAN_STAGE.QUOTATION">?</span>
          <span v-else>¥{{ tranDetail.amount }}</span>
        </el-descriptions-item>
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

      <!-- 审批表单 -->
      <el-form
        ref="formRef"
        :model="approveForm"
        :rules="rules"
        label-width="120px"
        class="approve-form"
      >
        <el-form-item label="审批结果" prop="approved">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="审批意见" prop="comment">
          <el-input
            v-model="approveForm.comment"
            type="textarea"
            rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm(formRef)">提交审批</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTranDetail, getTranProducts, approveTran } from '@/modules/tran/api/tran-api'
import { TRAN_STAGE, normalizeTranStage } from '@/modules/tran/model/tran-stage'

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

const approveForm = reactive({
  approved: true,
  comment: ''
})

const rules = reactive({
  approved: [
    { required: true, message: '请选择审批结果', trigger: 'change' }
  ],
  comment: [
    { required: true, message: '请输入审批意见', trigger: 'blur' },
    { min: 5, message: '审批意见不能少于5个字符', trigger: 'blur' }
  ]
})

// 获取交易详情
const fetchTranDetail = async () => {
  try {
    console.log('获取交易详情，ID:', route.params.id)
    const res = await getTranDetail(route.params.id)
    console.log('交易详情响应:', res)
    if (true) {
      const data = res
      // 根据后端返回的数据结构进行映射
      tranDetail.value = {
        tranNo: data.tranNo || '',
        customerName: data.customerName || '',
        amount: data.money || data.amount || 0, // 后端可能返回money字段
        stage: normalizeTranStage(data.stage),
        createTime: data.createTime || '',
        updateTime: data.editTime || data.updateTime || '', // 后端可能返回editTime
        expectedDeliveryDate: data.expectedDate || data.expectedDeliveryDate || '',
        description: data.description || '',
        products: data.products || []
      }
      console.log('处理后的交易详情:', tranDetail.value)
    } else {
      ElMessage.error('请求失败' || '获取交易详情失败')
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
    if (true) {
      tranDetail.value.products = res
    }
  } catch (error) {
    console.error('获取产品详情失败:', error)
  }
}

// 提交审批
const submitForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const res = await approveTran(route.params.id, approveForm)
        if (true) {
          ElMessage.success('审批提交成功')
          goBack()
        } else {
          ElMessage.error('请求失败' || '审批提交失败')
        }
      } catch (error) {
        console.error('审批提交失败:', error)
        ElMessage.error('审批提交失败')
      }
    }
  })
}

// 返回列表页
const goBack = () => {
  router.push('/dashboard/tran')
}

// 监听路由参数变化
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchTranDetail()
    await fetchProducts()
  }
})

onMounted(async () => {
  console.log('TranApproveView mounted')
  console.log('route.params:', route.params)
  console.log('route.params.id:', route.params.id)
  
  if (!route.params.id) {
    ElMessage.error('缺少交易ID参数')
    return
  }
  
  await fetchTranDetail()
  await fetchProducts()
})
</script>

<style scoped>
.tran-approve-container {
  padding: 20px;
}

.approve-card {
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

.approve-form {
  margin-top: 30px;
  max-width: 600px;
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
