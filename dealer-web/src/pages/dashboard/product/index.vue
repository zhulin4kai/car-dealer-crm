<template>
  <div class="product-container">
    <el-card class="action-card">
      <el-button type="primary" @click="handleAdd">新增产品</el-button>
      <el-button type="success" @click="handleCategory">分类管理</el-button>
      <el-button type="warning" @click="handlePromotion">促销设置</el-button>
      <el-button type="danger" @click="handleStockAlert">库存预警</el-button>
    </el-card>

    <el-card class="table-card">
      <el-table
        :data="productList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        element-loading-text="加载中..."
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" :index="startIndex" />
        <el-table-column prop="sku" label="SKU" show-overflow-tooltip />
        <el-table-column prop="name" label="产品名称" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" show-overflow-tooltip>
          <template #default="scope">
            ¥{{ scope.row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'stock-warning': scope.row.stock < scope.row.minStock }">
              {{ scope.row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-tag :type="scope.row.status === '上架' ? 'success' : 'info'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <el-button type="success" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="handleCurrentChange"
      @next-click="handleCurrentChange"
      @current-change="handleCurrentChange"
      style="margin-top: 12px; width: 100%;"
    />

    <!-- 产品表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增产品' : '编辑产品'"
      width="30%"
    >
      <el-form :model="productForm" label-width="100px" :rules="productRules" ref="productFormRef">
        <el-form-item label="SKU" prop="sku">
          <el-input v-model="productForm.sku" />
        </el-form-item>
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="productForm.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="productForm.category" placeholder="请选择分类">
            <el-option
              v-for="item in categoryOptions.list"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="productForm.specification" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="productForm.price" :precision="2" :step="0.1" :min="0.01" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="最低库存">
          <el-input-number v-model="productForm.minStock" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="productForm.status">
            <el-option label="上架" value="上架" />
            <el-option label="下架" value="下架" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { messageTip, messageConfirm } from '@/shared/utils/legacy-util'
import { useRouter } from 'vue-router'
import { 
  getProductList, 
  createProduct, 
  updateProduct, 
  deleteProduct,
  getCategoryList
} from '@/modules/product/api/product-api'

const router = useRouter()
const productFormRef = ref(null)
const productList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogType = ref('add')
const productForm = ref({
  sku: '',
  name: '',
  category: '',
  specification: '',
  price: 0,
  stock: 0,
  minStock: 0,
  status: '上架'
})
const categoryOptions = reactive({
  list: []
})

// 表单校验规则
const productRules = {
  sku: [
    { required: true, message: '请输入SKU', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入产品名称', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const loadCategoryOptions = async () => {
  try {
    const res = await getCategoryList()
    categoryOptions.list = res.list
  } catch (error) {
    messageTip('加载分类列表失败', 'error')
  }
}

// 加载产品列表
const loadProducts = async () => {
  try {
    const res = await getProductList({
      page: currentPage.value,
      size: pageSize.value
    })
    productList.value = res.list
    total.value = res.total
  } catch (error) {
    messageTip('加载产品列表失败', 'error')
  }
}

// 处理新增
const handleAdd = async () => {
  await loadCategoryOptions()
  dialogType.value = 'add'
  productForm.value = {
    sku: '',
    name: '',
    category: '',
    specification: '',
    price: 0,
    stock: 0,
    minStock: 0,
    status: '上架'
  }
  dialogVisible.value = true
}

// 处理编辑
const handleEdit = async (row) => {
  await loadCategoryOptions()
  dialogType.value = 'edit'
  productForm.value = { ...row }
  dialogVisible.value = true
}

// 处理删除
const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该产品？')
    await deleteProduct(row.id)
    messageTip('删除成功', 'success')
    loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('删除失败', 'error')
    }
  }
}

// 处理提交
const handleSubmit = async () => {
  if (!productFormRef.value) return
  
  try {
    const valid = await productFormRef.value.validate()
    if (!valid) return
    
    if (dialogType.value === 'add') {
      await createProduct(productForm.value)
      messageTip('新增成功', 'success')
    } else {
      await updateProduct(productForm.value.id, productForm.value)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    loadProducts()
  } catch (error) {
    messageTip('操作失败', 'error')
  }
}

// 处理分类管理
const handleCategory = () => {
  router.push('/dashboard/product/category')
}

// 处理促销设置
const handlePromotion = () => {
  router.push('/dashboard/product/promotion')
}

// 处理库存预警
const handleStockAlert = () => {
  router.push('/dashboard/product/stock')
}

// 处理分页
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadProducts()
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  // 处理表格选择逻辑
}

const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.product-container {
  padding: 20px;
}

.action-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 12px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.stock-warning {
  color: #f56c6c;
}

:deep(.el-table) {
  width: 100% !important;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}
</style> 