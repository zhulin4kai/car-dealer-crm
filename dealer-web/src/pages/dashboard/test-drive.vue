<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">试驾管理</h2>
            <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
              {{ total }} 条
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">管理预约、改期、到店签到、完成反馈、取消和爽约事实。</p>
        </div>
        <Button v-has-permission="PERMISSIONS.testDrive.create" class="gap-2" @click="openCreate">
          <Plus class="h-4 w-4" />
          新增预约
        </Button>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">关键词</Label>
            <Input v-model="filterForm.keyword" class="w-[220px]" placeholder="试驾编号/客户/VIN" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">客户ID</Label>
            <Input v-model="filterForm.customerId" class="w-[120px]" placeholder="客户ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">车辆ID</Label>
            <Input v-model="filterForm.vehicleId" class="w-[120px]" placeholder="车辆ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">状态</Label>
            <Select v-model="filterForm.status">
              <SelectTrigger class="w-[150px]">
                <SelectValue placeholder="全部状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in statusFilterOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="crm-toolbar-actions">
            <Button type="submit" class="gap-2" :disabled="loading">
              <Search class="h-4 w-4" />
              查询
            </Button>
            <Button type="button" variant="outline" class="gap-2" :disabled="loading" @click="handleReset">
              <RotateCcw class="h-4 w-4" />
              重置
            </Button>
          </div>
        </form>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="loading" class="py-10 text-center text-[var(--crm-text-tertiary)]">加载中...</div>
        <Table v-else class="min-w-[1240px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[170px]">试驾编号</TableHead>
              <TableHead class="w-[140px]">客户</TableHead>
              <TableHead class="w-[160px]">车辆</TableHead>
              <TableHead class="w-[150px]">负责人</TableHead>
              <TableHead class="w-[150px]">开始</TableHead>
              <TableHead class="w-[150px]">结束</TableHead>
              <TableHead class="w-[120px]">状态</TableHead>
              <TableHead class="w-[110px]">改期次数</TableHead>
              <TableHead class="w-[290px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="testDrives.length === 0">
              <TableCell colspan="9" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无试驾记录
              </TableCell>
            </TableRow>
            <TableRow v-for="item in testDrives" :key="item.id">
              <TableCell class="font-mono text-xs">{{ item.testDriveNo }}</TableCell>
              <TableCell>{{ item.customerName || `#${item.customerId}` }}</TableCell>
              <TableCell>
                <div class="max-w-[160px] truncate">{{ item.vehicleName || `#${item.vehicleId}` }}</div>
                <div class="font-mono text-xs text-[var(--crm-text-tertiary)]">{{ item.vin || '--' }}</div>
              </TableCell>
              <TableCell>{{ item.ownerName || `#${item.ownerId}` }}</TableCell>
              <TableCell>{{ formatDateTime(item.plannedStartTime) }}</TableCell>
              <TableCell>{{ formatDateTime(item.plannedEndTime) }}</TableCell>
              <TableCell>
                <StatusBadge :label="formatTestDriveStatus(item.status)" :tone="getTestDriveStatusTone(item.status)" />
              </TableCell>
              <TableCell>{{ item.rescheduleCount ?? 0 }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-has-permission="PERMISSIONS.testDrive.view" label="详情" @click="openDetail(item)">
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.ai.assistantUse"
                    label="询问 AI"
                    @click="openAiAssistant(item.id)"
                  >
                    <Sparkles class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canReschedule(item)"
                    v-has-permission="PERMISSIONS.testDrive.reschedule"
                    label="改期"
                    @click="openReschedule(item)"
                  >
                    <RefreshCw class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canCancel(item)"
                    v-has-permission="PERMISSIONS.testDrive.cancel"
                    label="取消"
                    @click="openCancel(item, 'cancel')"
                  >
                    <CircleX class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canCancel(item)"
                    v-has-permission="PERMISSIONS.testDrive.cancel"
                    label="爽约"
                    @click="openCancel(item, 'noShow')"
                  >
                    <Archive class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canCheckIn(item)"
                    v-has-permission="PERMISSIONS.testDrive.checkIn"
                    label="签到"
                    @click="openCheckIn(item)"
                  >
                    <ArrowRight class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="item.status === 'CHECKED_IN'"
                    v-has-permission="PERMISSIONS.testDrive.complete"
                    label="完成"
                    @click="openComplete(item)"
                  >
                    <Trophy class="h-4 w-4" />
                  </RowActionButton>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="crm-table-footer">
        <DataTablePagination :page="currentPage" :page-size="pageSize" :total="total" @change="handleCurrentChange" />
      </div>
    </section>

    <Dialog v-model:open="formDialogVisible">
      <DialogContent class="sm:max-w-[680px]">
        <DialogHeader>
          <DialogTitle>新增试驾预约</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCreate">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>客户</Label>
              <Select v-model="form.customerId">
                <SelectTrigger>
                  <SelectValue placeholder="选择客户" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="customer in customerOptions" :key="customer.customerId" :value="String(customer.customerId)">
                    {{ customer.customerName || `#${customer.customerId}` }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>商机ID</Label>
              <Input v-model="form.opportunityId" placeholder="可为空" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>试驾车辆</Label>
              <Select v-model="form.vehicleId">
                <SelectTrigger>
                  <SelectValue placeholder="选择可用车辆" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="vehicle in availableVehicles" :key="vehicle.id" :value="String(vehicle.id)">
                    {{ vehicle.productName || `#${vehicle.productId}` }} / {{ vehicle.vin }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label>联系人</Label>
              <Input v-model="form.contactName" placeholder="客户姓名" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>联系电话</Label>
              <Input v-model="form.contactPhone" placeholder="手机号" />
            </div>
            <div class="space-y-2">
              <Label>备注</Label>
              <Input v-model="form.remark" placeholder="预约备注" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>开始时间</Label>
              <Input v-model="form.plannedStartTime" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label>结束时间</Label>
              <Input v-model="form.plannedEndTime" type="datetime-local" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="formDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCreate">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[920px]">
        <DialogHeader>
          <DialogTitle>试驾详情</DialogTitle>
        </DialogHeader>
        <div v-if="selectedTestDrive" class="max-h-[68vh] space-y-5 overflow-y-auto pr-1">
          <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">试驾编号</span>
            <span>{{ selectedTestDrive.testDriveNo }}</span>
            <span class="text-[var(--crm-text-tertiary)]">客户</span>
            <span>{{ selectedTestDrive.customerName || `#${selectedTestDrive.customerId}` }}</span>
            <span class="text-[var(--crm-text-tertiary)]">商机</span>
            <span>{{ selectedTestDrive.opportunityNo || idLabel(selectedTestDrive.opportunityId) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">状态</span>
            <span>{{ formatTestDriveStatus(selectedTestDrive.status) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">到店时间</span>
            <span>{{ formatDateTime(selectedTestDrive.actualArriveTime) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">试驾结果</span>
            <span>{{ selectedTestDrive.result || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">客户反馈</span>
            <span>{{ selectedTestDrive.customerFeedback || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">下一步动作</span>
            <span>{{ selectedTestDrive.nextAction || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">取消原因</span>
            <span>{{ selectedTestDrive.cancelReason || '--' }}</span>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>原状态</TableHead>
                <TableHead>目标状态</TableHead>
                <TableHead>动作</TableHead>
                <TableHead>原因</TableHead>
                <TableHead class="w-[170px]">时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="history.length === 0">
                <TableCell colspan="5" class="h-24 text-center text-[var(--crm-text-tertiary)]">
                  暂无历史
                </TableCell>
              </TableRow>
              <TableRow v-for="item in history" :key="item.id">
                <TableCell>{{ formatTestDriveStatus(item.fromStatus) }}</TableCell>
                <TableCell>{{ formatTestDriveStatus(item.toStatus) }}</TableCell>
                <TableCell>{{ formatAction(item.actionType) }}</TableCell>
                <TableCell class="max-w-[260px] truncate">{{ item.reason || '--' }}</TableCell>
                <TableCell>{{ formatDateTime(item.operateTime) }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="rescheduleDialogVisible">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>试驾改期</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleReschedule">
          <div class="space-y-2">
            <Label>新车辆</Label>
            <Select v-model="rescheduleForm.vehicleId">
              <SelectTrigger>
                <SelectValue placeholder="保持原车辆或选择新车辆" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="vehicle in availableVehicles" :key="vehicle.id" :value="String(vehicle.id)">
                  {{ vehicle.productName || `#${vehicle.productId}` }} / {{ vehicle.vin }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>新开始时间</Label>
              <Input v-model="rescheduleForm.plannedStartTime" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label>新结束时间</Label>
              <Input v-model="rescheduleForm.plannedEndTime" type="datetime-local" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>改期原因</Label>
            <Textarea v-model="rescheduleForm.reason" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="rescheduleDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleReschedule">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="cancelDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>{{ cancelMode === 'noShow' ? '标记爽约' : '取消试驾' }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCancel">
          <div v-if="cancelMode === 'cancel'" class="space-y-2">
            <Label>取消类型</Label>
            <Select v-model="cancelForm.cancelType">
              <SelectTrigger>
                <SelectValue placeholder="选择取消类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CUSTOMER_CANCEL">客户取消</SelectItem>
                <SelectItem value="STORE_CANCEL">门店取消</SelectItem>
                <SelectItem value="VEHICLE_UNAVAILABLE">车辆不可用</SelectItem>
                <SelectItem value="OTHER">其他</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>原因</Label>
            <Textarea v-model="cancelForm.reason" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="cancelDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCancel">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="checkInDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>试驾签到</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCheckIn">
          <div class="space-y-2">
            <Label>到店时间</Label>
            <Input v-model="checkInForm.arrivedAt" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>客户确认方式</Label>
            <Input v-model="checkInForm.customerConfirmMethod" placeholder="例如 ONSITE_CONFIRM" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="checkInDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCheckIn">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="completeDialogVisible">
      <DialogContent class="sm:max-w-[640px]">
        <DialogHeader>
          <DialogTitle>完成试驾</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleComplete">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>实际开始</Label>
              <Input v-model="completeForm.actualStartTime" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label>实际结束</Label>
              <Input v-model="completeForm.actualEndTime" type="datetime-local" />
            </div>
          </div>
          <label class="flex items-center gap-2 text-sm">
            <input v-model="completeForm.safetyConfirmed" type="checkbox" class="h-4 w-4" />
            <span>已完成安全确认</span>
          </label>
          <div class="space-y-2">
            <Label>试驾结果</Label>
            <Input v-model="completeForm.result" placeholder="客户满意/需继续跟进/放弃" />
          </div>
          <div class="space-y-2">
            <Label>客户反馈</Label>
            <Textarea v-model="completeForm.customerFeedback" :rows="3" />
          </div>
          <div class="space-y-2">
            <Label>下一步动作</Label>
            <Textarea v-model="completeForm.nextAction" :rows="2" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="completeDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleComplete">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  Archive,
  ArrowRight,
  CircleX,
  Eye,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  Trophy,
} from '@lucide/vue'
import { fetchCustomerOptions } from '@/modules/customer/api/customer-api'
import { fetchProductVehicles } from '@/modules/product/api/product-api'
import type { ProductVehicle } from '@/modules/product/model/product.types'
import {
  cancelTestDrive,
  checkInTestDrive,
  completeTestDrive,
  createTestDrive,
  fetchTestDriveDetail,
  fetchTestDriveHistory,
  fetchTestDrivePage,
  markTestDriveNoShow,
  rescheduleTestDrive,
} from '@/modules/test-drive/api/test-drive-api'
import {
  formatTestDriveStatus,
  getTestDriveStatusTone,
  isTestDriveTerminal,
  TEST_DRIVE_STATUS_OPTIONS,
  type CancelTestDriveRequest,
  type TestDrive,
  type TestDriveQuery,
  type TestDriveStatus,
  type TestDriveStatusHistory,
} from '@/modules/test-drive/model/test-drive.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import type { SelectOption } from '@/shared/types/common'
import type { EntityId } from '@/shared/types/id'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { useAiAssistantStore } from '@/stores/ai-assistant.store'
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

const aiAssistantStore = useAiAssistantStore()

function openAiAssistant(id: EntityId): void {
  aiAssistantStore.openPanel({ objectType: 'TEST_DRIVE', objectId: String(id) })
}

type CustomerOption = SelectOption & {
  customerId?: EntityId
  customerName?: string
}

type CancelMode = 'cancel' | 'noShow'

const ALL_STATUS = '__ALL_TEST_DRIVE_STATUS__'
const statusFilterOptions = [{ value: ALL_STATUS, label: '全部状态' }, ...TEST_DRIVE_STATUS_OPTIONS]

const loading = ref(false)
const submitting = ref(false)
const testDrives = ref<TestDrive[]>([])
const history = ref<TestDriveStatusHistory[]>([])
const customerOptions = ref<Array<CustomerOption & { customerId: EntityId }>>([])
const availableVehicles = ref<ProductVehicle[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedTestDrive = ref<TestDrive | null>(null)
const cancelMode = ref<CancelMode>('cancel')

const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const rescheduleDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const checkInDialogVisible = ref(false)
const completeDialogVisible = ref(false)

const filterForm = ref({
  keyword: '',
  customerId: '',
  vehicleId: '',
  status: ALL_STATUS,
})
const form = ref({
  customerId: '',
  opportunityId: '',
  vehicleId: '',
  plannedStartTime: '',
  plannedEndTime: '',
  contactName: '',
  contactPhone: '',
  remark: '',
})
const rescheduleForm = ref({
  vehicleId: '',
  plannedStartTime: '',
  plannedEndTime: '',
  reason: '',
})
const cancelForm = ref<CancelTestDriveRequest>({
  cancelType: 'CUSTOMER_CANCEL',
  reason: '',
})
const checkInForm = ref({
  arrivedAt: '',
  customerConfirmMethod: 'ONSITE_CONFIRM',
})
const completeForm = ref({
  actualStartTime: '',
  actualEndTime: '',
  safetyConfirmed: false,
  result: '',
  customerFeedback: '',
  nextAction: '',
})

onMounted(() => {
  void Promise.all([loadTestDrives(), loadCustomerOptions(), loadAvailableVehicles()])
})

async function loadTestDrives() {
  loading.value = true
  try {
    const params: TestDriveQuery = {
      page: currentPage.value,
      size: pageSize.value,
    }
    const keyword = filterForm.value.keyword.trim()
    const customerId = parseOptionalId(filterForm.value.customerId)
    const vehicleId = parseOptionalId(filterForm.value.vehicleId)
    if (keyword) params.keyword = keyword
    if (customerId) params.customerId = customerId
    if (vehicleId) params.vehicleId = vehicleId
    if (filterForm.value.status !== ALL_STATUS) params.status = filterForm.value.status as TestDriveStatus
    const result = await fetchTestDrivePage(params)
    testDrives.value = result.list ?? []
    total.value = result.total ?? 0
  } catch {
    messageTip('加载试驾列表失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadCustomerOptions() {
  try {
    const options = (await fetchCustomerOptions()) as CustomerOption[]
    customerOptions.value = options.filter(
      (option): option is CustomerOption & { customerId: EntityId } =>
        option.customerId !== undefined && option.customerId !== null,
    )
  } catch {
    customerOptions.value = []
  }
}

async function loadAvailableVehicles() {
  try {
    const result = await fetchProductVehicles({ page: 1, size: 100, status: 'AVAILABLE' })
    availableVehicles.value = result.list ?? []
  } catch {
    availableVehicles.value = []
  }
}

function handleSearch() {
  currentPage.value = 1
  void loadTestDrives()
}

function handleReset() {
  filterForm.value = {
    keyword: '',
    customerId: '',
    vehicleId: '',
    status: ALL_STATUS,
  }
  currentPage.value = 1
  void loadTestDrives()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadTestDrives()
}

function openCreate() {
  form.value = {
    customerId: '',
    opportunityId: '',
    vehicleId: '',
    plannedStartTime: '',
    plannedEndTime: '',
    contactName: '',
    contactPhone: '',
    remark: '',
  }
  formDialogVisible.value = true
}

async function handleCreate() {
  const customerId = parseRequiredId(form.value.customerId, '客户')
  const vehicleId = parseRequiredId(form.value.vehicleId, '试驾车辆')
  const plannedStartTime = normalizeRequiredDateTime(form.value.plannedStartTime, '开始时间')
  const plannedEndTime = normalizeRequiredDateTime(form.value.plannedEndTime, '结束时间')
  const contactName = form.value.contactName.trim()
  const contactPhone = form.value.contactPhone.trim()
  if (!customerId || !vehicleId || !plannedStartTime || !plannedEndTime) return
  if (!contactName || !contactPhone) {
    messageTip('请填写联系人和联系电话', 'warning')
    return
  }
  submitting.value = true
  try {
    await createTestDrive({
      customerId,
      vehicleId,
      plannedStartTime,
      plannedEndTime,
      contactName,
      contactPhone,
      ...(parseOptionalId(form.value.opportunityId) ? { opportunityId: parseOptionalId(form.value.opportunityId) } : {}),
      ...(form.value.remark.trim() ? { remark: form.value.remark.trim() } : {}),
    })
    messageTip('试驾预约已创建', 'success')
    formDialogVisible.value = false
    void Promise.all([loadTestDrives(), loadAvailableVehicles()])
  } catch {
    messageTip('创建试驾预约失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function openDetail(item: TestDrive) {
  selectedTestDrive.value = item
  detailDialogVisible.value = true
  try {
    const [detail, rows] = await Promise.all([
      fetchTestDriveDetail(item.id),
      fetchTestDriveHistory(item.id),
    ])
    selectedTestDrive.value = detail
    history.value = rows
  } catch {
    messageTip('加载试驾详情失败', 'error')
  }
}

function openReschedule(item: TestDrive) {
  selectedTestDrive.value = item
  rescheduleForm.value = {
    vehicleId: String(item.vehicleId),
    plannedStartTime: toLocalInput(item.plannedStartTime),
    plannedEndTime: toLocalInput(item.plannedEndTime),
    reason: '',
  }
  rescheduleDialogVisible.value = true
}

async function handleReschedule() {
  if (!selectedTestDrive.value) return
  const plannedStartTime = normalizeRequiredDateTime(rescheduleForm.value.plannedStartTime, '新开始时间')
  const plannedEndTime = normalizeRequiredDateTime(rescheduleForm.value.plannedEndTime, '新结束时间')
  const reason = rescheduleForm.value.reason.trim()
  if (!plannedStartTime || !plannedEndTime) return
  if (!reason) {
    messageTip('请填写改期原因', 'warning')
    return
  }
  submitting.value = true
  try {
    await rescheduleTestDrive(selectedTestDrive.value.id, {
      plannedStartTime,
      plannedEndTime,
      reason,
      ...(parseOptionalId(rescheduleForm.value.vehicleId) ? { vehicleId: parseOptionalId(rescheduleForm.value.vehicleId) } : {}),
    })
    messageTip('试驾已改期', 'success')
    rescheduleDialogVisible.value = false
    void Promise.all([loadTestDrives(), loadAvailableVehicles()])
  } catch {
    messageTip('试驾改期失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openCancel(item: TestDrive, mode: CancelMode) {
  selectedTestDrive.value = item
  cancelMode.value = mode
  cancelForm.value = {
    cancelType: mode === 'noShow' ? 'NO_SHOW' : 'CUSTOMER_CANCEL',
    reason: '',
  }
  cancelDialogVisible.value = true
}

async function handleCancel() {
  if (!selectedTestDrive.value) return
  const reason = cancelForm.value.reason.trim()
  if (!reason) {
    messageTip('请填写原因', 'warning')
    return
  }
  submitting.value = true
  try {
    if (cancelMode.value === 'noShow') {
      await markTestDriveNoShow(selectedTestDrive.value.id, { cancelType: 'NO_SHOW', reason })
    } else {
      await cancelTestDrive(selectedTestDrive.value.id, {
        cancelType: cancelForm.value.cancelType,
        reason,
      })
    }
    messageTip('试驾状态已更新', 'success')
    cancelDialogVisible.value = false
    void Promise.all([loadTestDrives(), loadAvailableVehicles()])
  } catch {
    messageTip('处理试驾失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openCheckIn(item: TestDrive) {
  selectedTestDrive.value = item
  checkInForm.value = {
    arrivedAt: toLocalInput(new Date().toISOString()),
    customerConfirmMethod: 'ONSITE_CONFIRM',
  }
  checkInDialogVisible.value = true
}

async function handleCheckIn() {
  if (!selectedTestDrive.value) return
  const customerConfirmMethod = checkInForm.value.customerConfirmMethod.trim()
  if (!customerConfirmMethod) {
    messageTip('请填写客户确认方式', 'warning')
    return
  }
  submitting.value = true
  try {
    await checkInTestDrive(selectedTestDrive.value.id, {
      ...(checkInForm.value.arrivedAt ? { arrivedAt: normalizeDateTime(checkInForm.value.arrivedAt) } : {}),
      customerConfirmMethod,
    })
    messageTip('试驾已签到', 'success')
    checkInDialogVisible.value = false
    void loadTestDrives()
  } catch {
    messageTip('试驾签到失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openComplete(item: TestDrive) {
  selectedTestDrive.value = item
  completeForm.value = {
    actualStartTime: toLocalInput(item.actualArriveTime || item.plannedStartTime),
    actualEndTime: toLocalInput(new Date().toISOString()),
    safetyConfirmed: false,
    result: '',
    customerFeedback: '',
    nextAction: '',
  }
  completeDialogVisible.value = true
}

async function handleComplete() {
  if (!selectedTestDrive.value) return
  const result = completeForm.value.result.trim()
  const customerFeedback = completeForm.value.customerFeedback.trim()
  const nextAction = completeForm.value.nextAction.trim()
  if (!completeForm.value.safetyConfirmed) {
    messageTip('请确认已完成安全确认', 'warning')
    return
  }
  if (!result || !customerFeedback || !nextAction) {
    messageTip('请填写试驾结果、客户反馈和下一步动作', 'warning')
    return
  }
  submitting.value = true
  try {
    await completeTestDrive(selectedTestDrive.value.id, {
      safetyConfirmed: completeForm.value.safetyConfirmed,
      result,
      customerFeedback,
      nextAction,
      ...(completeForm.value.actualStartTime ? { actualStartTime: normalizeDateTime(completeForm.value.actualStartTime) } : {}),
      ...(completeForm.value.actualEndTime ? { actualEndTime: normalizeDateTime(completeForm.value.actualEndTime) } : {}),
    })
    messageTip('试驾已完成', 'success')
    completeDialogVisible.value = false
    void Promise.all([loadTestDrives(), loadAvailableVehicles()])
  } catch {
    messageTip('完成试驾失败', 'error')
  } finally {
    submitting.value = false
  }
}

function canReschedule(item: TestDrive): boolean {
  return item.status === 'SCHEDULED' || item.status === 'RESCHEDULED' || item.status === 'PENDING_CONFIRM'
}

function canCancel(item: TestDrive): boolean {
  return !isTestDriveTerminal(item.status) && item.status !== 'CHECKED_IN'
}

function canCheckIn(item: TestDrive): boolean {
  return item.status === 'SCHEDULED' || item.status === 'RESCHEDULED'
}

function parseOptionalId(value: string): EntityId | undefined {
  const trimmed = value.trim()
  if (!trimmed) return undefined
  const parsed = Number(trimmed)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

function parseRequiredId(value: string, label: string): EntityId | null {
  const id = parseOptionalId(value)
  if (!id) {
    messageTip(`请选择${label}`, 'warning')
    return null
  }
  return id
}

function normalizeRequiredDateTime(value: string, label: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) {
    messageTip(`请选择${label}`, 'warning')
    return null
  }
  return normalizeDateTime(trimmed)
}

function normalizeDateTime(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return trimmed
  const withoutZone = trimmed.replace('Z', '').slice(0, 19)
  return withoutZone.length === 16 ? `${withoutZone}:00` : withoutZone
}

function toLocalInput(value?: string): string {
  return value ? value.replace(' ', 'T').slice(0, 16) : ''
}

function idLabel(value?: EntityId): string {
  return value ? `#${value}` : '--'
}

function formatDateTime(value?: string): string {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 16)
}

function formatAction(value?: string): string {
  const map: Record<string, string> = {
    CREATE: '预约',
    RESCHEDULE: '改期',
    CANCEL: '取消',
    NO_SHOW: '爽约',
    CHECK_IN: '签到',
    COMPLETE: '完成',
  }
  return map[value ?? ''] ?? value ?? '--'
}
</script>
