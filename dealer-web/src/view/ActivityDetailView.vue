<template>
  <div class="activity-detail-container">
    <!-- 活动详情卡片 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">活动详情</span>
          <div class="header-actions">
            <el-button type="info" plain @click="goBack" size="small">
              返 回
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 基本信息区域 -->
      <div class="info-section">
        <h4 class="section-title">基本信息</h4>
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">活动ID：</span>
              <span class="value primary">{{ activityDetail.id || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">负责人：</span>
              <span class="value">{{ activityDetail.ownerDO?.name || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">活动预算：</span>
              <span class="value primary">{{ activityDetail.cost || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <span class="label">活动名称：</span>
              <span class="value">{{ activityDetail.name || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="info-item">
              <span class="label">开始时间：</span>
              <span class="value">{{ activityDetail.startTime || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="info-item">
              <span class="label">结束时间：</span>
              <span class="value">{{ activityDetail.endTime || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24" v-if="activityDetail.description">
          <el-col :span="24">
            <div class="info-item description">
              <span class="label">活动描述：</span>
              <div class="description-content">{{ activityDetail.description }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 管理信息区域 -->
      <div class="info-section">
        <h4 class="section-title">管理信息</h4>
        <el-row :gutter="24">
          <el-col :span="6">
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ activityDetail.createTime || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="info-item">
              <span class="label">创建人：</span>
              <span class="value">{{ activityDetail.createByDO?.name || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="info-item">
              <span class="label">编辑时间：</span>
              <span class="value">{{ activityDetail.editTime || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="info-item">
              <span class="label">编辑人：</span>
              <span class="value">{{ activityDetail.editByDO?.name || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 备注记录表单卡片 -->
    <el-card class="remark-card" shadow="hover">
      <template #header>
        <span class="header-title">添加活动备注</span>
      </template>
      
      <el-form
          ref="activityRemarkRefForm"
          :model="activityRemarkQuery"
          label-width="100px"
          :rules="activityRemarkRules"
          class="remark-form">
        
        <el-form-item label="活动备注" prop="noteContent">
          <el-input
              v-model="activityRemarkQuery.noteContent"
              :rows="6"
              type="textarea"
              placeholder="请输入详细的活动备注内容..."/>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="activityRemarkSubmit" :loading="submitting">
            <el-icon><EditPen /></el-icon>
            提交备注
          </el-button>
          <el-button @click="resetRemarkForm">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 备注记录列表卡片 -->
    <el-card class="records-card" shadow="hover">
      <template #header>
        <span class="header-title">活动备注记录</span>
      </template>
      
      <el-table
          :data="activityRemarkList"
          style="width: 100%"
          stripe
          border>
        <el-table-column type="index" label="序号" width="60" align="center"/>
        <el-table-column prop="noteContent" label="备注内容" min-width="200" show-overflow-tooltip/>
        <el-table-column property="createTime" label="备注时间" width="160" align="center"/>
        <el-table-column property="createByDO.name" label="备注人" width="100" align="center"/>
        <el-table-column property="editTime" label="编辑时间" width="160" align="center"/>
        <el-table-column property="editByDO.name" label="编辑人" width="100" align="center"/>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="edit(scope.row.id)">
              编辑
            </el-button>
            <el-button size="small" type="danger" link @click="del(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            background
            layout="prev, pager, next, total"
            :page-size="pageSize"
            :total="total"
            @prev-click="toPage"
            @next-click="toPage"
            @current-change="toPage"/>
      </div>
    </el-card>  </div>

</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { doDelete, doGet, doPost, doPut } from "../http/httpRequest.js"

// Router and route
const route = useRoute()
const router = useRouter()

// Template refs
const activityRemarkRefForm = ref()

// 活动详情对象
const activityDetail = ref({
  ownerDO: {},
  createByDO: {},
  editByDO: {}
})

// 市场活动备注对象，初始值是空
const activityRemarkQuery = reactive({})

// 活动备注的列表对象，初始值是空
const activityRemarkList = ref([])

// 分页时每页显示多少条数据
const pageSize = ref(10)

// 分页总共查询出多少条数据
const total = ref(0)

// 加载状态
const submitting = ref(false)

// 提交活动备注的验证规则
const activityRemarkRules = reactive({
  noteContent: [
    { required: true, message: '请输入活动备注', trigger: 'blur' },
    { min: 5, max: 255, message: '活动备注长度为5-255个字符', trigger: 'blur' }
  ]
})

// 重置备注表单
const resetRemarkForm = () => {
  Object.keys(activityRemarkQuery).forEach(key => delete activityRemarkQuery[key])
  if (activityRemarkRefForm.value) {
    activityRemarkRefForm.value.resetFields()
  }
}

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 加载市场活动详情
const loadActivityDetail = async () => {
  const id = route.params.id
  try {
    const resp = await doGet("/api/activity/" + id, {})
    if (resp.data.code === 200) {
      activityDetail.value = resp.data.data
      if (!activityDetail.value.ownerDO) {
        activityDetail.value.ownerDO = {}
      }
      if (!activityDetail.value.createByDO) {
        activityDetail.value.createByDO = {}
      }
      if (!activityDetail.value.editByDO) {
        activityDetail.value.editByDO = {}
      }
    }
  } catch (error) {
    console.error('加载活动详情失败:', error)
    ElMessage.error("加载活动详情失败")
  }
}

// 提交活动备注
const activityRemarkSubmit = async () => {
  if (!activityRemarkRefForm.value) return
  
  submitting.value = true
  try {
    const isValid = await activityRemarkRefForm.value.validate()
    if (isValid) {
      const resp = await doPost("/api/activity/remark", {
        activityId: activityDetail.value.id,
        noteContent: activityRemarkQuery.noteContent
      })
      if (resp.data.code === 200) {
        ElMessage.success("提交成功")
        // 重新加载数据而不是刷新整个页面
        await loadActivityRemarkList(1)
        // 清空表单
        resetRemarkForm()
      } else {
        ElMessage.error("提交失败")
      }
    }
  } catch (error) {
    console.error('提交活动备注失败:', error)
    ElMessage.error("提交失败")
  } finally {
    submitting.value = false
  }
}

// 查询活动备注列表数据
const loadActivityRemarkList = async (current) => {
  try {
    const resp = await doGet("/api/activity/remark", {
      current: current,
      activityId: route.params.id
    })
    if (resp.data.code === 200) {
      activityRemarkList.value = resp.data.data.list || []
      pageSize.value = resp.data.data.pageSize || 10
      total.value = resp.data.data.total || 0
    }
  } catch (error) {
    console.error('加载备注列表失败:', error)
    ElMessage.error("加载备注列表失败")
  }
}

// 分页函数(current这个参数是ele-plus组件传过来，就是传的当前页)
const toPage = (current) => {
  loadActivityRemarkList(current)
}

// 编辑备注记录 (待实现)
const edit = (id) => {
  // TODO: 实现编辑功能
  ElMessage.info("编辑功能待实现")
}

// 删除活动备注
const del = async (id) => {
  try {
    await ElMessageBox.confirm("您确定要删除该数据吗？", "提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const resp = await doDelete("/api/activity/remark/" + id, {})
    if (resp.data.code === 200) {
      ElMessage.success("删除成功")
      // 重新加载数据
      await loadActivityRemarkList(1)
    } else {
      ElMessage.error("删除失败，原因：" + resp.data.msg)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除备注失败:', error)
      ElMessage.error("删除失败")
    } else {
      ElMessage.info("取消删除")
    }
  }
}

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadActivityDetail()
    loadActivityRemarkList(1)
  }
})

// 组件挂载时执行
onMounted(() => {
  loadActivityDetail()
  loadActivityRemarkList(1)
})
</script>

<style scoped>
.activity-detail-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.detail-card,
.remark-card,
.records-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.info-section {
  margin-bottom: 30px;
}

.info-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin: 0 0 20px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
  position: relative;
}

.section-title::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 60px;
  height: 2px;
  background-color: #409eff;
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
  padding: 12px;
  background-color: #fff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.info-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.info-item .label {
  font-weight: 600;
  color: #606266;
  white-space: nowrap;
  margin-right: 12px;
  min-width: 80px;
}

.info-item .value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.info-item .value.primary {
  color: #409eff;
  font-weight: 600;
}

.info-item.description {
  flex-direction: column;
  align-items: flex-start;
}

.description-content {
  margin-top: 8px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  width: 100%;
  line-height: 1.6;
  color: #303133;
}

.remark-form {
  padding: 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .activity-detail-container {
    padding: 10px;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .info-item .label {
    margin-bottom: 8px;
    min-width: auto;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
}

/* Element Plus 组件样式覆盖 */
:deep(.el-card__header) {
  background-color: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-table) {
  border-radius: 6px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  color: #303133;
  font-weight: 600;
}

:deep(.el-table tr:hover > td) {
  background-color: #f0f9ff;
}

:deep(.el-pagination) {
  justify-content: center;
}

:deep(.el-form-item__label) {
  color: #606266;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-button) {
  border-radius: 6px;
}
</style>