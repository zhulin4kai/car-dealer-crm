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

          <div class="space-y-2" v-if="!isEditMode">
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
        </form>
        <DialogFooter>
          <Button variant="outline" @click="userDialogVisible = false" :disabled="submitting">关 闭</Button>
          <Button @click="userSubmit" :disabled="submitting">{{ submitting ? '提交中...' : '提 交' }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 用户详情的弹窗 -->
    <Dialog v-model:open="userDetailDialogVisible">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>用户详情</DialogTitle>
        </DialogHeader>
        <div v-if="loadingDetail" class="py-8 text-center text-muted-foreground">加载中...</div>
        <div v-else-if="userDetail" class="space-y-3">
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
        <div v-else class="py-8 text-center text-muted-foreground">加载失败</div>
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
import {
  fetchUserPage,
  fetchUserDetail,
  createUser,
  updateUser,
  disableUser,
  batchDisableUsers,
} from '@/modules/user/api/user-api'
import type { User } from '@/modules/user/model/user.types'
import {
  toCreateUserRequest,
  toUpdateUserRequest,
  type UserFormValues,
} from '@/modules/user/model/user.types'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

const userList = ref<User[]>([])
const pageSize = ref(10)
const total = ref(0)
const userDialogVisible = ref(false)
const userIdArray = ref<(number | string)[]>([])
const currentPage = ref(1)
const isEditMode = ref(false)
const editingUserId = ref<number | null>(null)
const submitting = ref(false)
const loadingDetail = ref(false)

const userDetailDialogVisible = ref(false)
const userDetail = ref<User | null>(null)

const createUserSchema = toTypedSchema(z.object({
  loginAct: z.string().min(1, '请输入登录账号'),
  loginPwd: z.string().min(6, '登录密码长度为6-16位').max(16, '登录密码长度为6-16位'),
  name: z.string().min(1, '请输入姓名'),
  phone: z.string().min(1, '请输入手机号码').regex(/^1[3-9]\d{9}$/, '手机号码格式有误'),
  email: z.string().min(1, '请输入邮箱').regex(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, '邮箱格式有误'),
}))

const updateUserSchema = toTypedSchema(z.object({
  loginAct: z.string().min(1, '请输入登录账号'),
  loginPwd: z.string().optional(),
  name: z.string().min(1, '请输入姓名'),
  phone: z.string().min(1, '请输入手机号码').regex(/^1[3-9]\d{9}$/, '手机号码格式有误'),
  email: z.string().min(1, '请输入邮箱').regex(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, '邮箱格式有误'),
}))

const validationSchema = computed(() => (isEditMode.value ? updateUserSchema : createUserSchema))

const { handleSubmit, errors, values, setValues, resetForm } = useForm<UserFormValues>({
  validationSchema,
  initialValues: {
    loginAct: '',
    loginPwd: '',
    name: '',
    phone: '',
    email: '',
  },
})

const allSelected = computed(() =>
  userList.value.length > 0 && userIdArray.value.length === userList.value.length
)

function toggleSelectAll(checked: boolean) {
  if (checked) {
    userIdArray.value = userList.value.map(data => data.id).filter((id): id is number | string => id !== undefined)
  } else {
    userIdArray.value = []
  }
}

function toggleRowSelection(row: User, checked: boolean) {
  if (checked && row.id !== undefined) {
    if (!userIdArray.value.includes(row.id)) {
      userIdArray.value.push(row.id)
    }
  } else {
    userIdArray.value = userIdArray.value.filter((id: number | string) => id !== row.id)
  }
}

async function getData(current: number) {
  try {
    const resp = await fetchUserPage({ current })
    userList.value = resp.list
    pageSize.value = resp.pageSize ?? 10
    total.value = resp.total
    userIdArray.value = []
  } catch {
    messageTip('获取用户列表失败', 'error')
  }
}

function toPage(current: number) {
  void getData(current)
  currentPage.value = current
}

async function view(id: number) {
  userDetailDialogVisible.value = true
  loadingDetail.value = true
  userDetail.value = null
  try {
    userDetail.value = await fetchUserDetail(id)
  } catch {
    messageTip('加载用户详情失败', 'error')
  } finally {
    loadingDetail.value = false
  }
}

function add() {
  isEditMode.value = false
  editingUserId.value = null
  resetForm({
    values: {
      loginAct: '',
      loginPwd: '',
      name: '',
      phone: '',
      email: '',
    },
  })
  userDialogVisible.value = true
}

const userSubmit = handleSubmit(async (formData) => {
  if (submitting.value) return
  submitting.value = true
  try {
    if (isEditMode.value) {
      if (editingUserId.value === null) {
        messageTip('编辑模式缺少用户 ID', 'error')
        return
      }
      await updateUser(toUpdateUserRequest(formData, editingUserId.value))
      messageTip('编辑成功', 'success')
    } else {
      await createUser(toCreateUserRequest(formData))
      messageTip('提交成功', 'success')
    }
    userDialogVisible.value = false
    try {
      await getData(currentPage.value)
    } catch {
      messageTip('用户已保存，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip(isEditMode.value ? '编辑失败' : '提交失败', 'error')
  } finally {
    submitting.value = false
  }
})

async function edit(id: number) {
  isEditMode.value = true
  editingUserId.value = id
  try {
    const user = await fetchUserDetail(id)
    if (editingUserId.value !== id) return
    setValues({
      loginAct: user.loginAct ?? '',
      loginPwd: '',
      name: user.name ?? '',
      phone: user.phone ?? '',
      email: user.email ?? '',
    })
    userDialogVisible.value = true
  } catch {
    messageTip('加载用户信息失败', 'error')
  }
}

async function del(id: number) {
  try {
    await messageConfirm('您确定要禁用该账号吗？')
  } catch {
    messageTip('取消禁用', 'warning')
    return
  }
  try {
    await disableUser(id)
    messageTip('禁用成功', 'success')
    await getData(currentPage.value)
  } catch {
    messageTip('禁用失败', 'error')
  }
}

async function batchDel() {
  if (userIdArray.value.length <= 0) {
    messageTip('请选择要禁用的数据', 'warning')
    return
  }
  try {
    await messageConfirm('您确定要禁用这些账号吗？')
  } catch {
    messageTip('取消批量禁用', 'warning')
    return
  }
  try {
    await batchDisableUsers(userIdArray.value)
    messageTip('批量禁用成功', 'success')
    await getData(currentPage.value)
  } catch {
    messageTip('批量禁用失败', 'error')
  }
}

function startIndex(index: number) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  void getData(1)
})
</script>
