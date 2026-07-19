<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="crm-panel-body space-y-4">
        <div class="crm-toolbar">
          <div class="crm-field">
            <Label class="crm-field-label">负责人</Label>
            <Select v-model="activityQuery.ownerId">
              <SelectTrigger class="w-[150px]">
                <SelectValue placeholder="请选择负责人" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in ownerOptions"
                  :key="item.userId"
                  :value="String(item.userId)"
                >
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="crm-field">
            <Label class="crm-field-label">活动状态</Label>
            <Select v-model="activityQuery.status">
              <SelectTrigger class="w-[140px]">
                <SelectValue placeholder="全部状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in activityStatusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="crm-field">
            <Label class="crm-field-label">活动名称</Label>
            <Input v-model="activityQuery.name" placeholder="请输入活动名称" class="w-[180px]" />
          </div>

          <div class="crm-field">
            <Label class="crm-field-label">渠道</Label>
            <Input v-model="activityQuery.channel" placeholder="请输入渠道" class="w-[150px]" />
          </div>

          <div class="crm-field">
            <Label class="crm-field-label">活动时间</Label>
            <div class="flex items-center gap-2">
              <Input type="datetime-local" v-model="searchStartTime" class="w-[200px]" />
              <span class="text-muted-foreground">至</span>
              <Input type="datetime-local" v-model="searchEndTime" class="w-[200px]" />
            </div>
          </div>

          <div class="crm-toolbar-actions">
            <Button class="gap-2" @click="onSearch">
              <Search class="h-4 w-4" />
              搜索
            </Button>
            <Button variant="outline" class="gap-2" @click="onReset">
              <RotateCcw class="h-4 w-4" />
              重置
            </Button>
          </div>
        </div>

        <div class="crm-toolbar-actions">
          <Button v-has-permission="PERMISSIONS.activity.add" class="gap-2" @click="add">
            <Plus class="h-4 w-4" />
            录入市场活动
          </Button>
          <Button
            v-has-permission="PERMISSIONS.activity.export"
            variant="outline"
            class="gap-2"
            :disabled="exporting"
            @click="exportCurrentQuery"
          >
            <Download class="h-4 w-4" />
            {{ exporting ? '导出中...' : '导出ROI' }}
          </Button>
          <Button
            v-has-permission="PERMISSIONS.activity.delete"
            variant="destructive"
            class="gap-2"
            @click="batchDel"
          >
            <Trash2 class="h-4 w-4" />
            批量删除草稿
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <Table class="min-w-[1280px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox :checked="isAllSelected" @update:checked="toggleSelectAll" />
              </TableHead>
              <TableHead class="w-[70px]">序号</TableHead>
              <TableHead class="w-[110px]">状态</TableHead>
              <TableHead class="w-[110px]">负责人</TableHead>
              <TableHead class="w-[180px]">活动名称</TableHead>
              <TableHead class="w-[120px]">渠道</TableHead>
              <TableHead class="w-[140px]">目标车型</TableHead>
              <TableHead class="w-[180px]">开始时间</TableHead>
              <TableHead class="w-[180px]">结束时间</TableHead>
              <TableHead class="w-[120px]">预算</TableHead>
              <TableHead class="w-[120px]">实际成本</TableHead>
              <TableHead class="w-[210px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="displayActivityList.length === 0">
              <TableCell colspan="12" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无市场活动数据
              </TableCell>
            </TableRow>
            <TableRow v-for="(activity, index) in displayActivityList" :key="activity.id">
              <TableCell>
                <Checkbox
                  :checked="selectedActivityIds.includes(activity.id as EntityId)"
                  @update:checked="(checked: boolean) => toggleSelection(activity.id, checked)"
                />
              </TableCell>
              <TableCell class="text-[var(--crm-text-tertiary)]">
                {{ (currentPage - 1) * pageSize + index + 1 }}
              </TableCell>
              <TableCell>
                <span class="inline-flex rounded px-2 py-1 text-xs font-medium" :class="activityStatusTone(activity.status)">
                  {{ activityStatusLabel(activity.status) }}
                </span>
              </TableCell>
              <TableCell class="font-medium text-[var(--crm-text-primary)]">
                {{ activity.ownerDO?.name || '--' }}
              </TableCell>
              <TableCell class="max-w-[200px] truncate font-semibold text-[var(--crm-text-primary)]">
                {{ activity.name || '--' }}
              </TableCell>
              <TableCell class="max-w-[130px] truncate">{{ activity.channel || '--' }}</TableCell>
              <TableCell class="max-w-[150px] truncate">{{ activity.targetModel || '--' }}</TableCell>
              <TableCell class="max-w-[190px] truncate">{{ activity.startTime || '--' }}</TableCell>
              <TableCell class="max-w-[190px] truncate">{{ activity.endTime || '--' }}</TableCell>
              <TableCell class="font-semibold text-[var(--crm-text-primary)]">
                {{ formatCurrency(activity.cost, { fractionDigits: 0 }) }}
              </TableCell>
              <TableCell class="font-semibold text-[var(--crm-text-primary)]">
                {{ activity.actualCost == null ? '--' : formatCurrency(activity.actualCost, { fractionDigits: 0 }) }}
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-has-permission="PERMISSIONS.activity.view" label="详情" @click="view(activity.id)">
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isActivityCoreLocked(activity.status)"
                    v-has-permission="PERMISSIONS.activity.edit"
                    label="编辑"
                    @click="edit(activity.id)"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="activity.status === 'DRAFT'"
                    v-has-permission="PERMISSIONS.activity.edit"
                    label="发布"
                    @click="runStatusAction(activity, 'publish')"
                  >
                    <Send class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="activity.status === 'PLANNED'"
                    v-has-permission="PERMISSIONS.activity.edit"
                    label="开始"
                    @click="runStatusAction(activity, 'start')"
                  >
                    <Play class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="activity.status === 'ONGOING'"
                    v-has-permission="PERMISSIONS.activity.edit"
                    label="结束"
                    @click="runStatusAction(activity, 'end')"
                  >
                    <Flag class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="activity.status === 'ENDED'"
                    v-has-permission="PERMISSIONS.activity.review"
                    label="复盘"
                    @click="openReview(activity)"
                  >
                    <ClipboardCheck class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canClose(activity.status)"
                    v-has-permission="PERMISSIONS.activity.close"
                    label="关闭"
                    @click="openReason(activity, 'close')"
                  >
                    <Archive class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canCancel(activity.status)"
                    v-has-permission="PERMISSIONS.activity.close"
                    label="取消"
                    danger
                    @click="openReason(activity, 'cancel')"
                  >
                    <Ban class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="activity.status === 'DRAFT'"
                    v-has-permission="PERMISSIONS.activity.delete"
                    label="删除"
                    danger
                    @click="del(activity.id)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </RowActionButton>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="crm-table-footer">
        <DataTablePagination
          :page="currentPage"
          :page-size="pageSize"
          :total="total"
          @change="toPage"
        />
      </div>
    </section>

    <Dialog v-model:open="activityDialogVisible">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
        </DialogHeader>

        <form @submit.prevent="onSubmitForm" class="space-y-4">
          <div class="space-y-2">
            <Label>活动名称</Label>
            <Input v-model="name" placeholder="请输入活动名称" />
            <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <Label>渠道</Label>
              <Input v-model="channel" placeholder="例如：店内活动" />
              <p v-if="errors.channel" class="text-sm text-destructive">{{ errors.channel }}</p>
            </div>
            <div class="space-y-2">
              <Label>目标车型</Label>
              <Input v-model="targetModel" placeholder="例如：SUV" />
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <Label>开始时间</Label>
              <Input type="datetime-local" v-model="startTime" class="w-full" />
              <p v-if="errors.startTime" class="text-sm text-destructive">{{ errors.startTime }}</p>
            </div>
            <div class="space-y-2">
              <Label>结束时间</Label>
              <Input type="datetime-local" v-model="endTime" class="w-full" />
              <p v-if="errors.endTime" class="text-sm text-destructive">{{ errors.endTime }}</p>
            </div>
          </div>

          <div class="space-y-2">
            <Label>活动预算</Label>
            <Input v-model="cost" placeholder="请输入活动预算" />
            <p v-if="errors.cost" class="text-sm text-destructive">{{ errors.cost }}</p>
          </div>

          <div class="space-y-2">
            <Label>活动描述</Label>
            <Textarea v-model="description" :rows="5" placeholder="请输入活动描述" />
            <p v-if="errors.description" class="text-sm text-destructive">{{ errors.description }}</p>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" @click="activityDialogVisible = false">取消</Button>
            <Button type="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交' }}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="reviewDialogVisible">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>活动复盘</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="onSubmitReview" class="space-y-4">
          <div class="space-y-2">
            <Label>实际成本</Label>
            <Input v-model="reviewActualCost" placeholder="请输入实际成本" />
            <p v-if="reviewErrors.actualCost" class="text-sm text-destructive">{{ reviewErrors.actualCost }}</p>
          </div>
          <div class="space-y-2">
            <Label>复盘结果</Label>
            <Textarea v-model="reviewResultSummary" :rows="4" placeholder="请输入转化表现和主要问题" />
            <p v-if="reviewErrors.resultSummary" class="text-sm text-destructive">{{ reviewErrors.resultSummary }}</p>
          </div>
          <div class="space-y-2">
            <Label>复盘结论</Label>
            <Textarea v-model="reviewConclusion" :rows="4" placeholder="请输入后续改进结论" />
            <p v-if="reviewErrors.reviewConclusion" class="text-sm text-destructive">{{ reviewErrors.reviewConclusion }}</p>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" @click="reviewDialogVisible = false">取消</Button>
            <Button type="submit" :disabled="submitting">{{ submitting ? '提交中...' : '完成复盘' }}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="reasonDialogVisible">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{{ reasonAction === 'close' ? '关闭活动' : '取消活动' }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-3">
          <Label>原因</Label>
          <Textarea v-model="reasonText" :rows="4" placeholder="请输入原因" />
        </div>
        <DialogFooter>
          <Button type="button" variant="outline" @click="reasonDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="submitReason">{{ submitting ? '提交中...' : '确认' }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { fromLocalDateTimeInput, toLocalDateTimeInput } from '@/shared/datetime/local-date'
import { saveBlob } from '@/shared/utils/browser-download'
import {
  batchDeleteActivities,
  cancelActivity,
  closeActivity,
  createActivity,
  deleteActivity,
  endActivity,
  exportActivities,
  getActivityById,
  getActivityList,
  publishActivity,
  reviewActivity,
  startActivity,
  updateActivity,
} from '@/modules/activity/api/activity-api'
import { fetchOwnerList } from '@/modules/user/api/user-api'
import {
  OWNER_QUALIFICATION_CONTEXT,
  type OwnerCandidate,
} from '@/modules/user/model/owner.types'
import {
  activityStatusLabel,
  activityStatusOptions,
  activityStatusTone,
  isActivityCoreLocked,
  type Activity,
} from '@/modules/activity/model/activity.types'
import { usePermissionStore } from '@/stores/permission.store'
import type { EntityId } from '@/shared/types/id'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import { formatCurrency } from '@/shared/utils/display-format'
import { useClientSort } from '@/shared/utils/table-sort'
import {
  Archive,
  Ban,
  ClipboardCheck,
  Download,
  Eye,
  Flag,
  Pencil,
  Play,
  Plus,
  RotateCcw,
  Search,
  Send,
  Trash2,
} from '@lucide/vue'

type ReasonAction = 'close' | 'cancel'
type StatusAction = 'publish' | 'start' | 'end'

const router = useRouter()
const permissionStore = usePermissionStore()
const activityQuery = reactive<Record<string, unknown>>({})
const searchStartTime = ref('')
const searchEndTime = ref('')
const activityList = ref<Activity[]>([])
const pageSize = ref(10)
const total = ref(0)
const ownerOptions = ref<OwnerCandidate[]>([])
const currentPage = ref(1)
const selectedActivityIds = ref<EntityId[]>([])
const submitting = ref(false)
const exporting = ref(false)

const {
  sortBy,
  sortDirection,
  sortedRows: displayActivityList,
  toggleSort,
} = useClientSort<Activity>(activityList, {
  index: row => row.id ?? 0,
  status: row => row.status ?? '',
  owner: row => row.ownerDO?.name ?? '',
  name: 'name',
  startTime: 'startTime',
  endTime: 'endTime',
  cost: 'cost',
})
void sortBy
void sortDirection
void toggleSort

const activityDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const reasonDialogVisible = ref(false)
const dialogTitle = ref('录入市场活动')
const isEditing = ref(false)
const editingId = ref<EntityId | null>(null)
const activeActivity = ref<Activity | null>(null)
const reasonAction = ref<ReasonAction>('close')
const reasonText = ref('')

const activityFormSchema = toTypedSchema(
  z.object({
    name: z.string().min(1, '请输入活动名称'),
    channel: z.string().min(1, '请输入渠道').max(64, '渠道不能超过64个字符'),
    targetModel: z.string().max(128, '目标车型不能超过128个字符').optional(),
    startTime: z.string().min(1, '请选择开始时间'),
    endTime: z.string().min(1, '请选择结束时间'),
    cost: z.string().min(1, '请输入活动预算').refine(v => /^[0-9]+(\.[0-9]{1,2})?$/.test(v), {
      message: '活动预算必须是整数或最多两位小数',
    }),
    description: z.string().max(255, '活动描述不能超过255个字符').optional(),
  }),
)

const { handleSubmit, errors, resetForm, defineField } = useForm({
  validationSchema: activityFormSchema,
  initialValues: {
    name: '',
    channel: '',
    targetModel: '',
    startTime: '',
    endTime: '',
    cost: '',
    description: '',
  },
})

const [name] = defineField('name')
const [channel] = defineField('channel')
const [targetModel] = defineField('targetModel')
const [startTime] = defineField('startTime')
const [endTime] = defineField('endTime')
const [cost] = defineField('cost')
const [description] = defineField('description')

const reviewSchema = toTypedSchema(
  z.object({
    actualCost: z.string().min(1, '请输入实际成本').refine(v => /^[0-9]+(\.[0-9]{1,2})?$/.test(v), {
      message: '实际成本必须是整数或最多两位小数',
    }),
    resultSummary: z.string().min(1, '请输入复盘结果').max(500, '复盘结果不能超过500个字符'),
    reviewConclusion: z.string().min(1, '请输入复盘结论').max(500, '复盘结论不能超过500个字符'),
  }),
)

const {
  handleSubmit: handleReviewSubmit,
  errors: reviewErrors,
  resetForm: resetReviewForm,
  defineField: defineReviewField,
} = useForm({
  validationSchema: reviewSchema,
  initialValues: {
    actualCost: '',
    resultSummary: '',
    reviewConclusion: '',
  },
})

const [reviewActualCost] = defineReviewField('actualCost')
const [reviewResultSummary] = defineReviewField('resultSummary')
const [reviewConclusion] = defineReviewField('reviewConclusion')

const isAllSelected = computed(
  () =>
    displayActivityList.value.length > 0 &&
    selectedActivityIds.value.length === displayActivityList.value.length,
)

function canCancel(status?: string) {
  return status === 'DRAFT' || status === 'PLANNED' || status === 'ONGOING'
}

function canClose(status?: string) {
  return status === 'ENDED' || status === 'REVIEWED'
}

function toggleSelectAll(checked: boolean) {
  selectedActivityIds.value = checked
    ? displayActivityList.value
        .map(item => item.id)
        .filter((id): id is EntityId => id != null)
    : []
}

function toggleSelection(id: EntityId | undefined, checked: boolean) {
  if (id == null) return
  if (checked && !selectedActivityIds.value.includes(id)) {
    selectedActivityIds.value.push(id)
  } else if (!checked) {
    selectedActivityIds.value = selectedActivityIds.value.filter(selectedId => selectedId !== id)
  }
}

function buildQuery(current: number) {
  const params: Record<string, unknown> = { page: current, size: pageSize.value }
  const start = fromLocalDateTimeInput(searchStartTime.value)
  const end = fromLocalDateTimeInput(searchEndTime.value)
  if (activityQuery.ownerId) params.ownerId = activityQuery.ownerId
  if (activityQuery.status) params.status = activityQuery.status
  if (typeof activityQuery.name === 'string' && activityQuery.name.trim()) params.name = activityQuery.name.trim()
  if (typeof activityQuery.channel === 'string' && activityQuery.channel.trim()) params.channel = activityQuery.channel.trim()
  if (start) params.startTime = start
  if (end) params.endTime = end
  return params
}

async function getData(current: number) {
  try {
    const res = await getActivityList(buildQuery(current))
    activityList.value = res.list
    pageSize.value = res.pageSize
    total.value = res.total
    selectedActivityIds.value = []
  } catch {
    messageTip('加载市场活动失败', 'error')
  }
  currentPage.value = current
}

function toPage(current: number) {
  void getData(current)
}

async function loadOwner(permissionCode?: string) {
  const effectivePermission = permissionCode
    ?? (permissionStore.hasPermission(PERMISSIONS.activity.add)
      ? PERMISSIONS.activity.add
      : permissionStore.hasPermission(PERMISSIONS.activity.edit)
        ? PERMISSIONS.activity.edit
        : undefined)
  if (!effectivePermission) {
    ownerOptions.value = []
    return
  }
  try {
    ownerOptions.value = await fetchOwnerList({
      permissionCode: effectivePermission,
      qualificationContext: OWNER_QUALIFICATION_CONTEXT.ACTIVITY_OWNER,
    })
  } catch {
    ownerOptions.value = []
  }
}

function onSearch() {
  void getData(1)
}

function onReset() {
  Object.keys(activityQuery).forEach(key => delete activityQuery[key])
  searchStartTime.value = ''
  searchEndTime.value = ''
  void getData(1)
}

async function add() {
  await loadOwner(PERMISSIONS.activity.add)
  dialogTitle.value = '录入市场活动'
  isEditing.value = false
  editingId.value = null
  resetForm({
    values: {
      name: '',
      channel: '',
      targetModel: '',
      startTime: '',
      endTime: '',
      cost: '',
      description: '',
    },
  })
  activityDialogVisible.value = true
}

async function edit(id: EntityId | undefined) {
  if (id == null) return
  await loadOwner(PERMISSIONS.activity.edit)
  dialogTitle.value = '编辑市场活动'
  isEditing.value = true
  await loadActivityForEdit(id)
  activityDialogVisible.value = true
}

function view(id: EntityId | undefined) {
  if (id == null) return
  router.push('/dashboard/activity/' + id)
}

async function batchDel() {
  if (selectedActivityIds.value.length === 0) {
    messageTip('请至少选择一条草稿活动', 'warning')
    return
  }
  try {
    await messageConfirm('确定要删除选中的草稿活动吗?')
    await batchDeleteActivities(selectedActivityIds.value)
    messageTip('删除成功', 'success')
    await getData(1)
  } catch {
    messageTip('删除未完成', 'info')
  }
}

async function del(id: EntityId | undefined) {
  if (id == null) return
  try {
    await messageConfirm('确定要删除该草稿活动吗?')
    await deleteActivity(id)
    messageTip('删除成功', 'success')
    await getData(currentPage.value)
  } catch {
    messageTip('删除未完成', 'info')
  }
}

async function loadActivityForEdit(id: EntityId) {
  try {
    const res = await getActivityById(id)
    editingId.value = res.id ?? null
    resetForm({
      values: {
        name: res.name ?? '',
        channel: res.channel ?? '',
        targetModel: res.targetModel ?? '',
        startTime: toLocalDateTimeInput(res.startTime),
        endTime: toLocalDateTimeInput(res.endTime),
        cost: String(res.cost ?? ''),
        description: res.description ?? '',
      },
    })
  } catch {
    messageTip('获取活动详情失败', 'error')
  }
}

const onSubmitForm = handleSubmit(async formData => {
  submitting.value = true
  try {
    const payload = {
      ...formData,
      id: isEditing.value ? editingId.value ?? undefined : undefined,
      startTime: fromLocalDateTimeInput(formData.startTime),
      endTime: fromLocalDateTimeInput(formData.endTime),
    }
    if (isEditing.value) {
      await updateActivity(payload)
      messageTip('编辑成功', 'success')
    } else {
      await createActivity(payload)
      messageTip('提交成功', 'success')
    }
    activityDialogVisible.value = false
    await getData(1)
  } catch {
    messageTip('提交失败，请检查后重试', 'error')
  } finally {
    submitting.value = false
  }
})

async function runStatusAction(activity: Activity, action: StatusAction) {
  if (activity.id == null) return
  const labels: Record<StatusAction, string> = {
    publish: '发布',
    start: '开始',
    end: '结束',
  }
  try {
    await messageConfirm(`确定要${labels[action]}该活动吗?`)
    if (action === 'publish') await publishActivity(activity.id)
    if (action === 'start') await startActivity(activity.id)
    if (action === 'end') await endActivity(activity.id)
    messageTip('操作成功', 'success')
    await getData(currentPage.value)
  } catch {
    messageTip('操作未完成', 'info')
  }
}

function openReview(activity: Activity) {
  activeActivity.value = activity
  resetReviewForm({
    values: {
      actualCost: String(activity.actualCost ?? activity.cost ?? ''),
      resultSummary: activity.resultSummary ?? '',
      reviewConclusion: activity.reviewConclusion ?? '',
    },
  })
  reviewDialogVisible.value = true
}

const onSubmitReview = handleReviewSubmit(async formData => {
  if (activeActivity.value?.id == null) return
  submitting.value = true
  try {
    await reviewActivity(activeActivity.value.id, formData)
    messageTip('复盘完成', 'success')
    reviewDialogVisible.value = false
    await getData(currentPage.value)
  } catch {
    messageTip('复盘失败，请检查后重试', 'error')
  } finally {
    submitting.value = false
  }
})

function openReason(activity: Activity, action: ReasonAction) {
  activeActivity.value = activity
  reasonAction.value = action
  reasonText.value = ''
  reasonDialogVisible.value = true
}

async function submitReason() {
  if (activeActivity.value?.id == null) return
  if (!reasonText.value.trim()) {
    messageTip('请输入原因', 'warning')
    return
  }
  submitting.value = true
  try {
    if (reasonAction.value === 'close') {
      await closeActivity(activeActivity.value.id, reasonText.value.trim())
    } else {
      await cancelActivity(activeActivity.value.id, reasonText.value.trim())
    }
    messageTip('操作成功', 'success')
    reasonDialogVisible.value = false
    await getData(currentPage.value)
  } catch {
    messageTip('操作失败，请检查后重试', 'error')
  } finally {
    submitting.value = false
  }
}

async function exportCurrentQuery() {
  if (exporting.value) return
  exporting.value = true
  try {
    const { blob, filename } = await exportActivities(buildQuery(currentPage.value))
    saveBlob(blob, filename)
    messageTip('导出成功', 'success')
  } catch {
    messageTip('导出失败', 'error')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  void loadOwner()
  void getData(1)
})
</script>
