<template>
  <div class="category-container">
    <div class="operation-bar">
      <el-button type="primary" plain @click="goBack">返 回</el-button>
      <el-button type="primary" @click="handleAdd">新增分类</el-button>
    </div>

    <el-table :data="categoryList" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" width="180" />
      <el-table-column prop="code" label="分类编码" width="120" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === '启用' ? 'success' : 'info'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
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

    <!-- 分类表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增分类' : '编辑分类'"
      width="50%"
    >
      <el-form :model="categoryForm" label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="categoryForm.code" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="categoryForm.description" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="categoryForm.status">
            <el-option label="启用" value="启用" />
            <el-option label="禁用" value="禁用" />
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

export default defineComponent({
  name: 'ProductCategoryView',
  
  setup() {
    const categoryList = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const dialogVisible = ref(false)
    const dialogType = ref('add')
    const categoryForm = ref({
      name: '',
      code: '',
      description: '',
      sort: 0,
      status: '启用'
    })

    // 加载分类列表
    const loadCategories = async () => {
      try {
        const res = await doGet('/api/product-categories', {
          page: currentPage.value,
          size: pageSize.value
        })
        categoryList.value = res.data.data.list
        total.value = res.data.data.total
      } catch (error) {
        messageTip('加载分类列表失败', 'error')
      }
    }

    // 处理新增
    const handleAdd = () => {
      dialogType.value = 'add'
      categoryForm.value = {
        name: '',
        code: '',
        description: '',
        sort: 0,
        status: '启用'
      }
      dialogVisible.value = true
    }

    // 处理编辑
    const handleEdit = (row) => {
      dialogType.value = 'edit'
      categoryForm.value = { ...row }
      dialogVisible.value = true
    }

    // 处理删除
    const handleDelete = async (row) => {
      try {
        await messageConfirm('确认删除该分类？')
        await doDelete(`/api/product-categories/${row.id}`)
        messageTip('删除成功', 'success')
        loadCategories()
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
          await doPost('/api/product-categories', categoryForm.value)
          messageTip('新增成功', 'success')
        } else {
          await doPut(`/api/product-categories/${categoryForm.value.id}`, categoryForm.value)
          messageTip('编辑成功', 'success')
        }
        dialogVisible.value = false
        loadCategories()
      } catch (error) {
        messageTip('操作失败', 'error')
      }
    }

    // 处理分页
    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadCategories()
    }

    const goBack = () => {
      window.history.length > 1 ? window.history.back() : window.location.href = '/dashboard/product';
    }

    onMounted(() => {
      loadCategories()
    })

    return {
      categoryList,
      currentPage,
      pageSize,
      total,
      dialogVisible,
      dialogType,
      categoryForm,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleCurrentChange,
      goBack
    }
  }
})
</script>

<style scoped>
.category-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 