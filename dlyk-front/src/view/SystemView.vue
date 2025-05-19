<template>
  <div class="system-container">
    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="handleAdd">新增系统信息</el-button>
      <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedIds.length">批量删除</el-button>
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
        <el-table-column prop="systemCode" label="系统代码" show-overflow-tooltip />
        <el-table-column prop="name" label="系统名称" show-overflow-tooltip />
        <el-table-column prop="title" label="系统标题" show-overflow-tooltip />
        <el-table-column prop="description" label="系统描述" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" show-overflow-tooltip />
        <el-table-column prop="isopen" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-switch
              v-model="scope.row.isopen"
              :active-value="'true'"
              :inactive-value="'false'"
              @change="handleStatusChange(scope.row)"
            />
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑系统信息' : '新增系统信息'"
      width="700px"
    >
      <el-form 
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统代码" prop="systemCode">
              <el-input v-model="form.systemCode" placeholder="请输入系统代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入系统名称" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入系统标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统网址" prop="site">
              <el-input v-model="form.site" placeholder="请输入系统网址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="系统描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入系统描述" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统Logo" prop="logo">
              <el-input v-model="form.logo" placeholder="请输入Logo地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="快捷图标" prop="shortcuticon">
              <el-input v-model="form.shortcuticon" placeholder="请输入快捷图标地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信" prop="weixin">
              <el-input v-model="form.weixin" placeholder="请输入微信号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="请输入系统版本" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>

        <el-form-item label="关闭提示" prop="closeMsg">
          <el-input v-model="form.closeMsg" type="textarea" placeholder="请输入系统关闭时的提示信息" />
        </el-form-item>

        <el-form-item label="系统状态" prop="isopen">
          <el-switch
            v-model="form.isopen"
            :active-value="'true'"
            :inactive-value="'false'"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSystemList, createSystem, updateSystem, deleteSystem, batchDeleteSystems, toggleSystemStatus } from '../api/system'
import { messageConfirm } from '../util/util'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const selectedIds = ref([])
const formRef = ref()

// 表单数据
const form = reactive({
  systemCode: '',
  name: '',
  site: '',
  logo: '',
  title: '',
  description: '',
  keywords: '',
  shortcuticon: '',
  tel: '',
  weixin: '',
  email: '',
  address: '',
  version: '',
  closeMsg: '',
  isopen: 'true'
})

// 表单验证规则
const rules = {
  systemCode: [{ required: true, message: '请输入系统代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  title: [{ required: true, message: '请输入系统标题', trigger: 'blur' }],
  site: [{ required: true, message: '请输入系统网址', trigger: 'blur' }],
  description: [{ required: true, message: '请输入系统描述', trigger: 'blur' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getSystemList()
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } catch (error) {
    console.error('获取系统列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.keys(form).forEach(key => form[key] = '')
  form.isopen = 'true'
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
    await messageConfirm('确认删除该系统信息吗？')
    const res = await deleteSystem(row.id)
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
    const res = await batchDeleteSystems(selectedIds.value)
    if (res.data.code === 200) {
      ElMessage.success('批量删除成功')
      loadData()
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
    await formRef.value.validate()
    const res = isEdit.value
      ? await updateSystem(form.id, form)
      : await createSystem(form)

    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交系统信息失败:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const res = await toggleSystemStatus(row.id, row.isopen)
    if (res.data.code === 200) {
      ElMessage.success('状态更新成功')
    } else {
      row.isopen = row.isopen === 'true' ? 'false' : 'true' // 恢复原状态
      ElMessage.error('状态更新失败')
    }
  } catch (error) {
    row.isopen = row.isopen === 'true' ? 'false' : 'true' // 恢复原状态
    ElMessage.error('状态更新失败')
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.system-container {
  padding: 20px;
}

.action-card {
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

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-textarea__inner) {
  min-height: 80px !important;
}
</style> 