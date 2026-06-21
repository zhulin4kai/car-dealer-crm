<template>
  <div>
    <!-- Action buttons -->
    <Card class="mb-5">
      <CardContent class="flex gap-2 pt-6">
        <Button @click="add" v-has-permission="PERMISSIONS.user.add">添加用户</Button>
        <Button variant="destructive" @click="batchDel" v-has-permission="PERMISSIONS.user.status" :disabled="!userIdArray.length">批量删除</Button>
      </CardContent>
    </Card>

    <!-- Data table -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox
                  :checked="allSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="w-[60px]">序号</TableHead>
              <TableHead>账号</TableHead>
              <TableHead>姓名</TableHead>
              <TableHead>手机</TableHead>
              <TableHead>邮箱</TableHead>
              <TableHead>创建时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, idx) in userList" :key="row.id">
              <TableCell>
                <Checkbox
                  :checked="userIdArray.includes(row.id)"
                  @update:checked="(v: boolean) => toggleRowSelection(row, v)"
                />
              </TableCell>
              <TableCell>{{ startIndex(idx) }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.loginAct }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.name }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.phone }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.email }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.createTime }}</TableCell>
              <TableCell>
                <div class="flex gap-1">
                  <Button variant="link" size="sm" @click="view(row.id)" v-has-permission="PERMISSIONS.user.view">详情</Button>
                  <Button variant="link" size="sm" @click="edit(row.id)" v-has-permission="PERMISSIONS.user.edit">编辑</Button>
                  <Button variant="destructive" size="sm" @click="del(row.id)" v-has-permission="PERMISSIONS.user.status">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- Pagination -->
    <DataTablePagination :page-size="pageSize" :total="total" @change="toPage" />

    <!-- 新增/编辑用户的弹窗 -->
    <Dialog v-model:open="userDialogVisible">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{{ isEditMode ? '编辑用户' : '新增用户' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="userSubmit">
          <div class="space-y-2">
            <Label>账号</Label>
            <Input v-model="values.loginAct" />
            <p v-if="errors.loginAct" class="text-sm text-destructive">{{ errors.loginAct }}</p>
          </div>

          <div class="space-y-2" v-if="isEditMode">
            <Label>密码</Label>
            <Input type="password" v-model="values.loginPwd" placeholder="******" />
            <p v-if="errors.loginPwd" class="text-sm text-destructive">{{ errors.loginPwd }}</p>
          </div>

          <div class="space-y-2" v-else>
            <Label>密码</Label>
            <Input type="password" v-model="values.loginPwd" />
            <p v-if="errors.loginPwd" class="text-sm text-destructive">{{ errors.loginPwd }}</p>
          </div>

          <div class="space-y-2">
            <Label>姓名</Label>
            <Input v-model="values.name" />
            <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
          </div>

          <div class="space-y-2">
            <Label>手机</Label>
            <Input v-model="values.phone" />
            <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
          </div>

          <div class="space-y-2">
            <Label>邮箱</Label>
            <Input v-model="values.email" />
            <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
          </div>

          <div class="space-y-2">
            <Label>账号未过期</Label>
            <Select v-model="values.accountNoExpired">
              <SelectTrigger>
                <SelectValue placeholder="请选择" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.accountNoExpired" class="text-sm text-destructive">{{ errors.accountNoExpired }}</p>
          </div>

          <div class="space-y-2">
            <Label>密码未过期</Label>
            <Select v-model="values.credentialsNoExpired">
              <SelectTrigger>
                <SelectValue placeholder="请选择" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.credentialsNoExpired" class="text-sm text-destructive">{{ errors.credentialsNoExpired }}</p>
          </div>

          <div class="space-y-2">
            <Label>账号未锁定</Label>
            <Select v-model="values.accountNoLocked">
              <SelectTrigger>
                <SelectValue placeholder="请选择" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.accountNoLocked" class="text-sm text-destructive">{{ errors.accountNoLocked }}</p>
          </div>

          <div class="space-y-2">
            <Label>账号是否启用</Label>
            <Select v-model="values.accountEnabled">
              <SelectTrigger>
                <SelectValue placeholder="请选择" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.accountEnabled" class="text-sm text-destructive">{{ errors.accountEnabled }}</p>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="userDialogVisible = false">关 闭</Button>
          <Button @click="userSubmit">提 交</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 用户详情的弹窗 -->
    <Dialog v-model:open="userDetailDialogVisible">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>用户详情</DialogTitle>
        </DialogHeader>
        <div class="space-y-3">
          <div class="space-y-1">
            <Label>ID</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.id }}</div>
          </div>
          <div class="space-y-1">
            <Label>账号</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.loginAct }}</div>
          </div>
          <div class="space-y-1">
            <Label>密码</Label>
            <div class="w-full bg-muted rounded px-4 py-2">******</div>
          </div>
          <div class="space-y-1">
            <Label>姓名</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.name }}</div>
          </div>
          <div class="space-y-1">
            <Label>手机</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.phone }}</div>
          </div>
          <div class="space-y-1">
            <Label>邮箱</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.email }}</div>
          </div>
          <div class="space-y-1">
            <Label>账号未过期</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.accountNoExpired === 1 ? '是' : '否' }}</div>
          </div>
          <div class="space-y-1">
            <Label>密码未过期</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.credentialsNoExpired === 1 ? '是' : '否' }}</div>
          </div>
          <div class="space-y-1">
            <Label>账号未锁定</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.accountNoLocked === 1 ? '是' : '否' }}</div>
          </div>
          <div class="space-y-1">
            <Label>账号是否启用</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.accountEnabled === 1 ? '是' : '否' }}</div>
          </div>
          <div class="space-y-1">
            <Label>创建时间</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.createTime }}</div>
          </div>
          <div class="space-y-1">
            <Label>创建人</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.createByDO?.name }}</div>
          </div>
          <div class="space-y-1">
            <Label>编辑时间</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.editTime }}</div>
          </div>
          <div class="space-y-1">
            <Label>编辑人</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.editByDO?.name }}</div>
          </div>
          <div class="space-y-1">
            <Label>最近登录时间</Label>
            <div class="w-full bg-muted rounded px-4 py-2">{{ userDetail.lastLoginTime }}</div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="secondary" @click="userDetailDialogVisible = false">返 回</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { doDelete, doGet, doPost, doPut } from '@/shared/api/http-client'
import type { User } from '@/modules/user/model/user.types'
import { messageConfirm, messageTip } from '@/shared/utils/legacy-util'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

// 响应式数据
const userList = ref([{}])
const pageSize = ref(10)
const total = ref(0)
const userDialogVisible = ref(false)
const userQuery = ref({})
const userIdArray = ref([])
const currentPage = ref(1)
const isEditMode = ref(false)
const router = useRouter()

// 用户详情相关数据
const userDetailDialogVisible = ref(false)
const userDetail = ref({
  createByDO: {},
  editByDO: {}
})

// 表单验证 schema (动态：新增时密码必填，编辑时可选)
const userSchema = computed(() =>
  toTypedSchema(z.object({
    id: z.number().optional().default(0),
    loginAct: z.string().min(1, '请输入登录账号'),
    loginPwd: isEditMode.value
      ? z.string().optional()
      : z.string().min(6, '登录密码长度为6-16位').max(16, '登录密码长度为6-16位'),
    name: z.string().min(1, '请输入姓名').regex(/^[\u4E00-\u9FA5]{1,5}$/, '姓名必须是中文'),
    phone: z.string().min(1, '请输入手机号码').regex(/^1[3-9]\d{9}$/, '手机号码格式有误'),
    email: z.string().min(1, '请输入邮箱').regex(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, '邮箱格式有误'),
    accountNoExpired: z.number({ required_error: '请选择账号是否未过期' }),
    credentialsNoExpired: z.number({ required_error: '请选择密码是否未过期' }),
    accountNoLocked: z.number({ required_error: '请选择账号是否未锁定' }),
    accountEnabled: z.number({ required_error: '请选择账号是否可用' }),
  }))
)

const { handleSubmit, errors, values, setValues, resetForm } = useForm({
  validationSchema: userSchema,
  initialValues: {
    id: 0,
    loginAct: '',
    loginPwd: '',
    name: '',
    phone: '',
    email: '',
    accountNoExpired: 0,
    credentialsNoExpired: 0,
    accountNoLocked: 0,
    accountEnabled: 0,
  },
})

// 下拉选项
const options = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
]

// 全选相关
const allSelected = computed(() =>
  userList.value.length > 0 && userIdArray.value.length === userList.value.length
)

const toggleSelectAll = (checked: boolean) => {
  if (checked) {
    userIdArray.value = userList.value.map(data => data.id)
  } else {
    userIdArray.value = []
  }
}

const toggleRowSelection = (row: User, checked: boolean) => {
  if (checked) {
    if (!userIdArray.value.includes(row.id)) {
      userIdArray.value.push(row.id)
    }
  } else {
    userIdArray.value = userIdArray.value.filter((id: number | string) => id !== row.id)
  }
}

// 方法
const handleSelectionChange = (selectionnDataArray) => {
  userIdArray.value = selectionnDataArray.map(data => data.id)
}

const getData = (current) => {
  doGet("/api/users", { current }).then(resp => {
    if (true) {
      userList.value = resp.list
      pageSize.value = resp.pageSize
      total.value = resp.total
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
  isEditMode.value = false
  resetForm({
    values: {
      id: 0,
      loginAct: '',
      loginPwd: '',
      name: '',
      phone: '',
      email: '',
      accountNoExpired: 0,
      credentialsNoExpired: 0,
      accountNoLocked: 0,
      accountEnabled: 0,
    },
  })
  userDialogVisible.value = true
}

const userSubmit = handleSubmit(async (formData) => {
  // 同步到 userQuery 以保留原有业务逻辑
  userQuery.value = { ...formData }
  const formDataObj = new FormData()
  for (let field in userQuery.value) {
    formDataObj.append(field, userQuery.value[field])
  }
  const request = userQuery.value.id > 0 ? doPut : doPost
  request("/api/user", formDataObj).then(resp => {
    if (true) {
      messageTip(userQuery.value.id > 0 ? "编辑成功" : "提交成功", "success")
      userDialogVisible.value = false
      userQuery.value = {}
      getData(currentPage.value)
    } else {
      messageTip(userQuery.value.id > 0 ? "编辑失败" : "提交失败", "error")
    }
  })
})

const edit = (id) => {
  userDialogVisible.value = true
  loadUser(id)
}

const loadUser = (id) => {
  doGet("/api/user/" + id, {}).then(resp => {
    console.log(resp);
    if (true) {
      userQuery.value = resp
      userQuery.value.loginPwd = ""
      isEditMode.value = true
      setValues({
        id: resp.id || 0,
        loginAct: resp.loginAct || '',
        loginPwd: '',
        name: resp.name || '',
        phone: resp.phone || '',
        email: resp.email || '',
        accountNoExpired: resp.accountNoExpired ?? 0,
        credentialsNoExpired: resp.credentialsNoExpired ?? 0,
        accountNoLocked: resp.accountNoLocked ?? 0,
        accountEnabled: resp.accountEnabled ?? 0,
      })
    }
  })
}

// 加载用户详情
const loadUserDetail = (id) => {
  doGet("/api/user/" + id, {}).then(resp => {
    if (true) {
      userDetail.value = resp
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
  messageConfirm("您确定要禁用该账号吗？").then(() => {
    doPut(`/api/user/${id}/disable`, {}).then(resp => {
      if (true) {
        messageTip("禁用成功", "success")
        getData(currentPage.value)
      } else {
        messageTip("禁用失败，原因：" + '请求失败', "error")
      }
    })
  }).catch(() => {
    messageTip("取消禁用", "warning")
  })
}

const batchDel = () => {
  if (userIdArray.value.length <= 0) {
    messageTip("请选择要禁用的数据", "warning")
    return
  }
  messageConfirm("您确定要禁用这些账号吗？").then(() => {
    doPut("/api/users/batch-disable", { ids: userIdArray.value }).then(resp => {
      if (true) {
        messageTip("批量禁用成功", "success")
        getData(currentPage.value)
      } else {
        messageTip("批量禁用失败，原因：" + '请求失败', "error")
      }
    })
  }).catch(() => {
    messageTip("取消批量禁用", "warning")
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
