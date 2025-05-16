<template>
  <div class="tran-record-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑交易' : '新增交易' }}</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="tran-form"
      >
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>

        <el-form-item label="交易金额" prop="amount">
          <el-input-number 
            v-model="form.amount" 
            :precision="2" 
            :step="100" 
            :min="0"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="产品选择" prop="products">
          <el-select
            v-model="form.products"
            multiple
            filterable
            placeholder="请选择产品"
            style="width: 100%"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="交易描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            rows="4"
            placeholder="请输入交易描述"
          />
        </el-form-item>

        <el-form-item label="预计交付日期" prop="expectedDeliveryDate">
          <el-date-picker
            v-model="form.expectedDeliveryDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm(formRef)">保存</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTran, updateTran, getTranDetail } from '../api/tran'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const isEdit = ref(false)

// 模拟产品数据，实际应从API获取
const productOptions = ref([
  { id: 1, name: '产品A' },
  { id: 2, name: '产品B' },
  { id: 3, name: '产品C' }
])

const form = reactive({
  customerName: '',
  amount: 0,
  products: [],
  description: '',
  expectedDeliveryDate: ''
})

const rules = reactive({
  customerName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' }
  ],
  amount: [
    { required: true, message: '请输入交易金额', trigger: 'blur' }
  ],
  products: [
    { required: true, message: '请选择产品', trigger: 'change' }
  ],
  expectedDeliveryDate: [
    { required: true, message: '请选择预计交付日期', trigger: 'change' }
  ]
})

// 获取交易详情
const fetchTranDetail = async (id) => {
  try {
    const res = await getTranDetail(id)
    if (res.data.code === 200) {
      const data = res.data.data
      Object.keys(form).forEach(key => {
        if (data[key] !== undefined) {
          // Convert date string to Date object for the date picker
          if (key === 'expectedDeliveryDate' && data[key]) {
            // 从后端接收的日期格式 yyyy-MM-dd HH:mm:ss 转换为 Date 对象
            form[key] = new Date(data[key])
          } else {
            form[key] = data[key]
          }
        }
      })
    } else {
      ElMessage.error(res.data.msg || '获取交易详情失败')
    }
  } catch (error) {
    console.error('获取交易详情失败:', error)
    ElMessage.error('获取交易详情失败')
  }
}

// 提交表单
const submitForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        // Format the date before submission
        const formData = { ...form }
        if (formData.expectedDeliveryDate) {
          // 格式化日期为 yyyy-MM-dd HH:mm:ss
          const date = new Date(formData.expectedDeliveryDate)
          const year = date.getFullYear()
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          formData.expectedDeliveryDate = `${year}-${month}-${day} 00:00:00`
        }
        
        const api = isEdit.value ? updateTran : createTran
        const res = await api(formData)
        if (res.data.code === 200) {
          ElMessage.success('保存成功')
          goBack()
        } else {
          ElMessage.error(res.data.msg || '保存失败')
        }
      } catch (error) {
        console.error('保存失败:', error)
        ElMessage.error('保存失败')
      }
    }
  })
}

// 返回列表页
const goBack = () => {
  router.push('/dashboard/tran')
}

// 格式化日期显示
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day} 00:00:00`
}

onMounted(async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    await fetchTranDetail(id)
  }
})
</script>

<style scoped>
.tran-record-container {
  padding: 20px;
}

.form-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tran-form {
  margin-top: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
  width: 100%;
}

:deep(.el-select),
:deep(.el-input),
:deep(.el-input-number),
:deep(.el-date-picker) {
  width: 100%;
}
</style> 