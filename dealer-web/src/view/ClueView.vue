<template>
  <el-card class="action-card">
    <el-button type="primary" class="btn" @click="addClue" v-hasPermission="'clue:add'">录入线索</el-button>
    <el-button type="success" class="btn" @click="importExcel" v-hasPermission="'clue:import'">导入线索(Excel)</el-button>
    <el-button type="danger" class="btn" @click="handleBatchDelete" v-hasPermission="'clue:delete'">批量删除</el-button>
  </el-card>
  <el-card class="table-card">    <el-table
        :data="clueList"
        style="width: 100%; min-width: 1400px;"
        @selection-change="handleSelectionChange">      <el-table-column type="selection" width="55"/>
      <el-table-column 
        type="index" 
        label="序号" 
        width="60" 
        :index="startIndex"
        align="center"
      />      <el-table-column property="ownerDO.name" label="负责人" width="90" show-overflow-tooltip />      
      <el-table-column property="activityDO.name" label="所属活动" min-width="120" show-overflow-tooltip align="center"/>
      <el-table-column label="姓名" width="90" show-overflow-tooltip>
        <template #default="scope">
          <a href="javascript:" @click="view(scope.row.id)" class="name-link">{{ scope.row.fullName }}</a>
        </template>
      </el-table-column>
      <el-table-column property="appellationDO.typeValue" label="称呼" width="70" show-overflow-tooltip />
      <el-table-column property="phone" label="手机" width="120" show-overflow-tooltip />
      <el-table-column property="weixin" label="微信" width="110" show-overflow-tooltip />
      <el-table-column property="needLoanDO.typeValue" label="是否贷款" width="90" show-overflow-tooltip />
      <el-table-column property="intentionStateDO.typeValue" label="意向状态" width="90" show-overflow-tooltip />
      <el-table-column property="intentionProductDO.name" label="意向产品" min-width="120" show-overflow-tooltip align="center" />
      <el-table-column label="线索状态" width="90" align="center">
        <template #default="scope">
          <el-tag 
            :type="scope.row.state === -1 ? 'warning' : 'info'" 
            size="small">
            {{ scope.row.stateDO.typeValue }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column property="sourceDO.typeValue" label="线索来源" width="90" show-overflow-tooltip align="center"/>
      <el-table-column property="nextContactTime" label="下次联系时间" min-width="150" show-overflow-tooltip align="center"/>
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button size="small" type="primary" @click="view(scope.row.id)" v-hasPermission="'clue:view'">详情</el-button>
            <el-button size="small" type="success" @click="edit(scope.row.id)" v-hasPermission="'clue:edit'">编辑</el-button>
            <el-button size="small" type="danger" @click="del(scope.row.id)" v-hasPermission="'clue:delete'">删除</el-button>
          </div>
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
  </p>  <!--导入线索Excel弹窗-->
  <el-dialog 
    v-model="importExcelDialogVisible" 
    title="导入线索Excel" 
    width="55%" 
    center 
    draggable
    :append-to-body="true"
    :destroy-on-close="true">
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

  <!--线索录入/编辑弹窗-->
  <el-dialog 
    v-model="clueDialogVisible" 
    :title="dialogTitle" 
    width="55%" 
    center 
    draggable
    :append-to-body="true"
    :destroy-on-close="true"
    @close="handleDialogClose">
    <el-form
        ref="clueRefForm"
        :model="clueQuery"
        :rules="clueRules"
        label-width="100px"
        style="max-width: 95%;">

      <el-form-item label="负责人">
        <el-select
            v-model="clueQuery.ownerId"
            placeholder="请选择负责人"
            style="width: 100%"
            clearable
            disabled>
          <el-option
              v-for="item in ownerOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="所属活动">
        <el-select
            v-model="clueQuery.activityId"
            placeholder="请选择所属活动"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in activityOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="姓名" prop="fullName">
        <el-input v-model="clueQuery.fullName"/>
      </el-form-item>

      <el-form-item label="称呼">
        <el-select
            v-model="clueQuery.appellation"
            placeholder="请选择称呼"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in appellationOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="手机" v-if="clueQuery.id > 0"><!--此时是编辑-->
        <el-input v-model="clueQuery.phone" disabled/>
      </el-form-item>

      <el-form-item label="手机" prop="phone" v-else><!--此时是录入-->
        <el-input v-model="clueQuery.phone"/>
      </el-form-item>

      <el-form-item label="微信">
        <el-input v-model="clueQuery.weixin"/>
      </el-form-item>

      <el-form-item label="QQ" prop="qq">
        <el-input v-model="clueQuery.qq"/>
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="clueQuery.email"/>
      </el-form-item>

      <el-form-item label="年龄" prop="age">
        <el-input v-model="clueQuery.age"/>
      </el-form-item>

      <el-form-item label="职业">
        <el-input v-model="clueQuery.job"/>
      </el-form-item>

      <el-form-item label="年收入" prop="yearIncome">
        <el-input v-model="clueQuery.yearIncome"/>
      </el-form-item>

      <el-form-item label="住址">
        <el-input v-model="clueQuery.address"/>
      </el-form-item>

      <el-form-item label="贷款">
        <el-select
            v-model="clueQuery.needLoan"
            placeholder="请选择是否需要贷款"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in needLoanOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="意向状态">
        <el-select
            v-model="clueQuery.intentionState"
            placeholder="请选择意向状态"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in intentionStateOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="意向产品">
        <el-select
            v-model="clueQuery.intentionProduct"
            placeholder="请选择意向产品"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="线索状态">
        <el-select
            v-model="clueQuery.state"
            placeholder="请选择线索状态"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in clueStateOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="线索来源">
        <el-select
            v-model="clueQuery.source"
            placeholder="请选择线索来源"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in sourceOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>

      <el-form-item label="线索描述" prop="description">
        <el-input
            v-model="clueQuery.description"
            :rows="5"
            type="textarea"
            placeholder="请输入线索描述"/>
      </el-form-item>

      <el-form-item label="下次联系时间">
        <el-date-picker
            v-model="clueQuery.nextContactTime"
            type="datetime"
            style="width: 100%;"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择下次联系时间"/>
      </el-form-item>

    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="clueDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="addClueSubmit">提 交</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { messageConfirm, messageTip } from "../util/util.js";
import { 
  batchDeleteCluesByIds, 
  getCurrentClues, 
  importExcelAPI, 
  delClueById,
  checkPhoneIsExist,
  getLoginInfo,
  getClueDetail,
  addClue as addClueAPI,
  updateClue
} from '../api/clue.js';
import { getOwnerList } from '../api/activity.js';
import { getDictValueList } from "../api/dict.js";
import { ElMessage, ElMessageBox } from 'element-plus';
import router from '../router/router.js';
import { getProductList } from '../api/product.js';
import { getActivityList } from '../api/activity.js';

// 原有的线索列表相关数据
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

const pageSize = ref(10);
const total = ref(0);
const importExcelDialogVisible = ref(false);
const currentPage = ref(1);
const selectedIds = ref([]);
const uploadRef = ref(null);

// 线索录入/编辑相关数据
const clueDialogVisible = ref(false);
const dialogTitle = ref('录入线索');
const clueRefForm = ref(null);
const clueQuery = reactive({});

// 加载动态数据
const activityOptions = ref([{}]);
const productOptions = ref([{}]);
// 加载字典数据
const ownerOptions = ref([{}]);
const appellationOptions = ref([{}]);
const needLoanOptions = ref([{}]);
const intentionStateOptions = ref([{}]);
const clueStateOptions = ref([{}]);
const sourceOptions = ref([{}]);

// 验证手机号有没有录入过，录入过的手机号，是不能再录入的
const checkPhone = (rule, value, callback) => {
  //验证该手机号是否已经录入过，如果录入过了，就不能再录入了
  let phone = value; // value 就是我们input框里面输入的手机号
  if (phone) {
    // 如果是编辑模式且手机号未变化，跳过验证
    if (clueQuery.id > 0 && clueQuery.phone === phone) {
      return callback();
    }
    checkPhoneIsExist(phone).then(resp => {
      if (resp.data.code === 500) {
        //手机号录入过了，不能再录入了
        return callback(new Error('该手机号录入过了，不能再录入.'));
      } else {
        return callback(); //验证通过了，直接调用一下callback()函数
      }
    });
  } else {
    return callback();
  }
};

// 录入线索验证规则
const clueRules = reactive({
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号码格式有误', trigger: 'blur'},
    { validator: checkPhone, trigger: 'blur'}
  ],
  fullName: [
    { min: 2, message: '姓名至少2个汉字', trigger: 'blur'},
    { pattern: /^[\u4e00-\u9fa5]{0,}$/, message: '姓名必须为中文汉字', trigger: 'blur'},
  ],
  qq: [
    { min: 5, message: 'QQ号至少为5位', trigger: 'blur'},
    { pattern: /^\d+$/, message: 'QQ号码必须为数字', trigger: 'blur'},
  ],
  email: [
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式有误', trigger: 'blur'},
  ],
  age: [
    { pattern: /^\d+$/, message: '年龄必须为数字', trigger: 'blur'},
  ],
  yearIncome: [
    { pattern: /^[0-9]+(\.[0-9]{2})?$/, message: '年收入必须是整数或者两位小数', trigger: 'blur'}
  ],
  description: [
    { min: 5, max: 255, message: '线索描述长度为5-255个字符', trigger: 'blur'},
  ],
});

// Computed
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1;
};

// 原有的方法
const getData = (current) => {
  getCurrentClues(current).then(resp => {
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
      importExcelDialogVisible.value = false;
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

const view = (id) => {
  router.push("/dashboard/clue/detail/" + id);
};

const del = (id) => {
  messageConfirm("您确定要删除该数据吗？").then(() => {
    delClueById(id).then(resp => {
      if (resp.data.code === 200) {
        messageTip("删除成功", "success");
        getData(currentPage.value);
      } else {
        messageTip("删除失败，原因：" + resp.data.msg, "error");
      }
    });
  }).catch(() => {
    messageTip("取消删除", "warning");
  });
};

// 新增的线索录入/编辑相关方法
const addClue = async () => {
  dialogTitle.value = '录入线索';
  resetClueForm();
  await loadData();
  loadOwner();
  loadLoginUser();
  clueDialogVisible.value = true;
};

const edit = async (id) => {
  dialogTitle.value = '编辑线索';
  resetClueForm();
  await loadData();
  loadOwner();
  loadClue(id);
  clueDialogVisible.value = true;
};

const resetClueForm = () => {
  Object.keys(clueQuery).forEach(key => {
    delete clueQuery[key];
  });
  if (clueRefForm.value) {
    clueRefForm.value.resetFields();
  }
};

const handleDialogClose = () => {
  resetClueForm();
};

// 加载字典数据
const loadData = () => {
  loadDicValue('appellation');
  loadDicValue('needLoan');
  loadDicValue('intentionState');
  loadDicValue('clueState');
  loadDicValue('source');
  loadActivityAndProduct();
};

const loadActivityAndProduct = async () => {
  const param = {
    startTime: '',
    endTime: '',
  }
  const activityRes = await getActivityList(param);
  if (activityRes.data.code === 200) {
    activityOptions.value = activityRes.data.data.list;
  }
  const productRes = await getProductList({
      page: 1,
      size: 100
  })
  if (productRes.data.code === 200) {
    productOptions.value = productRes.data.data.list;
  }

  // console.log(activityOptions.value);
  // console.log(productOptions.value);
}

const loadDicValue = async (typeCode) => {
  //调用字典值的接口，获取字典值列表
  await getDictValueList({typeCode}).then(resp => {
    if (resp.data.code === 200) {
      if (typeCode === 'appellation') {
        appellationOptions.value = resp.data.data.list;
      } else if (typeCode === 'needLoan') {
        needLoanOptions.value = resp.data.data.list;
      } else if (typeCode === 'intentionState') {
        intentionStateOptions.value = resp.data.data.list;
      } else if (typeCode === 'clueState') {
        clueStateOptions.value = resp.data.data.list;
      } else if (typeCode === 'source') {
        sourceOptions.value = resp.data.data.list;
      } else if (typeCode === 'activity') {
        activityOptions.value = resp.data.data.list;
      } else if (typeCode === 'product') {
        productOptions.value = resp.data.data.list;
      }
    }
  });
  // console.log(appellationOptions.value);
  // console.log(needLoanOptions.value);
  // console.log(intentionStateOptions.value);
  // console.log(clueStateOptions.value);
  // console.log(sourceOptions.value);
  // console.log(activityOptions.value);
  // console.log(productOptions.value);
};

// 加载负责人
const loadOwner = () => {
  getOwnerList().then(resp => {
    if (resp.data.code === 200) {
      ownerOptions.value = resp.data.data;
    }
  });
};

// 加载当前登录用户
const loadLoginUser = () => {
  getLoginInfo().then((resp) => {
    let user = resp.data.data;
    clueQuery.ownerId = user.id;
  });
};

// 加载要编辑的线索数据
const loadClue = (id) => {
  if (id) { //id存在，id有值，id不为空，说明的编辑
    getClueDetail(id).then(resp => {
      if (resp.data.code === 200) {
        Object.assign(clueQuery, resp.data.data);
      }
    });
  }
};

// 提交表单
const addClueSubmit = () => {
  clueRefForm.value.validate((isValid) => {
    if (isValid) {
      let formData = new FormData();
      for (let field in clueQuery) {
        // 编辑模式下排除手机号字段
        if (clueQuery.id > 0 && field === 'phone') {
          continue;
        }
        if (clueQuery[field]) { //clueQuery[field]有值，clueQuery[field]存在，clueQuery[field]不为空
          formData.append(field, clueQuery[field]);
        }
      }
      if (clueQuery.id > 0) {/*编辑*/
        updateClue(formData).then((resp) => { //获取ajax异步请求后的结果
          if (resp.data.code === 200) {
            //编辑成功，提示一下
            messageTip("编辑成功", "success");
            //关闭弹窗
            clueDialogVisible.value = false;
            //重新加载数据
            getData(currentPage.value);
          } else {
            //编辑失败，提示一下
            messageTip("编辑失败", "error");
          }
        });
      } else {
        addClueAPI(formData).then((resp) => {
          if (resp.data.code === 200) {
            //录入成功，提示一下
            messageTip("录入成功", "success");
            //关闭弹窗
            clueDialogVisible.value = false;
            //重新加载数据
            getData(currentPage.value);
          } else {
            //录入失败，提示一下
            messageTip("录入失败", "error");
          }
        });
      }
    }
  });
};

// 生命周期钩子
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

.el-table {
  margin-top: 15px;
}

.fileTip {
  padding-top: 15px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.operation-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 52px;
}

.name-link {
  color: #409eff;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.3s ease;
}

.name-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}

/* 表格样式优化 */
:deep(.el-table) {
  border-radius: 6px;
  overflow: hidden;
  width: 100% !important;
}

:deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  color: #303133;
  font-weight: 600;
  text-align: center;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table tr:hover > td) {
  background-color: #f0f9ff;
}

:deep(.el-table .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

:deep(.el-dialog__body) {
  max-height: 60vh;
  overflow-y: auto;
}

/* 分页样式 */
.el-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 表格固定列阴影优化 */
:deep(.el-table__fixed-right) {
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

/* 响应式处理 */
@media (max-width: 1200px) {
  .operation-buttons {
    flex-direction: column;
    gap: 4px;
  }
  
  .operation-buttons .el-button {
    min-width: 50px;
    font-size: 12px;
    padding: 4px 8px;
  }
}
</style>