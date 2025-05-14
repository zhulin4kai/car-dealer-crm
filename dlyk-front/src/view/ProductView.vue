<template>
  <div class="product-container">
    <div class="operation-bar">
      <el-button type="primary" @click="handleAdd">新增产品</el-button>
      <el-button type="success" @click="handleCategory">分类管理</el-button>
      <el-button type="warning" @click="handlePromotion">促销设置</el-button>
      <el-button type="danger" @click="handleStockAlert">库存预警</el-button>
    </div>

    <el-table
      :data="productList"
      style="width: 100%; margin-top: 10px;"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="sku" label="SKU" min-width="10%" />
      <el-table-column prop="name" label="产品名称" min-width="16%" />
      <el-table-column prop="category" label="分类" min-width="12%" />
      <el-table-column prop="specification" label="规格" min-width="14%" />
      <el-table-column prop="price" label="价格" min-width="10%">
        <template #default="scope">
          ¥{{ scope.row.price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" min-width="10%">
        <template #default="scope">
          <span :class="{ 'stock-warning': scope.row.stock < scope.row.minStock }">
            {{ scope.row.stock }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="8%">
        <template #default="scope">
          <el-tag :type="scope.row.status === '上架' ? 'success' : 'info'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="10%">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
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
      width="50%"
    >
      <el-form :model="productForm" label-width="100px">
        <el-form-item label="SKU">
          <el-input v-model="productForm.sku" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="productForm.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="productForm.category" placeholder="请选择分类">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="productForm.specification" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="productForm.price" :precision="2" :step="0.1" :min="0" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="productForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="最低库存">
          <el-input-number v-model="productForm.minStock" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
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

<script>
import { defineComponent, ref, onMounted } from 'vue'
import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'
import { messageTip, messageConfirm } from '../util/util'
import { useRouter } from 'vue-router'

export default defineComponent({
  name: 'ProductView',
  
  setup() {
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
    const categoryOptions = ref([
      { value: '电子产品', label: '电子产品' },
      { value: '服装', label: '服装' },
      { value: '食品', label: '食品' }
    ])
    const router = useRouter()

    // 加载产品列表
    const loadProducts = async () => {
      try {
        const res = await doGet('/api/products', {
          page: currentPage.value,
          size: pageSize.value
        })
        productList.value = res.data.data.list
        total.value = res.data.data.total
      } catch (error) {
        messageTip('加载产品列表失败', 'error')
      }
    }

    // 处理新增
    const handleAdd = () => {
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
    const handleEdit = (row) => {
      dialogType.value = 'edit'
      productForm.value = { ...row }
      dialogVisible.value = true
    }

    // 处理删除
    const handleDelete = async (row) => {
      try {
        await messageConfirm('确认删除该产品？')
        await doDelete(`/api/products/${row.id}`)
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
      try {
        if (dialogType.value === 'add') {
          await doPost('/api/products', productForm.value)
          messageTip('新增成功', 'success')
        } else {
          await doPut(`/api/products/${productForm.value.id}`, productForm.value)
          messageTip('编辑成功', 'success')
        }
        dialogVisible.value = false
        loadProducts()
      } catch (error) {
        messageTip('操作失败', 'error')
      }
    }

    // 处理分页
    const handleSizeChange = (val) => {
      pageSize.value = val
      loadProducts()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadProducts()
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

    onMounted(() => {
      loadProducts()
    })

    return {
      productList,
      currentPage,
      pageSize,
      total,
      dialogVisible,
      dialogType,
      productForm,
      categoryOptions,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleSizeChange,
      handleCurrentChange,
      handleCategory,
      handlePromotion,
      handleStockAlert
    }
  }
})
</script>

<style scoped>
.product-container {
  padding: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.operation-bar {
  margin: 20px 0 10px 0;
  display: flex;
  gap: 10px;
}
.el-table {
  margin-top: 10px;
  width: 100%;
}
.el-pagination {
  margin-top: 12px;
  width: 100%;
}
.el-select {
  width: 100%;
}
.stock-warning {
  color: #f56c6c;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 