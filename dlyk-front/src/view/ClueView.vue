<template>
  <el-card class="action-card">
    <el-button type="primary" class="btn" @click="addClue" v-hasPermission="'clue:add'">录入线索</el-button>
    <el-button type="success" class="btn" @click="importExcel" v-hasPermission="'clue:import'">导入线索(Excel)</el-button>
    <el-button type="danger" class="btn" @click="handleBatchDelete" v-hasPermission="'clue:delete'">批量删除</el-button>
  </el-card>

  <el-card class="table-card">
    <el-table
        :data="clueList"
        style="width: 100%"
        @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50"/>
      <el-table-column 
        type="index" 
        label="序号" 
        width="80" 
        :index="startIndex"
      />
      <el-table-column property="ownerDO.name" label="负责人" show-overflow-tooltip />
      <el-table-column property="activityDO.name" label="所属活动" show-overflow-tooltip />
      <el-table-column label="姓名" show-overflow-tooltip>
        <template #default="scope">
          <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.fullName }}</a>
        </template>
      </el-table-column>
      <el-table-column property="appellationDO.typeValue" label="称呼" show-overflow-tooltip />
      <el-table-column property="phone" label="手机" show-overflow-tooltip />
      <el-table-column property="weixin" label="微信" show-overflow-tooltip />
      <el-table-column property="needLoanDO.typeValue" label="是否贷款" show-overflow-tooltip />
      <el-table-column property="intentionStateDO.typeValue" label="意向状态" show-overflow-tooltip />
      <el-table-column property="intentionProductDO.name" label="意向产品" show-overflow-tooltip />
      <el-table-column label="线索状态">
        <template #default="scope">
          <span style="background: lightgoldenrodyellow" v-if="scope.row.state === -1"> {{ scope.row.stateDO.typeValue }} </span>
          <span v-else> {{ scope.row.stateDO.typeValue }} </span>
        </template>
      </el-table-column>
      <el-table-column property="sourceDO.typeValue" label="线索来源" show-overflow-tooltip />
      <el-table-column property="nextContactTime" label="下次联系时间" show-overflow-tooltip />
      <el-table-column label="操作" show-overflow-tooltip>
        <template #default="scope">
          <el-button type="primary" @click="view(scope.row.id)" v-hasPermission="'clue:view'">详情</el-button>
          <el-button type="success" @click="edit(scope.row.id)" v-hasPermission="'clue:edit'">编辑</el-button>
          <el-button type="danger" @click="del(scope.row.id)" v-hasPermission="'clue:delete'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="toPage"
        @next-click="toPage"
        @current-change="toPage"/>
  </p>

  <!--活动备注记录的弹窗-->
  <el-dialog v-model="importExcelDialogVisible" title="导入线索Excel" width="55%" center draggable>
    <el-upload
        ref="uploadRef"
        method="post"
        :http-request="uploadFile"
        :auto-upload="false">

      <template #trigger>
        <el-button type="primary">选择Excel文件</el-button>
      </template>
      仅支持后缀名为.xls或.xlsx的文件

      <template #tip>
        <div class="fileTip">
          重要提示：
          <ul>
            <li>上传仅支持后缀名为.xls或.xlsx的文件；</li>
            <li>给定Excel文件的第一行将视为字段名；</li>
            <li>请确认您的文件大小不超过50MB；</li>
            <li>日期值以文本形式保存，必须符合yyyy-MM-dd格式；</li>
            <li>日期时间以文本形式保存，必须符合yyyy-MM-dd HH:mm:ss的格式；</li>
          </ul>
        </div>
      </template>
    </el-upload>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="importExcelDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="submitExcel">导 入</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script setup>
import { ref } from 'vue';
import { messageConfirm, messageTip } from "../util/util.js";
import { batchDeleteCluesByIds, getCurrentClues, importExcelAPI } from '../api/clue.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import router from '../router/router.js';

const clueList = ref([{
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {}
}]);

const pageSize = ref(0);
const total = ref(0);
const importExcelDialogVisible = ref(false);
const currentPage = ref(1);
const selectedIds = ref([]);

// Computed
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1;
};

// Methods
const getData = (current) => {
  getCurrentClues(current).then(resp => {
    console.log(resp);
    if (resp.data.code === 200) {
      clueList.value = resp.data.data.list;
      pageSize.value = resp.data.data.pageSize;
      total.value = resp.data.data.total;
    }
  });
  currentPage.value = current;
};

const toPage = (current) => {
  getData(current);
};

const importExcel = () => {
  importExcelDialogVisible.value = true;
};

const uploadFile = (param) => {
  console.log(param);
  let fileObj = param.file;
  let formData = new FormData();
  formData.append('file', fileObj);
  importExcelAPI(formData).then(resp => {
    if (resp.data.code === 200) {
      messageTip("导入成功", "success");
      // Clear uploaded files
      uploadRef.value.clearFiles();
      // Reload page
      getData(currentPage.value);
    } else {
      messageTip("导入失败", "error");
    }
  });
};

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id);
};

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请至少选择一条记录');
    return;
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的线索吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const res = await batchDeleteCluesByIds(selectedIds.value);
    console.log(res);
    if (res.data.code === 200) {
      ElMessage.success('批量删除成功');
      getData(currentPage.value);
    } else {
      ElMessage.error('批量删除失败');
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

const submitExcel = () => {
  uploadRef.value.submit();
};

const addClue = () => {
  router.push("/dashboard/clue/add");
};

const edit = (id) => {
  router.push("/dashboard/clue/edit/" + id);
};

const view = (id) => {
  console.log(id)
  router.push("/dashboard/clue/detail/" + id);
};

const del = (id) => {
  messageConfirm("您确定要删除该数据吗？").then(() => {
    delClueById(id).then(resp => {
      if (resp.data.code === 200) {
        messageTip("删除成功", "success");
        reload();
      } else {
        messageTip("删除失败，原因：" + resp.data.msg, "error");
      }
    });
  }).catch(() => {
    messageTip("取消删除", "warning");
  });
};

// Mounted
getData(1);
</script>

<style scoped>
.action-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 15px;
}
.fileTip {
  padding-top: 15px;
}
</style>