<template>
  <div class="tran-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="交易编号">
          <el-input v-model="searchForm.tranNo" @keyup.enter="handleSearch" placeholder="请输入交易编号" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" @keyup.enter="handleSearch" placeholder="请输入客户名称" clearable style="width: 200px;" />
        </el-form-item>        <el-form-item label="交易状态">
          <el-select v-model="searchForm.stage" @keyup.enter="handleSearch" placeholder="请选择状态" clearable style="width: 200px;">
            <el-option label="待报价" value="41" />
            <el-option label="待审批" value="42" />
            <el-option label="已审批" value="43" />
            <el-option label="已完成" value="46" />
          </el-select>
        </el-form-item><el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button type="success" @click="handleAdd">新增交易</el-button>
          <el-button type="danger" @click="handleBatchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">      
      <el-table :data="tableData" style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column 
          type="selection" 
          width="55"
        />
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
            <span v-if="scope.row.stage === 'QUOTATION'">?</span>
            <span v-else>¥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.stage)">
              {{ getStatusText(scope.row.stage) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip />        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <div class="operation-buttons">
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
              >审批</el-button>              <el-button 
                type="warning" 
                @click="handleInvoice(scope.row)"
                v-if="scope.row.stage === 'APPROVED'"
              >开票</el-button>
              <el-button 
                type="danger" 
                @click="handleDelete(scope.row.id)"
                v-if="scope.row.stage === 'QUOTATION'"
              >删除</el-button>
            </div>
          </template>
        </el-table-column></el-table>
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

    <!-- 交易编辑Dialog -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑交易' : '新增交易'"
      width="800px"
      :before-close="handleDialogClose"
    >
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
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
    
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getTranList, updateTran, createTran, getTranDetail, getTranProducts, deleteTran, batchDeleteTran } from '../api/tran'
import { getProductList } from '../api/product'
import { getCustomerOptions } from '../api/customer'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const productOptions = reactive({
  list: []
})
const customerOptions = ref([])

const searchForm = reactive({
  tranNo: '',
  customerId: '',
  customerName: '',
  stage: ''
})

// 表单数据
const form = reactive({
  id: null, // 添加id字段用于编辑
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

// 表单验证规则
const rules = reactive({
  customerId: [
    { required: true, message: '请选择客户', trigger: 'change' }
  ],
  expectedDeliveryDate: [
    { required: true, message: '请选择预计交付日期', trigger: 'change' }
  ]
})

// 状态映射
const statusMap = {
  'QUOTATION': { type: 'info', text: '待报价' },
  'PENDING': { type: 'warning', text: '待审批' },
  'APPROVED': { type: 'success', text: '已审批' },
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
    // 只有当searchForm.stage有值时才转换为数字
    if (params.stage) {
      params.stage = parseInt(params.stage)
    }
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
    46: 'COMPLETED'  // 已完成
  }
  return stageMap[stage] || 'QUOTATION'
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 单个删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '您确定要删除该交易吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const res = await deleteTran(id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的交易')
    return
  }

  try {
    await ElMessageBox.confirm(
      `您确定要删除选中的 ${selectedIds.value.length} 条交易吗？`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const res = await batchDeleteTran(selectedIds.value)
    if (res.data.code === 200) {
      ElMessage.success(`成功删除 ${selectedIds.value.length} 条交易`)
      selectedIds.value = []
      fetchData()
    } else {
      ElMessage.error(res.data.msg || '批量删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 新增交易
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  loadCustomers()
  loadProducts()
}

// 编辑交易
const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  dialogVisible.value = true
  loadCustomers()
  loadProducts()
  // 存储当前编辑的交易ID
  form.id = row.id
  setTimeout(() => {
    fetchTranDetail(row.id)
  }, 100)
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.customerId = null
  form.customerName = ''
  form.amount = 0
  form.products = [{
    productId: null,
    quantity: 1,
    price: 0
  }]
  form.description = ''
  form.expectedDeliveryDate = ''
}

// 关闭Dialog
const handleDialogClose = () => {
  dialogVisible.value = false
  resetForm()
}

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
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
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
          id: isEdit.value ? form.id : undefined,
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
        let res
        if (isEdit.value) {
          res = await updateTran(formData)
        } else {
          res = await createTran(formData)
        }
        if (res.data.code === 200) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          resetForm()
          fetchData()
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

// 查看详情
const handleView = (row) => {
  router.push(`/dashboard/tran/${row.id}`)
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

.demo-form-inline {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}

.demo-form-inline .el-form-item {
  margin-right: 15px;
  margin-bottom: 0;
  flex-shrink: 0;
}

.demo-form-inline .el-form-item:last-child {
  margin-right: 0;
  margin-left: 0;
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

.tran-form {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}

.operation-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 52px;
}

:deep(.el-table) {
  width: 100% !important;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
  width: 100%;
}

:deep(.demo-form-inline .el-form-item) {
  margin-bottom: 0 !important;
  width: auto !important;
}

:deep(.demo-form-inline .el-form-item__label) {
  white-space: nowrap;
}

:deep(.demo-form-inline .el-form-item__content) {
  flex-shrink: 0;
}

:deep(.el-select),
:deep(.el-input),
:deep(.el-input-number),
:deep(.el-date-picker) {
  width: 100%;
}
</style> 