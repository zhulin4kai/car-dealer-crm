<template>
  <div>
    <el-card class="action-card">
      <el-button type="primary" @click="add" v-hasPermission="'user:add'">添加用户</el-button>
      <el-button type="danger" @click="batchDel" v-hasPermission="'user:delete'" :disabled="!userIdArray.length">批量删除</el-button>
    </el-card>

    <el-card class="table-card">
      <el-table :data="userList" style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column 
          type="index" 
          label="序号" 
          width="60" 
          :index="startIndex" 
        />
        <el-table-column prop="loginAct" label="账号" />
        <el-table-column property="name" label="姓名" show-overflow-tooltip />
        <el-table-column property="phone" label="手机" show-overflow-tooltip />
        <el-table-column property="email" label="邮箱" show-overflow-tooltip />
        <el-table-column property="createTime" label="创建时间" show-overflow-tooltip />
        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <el-button type="primary" @click="view(scope.row.id)" v-hasPermission="'user:view'">详情</el-button>
            <el-button type="success" @click="edit(scope.row.id)" v-hasPermission="'user:edit'">编辑</el-button>
            <el-button type="danger" @click="del(scope.row.id)" v-hasPermission="'user:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination background layout="prev, pager, next" :page-size="pageSize" :total="total" @prev-click="toPage"
      @next-click="toPage" @current-change="toPage" />

    <!--新增用户的弹窗(对话框)-->
    <el-dialog v-model="userDialogVisible" :title="userQuery.id > 0 ? '编辑用户' : '新增用户'" width="20%" center draggable>

      <el-form ref="userRefForm" :model="userQuery" label-width="110px" :rules="userRules">
        <el-form-item label="账号" prop="loginAct">
          <el-input v-model="userQuery.loginAct" />
        </el-form-item>

        <el-form-item label="密码" prop="loginPwd" v-if="userQuery.id > 0"><!--编辑-->
          <el-input type="password" v-model="userQuery.loginPwd" placeholder="******" />
        </el-form-item>

        <el-form-item label="密码" prop="loginPwd" v-else><!--新增-->
          <el-input type="password" v-model="userQuery.loginPwd" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="userQuery.name" />
        </el-form-item>

        <el-form-item label="手机" prop="phone">
          <el-input v-model="userQuery.phone" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userQuery.email" />
        </el-form-item>

        <el-form-item label="账号未过期" prop="accountNoExpired">
          <el-select v-model="userQuery.accountNoExpired" placeholder="请选择">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="密码未过期" prop="credentialsNoExpired">
          <el-select v-model="userQuery.credentialsNoExpired" placeholder="请选择">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="账号未锁定" prop="accountNoLocked">
          <el-select v-model="userQuery.accountNoLocked" placeholder="请选择">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="账号是否启用" prop="accountEnabled">
          <el-select v-model="userQuery.accountEnabled" placeholder="请选择">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">关 闭</el-button>
          <el-button type="primary" @click="userSubmit">提 交</el-button>
        </span>
      </template>
    </el-dialog>    <!--用户详情的弹窗(对话框)-->
    <el-dialog v-model="userDetailDialogVisible" title="用户详情" width="20%" center draggable>
      <el-form :model="userDetail" label-width="120px">
        <el-form-item label="ID">
          <div class="detail">&nbsp;{{userDetail.id}}</div>
        </el-form-item>

        <el-form-item label="账号">
          <div class="detail">&nbsp;{{userDetail.loginAct}}</div>
        </el-form-item>

        <el-form-item label="密码">
          <div class="detail">&nbsp;******</div>
        </el-form-item>

        <el-form-item label="姓名">
          <div class="detail">&nbsp;{{userDetail.name}}</div>
        </el-form-item>

        <el-form-item label="手机">
          <div class="detail">&nbsp;{{userDetail.phone}}</div>
        </el-form-item>

        <el-form-item label="邮箱">
          <div class="detail">&nbsp;{{userDetail.email}}</div>
        </el-form-item>

        <el-form-item label="账号未过期">
          <div class="detail">&nbsp;{{userDetail.accountNoExpired === 1 ? '是' : '否'}}</div>
        </el-form-item>

        <el-form-item label="密码未过期">
          <div class="detail">&nbsp;{{userDetail.credentialsNoExpired === 1 ? '是' : '否'}}</div>
        </el-form-item>

        <el-form-item label="账号未锁定">
          <div class="detail">&nbsp;{{userDetail.accountNoLocked === 1 ? '是' : '否'}}</div>
        </el-form-item>

        <el-form-item label="账号是否启用">
          <div class="detail">&nbsp;{{userDetail.accountEnabled === 1 ? '是' : '否'}}</div>
        </el-form-item>

        <el-form-item label="创建时间">
          <div class="detail">&nbsp;{{userDetail.createTime}}</div>
        </el-form-item>

        <el-form-item label="创建人">
          <div class="detail">&nbsp;{{userDetail.createByDO?.name}}</div>
        </el-form-item>

        <el-form-item label="编辑时间">
          <div class="detail">&nbsp;{{userDetail.editTime}}</div>
        </el-form-item>

        <el-form-item label="编辑人">
          <div class="detail">&nbsp;{{userDetail.editByDO?.name}}</div>
        </el-form-item>

        <el-form-item label="最近登录时间">
          <div class="detail">&nbsp;{{userDetail.lastLoginTime}}</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="success" @click="userDetailDialogVisible = false">返 回</el-button>
        </span>
      </template>
    </el-dialog>
  </div>

</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { doDelete, doGet, doPost, doPut } from "../http/httpRequest.js"
import { messageConfirm, messageTip } from "../util/util.js"
import { useRouter } from 'vue-router'

// // 注入父级页面提供的属性
// const age = inject('age')
// const arr = inject('arr')
// const content = inject('content')
// const reload = inject('reload')
// const user = inject('user')

// 响应式数据
const userList = ref([{}])
const pageSize = ref(10)
const total = ref(0)
const userDialogVisible = ref(false)
const userQuery = ref({})
const userIdArray = ref([])
const userRefForm = ref(null)
const currentPage = ref(1)
const router = useRouter()

// 用户详情相关数据
const userDetailDialogVisible = ref(false)
const userDetail = ref({
  createByDO: {},
  editByDO: {}
})

// 表单验证规则
const userRules = {
  loginAct: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  loginPwd: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 16, message: '登录密码长度为6-16位', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { pattern: /^[\u4E00-\u9FA5]{1,5}$/, message: '姓名必须是中文', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号码格式有误', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式有误', trigger: 'blur' }
  ],
  accountNoExpired: [{ required: true, message: '请选择账号是否未过期', trigger: 'blur' }],
  credentialsNoExpired: [{ required: true, message: '请选择密码是否未过期', trigger: 'blur' }],
  accountNoLocked: [{ required: true, message: '请选择账号是否未锁定', trigger: 'blur' }],
  accountEnabled: [{ required: true, message: '请选择账号是否可用', trigger: 'blur' }]
}

// 下拉选项
const options = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
]

// 方法
const handleSelectionChange = (selectionnDataArray) => {
  userIdArray.value = selectionnDataArray.map(data => data.id)
}

const getData = (current) => {
  doGet("/api/users", { current }).then(resp => {
    if (resp.data.code === 200) {
      userList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize
      total.value = resp.data.data.total
    }
  })
}

const toPage = (current) => {
  getData(current)
  currentPage.value = current
}

const view = (id) => {
  userDetailDialogVisible.value = true
  loadUserDetail(id)
}

const add = () => {
  userQuery.value = {}
  userDialogVisible.value = true
}

const userSubmit = () => {
  const formData = new FormData()
  for (let field in userQuery.value) {
    formData.append(field, userQuery.value[field])
  }
  userRefForm.value.validate((isValid) => {
    if (isValid) {
      const request = userQuery.value.id > 0 ? doPut : doPost
      request("/api/user", formData).then(resp => {
        if (resp.data.code === 200) {
          messageTip(userQuery.value.id > 0 ? "编辑成功" : "提交成功", "success")
          userDialogVisible.value = false
          userQuery.value = {}
          getData(currentPage.value) 
        } else {
          messageTip(userQuery.value.id > 0 ? "编辑失败" : "提交失败", "error")
        }
      })
    }
  })
}

const edit = (id) => {
  userDialogVisible.value = true
  loadUser(id)
}

const loadUser = (id) => {
  doGet("/api/user/" + id, {}).then(resp => {
    console.log(resp);
    if (resp.data.code === 200) {
      userQuery.value = resp.data.data
      userQuery.value.loginPwd = ""
    }
  })
}

// 加载用户详情
const loadUserDetail = (id) => {
  doGet("/api/user/" + id, {}).then(resp => {
    if (resp.data.code === 200) {
      userDetail.value = resp.data.data
      if (!userDetail.value.createByDO) {
        userDetail.value.createByDO = {}
      }
      if (!userDetail.value.editByDO) {
        userDetail.value.editByDO = {}
      }
    }
  })
}

const del = (id) => {
  messageConfirm("您确定要删除该数据吗？").then(() => {
    doDelete("/api/user/" + id, {}).then(resp => {
      if (resp.data.code === 200) {
        messageTip("删除成功", "success")
        getData(currentPage.value) 
      } else {
        messageTip("删除失败，原因：" + resp.data.msg, "error")
      }
    })
  }).catch(() => {
    messageTip("取消删除", "warning")
  })
}

const batchDel = () => {
  if (userIdArray.value.length <= 0) {
    messageTip("请选择要删除的数据", "warning")
    return
  }
  messageConfirm("您确定要删除这些数据吗？").then(() => {
    doDelete("/api/user", userIdArray.value).then(resp => {
      if (resp.data.code === 200) {
        messageTip("批量删除成功", "success")
        getData(currentPage.value) 
      } else {
        messageTip("批量删除失败，原因：" + resp.data.msg, "error")
      }
    })
  }).catch(() => {
    messageTip("取消批量删除", "warning")
  })
}

// 计算序号起始值
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 组件挂载时加载数据
onMounted(() => {
  getData(1)
})
</script>

<style scoped>
.el-table {
  margin-top: 12px;
}

.el-pagination {
  margin-top: 12px;
}

.el-select {
  width: 100%;
}

.action-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.detail {
  background-color: #e9eae3;
  width: 100%;
  padding-left: 15px;
}
</style>