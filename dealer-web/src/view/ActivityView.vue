<template>
  <el-card class="action-card">
    <el-form :inline="true" :model="activityQuery" :rules="activityRules">
      <el-form-item label="负责人">
        <el-select
            v-model="activityQuery.ownerId"
            placeholder="请选择负责人"
            @click="loadOwner"
            clearable
            style="width: 150px;"
            >
          <el-option
              v-for="item in ownerOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="活动名称">
        <el-input v-model="activityQuery.name" placeholder="请输入活动名称" clearable />
      </el-form-item>

      <el-form-item label="活动时间">
        <el-date-picker
            v-model="activityQuery.activityTime"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"/>
      </el-form-item>

      <el-form-item label="活动最低预算" prop="cost">
        <el-input v-model="activityQuery.cost" placeholder="请输入活动最低预算" clearable />
      </el-form-item>

      <el-form-item label="创建时间">
        <el-date-picker
            v-model="activityQuery.createTime"
            type="datetime"
            placeholder="请选择创建时间"
            value-format="YYYY-MM-DD HH:mm:ss"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="onSearch">搜 索</el-button>
        <el-button type="primary" plain @click="onReset">重 置</el-button>
      </el-form-item>
    </el-form>

    <el-button type="primary" @click="add">录入市场活动</el-button>
    <el-button type="danger" @click="batchDel">批量删除</el-button>
  </el-card>

  <el-card class="table-card">
    <el-table
        :data="activityList"
        style="width: 100%"
        @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column type="index" label="序号" width="80" :index="startIndex" />
      <el-table-column prop="ownerDO.name" label="负责人" width="100"/>
      <el-table-column property="name" label="活动名称" width="150"/>
      <el-table-column property="startTime" label="开始时间" show-overflow-tooltip/>
      <el-table-column property="endTime" label="结束时间" show-overflow-tooltip/>
      <el-table-column property="cost" label="活动预算" width="100"/>
      <el-table-column property="createTime" label="创建时间" show-overflow-tooltip/>
      <el-table-column label="操作" show-overflow-tooltip>
        <template #default="scope">
          <el-button type="primary" @click="view(scope.row.id)">详情</el-button>
          <el-button type="success" @click="edit(scope.row.id)">编辑</el-button>
          <el-button type="danger" @click="del(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="toPage"
      @next-click="toPage"
      @current-change="toPage"/>

  <!-- 活动录入/编辑对话框 -->
  <el-dialog 
      v-model="activityDialogVisible" 
      :title="dialogTitle" 
      width="30%" 
      center 
      draggable
      @close="resetActivityForm">
    <el-form ref="activityFormRef" :model="activityForm" label-width="110px" :rules="activityFormRules">
      <el-form-item label="负责人" prop="ownerId">
        <el-select v-model="activityForm.ownerId" placeholder="请选择" style="width: 100%;">
          <el-option
              v-for="item in ownerOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="活动名称" prop="name">
        <el-input v-model="activityForm.name" placeholder="请输入活动名称" />
      </el-form-item>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
            v-model="activityForm.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss" 
            style="width:100%;"/>
      </el-form-item>

      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
            v-model="activityForm.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss" 
            style="width:100%;"/>
      </el-form-item>

      <el-form-item label="活动预算" prop="cost">
        <el-input v-model="activityForm.cost" placeholder="请输入活动预算" />
      </el-form-item>

      <el-form-item label="活动描述" prop="description">
        <el-input
            v-model="activityForm.description"
            :rows="6"
            type="textarea"
            placeholder="请输入活动描述"/>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="activityDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitActivityForm">提 交</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { 
  getActivityList, 
  getOwnerList, 
  batchDeleteActivities,
  deleteActivity,
  createActivity,
  updateActivity,
  getActivityById
} from '../api/activity';

// Reactive state
const router = useRouter();
const activityQuery = reactive({});
const activityList = ref([{ ownerDO: {} }]);
const pageSize = ref(10);
const total = ref(0);
const ownerOptions = ref([{}]);
const currentPage = ref(1);
const selectedActivityIds = ref([]);

// Dialog state
const activityDialogVisible = ref(false);
const dialogTitle = ref('录入市场活动');
const activityForm = reactive({});
const activityFormRef = ref(null);
const isEditing = ref(false);

// Validation rules
const activityRules = {
  cost: [
    { pattern: /^[0-9]+(\.[0-9]{1,2})?$/, message: '活动预算必须是整数或最多两位小数', trigger: 'blur' }
  ]
};

// Activity form validation rules
const activityFormRules = {
  ownerId: [
    { required: true, message: '请选择负责人', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'blur' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'blur' }
  ],
  cost: [
    { required: true, message: '请输入活动预算', trigger: 'blur' },
    { pattern: /^[0-9]+(\.[0-9]{1,2})?$/, message: '活动预算必须是整数或最多两位小数', trigger: 'blur'}
  ],
  description: [
    { required: true, message: '请输入活动描述', trigger: 'blur' },
    { min: 5, max: 255, message: '活动描述长度为5-255个字符', trigger: 'blur' }
  ]
};

// Computed property for startIndex
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1;
};

// Fetch data
const getData = async (current) => {
  let startTime = '';
  let endTime = '';
  for (let key in activityQuery.activityTime) {
    if (key === '0') startTime = activityQuery.activityTime[key];
    if (key === '1') endTime = activityQuery.activityTime[key];
  }

  const params = {
    current: current,
    ownerId: activityQuery.ownerId,
    name: activityQuery.name,
    startTime: startTime,
    endTime: endTime,
    cost: activityQuery.cost,
    createTime: activityQuery.createTime
  };

  try {
    const res = await getActivityList(params);
    if (res.data.code === 200) {
      activityList.value = res.data.data.list;
      pageSize.value = res.data.data.pageSize;
      total.value = res.data.data.total;
    }
  } catch (error) {
    console.error('获取活动列表失败:', error);
  }
  currentPage.value = current;
};

// Pagination
const toPage = (current) => {
  getData(current);
};

// Load owner
const loadOwner = async () => {
  try {
    const res = await getOwnerList();
    if (res.data.code === 200) {
      ownerOptions.value = res.data.data;
    }
  } catch (error) {
    console.error('获取负责人列表失败:', error);
  }
};

// Search
const onSearch = () => {
  getData(1);
};

// Reset search
const onReset = () => {
  Object.keys(activityQuery).forEach(key => delete activityQuery[key]);
  getData(1);
};

// Navigation
const add = async () => {
  dialogTitle.value = '录入市场活动';
  isEditing.value = false;
  resetActivityForm();
  await loadOwner();
  activityDialogVisible.value = true;
};

const edit = async (id) => {
  dialogTitle.value = '编辑市场活动';
  isEditing.value = true;
  await loadOwner();
  await loadActivityForEdit(id);
  activityDialogVisible.value = true;
};

const view = (id) => {
  router.push("/dashboard/activity/" + id);
};

// Handle selection change
const handleSelectionChange = (selection) => {
  selectedActivityIds.value = selection.map(item => item.id);
};

// Batch delete
const batchDel = async () => {
  if (selectedActivityIds.value.length === 0) {
    ElMessage.warning('请至少选择一条记录');
    return;
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的活动吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res = await batchDeleteActivities(selectedActivityIds.value);
    if (res.data.code === 200) {
      ElMessage.success('删除成功');
      getData(1);
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('请求失败，请检查网络或重试');
    } else {
      ElMessage.info('已取消删除');
    }
  }
};

// Single delete
const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res = await deleteActivity(id);
    if (res.data.code === 200) {
      ElMessage.success('删除成功');
      getData(currentPage.value);
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('请求失败，请检查网络或重试');
    } else {
      ElMessage.info('已取消删除');
    }
  }
};

// Dialog form methods
const resetActivityForm = () => {
  Object.keys(activityForm).forEach(key => delete activityForm[key]);
  if (activityFormRef.value) {
    activityFormRef.value.clearValidate();
  }
};

const loadActivityForEdit = async (id) => {
  try {
    const res = await getActivityById(id);
    if (res.data.code === 200) {
      Object.assign(activityForm, res.data.data);
    }
  } catch (error) {
    console.error('获取活动详情失败:', error);
    ElMessage.error('获取活动详情失败');
  }
};

const submitActivityForm = async () => {
  if (!activityFormRef.value) return;
  
  try {
    const valid = await activityFormRef.value.validate();
    if (!valid) return;

    const formData = new FormData();
    for (let field in activityForm) {
      if (activityForm[field]) {
        formData.append(field, activityForm[field]);
      }
    }

    let res;
    if (isEditing.value) {
      res = await updateActivity(formData);
      if (res.data.code === 200) {
        ElMessage.success('编辑成功');
      } else {
        ElMessage.error('编辑失败');
      }
    } else {
      res = await createActivity(formData);
      if (res.data.code === 200) {
        ElMessage.success('提交成功');
      } else {
        ElMessage.error('提交失败');
      }
    }

    if (res.data.code === 200) {
      activityDialogVisible.value = false;
      getData(1);
    }
  } catch (error) {
    console.error('提交失败:', error);
    ElMessage.error('提交失败，请检查网络或重试');
  }
};

// Lifecycle hook
onMounted(() => {
  getData(1);
});
</script>

<style scoped>
.action-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
.el-form {
  margin-bottom: 20px;
}
.el-table {
  margin-top: 12px;
}
.el-pagination {
  margin-top: 12px;
}
</style>