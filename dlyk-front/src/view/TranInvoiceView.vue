<template>
  <div class="tran-invoice-container">
    <el-card class="invoice-card">
      <template #header>
        <div class="card-header">
          <span>开具发票</span>
          <el-tag type="warning">待开票</el-tag>
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
      </el-descriptions>

      <!-- 开票表单 -->
      <el-form
        ref="formRef"
        :model="invoiceForm"
        :rules="rules"
        label-width="120px"
        class="invoice-form"
      >
        <el-form-item label="发票类型" prop="type">
          <el-select v-model="invoiceForm.type" placeholder="请选择发票类型">
            <el-option label="增值税普通发票" value="VAT_NORMAL" />
            <el-option label="增值税专用发票" value="VAT_SPECIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="发票抬头" prop="title">
          <el-input v-model="invoiceForm.title" placeholder="请输入发票抬头" />
        </el-form-item>

        <el-form-item label="纳税人识别号" prop="taxNumber">
          <el-input v-model="invoiceForm.taxNumber" placeholder="请输入纳税人识别号" />
        </el-form-item>

        <el-form-item label="开户行" prop="bankName" v-if="invoiceForm.type === 'VAT_SPECIAL'">
          <el-input v-model="invoiceForm.bankName" placeholder="请输入开户行" />
        </el-form-item>

        <el-form-item label="银行账号" prop="bankAccount" v-if="invoiceForm.type === 'VAT_SPECIAL'">
          <el-input v-model="invoiceForm.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>

        <el-form-item label="注册地址" prop="address" v-if="invoiceForm.type === 'VAT_SPECIAL'">
          <el-input v-model="invoiceForm.address" placeholder="请输入注册地址" />
        </el-form-item>

        <el-form-item label="注册电话" prop="phone" v-if="invoiceForm.type === 'VAT_SPECIAL'">
          <el-input v-model="invoiceForm.phone" placeholder="请输入注册电话" />
        </el-form-item>

        <el-form-item label="发票金额" prop="amount">
          <el-input-number 
            v-model="invoiceForm.amount" 
            :precision="2" 
            :step="100" 
            :min="0"
            :max="tranDetail.amount"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="invoiceForm.remark"
            type="textarea"
            rows="4"
            placeholder="请输入备注信息"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm(formRef)">提交开票</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTranDetail, createInvoice } from '../api/tran'

const route = useRoute()
const router = useRouter()
const formRef = ref()

const tranDetail = ref({
  tranNo: '',
  customerName: '',
  amount: 0,
  status: '',
  createTime: '',
  updateTime: '',
  expectedDeliveryDate: ''
})

const invoiceForm = reactive({
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

// 根据发票类型动态设置验证规则
const getSpecialRules = () => {
  return invoiceForm.type === 'VAT_SPECIAL'
    ? {
        bankName: [
          { required: true, message: '请输入开户行', trigger: 'blur' }
        ],
        bankAccount: [
          { required: true, message: '请输入银行账号', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入注册地址', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入注册电话', trigger: 'blur' }
        ]
      }
    : {}
}

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
  ],
  ...getSpecialRules()
})

// 监听发票类型变化，更新验证规则
watch(() => invoiceForm.type, () => {
  Object.assign(rules, getSpecialRules())
})

// 获取交易详情
const fetchTranDetail = async () => {
  try {
    const res = await getTranDetail(route.params.id)
    if (res.data.code === 200) {
      tranDetail.value = res.data.data
      invoiceForm.amount = tranDetail.value.amount
    } else {
      ElMessage.error(res.data.msg || '获取交易详情失败')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    ElMessage.error('获取交易详情失败')
  }
}

// 提交开票
const submitForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const res = await createInvoice({
          tranId: route.params.id,
          ...invoiceForm
        })
        if (res.data.code === 200) {
          ElMessage.success('开票申请提交成功')
          goBack()
        } else {
          ElMessage.error(res.data.msg || '开票申请提交失败')
        }
      } catch (error) {
        console.error('开票申请提交失败:', error)
        ElMessage.error('开票申请提交失败')
      }
    }
  })
}

// 返回列表页
const goBack = () => {
  router.push('/dashboard/tran')
}

onMounted(async () => {
  await fetchTranDetail()
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

.invoice-form {
  margin-top: 30px;
  max-width: 600px;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-select),
:deep(.el-input),
:deep(.el-input-number) {
  width: 100%;
}
</style> 