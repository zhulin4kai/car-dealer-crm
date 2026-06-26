<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">跟进任务</h2>
            <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
              {{ taskTotal }} 条
            </span>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Button v-has-permission="PERMISSIONS.communicationRecord.create" variant="outline" class="gap-2" @click="openRecordCreate()">
            <MessageSquarePlus class="h-4 w-4" />
            新增沟通
          </Button>
          <Button v-has-permission="PERMISSIONS.followTask.create" class="gap-2" @click="openTaskCreate">
            <Plus class="h-4 w-4" />
            新增任务
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">关键词</Label>
            <Input v-model="filterForm.keyword" class="w-[220px]" placeholder="标题/结果" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">状态</Label>
            <Select v-model="filterForm.status">
              <SelectTrigger class="w-[150px]">
                <SelectValue placeholder="全部状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">全部状态</SelectItem>
                <SelectItem v-for="option in FOLLOW_STATUS_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">对象类型</Label>
            <Select v-model="filterForm.relatedObjectType">
              <SelectTrigger class="w-[150px]">
                <SelectValue placeholder="全部对象" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">全部对象</SelectItem>
                <SelectItem v-for="option in FOLLOW_OBJECT_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">对象ID</Label>
            <Input v-model="filterForm.relatedObjectId" class="w-[120px]" placeholder="对象ID" />
          </div>
          <label class="flex h-9 items-center gap-2 self-end text-sm text-[var(--crm-text-secondary)]">
            <input v-model="filterForm.overdueOnly" type="checkbox" class="h-4 w-4" />
            <span>仅逾期</span>
          </label>
          <div class="crm-toolbar-actions">
            <Button type="submit" class="gap-2" :disabled="taskLoading">
              <Search class="h-4 w-4" />
              查询
            </Button>
            <Button type="button" variant="outline" class="gap-2" :disabled="taskLoading" @click="handleReset">
              <RotateCcw class="h-4 w-4" />
              重置
            </Button>
          </div>
        </form>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="taskLoading" class="py-10 text-center text-[var(--crm-text-tertiary)]">加载中...</div>
        <Table v-else class="min-w-[1260px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[220px]">任务</TableHead>
              <TableHead class="w-[120px]">类型</TableHead>
              <TableHead class="w-[150px]">关联对象</TableHead>
              <TableHead class="w-[130px]">负责人</TableHead>
              <TableHead class="w-[150px]">计划时间</TableHead>
              <TableHead class="w-[100px]">状态</TableHead>
              <TableHead class="w-[90px]">优先级</TableHead>
              <TableHead class="w-[220px]">结果</TableHead>
              <TableHead class="w-[250px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="tasks.length === 0">
              <TableCell colspan="9" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无跟进任务
              </TableCell>
            </TableRow>
            <TableRow v-for="task in tasks" :key="task.id">
              <TableCell>
                <div class="max-w-[220px] truncate font-medium">{{ task.title }}</div>
                <div class="text-xs text-[var(--crm-text-tertiary)]">#{{ task.id }}</div>
              </TableCell>
              <TableCell>{{ formatFollowTaskType(task.taskType) }}</TableCell>
              <TableCell>
                <div>{{ formatFollowObjectType(task.relatedObjectType) }}</div>
                <div class="font-mono text-xs text-[var(--crm-text-tertiary)]">
                  {{ task.relatedObjectName || `#${task.relatedObjectId}` }}
                </div>
              </TableCell>
              <TableCell>{{ task.ownerName || `#${task.ownerId}` }}</TableCell>
              <TableCell>{{ formatDateTime(task.dueTime) }}</TableCell>
              <TableCell>
                <StatusBadge :label="formatFollowStatus(task.status)" :tone="getFollowStatusTone(task.status)" />
              </TableCell>
              <TableCell>{{ formatPriority(task.priority) }}</TableCell>
              <TableCell class="max-w-[220px] truncate">{{ task.result || task.postponeReason || task.cancelReason || '--' }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-if="task.status !== 'IN_PROGRESS' && !isFollowTaskTerminal(task.status)" v-has-permission="PERMISSIONS.followTask.update" label="开始" @click="handleStart(task)">
                    <Play class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-if="!isFollowTaskTerminal(task.status)" v-has-permission="PERMISSIONS.followTask.update" label="延期" @click="openPostpone(task)">
                    <Clock class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-if="!isFollowTaskTerminal(task.status)" v-has-permission="PERMISSIONS.followTask.complete" label="完成" @click="openComplete(task)">
                    <CheckCircle class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-if="!isFollowTaskTerminal(task.status)" v-has-permission="PERMISSIONS.followTask.cancel" label="取消" danger @click="openCancel(task)">
                    <CircleX class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-has-permission="PERMISSIONS.communicationRecord.create" label="沟通" @click="openRecordCreate(task)">
                    <MessageSquare class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-has-permission="PERMISSIONS.communicationRecord.list" label="记录" @click="filterRecordsByTask(task)">
                    <ListFilter class="h-4 w-4" />
                  </RowActionButton>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="border-t border-[var(--crm-border-light)] px-5 py-4">
        <DataTablePagination :page="taskPage" :page-size="taskPageSize" :total="taskTotal" @change="handleTaskPageChange" />
      </div>
    </section>

    <section class="crm-panel">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <h3 class="text-base font-semibold">沟通记录</h3>
        <Button variant="outline" size="sm" class="gap-2" :disabled="recordLoading" @click="loadRecords">
          <RotateCcw class="h-4 w-4" />
          刷新
        </Button>
      </div>
      <div class="crm-table-shell">
        <div v-if="recordLoading" class="py-10 text-center text-[var(--crm-text-tertiary)]">加载中...</div>
        <Table v-else class="min-w-[1080px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[150px]">时间</TableHead>
              <TableHead class="w-[110px]">方式</TableHead>
              <TableHead class="w-[150px]">关联对象</TableHead>
              <TableHead>摘要</TableHead>
              <TableHead class="w-[180px]">下一步</TableHead>
              <TableHead class="w-[100px]">状态</TableHead>
              <TableHead class="w-[100px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="records.length === 0">
              <TableCell colspan="7" class="h-28 text-center text-[var(--crm-text-tertiary)]">
                暂无沟通记录
              </TableCell>
            </TableRow>
            <TableRow v-for="record in records" :key="record.id">
              <TableCell>{{ formatDateTime(record.communicationTime) }}</TableCell>
              <TableCell>{{ formatCommunicationMethod(record.communicationMethod) }}</TableCell>
              <TableCell>
                <div>{{ formatFollowObjectType(record.relatedObjectType) }}</div>
                <div class="font-mono text-xs text-[var(--crm-text-tertiary)]">
                  {{ record.relatedObjectName || `#${record.relatedObjectId}` }}
                </div>
              </TableCell>
              <TableCell class="max-w-[360px] truncate">{{ record.summary }}</TableCell>
              <TableCell class="max-w-[180px] truncate">{{ record.nextAction || '--' }}</TableCell>
              <TableCell>{{ formatCommunicationStatus(record.status) }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-if="record.status === 'ACTIVE'" v-has-permission="PERMISSIONS.communicationRecord.correct" label="更正" @click="openCorrect(record)">
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-if="record.status === 'ACTIVE'" v-has-permission="PERMISSIONS.communicationRecord.void" label="作废" danger @click="openVoid(record)">
                    <Ban class="h-4 w-4" />
                  </RowActionButton>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </section>

    <Dialog v-model:open="taskDialogVisible">
      <DialogContent class="sm:max-w-[680px]">
        <DialogHeader>
          <DialogTitle>新增跟进任务</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleTaskCreate">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>任务标题</Label>
              <Input v-model="taskForm.title" />
            </div>
            <div class="space-y-2">
              <Label>任务类型</Label>
              <Select v-model="taskForm.taskType">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in FOLLOW_TASK_TYPE_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="space-y-2">
              <Label>对象类型</Label>
              <Select v-model="taskForm.relatedObjectType">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in FOLLOW_OBJECT_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>对象ID</Label>
              <Input v-model="taskForm.relatedObjectId" />
            </div>
            <div class="space-y-2">
              <Label>负责人ID</Label>
              <Input v-model="taskForm.ownerId" />
            </div>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="space-y-2">
              <Label>优先级</Label>
              <Select v-model="taskForm.priority">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in FOLLOW_PRIORITY_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>计划时间</Label>
              <Input v-model="taskForm.dueTime" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label>提醒时间</Label>
              <Input v-model="taskForm.remindTime" type="datetime-local" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="taskDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleTaskCreate">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="postponeDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>延期跟进</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handlePostpone">
          <div class="space-y-2">
            <Label>新的计划时间</Label>
            <Input v-model="postponeForm.newDueTime" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>提醒时间</Label>
            <Input v-model="postponeForm.remindTime" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>延期原因</Label>
            <Textarea v-model="postponeForm.reason" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="postponeDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handlePostpone">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="cancelDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>取消跟进</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCancel">
          <div class="space-y-2">
            <Label>取消原因</Label>
            <Textarea v-model="cancelForm.reason" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="cancelDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCancel">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="completeDialogVisible">
      <DialogContent class="sm:max-w-[680px]">
        <DialogHeader>
          <DialogTitle>完成跟进</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleComplete">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>沟通方式</Label>
              <Select v-model="completeForm.communicationMethod">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in COMMUNICATION_METHOD_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>沟通时间</Label>
              <Input v-model="completeForm.communicationTime" type="datetime-local" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>跟进摘要</Label>
            <Textarea v-model="completeForm.summary" :rows="3" />
          </div>
          <div class="space-y-2">
            <Label>完成结果</Label>
            <Input v-model="completeForm.result" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>客户反馈</Label>
              <Textarea v-model="completeForm.customerFeedback" :rows="2" />
            </div>
            <div class="space-y-2">
              <Label>下一步动作</Label>
              <Textarea v-model="completeForm.nextAction" :rows="2" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>下次跟进时间</Label>
            <Input v-model="completeForm.nextFollowTime" type="datetime-local" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="completeDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleComplete">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="recordDialogVisible">
      <DialogContent class="sm:max-w-[680px]">
        <DialogHeader>
          <DialogTitle>新增沟通记录</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleRecordCreate">
          <div class="grid grid-cols-3 gap-3">
            <div class="space-y-2">
              <Label>对象类型</Label>
              <Select v-model="recordForm.relatedObjectType">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in FOLLOW_OBJECT_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>对象ID</Label>
              <Input v-model="recordForm.relatedObjectId" />
            </div>
            <div class="space-y-2">
              <Label>任务ID</Label>
              <Input v-model="recordForm.followTaskId" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>沟通方式</Label>
              <Select v-model="recordForm.communicationMethod">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in COMMUNICATION_METHOD_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>沟通时间</Label>
              <Input v-model="recordForm.communicationTime" type="datetime-local" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>沟通摘要</Label>
            <Textarea v-model="recordForm.summary" :rows="3" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>客户反馈</Label>
              <Textarea v-model="recordForm.customerFeedback" :rows="2" />
            </div>
            <div class="space-y-2">
              <Label>下一步动作</Label>
              <Textarea v-model="recordForm.nextAction" :rows="2" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>下次跟进时间</Label>
            <Input v-model="recordForm.nextFollowTime" type="datetime-local" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="recordDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleRecordCreate">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="correctDialogVisible">
      <DialogContent class="sm:max-w-[620px]">
        <DialogHeader>
          <DialogTitle>更正沟通记录</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCorrect">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>沟通方式</Label>
              <Select v-model="correctForm.communicationMethod">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in COMMUNICATION_METHOD_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>沟通时间</Label>
              <Input v-model="correctForm.communicationTime" type="datetime-local" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>沟通摘要</Label>
            <Textarea v-model="correctForm.summary" :rows="3" />
          </div>
          <div class="space-y-2">
            <Label>更正原因</Label>
            <Textarea v-model="correctForm.correctionReason" :rows="2" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="correctDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCorrect">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="voidDialogVisible">
      <DialogContent class="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle>作废沟通记录</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleVoid">
          <div class="space-y-2">
            <Label>作废原因</Label>
            <Textarea v-model="voidForm.reason" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="voidDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleVoid">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  Ban,
  CheckCircle,
  CircleX,
  Clock,
  ListFilter,
  MessageSquare,
  MessageSquarePlus,
  Pencil,
  Play,
  Plus,
  RotateCcw,
  Search,
} from '@lucide/vue'
import {
  cancelFollowTask,
  completeFollowTask,
  correctCommunicationRecord,
  createCommunicationRecord,
  createFollowTask,
  fetchCommunicationRecordPage,
  fetchFollowTaskPage,
  postponeFollowTask,
  startFollowTask,
  voidCommunicationRecord,
} from '@/modules/follow/api/follow-api'
import {
  COMMUNICATION_METHOD_OPTIONS,
  FOLLOW_OBJECT_OPTIONS,
  FOLLOW_PRIORITY_OPTIONS,
  FOLLOW_STATUS_OPTIONS,
  FOLLOW_TASK_TYPE_OPTIONS,
  formatCommunicationMethod,
  formatCommunicationStatus,
  formatFollowObjectType,
  formatFollowStatus,
  formatFollowTaskType,
  getFollowStatusTone,
  isFollowTaskTerminal,
  type CommunicationMethod,
  type CommunicationRecord,
  type CommunicationRecordQuery,
  type FollowRelatedObjectType,
  type FollowTask,
  type FollowTaskPriority,
  type FollowTaskQuery,
  type FollowTaskStatus,
  type FollowTaskType,
} from '@/modules/follow/model/follow.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import type { EntityId } from '@/shared/types/id'
import { messageTip } from '@/shared/utils/feedback'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'

type SelectAll = 'ALL'

const taskLoading = ref(false)
const recordLoading = ref(false)
const submitting = ref(false)
const tasks = ref<FollowTask[]>([])
const records = ref<CommunicationRecord[]>([])
const taskTotal = ref(0)
const taskPage = ref(1)
const taskPageSize = 10
const selectedTask = ref<FollowTask | null>(null)
const selectedRecord = ref<CommunicationRecord | null>(null)

const filterForm = ref<{
  keyword: string
  status: FollowTaskStatus | SelectAll
  relatedObjectType: FollowRelatedObjectType | SelectAll
  relatedObjectId: string
  overdueOnly: boolean
}>({
  keyword: '',
  status: 'ALL',
  relatedObjectType: 'ALL',
  relatedObjectId: '',
  overdueOnly: false,
})

const taskDialogVisible = ref(false)
const postponeDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const recordDialogVisible = ref(false)
const correctDialogVisible = ref(false)
const voidDialogVisible = ref(false)

const taskForm = ref({
  title: '',
  taskType: 'PHONE_FOLLOW_UP' as FollowTaskType,
  relatedObjectType: 'CUSTOMER' as FollowRelatedObjectType,
  relatedObjectId: '',
  ownerId: '',
  priority: 'NORMAL' as FollowTaskPriority,
  dueTime: '',
  remindTime: '',
})

const postponeForm = ref({ newDueTime: '', remindTime: '', reason: '' })
const cancelForm = ref({ reason: '' })
const completeForm = ref({
  communicationMethod: 'PHONE' as CommunicationMethod,
  communicationTime: '',
  summary: '',
  customerFeedback: '',
  result: '',
  nextAction: '',
  nextFollowTime: '',
})
const recordForm = ref({
  followTaskId: '',
  relatedObjectType: 'CUSTOMER' as FollowRelatedObjectType,
  relatedObjectId: '',
  communicationMethod: 'PHONE' as CommunicationMethod,
  communicationTime: '',
  summary: '',
  customerFeedback: '',
  nextAction: '',
  nextFollowTime: '',
})
const correctForm = ref({
  communicationMethod: 'PHONE' as CommunicationMethod,
  communicationTime: '',
  summary: '',
  customerFeedback: '',
  nextAction: '',
  nextFollowTime: '',
  correctionReason: '',
})
const voidForm = ref({ reason: '' })
const recordQuery = ref<CommunicationRecordQuery>({ page: 1, size: 20 })

onMounted(() => {
  void loadTasks()
  void loadRecords()
})

async function loadTasks() {
  taskLoading.value = true
  try {
    const query: FollowTaskQuery = {
      page: taskPage.value,
      size: taskPageSize,
      keyword: filterForm.value.keyword.trim() || undefined,
      status: filterForm.value.status === 'ALL' ? undefined : filterForm.value.status,
      relatedObjectType:
        filterForm.value.relatedObjectType === 'ALL' ? undefined : filterForm.value.relatedObjectType,
      relatedObjectId: filterForm.value.relatedObjectId.trim()
        ? toEntityId(filterForm.value.relatedObjectId)
        : undefined,
      overdueOnly: filterForm.value.overdueOnly || undefined,
    }
    const page = await fetchFollowTaskPage(query)
    tasks.value = page.list
    taskTotal.value = page.total
  } finally {
    taskLoading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const page = await fetchCommunicationRecordPage(recordQuery.value)
    records.value = page.list
  } finally {
    recordLoading.value = false
  }
}

function handleSearch() {
  taskPage.value = 1
  void loadTasks()
}

function handleReset() {
  filterForm.value = {
    keyword: '',
    status: 'ALL',
    relatedObjectType: 'ALL',
    relatedObjectId: '',
    overdueOnly: false,
  }
  taskPage.value = 1
  void loadTasks()
}

function handleTaskPageChange(page: number) {
  taskPage.value = page
  void loadTasks()
}

function openTaskCreate() {
  taskForm.value = {
    title: '',
    taskType: 'PHONE_FOLLOW_UP',
    relatedObjectType: 'CUSTOMER',
    relatedObjectId: '',
    ownerId: '',
    priority: 'NORMAL',
    dueTime: '',
    remindTime: '',
  }
  taskDialogVisible.value = true
}

async function handleTaskCreate() {
  submitting.value = true
  try {
    await createFollowTask({
      title: taskForm.value.title.trim(),
      taskType: taskForm.value.taskType,
      relatedObjectType: taskForm.value.relatedObjectType,
      relatedObjectId: toEntityId(taskForm.value.relatedObjectId),
      ownerId: toEntityId(taskForm.value.ownerId),
      priority: taskForm.value.priority,
      dueTime: toApiDateTime(taskForm.value.dueTime),
      remindTime: optionalDateTime(taskForm.value.remindTime),
    })
    taskDialogVisible.value = false
    messageTip.success('已创建跟进任务')
    await loadTasks()
  } finally {
    submitting.value = false
  }
}

async function handleStart(task: FollowTask) {
  await startFollowTask(task.id)
  messageTip.success('已开始处理')
  await loadTasks()
}

function openPostpone(task: FollowTask) {
  selectedTask.value = task
  postponeForm.value = { newDueTime: '', remindTime: '', reason: '' }
  postponeDialogVisible.value = true
}

async function handlePostpone() {
  if (!selectedTask.value) return
  submitting.value = true
  try {
    await postponeFollowTask(selectedTask.value.id, {
      newDueTime: toApiDateTime(postponeForm.value.newDueTime),
      remindTime: optionalDateTime(postponeForm.value.remindTime),
      reason: postponeForm.value.reason.trim(),
    })
    postponeDialogVisible.value = false
    messageTip.success('已延期')
    await loadTasks()
  } finally {
    submitting.value = false
  }
}

function openCancel(task: FollowTask) {
  selectedTask.value = task
  cancelForm.value = { reason: '' }
  cancelDialogVisible.value = true
}

async function handleCancel() {
  if (!selectedTask.value) return
  submitting.value = true
  try {
    await cancelFollowTask(selectedTask.value.id, { reason: cancelForm.value.reason.trim() })
    cancelDialogVisible.value = false
    messageTip.success('已取消')
    await loadTasks()
  } finally {
    submitting.value = false
  }
}

function openComplete(task: FollowTask) {
  selectedTask.value = task
  completeForm.value = {
    communicationMethod: 'PHONE',
    communicationTime: '',
    summary: '',
    customerFeedback: '',
    result: '',
    nextAction: '',
    nextFollowTime: '',
  }
  completeDialogVisible.value = true
}

async function handleComplete() {
  if (!selectedTask.value) return
  submitting.value = true
  try {
    await completeFollowTask(selectedTask.value.id, {
      communicationMethod: completeForm.value.communicationMethod,
      communicationTime: optionalDateTime(completeForm.value.communicationTime),
      summary: completeForm.value.summary.trim(),
      customerFeedback: optionalText(completeForm.value.customerFeedback),
      result: completeForm.value.result.trim(),
      nextAction: optionalText(completeForm.value.nextAction),
      nextFollowTime: optionalDateTime(completeForm.value.nextFollowTime),
    })
    completeDialogVisible.value = false
    messageTip.success('已完成跟进')
    await loadTasks()
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function openRecordCreate(task?: FollowTask) {
  selectedTask.value = task ?? null
  recordForm.value = {
    followTaskId: task ? String(task.id) : '',
    relatedObjectType: task?.relatedObjectType ?? 'CUSTOMER',
    relatedObjectId: task ? String(task.relatedObjectId) : '',
    communicationMethod: 'PHONE',
    communicationTime: '',
    summary: '',
    customerFeedback: '',
    nextAction: '',
    nextFollowTime: '',
  }
  recordDialogVisible.value = true
}

async function handleRecordCreate() {
  submitting.value = true
  try {
    await createCommunicationRecord({
      followTaskId: optionalId(recordForm.value.followTaskId),
      relatedObjectType: recordForm.value.relatedObjectType,
      relatedObjectId: toEntityId(recordForm.value.relatedObjectId),
      communicationMethod: recordForm.value.communicationMethod,
      communicationTime: optionalDateTime(recordForm.value.communicationTime),
      summary: recordForm.value.summary.trim(),
      customerFeedback: optionalText(recordForm.value.customerFeedback),
      nextAction: optionalText(recordForm.value.nextAction),
      nextFollowTime: optionalDateTime(recordForm.value.nextFollowTime),
    })
    recordDialogVisible.value = false
    messageTip.success('已新增沟通记录')
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function filterRecordsByTask(task: FollowTask) {
  recordQuery.value = { page: 1, size: 20, followTaskId: task.id }
  void loadRecords()
}

function openCorrect(record: CommunicationRecord) {
  selectedRecord.value = record
  correctForm.value = {
    communicationMethod: record.communicationMethod,
    communicationTime: toDateTimeInput(record.communicationTime),
    summary: record.summary,
    customerFeedback: record.customerFeedback ?? '',
    nextAction: record.nextAction ?? '',
    nextFollowTime: toDateTimeInput(record.nextFollowTime),
    correctionReason: '',
  }
  correctDialogVisible.value = true
}

async function handleCorrect() {
  if (!selectedRecord.value) return
  submitting.value = true
  try {
    await correctCommunicationRecord(selectedRecord.value.id, {
      communicationMethod: correctForm.value.communicationMethod,
      communicationTime: optionalDateTime(correctForm.value.communicationTime),
      summary: correctForm.value.summary.trim(),
      customerFeedback: optionalText(correctForm.value.customerFeedback),
      nextAction: optionalText(correctForm.value.nextAction),
      nextFollowTime: optionalDateTime(correctForm.value.nextFollowTime),
      correctionReason: correctForm.value.correctionReason.trim(),
    })
    correctDialogVisible.value = false
    messageTip.success('已更正沟通记录')
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function openVoid(record: CommunicationRecord) {
  selectedRecord.value = record
  voidForm.value = { reason: '' }
  voidDialogVisible.value = true
}

async function handleVoid() {
  if (!selectedRecord.value) return
  submitting.value = true
  try {
    await voidCommunicationRecord(selectedRecord.value.id, { reason: voidForm.value.reason.trim() })
    voidDialogVisible.value = false
    messageTip.success('已作废沟通记录')
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function toEntityId(value: string): EntityId {
  const trimmed = value.trim()
  const numeric = Number(trimmed)
  return Number.isFinite(numeric) && trimmed !== '' ? numeric : trimmed
}

function optionalId(value: string): EntityId | undefined {
  return value.trim() ? toEntityId(value) : undefined
}

function toApiDateTime(value: string): string {
  return value.trim()
}

function optionalDateTime(value?: string): string | undefined {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function optionalText(value: string): string | undefined {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

function toDateTimeInput(value?: string): string {
  if (!value) return ''
  return value.slice(0, 16)
}

function formatDateTime(value?: string): string {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 16)
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day} ${hour}:${minute}`
}

function formatPriority(priority?: string): string {
  return FOLLOW_PRIORITY_OPTIONS.find(option => option.value === priority)?.label ?? priority ?? '--'
}
</script>
