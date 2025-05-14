<template>
  <div class="stock-alert-container">
    <div class="operation-bar">
      <el-button type="primary" plain @click="goBack">返 回</el-button>
      <el-button type="info" plain @click="loadStockAlerts" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>
    </div>
    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="SKU">
          <el-input 
            v-model="filterForm.sku" 
            placeholder="请输入SKU" 
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input 
            v-model="filterForm.name" 
            placeholder="请输入产品名称" 
            @keyup.enter="handleSearch" 
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="请选择分类" style="width: 180px;">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset" :disabled="loading">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table 
      :data="stockAlertList" 
      style="width: 100%"
      v-loading="loading"
      element-loading-text="加载中..."
    >
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="sku" label="SKU" min-width="10%" />
      <el-table-column prop="name" label="产品名称" min-width="16%" />
      <el-table-column prop="category" label="分类" min-width="12%" />
      <el-table-column prop="specification" label="规格" min-width="14%" />
      <el-table-column prop="stock" label="当前库存" min-width="10%">
        <template #default="scope">
          <span :class="{ 'stock-warning': scope.row.stock < scope.row.minStock }">{{ scope.row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="minStock" label="最低库存" min-width="10%" />
      <el-table-column prop="updateTime" label="最后更新时间" min-width="18%">
        <template #default="scope">
          {{ formatDateTime(scope.row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="10%">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleRestock(scope.row)">补货</el-button>
          <el-button size="small" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <el-icon><MessageBox /></el-icon>
          <p>暂无符合条件的库存预警产品</p>
        </div>
      </template>
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

    <!-- 补货对话框 -->
    <el-dialog
      v-model="restockDialogVisible"
      title="补货"
      width="30%"
      destroy-on-close
    >
      <div class="product-info" v-if="currentProduct">
        <div class="info-item">
          <span class="label">产品:</span>
          <span class="value">{{ currentProduct.name }}</span>
        </div>
        <div class="info-item">
          <span class="label">SKU:</span>
          <span class="value">{{ currentProduct.sku }}</span>
        </div>
        <div class="info-item">
          <span class="label">当前库存:</span>
          <span class="value" :class="{ 'stock-warning': currentProduct.stock < currentProduct.minStock }">
            {{ currentProduct.stock }}
          </span>
        </div>
        <div class="info-item">
          <span class="label">最低库存:</span>
          <span class="value">{{ currentProduct.minStock }}</span>
        </div>
      </div>
      <el-divider />
      <el-form :model="restockForm" label-width="100px">
        <el-form-item label="补货数量" required>
          <el-input-number v-model="restockForm.quantity" :min="1" :precision="0" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" v-model="restockForm.remark" :rows="3" placeholder="请输入补货备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="restockDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRestockSubmit" :loading="restockLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="库存变动记录"
      width="50%"
      destroy-on-close
    >
      <el-table :data="stockRecords" style="width: 100%">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="quantity" label="变动数量" width="100" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
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
import { MessageBox, Refresh } from '@element-plus/icons-vue'

export default defineComponent({
  name: 'ProductStockAlertView',
  
  components: {
    MessageBox,
    Refresh
  },
  
  setup() {
    const stockAlertList = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const loading = ref(false)
    const restockLoading = ref(false)
    const restockDialogVisible = ref(false)
    const currentProduct = ref(null)
    const restockForm = ref({
      productId: null,
      quantity: 1,
      remark: ''
    })
    const filterForm = ref({
      sku: '',
      name: '',
      category: ''
    })
    const categoryOptions = ref([])
    const router = useRouter()
    // 详情弹窗相关
    const detailDialogVisible = ref(false)
    const stockRecords = ref([])

    // 加载分类选项
    const loadCategoryOptions = async () => {
      try {
        const res = await doGet('/api/product-categories', {
          page: 1,
          size: 100
        })
        
        // 添加"全部"选项
        const options = [{ value: '', label: '全部' }]
        
        if (res.data && res.data.data && res.data.data.list) {
          const categoryList = res.data.data.list.map(item => ({
            value: item.name,
            label: item.name
          }))
          categoryOptions.value = [...options, ...categoryList]
        } else {
          categoryOptions.value = options
        }
      } catch (error) {
        categoryOptions.value = [{ value: '', label: '全部' }]
        messageTip('加载分类选项失败', 'error')
      }
    }

    // 加载库存预警列表
    const loadStockAlerts = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        // 添加筛选条件
        if (filterForm.value.sku && filterForm.value.sku.trim() !== '') {
          params.sku = filterForm.value.sku.trim()
        }
        if (filterForm.value.name && filterForm.value.name.trim() !== '') {
          params.name = filterForm.value.name.trim()
        }
        if (filterForm.value.category) {
          params.category = filterForm.value.category
        }
        
        const res = await doGet('/api/products/stockalerts', params)
        
        if (res.data && res.data.data) {
          stockAlertList.value = res.data.data.list || []
          total.value = res.data.data.total || 0
        }
      } catch (error) {
        messageTip('加载库存预警失败', 'error')
      } finally {
        loading.value = false
      }
    }

    // 格式化日期时间
    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '';
      const date = new Date(dateTimeStr);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };

    // 处理搜索
    const handleSearch = () => {
      currentPage.value = 1
      loadStockAlerts()
    }

    // 处理重置
    const handleReset = () => {
      filterForm.value = {
        sku: '',
        name: '',
        category: ''
      }
      currentPage.value = 1
      loadStockAlerts()
    }

    // 处理补货
    const handleRestock = (row) => {
      currentProduct.value = row
      restockForm.value = {
        productId: row.id,
        quantity: 1,
        remark: ''
      }
      restockDialogVisible.value = true
    }

    // 处理补货提交
    const handleRestockSubmit = async () => {
      if (!restockForm.value.quantity || restockForm.value.quantity <= 0) {
        messageTip('补货数量必须大于0', 'warning')
        return
      }
      
      restockLoading.value = true
      try {
        await doPost('/api/productstock/restock', restockForm.value)
        messageTip('补货成功', 'success')
        restockDialogVisible.value = false
        loadStockAlerts()
      } catch (error) {
        messageTip('补货失败', 'error')
      } finally {
        restockLoading.value = false
      }
    }

    // 处理详情
    const handleDetail = async (row) => {
      try {
        const res = await doGet(`/api/productstock/records/${row.id}`, { page: 1, size: 100 })
        stockRecords.value = res.data.data.list || []
        detailDialogVisible.value = true
      } catch (error) {
        messageTip('加载库存变动记录失败', 'error')
      }
    }

    // 处理分页
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1  // Reset to first page when changing page size
      loadStockAlerts()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadStockAlerts()
    }

    const goBack = () => {
      window.history.length > 1 ? window.history.back() : window.location.href = '/dashboard/product';
    }

    onMounted(() => {
      loadCategoryOptions()
      loadStockAlerts()
    })

    return {
      stockAlertList,
      currentPage,
      pageSize,
      total,
      loading,
      restockLoading,
      restockDialogVisible,
      currentProduct,
      restockForm,
      filterForm,
      categoryOptions,
      formatDateTime,
      handleSearch,
      handleReset,
      handleRestock,
      handleRestockSubmit,
      handleDetail,
      handleSizeChange,
      handleCurrentChange,
      goBack,
      loadStockAlerts,
      detailDialogVisible,
      stockRecords
    }
  }
})
</script>

<style scoped>
.stock-alert-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.el-table {
  margin-top: 10px;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.el-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 15px 0;
}

.stock-warning {
  color: #f56c6c;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 3px;
  background-color: rgba(245, 108, 108, 0.1);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.product-info {
  padding: 10px 0;
  margin-bottom: 10px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item .label {
  width: 80px;
  color: #606266;
  font-weight: bold;
}

.info-item .value {
  flex: 1;
  color: #303133;
}

/* 增加选择器样式 */
.el-select {
  width: 180px;
}

.el-input {
  width: 180px;
}

.empty-state {
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #909399;
}

.empty-state .el-icon {
  font-size: 40px;
  margin-bottom: 10px;
}
</style> 