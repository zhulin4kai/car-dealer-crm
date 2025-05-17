<template>
  <div class="dict-value-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="字典类型">
          <el-select v-model="searchForm.typeCode" placeholder="请选择字典类型" clearable style="width: 200px">
            <el-option
              v-for="item in dictTypes"
              :key="item.typeCode"
              :label="item.typeName"
              :value="item.typeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="字典值">
          <el-input 
            v-model="searchForm.typeValue" 
            placeholder="请输入字典值" 
            clearable 
            style="width: 200px" 
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button type="success" @click="handleAdd">新增字典值</el-button>
          <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedIds.length">批量删除</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column 
          type="index" 
          label="序号" 
          width="80"
          :index="startIndex"
        />
        <el-table-column prop="typeCode" label="字典类型" width="180">
          <template #default="scope">
            {{ getDictTypeName(scope.row.typeCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="typeValue" label="字典值" width="180" />
        <el-table-column prop="order" label="排序" width="100" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; width: 100%;"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑字典值' : '新增字典值'"
      width="500px"
    >
      <el-form 
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="字典类型" prop="typeCode">
          <el-select v-model="form.typeCode" placeholder="请选择字典类型" style="width: 100%">
            <el-option
              v-for="item in dictTypes"
              :key="item.typeCode"
              :label="item.typeName"
              :value="item.typeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="字典值" prop="typeValue">
          <el-input v-model="form.typeValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="排序" prop="order">
          <el-input-number v-model="form.order" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="form.remark" 
            type="textarea" 
            placeholder="请输入备注"
            :rows="3"
          />
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

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getDictValueList, 
  createDictValue, 
  updateDictValue, 
  deleteDictValue, 
  batchDeleteDictValues,
  clearCache,
  getDictTypeList
} from '../api/dict'
import { messageConfirm } from '../util/util'
import { useRouter } from 'vue-router'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formRef = ref()
const dictTypes = ref([])
const router = useRouter()

const searchForm = reactive({
  typeCode: '',
  typeValue: ''
})

const form = reactive({
  typeCode: '',
  typeValue: '',
  order: 1,
  remark: ''
})

const rules = {
  typeCode: [
    { required: true, message: '请选择字典类型', trigger: 'change' }
  ],
  typeValue: [
    { required: true, message: '请输入字典值', trigger: 'blur' }
  ],
  order: [
    { required: true, message: '请输入排序', trigger: 'blur' }
  ]
}

// 加载字典类型
const loadDictTypes = async () => {
  try {
    const res = await getDictTypeList({ page: 1, size: 100 })
    if (res.data.code === 200) {
      dictTypes.value = res.data.data.list
    }
  } catch (error) {
    console.error('获取字典类型列表失败:', error)
  }
}

// 获取字典类型名称
const getDictTypeName = (typeCode) => {
  const type = dictTypes.value.find(item => item.typeCode === typeCode)
  return type ? type.typeName : typeCode
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    console.log(params)
    const res = await getDictValueList(params)
    if (res.data.code === 200) {
      // 对数据按照id升序排序
      tableData.value = res.data.data.list.sort((a, b) => a.id - b.id)
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取字典值列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = async () => {
  try {
    loading.value = true
    const res = await getDictValues(searchForm)
    if (res.code === 200) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('获取字典值列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = async () => {
  searchForm.typeCode = ''
  searchForm.typeValue = ''
  await clearCache()
  await handleSearch()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  form.typeCode = ''
  form.typeValue = ''
  form.order = 1
  form.remark = ''
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该字典值吗？')
    const res = await deleteDictValue(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return
  try {
    await messageConfirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`)
    const res = await batchDeleteDictValues(selectedIds.value)
    if (res.data.code === 200) {
      ElMessage.success('批量删除成功')
      loadData()
    } else {
      ElMessage.error(res.data.msg || '批量删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    // 1. 表单验证
    const valid = await formRef.value.validate()
    if (!valid) return

    // 2. 准备请求数据
    const requestData = {
      typeCode: form.typeCode,
      typeValue: form.typeValue,
      order: form.order,
      remark: form.remark
    }

    // 3. 根据编辑状态选择API并调用
    let res
    if (isEdit.value) {
      res = await updateDictValue(form.id, requestData)
    } else {
      console.log(requestData)
      res = await createDictValue(requestData)
    }
    console.log(res)

    // 4. 处理响应结果
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.data.msg || (isEdit.value ? '更新失败' : '创建失败'))
    }
  } catch (error) {
    console.error('提交字典值失败:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 分页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadData()
}

// 计算序号起始值
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  loadDictTypes()
  loadData()
})
</script>

<style scoped>
.dict-value-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

:deep(.el-table) {
  width: 100% !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-select),
:deep(.el-input),
:deep(.el-input-number) {
  width: 100%;
}
</style> 