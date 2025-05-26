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
        <el-form-item label="客户名称" prop="customerId">
          <el-select
            v-model="form.customerId"
            filterable
            placeholder="请选择客户"
            style="width: 100%"
            @change="onCustomerChange"
          >
            <el-option
              v-for="customer in customerOptions"
              :key="customer.customerId"
              :label="customer.customerName"
              :value="customer.customerId"
            />
          </el-select>
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

        <!-- 产品选择区域 -->
        <div class="product-section">
          <div 
            v-for="(product, index) in form.products" 
            :key="index" 
            class="product-item"
          >
            <el-form-item 
              :label="index === 0 ? '产品选择' : ''" 
              :prop="`products.${index}.productId`"
              :rules="{ required: true, message: '请选择产品', trigger: 'change' }"
            >
              <div class="product-row">
                <el-select
                  v-model="product.productId"
                  filterable
                  placeholder="请选择产品"
                  style="width: 60%; margin-right: 10px;"
                  @change="onProductChange(index, $event)"
                >
                  <el-option
                    v-for="item in productOptions.list"
                    :key="item.id"
                    :label="`${item.name} (¥${item.price})`"
                    :value="item.id"
                  />
                </el-select>
                
                <el-input-number
                  v-model="product.quantity"
                  :min="1"
                  :max="999"
                  placeholder="数量"
                  style="width: 25%; margin-right: 10px;"
                />
                
                <el-button 
                  v-if="form.products.length > 1"
                  type="danger" 
                  size="small" 
                  @click="removeProduct(index)"
                  style="width: 10%;"
                >
                  删除
                </el-button>
              </div>
            </el-form-item>
          </div>
          
          <el-form-item>
            <el-button type="primary" plain @click="addProduct">追加商品</el-button>
          </el-form-item>
        </div>

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
import { createTran, updateTran, getTranDetail, getTranProducts } from '../api/tran'
import { getProductList } from '../api/product'
import { getCustomerOptions } from '../api/customer'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const isEdit = ref(false)
const productOptions = reactive({
  list: []
})
const customerOptions = ref([])
const form = reactive({
  customerId: null,
  customerName: '',
  amount: 0,
  products: [
    {
      productId: null,
      quantity: 1,
      price: 0
    }
  ],
  description: '',
  expectedDeliveryDate: ''
})

const rules = reactive({
  customerId: [
    { required: true, message: '请选择客户', trigger: 'change' }
  ],
  amount: [
    { required: true, message: '请输入交易金额', trigger: 'blur' }
  ],
  expectedDeliveryDate: [
    { required: true, message: '请选择预计交付日期', trigger: 'change' }
  ]
})

// 添加产品
const addProduct = () => {
  form.products.push({
    productId: null,
    quantity: 1,
    price: 0
  })
}

// 删除产品
const removeProduct = (index) => {
  if (form.products.length > 1) {
    form.products.splice(index, 1)
  }
}

// 客户选择变化时更新客户名称
const onCustomerChange = (customerId) => {
  const selectedCustomer = customerOptions.value.find(c => c.customerId === customerId)
  if (selectedCustomer) {
    form.customerName = selectedCustomer.customerName
  }
}

// 产品选择变化时更新价格
const onProductChange = (index, productId) => {
  const selectedProduct = productOptions.list.find(p => p.id === productId)
  if (selectedProduct) {
    form.products[index].price = selectedProduct.price
  }
}

// 加载客户选项
const loadCustomers = async () => {
  try {
    const res = await getCustomerOptions()
    if (res.data.code === 200) {
      customerOptions.value = res.data.data
      console.log("获取客户选项：", customerOptions.value)
    }
  } catch (error) {
    ElMessage.error('加载客户列表失败')
  }
}

// 加载产品列表
const loadProducts = async () => {
  try {
    const res = await getProductList({
      page: 1,
      size: 1000
    })
    if (res.data.code === 200) {
      productOptions.list = res.data.data.list
      console.log("获取产品列表：", productOptions.list)
    }
  } catch (error) {
    ElMessage.error('加载产品列表失败')
  }
}

// 获取交易详情
const fetchTranDetail = async (id) => {
  try {
    const res = await getTranDetail(id)
    if (res.data.code === 200) {
      const data = res.data.data
      form.customerId = data.customerId
      form.customerName = data.customerName || ''
      form.amount = data.money || data.amount || 0
      form.description = data.description || ''
      
      if (data.expectedDate) {
        form.expectedDeliveryDate = new Date(data.expectedDate)
      }
      
      // 获取产品详情
      const productRes = await getTranProducts(id)
      if (productRes.data.code === 200 && productRes.data.data.length > 0) {
        form.products = productRes.data.data.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          price: item.price
        }))
      }
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
        // 验证产品选择
        const hasEmptyProduct = form.products.some(p => !p.productId)
        if (hasEmptyProduct) {
          ElMessage.error('请选择所有产品')
          return
        }
        
        // 格式化数据
        const formData = {
          id: isEdit.value ? route.params.id : undefined,
          customerId: form.customerId,
          customerName: form.customerName,
          amount: form.amount,
          products: form.products.map(p => ({
            productId: p.productId,
            quantity: p.quantity,
            price: p.price
          })),
          description: form.description,
          expectedDeliveryDate: form.expectedDeliveryDate ? 
            new Date(form.expectedDeliveryDate).toISOString().split('T')[0] + ' 00:00:00' : null
        }
        
        console.log('提交数据:', formData)
        
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

onMounted(async () => {
  await loadCustomers()
  await loadProducts()
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

.product-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 20px;
  background-color: #fafafa;
}

.product-item {
  margin-bottom: 10px;
}

.product-row {
  display: flex;
  align-items: center;
  width: 100%;
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