<template>
  <div class="clue-detail-container">
    <!-- 线索详情卡片 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">线索详情</span>
          <div class="header-actions">
            <el-button type="success" @click="convertCustomer" v-if="clueDetail.state !== -1" size="small">
              转换客户
            </el-button>
            <el-button type="info" plain @click="handleGoBack" size="small">
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
              <span class="label">负责人：</span>
              <span class="value">{{ clueDetail.ownerDO?.name || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">所属活动：</span>
              <span class="value">{{ clueDetail.activityDO?.name || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">姓名：</span>
              <span class="value primary">{{ clueDetail.fullName || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">称呼：</span>
              <span class="value">{{ clueDetail.appellationDO?.typeValue || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">手机：</span>
              <span class="value primary">{{ clueDetail.phone || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">年龄：</span>
              <span class="value">{{ clueDetail.age || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">微信：</span>
              <span class="value">{{ clueDetail.weixin || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">QQ：</span>
              <span class="value">{{ clueDetail.qq || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">邮箱：</span>
              <span class="value">{{ clueDetail.email || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">职业：</span>
              <span class="value">{{ clueDetail.job || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">年收入：</span>
              <span class="value">{{ clueDetail.yearIncome || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">是否贷款：</span>
              <span class="value">{{ clueDetail.needLoanDO?.typeValue || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <span class="label">住址：</span>
              <span class="value">{{ clueDetail.address || '暂无' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <span class="label">下次联系时间：</span>
              <span class="value">{{ clueDetail.nextContactTime || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 线索状态区域 -->
      <div class="info-section">
        <h4 class="section-title">线索状态</h4>
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">意向状态：</span>
              <el-tag type="primary" size="small">{{ clueDetail.intentionStateDO?.typeValue || '暂无' }}</el-tag>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">意向产品：</span>
              <el-tag type="success" size="small">{{ clueDetail.intentionProductDO?.name || '暂无' }}</el-tag>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">线索状态：</span>
              <el-tag :type="clueDetail.state === -1 ? 'warning' : 'info'" size="small">
                {{ clueDetail.stateDO?.typeValue || '暂无' }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="24">
            <div class="info-item">
              <span class="label">线索来源：</span>
              <span class="value">{{ clueDetail.sourceDO?.typeValue || '暂无' }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24" v-if="clueDetail.description">
          <el-col :span="24">
            <div class="info-item description">
              <span class="label">线索描述：</span>
              <div class="description-content">{{ clueDetail.description }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 跟踪记录表单卡片 -->
    <el-card class="remark-card" shadow="hover">
      <template #header>
        <span class="header-title">添加跟踪记录</span>
      </template>
      
      <el-form
          ref="clueRemarkRefForm"
          :model="clueRemark"
          label-width="100px"
          :rules="clueRemarkRules"
          class="remark-form">
        
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="跟踪方式" prop="noteWay">
              <el-select
                  v-model="clueRemark.noteWay"
                  placeholder="请选择跟踪方式"
                  style="width: 100%"
                  @click="loadDicValue('noteWay')"
                  clearable>
                <el-option
                    v-for="item in noteWayOptions"
                    :key="item.id"
                    :label="item.typeValue"
                    :value="item.id"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="跟踪记录" prop="noteContent">
          <el-input
              v-model="clueRemark.noteContent"
              :rows="6"
              type="textarea"
              placeholder="请输入详细的跟踪记录内容..."/>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="clueRemarkSubmit" :loading="submitting">
            <el-icon><EditPen /></el-icon>
            提交记录
          </el-button>
          <el-button @click="resetRemarkForm">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <!-- 跟踪记录列表卡片 -->
    <el-card class="records-card" shadow="hover">
      <template #header>
        <span class="header-title">跟踪记录列表</span>
      </template>
      
      <el-table
          :data="clueRemarkList"
          style="width: 100%"
          stripe
          border>
        <el-table-column type="index" label="序号" width="60" align="center"/>
        <el-table-column prop="noteWayDO.typeValue" label="跟踪方式" width="120" align="center">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.noteWayDO?.typeValue || '暂无' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="noteContent" label="跟踪内容" min-width="200" show-overflow-tooltip/>
        <el-table-column property="createTime" label="跟踪时间" width="160" align="center"/>
        <el-table-column property="createByDO.name" label="跟踪人" width="100" align="center"/>
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
    </el-card>
  </div>

  <!--线索转换为客户的弹窗（对话框）-->
  <el-dialog 
    v-model="convertCustomerDialogVisible" 
    title="线索转换客户" 
    width="600px" 
    center
    draggable
    :append-to-body="true"
    :destroy-on-close="true">
    <el-form ref="convertCustomerRefForm" :model="customerQuery" label-width="120px" :rules="convertCustomerRules">
      <el-form-item label="意向产品" prop="product">
        <el-select 
          v-model="customerQuery.product" 
          placeholder="请选择意向产品" 
          style="width: 100%;" 
          @click="ProductList()"
          clearable>
          <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="客户描述" prop="description">
        <el-input
            v-model="customerQuery.description"
            :rows="6"
            type="textarea"
            placeholder="请输入客户描述"/>
      </el-form-item>
      <el-form-item label="下次跟踪时间" prop="nextContactTime">
        <el-date-picker
            v-model="customerQuery.nextContactTime"
            type="datetime"
            style="width: 100%;"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择下次跟踪时间"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="convertCustomerDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="convertCustomerSubmit" :loading="converting">确认转换</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { messageTip } from "../util/util.js"
import { getClueDetail, addClueRemark, getClueRemarkList, convertClueToCustomer } from "../api/clue.js"
import { getDictValueList } from '../api/dict.js'
import { getProductList } from '../api/product.js'
// Router and route
const route = useRoute()
const router = useRouter()

// Template refs
const clueRemarkRefForm = ref()
const convertCustomerRefForm = ref()

// 线索详情对象，初始值是空
const clueDetail = ref({
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {}
})

// 线索跟踪记录对象，初始值是空
const clueRemark = ref({})

// 跟踪方式的下拉选项，初始值是空数组
const noteWayOptions = ref([])

// 线索跟踪记录列表，初始值是空数组
const clueRemarkList = ref([])

// 分页时每页显示多少条数据
const pageSize = ref(0)

// 分页总共查询出多少条数据
const total = ref(0)

// 定义转换客户的弹窗是否弹出来，默认是false不弹出来，true就弹出来
const convertCustomerDialogVisible = ref(false)

// 线索转换为客户的form表单对象，初始值是空
const customerQuery = ref({})

// 定义线索转换为客户的验证规则
const convertCustomerRules = reactive({
  product: [
    { required: true, message: '请选择意向产品', trigger: ['blur', 'change'] }
  ],
  description: [
    { required: true, message: '客户描述不能为空', trigger: 'blur' },
    { min: 5, max: 255, message: '客户描述长度为5-255个字符', trigger: 'blur' }
  ],
  nextContactTime: [
    { required: true, message: '请选择下次联系时间', trigger: 'blur' }
  ]
})

// 线索跟踪记录表单验证规则
const clueRemarkRules = reactive({
  noteContent: [
    { required: true, message: '跟踪记录内容不能为空', trigger: 'blur' }
  ],
  noteWay: [
    { required: true, message: '请选择跟踪方式', trigger: 'change' }
  ]
})

// 意向产品的下拉选项，初始值是空数组
const productOptions = ref([])

// 加载状态
const submitting = ref(false)
const converting = ref(false)

// 重置跟踪记录表单
const resetRemarkForm = () => {
  clueRemark.value = {}
  if (clueRemarkRefForm.value) {
    clueRemarkRefForm.value.resetFields()
  }
}

// 加载线索详情
const loadClueDetail = async () => {
  const id = route.params.id
  try {
    const resp = await getClueDetail(id)
    if (resp.data.code === 200) {
      clueDetail.value = resp.data.data
    }
  } catch (error) {
    console.error('加载线索详情失败:', error)
    messageTip("加载线索详情失败", "error")
  }
}

// 提交线索跟踪记录
const clueRemarkSubmit = async () => {
  if (!clueRemarkRefForm.value) return
  
  submitting.value = true
  try {
    const isValid = await clueRemarkRefForm.value.validate()
    if (isValid) {
      const resp = await addClueRemark(
        clueDetail.value.id, 
        clueRemark.value.noteContent, 
        clueRemark.value.noteWay
      )
      if (resp.data.code === 200) {
        messageTip("提交成功", "success")
        // 重新加载数据而不是刷新整个页面
        await loadClueRemarkList(1)
        // 清空表单
        resetRemarkForm()
      } else {
        messageTip("提交失败", "error")
      }
    }
  } catch (error) {
    console.error('提交跟踪记录失败:', error)
    messageTip("提交失败", "error")
  } finally {
    submitting.value = false
  }
}

// 加载字典数据
const loadDicValue = async (typeCode) => {
  try {
    const resp = await getDictValueList({typeCode})
    if (resp.data.code === 200) {
      if (typeCode === 'noteWay') {
        noteWayOptions.value = resp.data.data.list
      }
    }
  } catch (error) {
    console.error('加载字典数据失败:', error)
    messageTip("加载字典数据失败", "error")
  }
}

// 查询线索跟踪记录列表数据
const loadClueRemarkList = async (current) => {
  try {
    const resp = await getClueRemarkList(current, route.params.id)
    console.log(resp)
    if (resp.data.code === 200) {
      clueRemarkList.value = resp.data.data.list || []
      pageSize.value = resp.data.data.pageSize || 10
      total.value = resp.data.data.total || 0
    }
  } catch (error) {
    console.error('加载跟踪记录列表失败:', error)
    messageTip("加载跟踪记录列表失败", "error")
  }
}

// 分页函数(current这个参数是ele-plus组件传过来，就是传的当前页)
const toPage = (current) => {
  loadClueRemarkList(current)
}

// 转换客户
const convertCustomer = () => {
  convertCustomerDialogVisible.value = true
}

// 线索转换客户
const convertCustomerSubmit = async () => {
  if (!convertCustomerRefForm.value) return
  
  converting.value = true
  try {
    const isValid = await convertCustomerRefForm.value.validate()
    if (isValid) {
      const resp = await convertClueToCustomer(
        clueDetail.value.id,
        customerQuery.value.product,
        customerQuery.value.description,
        customerQuery.value.nextContactTime
      )
      if (resp.data.code === 200) {
        messageTip("转换成功", "success")
        convertCustomerDialogVisible.value = false
        // 重新加载线索详情以更新状态
        await loadClueDetail()
      } else {
        messageTip("转换失败", "error")
      }
    }
  } catch (error) {
    console.error('转换客户失败:', error)
    messageTip("转换失败", "error")
  } finally {
    converting.value = false
  }
}

const ProductList = async () => {
  try {
    const resp = await getProductList()
    if (resp.data.code === 200) {
      productOptions.value = resp.data.data.list || []
    }
  } catch (error) {
    console.error('加载意向产品列表失败:', error)
    messageTip("加载意向产品列表失败", "error")
  }
}

// 编辑跟踪记录 (待实现)
const edit = (id) => {
  // TODO: 实现编辑功能
  console.log('编辑跟踪记录:', id)
  messageTip("编辑功能待实现", "info")
}

// 删除跟踪记录 (待实现)
const del = (id) => {
  // TODO: 实现删除功能
  console.log('删除跟踪记录:', id)
  messageTip("删除功能待实现", "info")
}

// 使用 router 返回上一页
const handleGoBack = () => {
  router.go(-1)
}

// 组件挂载时执行
onMounted(() => {
  loadClueDetail()
  loadClueRemarkList(1)
})
</script>

<style scoped>
.clue-detail-container {
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .clue-detail-container {
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

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  background-color: #f8f9fa;
  border-radius: 12px 12px 0 0;
  padding: 20px 24px;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>