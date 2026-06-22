<template>
  <Card class="mb-5">
    <CardContent>
      <div class="flex flex-wrap items-end gap-4 mb-5">
        <div class="space-y-1">
          <Label>负责人</Label>
          <Select v-model="activityQuery.ownerId" @update:model-value="loadOwner">
            <SelectTrigger class="w-[150px]">
              <SelectValue placeholder="请选择负责人" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in ownerOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-1">
          <Label>活动名称</Label>
          <Input v-model="activityQuery.name" placeholder="请输入活动名称" class="w-[180px]" />
        </div>

        <div class="space-y-1">
          <Label>活动时间</Label>
          <div class="flex items-center gap-2">
            <Input type="datetime-local" v-model="searchStartTime" class="w-[200px]" />
            <span class="text-muted-foreground">至</span>
            <Input type="datetime-local" v-model="searchEndTime" class="w-[200px]" />
          </div>
        </div>

        <div class="space-y-1">
          <Label>活动最低预算</Label>
          <Input v-model="activityQuery.cost" placeholder="请输入活动最低预算" class="w-[180px]" />
        </div>

        <div class="space-y-1">
          <Label>创建时间</Label>
          <Input type="datetime-local" v-model="activityQuery.createTime" class="w-[200px]" />
        </div>

        <div class="flex items-end gap-2">
          <Button @click="onSearch">搜 索</Button>
          <Button variant="outline" @click="onReset">重 置</Button>
        </div>
      </div>

      <div class="flex gap-2">
        <Button v-has-permission="PERMISSIONS.activity.add" @click="add">录入市场活动</Button>
        <Button v-has-permission="PERMISSIONS.activity.delete" variant="destructive" @click="batchDel">批量删除</Button>
      </div>
    </CardContent>
  </Card>

  <Card class="mb-5">
    <CardContent>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead class="w-[55px]">
              <Checkbox
                :checked="isAllSelected"
                @update:checked="toggleSelectAll"
              />
            </TableHead>
            <TableHead class="w-[80px]">序号</TableHead>
            <TableHead class="w-[100px]">负责人</TableHead>
            <TableHead class="w-[150px]">活动名称</TableHead>
            <TableHead>开始时间</TableHead>
            <TableHead>结束时间</TableHead>
            <TableHead class="w-[100px]">活动预算</TableHead>
            <TableHead>创建时间</TableHead>
            <TableHead>操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="(activity, index) in activityList" :key="activity.id">
            <TableCell>
              <Checkbox
                :checked="selectedActivityIds.includes(activity.id)"
                @update:checked="(checked: boolean) => toggleSelection(activity.id, checked)"
              />
            </TableCell>
            <TableCell>{{ (currentPage - 1) * pageSize + index + 1 }}</TableCell>
            <TableCell>{{ activity.ownerDO?.name }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ activity.name }}</TableCell>
            <TableCell class="truncate max-w-[180px]">{{ activity.startTime }}</TableCell>
            <TableCell class="truncate max-w-[180px]">{{ activity.endTime }}</TableCell>
            <TableCell>{{ activity.cost }}</TableCell>
            <TableCell class="truncate max-w-[180px]">{{ activity.createTime }}</TableCell>
            <TableCell>
              <div class="flex gap-1">
                <Button v-has-permission="PERMISSIONS.activity.view" size="sm" @click="view(activity.id)">详情</Button>
                <Button v-has-permission="PERMISSIONS.activity.edit" size="sm" variant="secondary" @click="edit(activity.id)">编辑</Button>
                <Button v-has-permission="PERMISSIONS.activity.delete" size="sm" variant="destructive" @click="del(activity.id)">删除</Button>
              </div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </CardContent>
  </Card>

  <DataTablePagination :page-size="pageSize" :total="total" @change="toPage" />

  <!-- 活动录入/编辑对话框 -->
  <Dialog v-model:open="activityDialogVisible">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>{{ dialogTitle }}</DialogTitle>
      </DialogHeader>

      <form @submit.prevent="onSubmitForm" class="space-y-4">
        <div class="space-y-2">
          <Label>负责人</Label>
          <Select v-model="values.ownerId">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in ownerOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.ownerId" class="text-sm text-destructive">{{ errors.ownerId }}</p>
        </div>

        <div class="space-y-2">
          <Label>活动名称</Label>
          <Input v-model="values.name" placeholder="请输入活动名称" />
          <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
        </div>

        <div class="space-y-2">
          <Label>开始时间</Label>
          <Input type="datetime-local" v-model="values.startTime" class="w-full" />
          <p v-if="errors.startTime" class="text-sm text-destructive">{{ errors.startTime }}</p>
        </div>

        <div class="space-y-2">
          <Label>结束时间</Label>
          <Input type="datetime-local" v-model="values.endTime" class="w-full" />
          <p v-if="errors.endTime" class="text-sm text-destructive">{{ errors.endTime }}</p>
        </div>

        <div class="space-y-2">
          <Label>活动预算</Label>
          <Input v-model="values.cost" placeholder="请输入活动预算" />
          <p v-if="errors.cost" class="text-sm text-destructive">{{ errors.cost }}</p>
        </div>

        <div class="space-y-2">
          <Label>活动描述</Label>
          <Textarea v-model="values.description" :rows="6" placeholder="请输入活动描述" />
          <p v-if="errors.description" class="text-sm text-destructive">{{ errors.description }}</p>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" @click="activityDialogVisible = false">取 消</Button>
          <Button type="submit">提 交</Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import {
  getActivityList,
  getOwnerList,
  batchDeleteActivities,
  deleteActivity,
  createActivity,
  updateActivity,
  getActivityById
} from '@/modules/activity/api/activity-api'
import type { Activity } from '@/modules/activity/model/activity.types'
import type { User } from '@/modules/user/model/user.types'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

// 响应式状态
const router = useRouter()
const activityQuery = reactive<Record<string, unknown>>({})
const searchStartTime = ref('')
const searchEndTime = ref('')
const activityList = ref([{ ownerDO: {} }])
const pageSize = ref(10)
const total = ref(0)
const ownerOptions = ref([{}])
const currentPage = ref(1)
const selectedActivityIds = ref<(number | string)[]>([])

// 对话框状态
const activityDialogVisible = ref(false)
const dialogTitle = ref('录入市场活动')
const isEditing = ref(false)
const editingId = ref<number | string | null>(null)

// 活动表单校验规则 (zod)
const activityFormSchema = toTypedSchema(z.object({
  ownerId: z.string().min(1, '请选择负责人'),
  name: z.string().min(1, '请输入活动名称'),
  startTime: z.string().min(1, '请选择开始时间'),
  endTime: z.string().min(1, '请选择结束时间'),
  cost: z.string().min(1, '请输入活动预算')
    .refine(v => /^[0-9]+(\.[0-9]{1,2})?$/.test(v), { message: '活动预算必须是整数或最多两位小数' }),
  description: z.string().min(5, '活动描述长度为5-255个字符').max(255, '活动描述长度为5-255个字符'),
}))

const { handleSubmit, errors, values, resetForm, setValues } = useForm({
  validationSchema: activityFormSchema,
  initialValues: {
    ownerId: '',
    name: '',
    startTime: '',
    endTime: '',
    cost: '',
    description: '',
  },
})

// 全选计算
const isAllSelected = computed(() =>
  activityList.value.length > 0 && selectedActivityIds.value.length === activityList.value.length
)

const toggleSelectAll = (checked: boolean) => {
  selectedActivityIds.value = checked ? activityList.value.map((item: Activity) => item.id) : []
}

const toggleSelection = (id: number | string, checked: boolean) => {
  if (checked) {
    selectedActivityIds.value.push(id)
  } else {
    selectedActivityIds.value = selectedActivityIds.value.filter((sid: number | string) => sid !== id)
  }
}

// 获取数据 (严禁修改)
const getData = async (current: number) => {
  let startTime = ''
  let endTime = ''
  if (searchStartTime.value) {
    startTime = searchStartTime.value.replace('T', ' ') + ':00'
  }
  if (searchEndTime.value) {
    endTime = searchEndTime.value.replace('T', ' ') + ':00'
  }

  const params = {
    current: current,
    ownerId: activityQuery.ownerId,
    name: activityQuery.name,
    startTime: startTime,
    endTime: endTime,
    cost: activityQuery.cost,
    createTime: activityQuery.createTime
  }

  try {
    const res = await getActivityList(params)
    activityList.value = res.list
    pageSize.value = res.pageSize
    total.value = res.total
  } catch (error) {
    // ignore
  }
  currentPage.value = current
}

// 分页
const toPage = (current: number) => {
  getData(current)
}

// 加载负责人 (严禁修改)
const loadOwner = async () => {
  try {
    const res = await getOwnerList()
    ownerOptions.value = res
  } catch (error) {
    // ignore
  }
}

// 搜索
const onSearch = () => {
  getData(1)
}

// 重置搜索
const onReset = () => {
  Object.keys(activityQuery).forEach(key => delete activityQuery[key])
  searchStartTime.value = ''
  searchEndTime.value = ''
  getData(1)
}

// 导航
const add = async () => {
  dialogTitle.value = '录入市场活动'
  isEditing.value = false
  editingId.value = null
  resetForm()
  await loadOwner()
  activityDialogVisible.value = true
}

const edit = async (id: number | string) => {
  dialogTitle.value = '编辑市场活动'
  isEditing.value = true
  await loadOwner()
  await loadActivityForEdit(id)
  activityDialogVisible.value = true
}

const view = (id: number | string) => {
  router.push("/dashboard/activity/" + id)
}

// 批量删除
const batchDel = async () => {
  if (selectedActivityIds.value.length === 0) {
    messageTip('请至少选择一条记录', 'warning')
    return
  }

  try {
    await messageConfirm('确定要删除选中的活动吗?')
  } catch {
    messageTip('已取消删除', 'info')
    return
  }

  try {
    const res = await batchDeleteActivities(selectedActivityIds.value)
    messageTip('删除成功', 'success')
    getData(1)
  } catch (error) {
    messageTip('请求失败，请检查网络或重试', 'error')
  }
}

// 单个删除
const del = async (id: number | string) => {
  try {
    await messageConfirm('确定要删除该活动吗?')
  } catch {
    messageTip('已取消删除', 'info')
    return
  }

  try {
    const res = await deleteActivity(id)
    messageTip('删除成功', 'success')
    getData(currentPage.value)
  } catch (error) {
    messageTip('请求失败，请检查网络或重试', 'error')
  }
}

// 对话框表单方法 (严禁修改 loadActivityForEdit 中的 API 调用)
const loadActivityForEdit = async (id: number | string) => {
  try {
    const res = await getActivityById(id)
    editingId.value = res.id
    setValues({
      ownerId: String(res.ownerId ?? ''),
      name: res.name ?? '',
      startTime: res.startTime ?? '',
      endTime: res.endTime ?? '',
      cost: String(res.cost ?? ''),
      description: res.description ?? '',
    })
  } catch (error) {
    messageTip('获取活动详情失败', 'error')
  }
}

// 提交表单 (严禁修改 FormData + API 逻辑)
const onSubmitForm = handleSubmit(async (formData) => {
  try {
    const fd = new FormData()
    for (let field in formData) {
      if (formData[field]) {
        fd.append(field, formData[field])
      }
    }
    if (isEditing.value && editingId.value) {
      fd.append('id', editingId.value)
    }

    if (isEditing.value) {
      await updateActivity(fd)
      messageTip('编辑成功', 'success')
    } else {
      await createActivity(fd)
      messageTip('提交成功', 'success')
    }

    activityDialogVisible.value = false
    getData(1)
  } catch (error) {
    messageTip('提交失败，请检查网络或重试', 'error')
  }
})

// 生命周期钩子
onMounted(() => {
  getData(1)
})
</script>
