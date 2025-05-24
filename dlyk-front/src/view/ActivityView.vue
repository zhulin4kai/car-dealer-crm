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

      <el-form-item label="活动预算" prop="cost">
        <el-input v-model="activityQuery.cost" placeholder="请输入活动预算" clearable />
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

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  getActivityList, 
  getOwnerList, 
  batchDeleteActivities 
} from '../api/activity';

// Reactive state
const activityQuery = reactive({});
const activityList = ref([{ ownerDO: {} }]);
const pageSize = ref(0);
const total = ref(0);
const ownerOptions = ref([{}]);
const currentPage = ref(1);
const selectedActivityIds = ref([]);

// Validation rules
const activityRules = {
  cost: [
    { pattern: /^[0-9]+(\.[0-9]{2})?$/, message: '活动预算必须是整数或者两位小数', trigger: 'blur' }
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
};

// Navigation
const add = () => {
  router.push("/dashboard/activity/add");
};

const edit = (id) => {
  router.push("/dashboard/activity/edit/" + id);
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