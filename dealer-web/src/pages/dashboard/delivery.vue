<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">交付管理</h2>
            <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
              {{ total }} 单
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">管理交付记录、准备清单、签收和异常。</p>
        </div>
        <Button v-has-permission="PERMISSIONS.delivery.create" class="gap-2" @click="openCreate">
          <Plus class="h-4 w-4" />
          新增交付
        </Button>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">交易ID</Label>
            <Input v-model="filterForm.tranId" class="w-[140px]" placeholder="交易ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">客户ID</Label>
            <Input v-model="filterForm.customerId" class="w-[140px]" placeholder="客户ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">状态</Label>
            <Select v-model="filterForm.status">
              <SelectTrigger class="w-[180px]">
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
        <Table v-else class="min-w-[1080px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[90px]">交付ID</TableHead>
              <TableHead class="w-[100px]">交易ID</TableHead>
              <TableHead class="w-[100px]">客户ID</TableHead>
              <TableHead class="w-[110px]">车辆ID</TableHead>
              <TableHead class="w-[150px]">状态</TableHead>
              <TableHead class="w-[180px]">计划交付</TableHead>
              <TableHead class="w-[180px]">实际交付</TableHead>
              <TableHead class="w-[170px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="deliveryList.length === 0">
              <TableCell colspan="8" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无交付数据
              </TableCell>
            </TableRow>
            <TableRow v-for="delivery in deliveryList" :key="delivery.id">
              <TableCell class="font-mono text-xs">#{{ delivery.id }}</TableCell>
              <TableCell>{{ delivery.tranId }}</TableCell>
              <TableCell>{{ delivery.customerId }}</TableCell>
              <TableCell>{{ delivery.vehicleId }}</TableCell>
              <TableCell>
                <StatusBadge :label="formatDeliveryStatus(delivery.status)" :tone="getDeliveryStatusTone(delivery.status)" />
              </TableCell>
              <TableCell>{{ formatDateTime(delivery.plannedDeliveryTime) || '--' }}</TableCell>
              <TableCell>{{ formatDateTime(delivery.actualDeliveryTime) || '--' }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-has-permission="PERMISSIONS.delivery.view" label="详情" @click="openDetail(delivery)">
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-has-permission="PERMISSIONS.delivery.sign" label="签收" @click="openSign(delivery)">
                    <CheckCircle2 class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton v-has-permission="PERMISSIONS.delivery.exception" label="异常" @click="openException(delivery)">
                    <AlertTriangle class="h-4 w-4" />
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

    <Dialog v-model:open="createDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>新增交付</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleCreate">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>交易ID</Label>
              <Input v-model="createForm.tranId" placeholder="交易ID" />
            </div>
            <div class="space-y-2">
              <Label>车辆ID</Label>
              <Input v-model="createForm.vehicleId" placeholder="车辆ID" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>计划交付时间</Label>
            <Input v-model="createForm.plannedDeliveryTime" type="datetime-local" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="createDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleCreate">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[920px]">
        <DialogHeader>
          <DialogTitle>交付详情</DialogTitle>
        </DialogHeader>
        <div v-if="selectedDelivery" class="max-h-[68vh] space-y-5 overflow-y-auto pr-1">
          <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">交易ID</span>
            <span>{{ selectedDelivery.tranId }}</span>
            <span class="text-[var(--crm-text-tertiary)]">车辆ID</span>
            <span>{{ selectedDelivery.vehicleId }}</span>
            <span class="text-[var(--crm-text-tertiary)]">状态</span>
            <span>{{ formatDeliveryStatus(selectedDelivery.status) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">签收人</span>
            <span>{{ selectedDelivery.signerName || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">签收时间</span>
            <span>{{ formatDateTime(selectedDelivery.signedAt) || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">异常原因</span>
            <span>{{ selectedDelivery.exceptionReason || '--' }}</span>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>准备项</TableHead>
                <TableHead class="w-[130px]">状态</TableHead>
                <TableHead class="w-[180px]">完成时间</TableHead>
                <TableHead>备注</TableHead>
                <TableHead class="w-[90px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="item in checkItems" :key="item.id">
                <TableCell>{{ item.itemName }}</TableCell>
                <TableCell>
                  <StatusBadge :label="formatDeliveryCheckStatus(item.status)" :tone="getDeliveryCheckStatusTone(item.status)" />
                </TableCell>
                <TableCell>{{ formatDateTime(item.completedTime) || '--' }}</TableCell>
                <TableCell class="max-w-[260px] truncate">{{ item.remark || '--' }}</TableCell>
                <TableCell>
                  <RowActionButton v-has-permission="PERMISSIONS.delivery.check" label="更新" @click="openCheckItem(item)">
                    <ClipboardCheck class="h-4 w-4" />
                  </RowActionButton>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
        <DialogFooter>
          <Button
            v-if="selectedDelivery && canCancelDelivery(selectedDelivery.status)"
            v-has-permission="PERMISSIONS.delivery.cancel"
            variant="outline"
            class="gap-2"
            @click="openCancel(selectedDelivery)"
          >
            <XCircle class="h-4 w-4" />
            取消交付
          </Button>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="checkDialogVisible">
      <DialogContent class="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle>更新准备项</DialogTitle>
        </DialogHeader>
        <div class="grid gap-4">
          <div class="space-y-2">
            <Label>状态</Label>
            <Select v-model="checkForm.status">
              <SelectTrigger>
                <SelectValue placeholder="选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="PENDING">待处理</SelectItem>
                <SelectItem value="COMPLETED">已完成</SelectItem>
                <SelectItem value="BLOCKED">已阻塞</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="checkForm.remark" :rows="3" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="checkDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleUpdateCheck">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="signDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>客户签收</DialogTitle>
        </DialogHeader>
        <div class="grid gap-4">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>签收人</Label>
              <Input v-model="signForm.signerName" placeholder="签收人" />
            </div>
            <div class="space-y-2">
              <Label>签收方式</Label>
              <Input v-model="signForm.signMethod" placeholder="PAPER / ELECTRONIC" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>签收时间</Label>
            <Input v-model="signForm.signedAt" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>签收凭证</Label>
            <Input v-model="signForm.signEvidence" placeholder="凭证编号或文件地址" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="signDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleSign">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="exceptionDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>登记交付异常</DialogTitle>
        </DialogHeader>
        <div class="grid gap-4">
          <div class="space-y-2">
            <Label>异常类型</Label>
            <Input v-model="exceptionForm.exceptionType" placeholder="VEHICLE / DOCUMENT / CUSTOMER / PAYMENT" />
          </div>
          <div class="space-y-2">
            <Label>异常原因</Label>
            <Textarea v-model="exceptionForm.reason" :rows="3" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="exceptionDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleException">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="cancelDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>取消交付</DialogTitle>
        </DialogHeader>
        <div class="space-y-2">
          <Label>取消原因</Label>
          <Textarea v-model="cancelForm.reason" :rows="3" />
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="cancelDialogVisible = false">取消</Button>
          <Button variant="destructive" :disabled="submitting" @click="handleCancel">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { AlertTriangle, CheckCircle2, ClipboardCheck, Eye, Plus, RotateCcw, Search, XCircle } from '@lucide/vue'
import {
  cancelDelivery,
  createDelivery,
  fetchDeliveryCheckItems,
  fetchDeliveryDetail,
  fetchDeliveryPage,
  markDeliveryException,
  signDelivery,
  updateDeliveryCheckItem,
} from '@/modules/delivery/api/delivery-api'
import {
  formatDeliveryCheckStatus,
  formatDeliveryStatus,
  canCancelDelivery,
  getDeliveryCheckStatusTone,
  getDeliveryStatusTone,
  type Delivery,
  type DeliveryCheckItem,
  type DeliveryCheckStatus,
  type DeliveryQuery,
  type DeliveryStatus,
} from '@/modules/delivery/model/delivery.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
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

const ALL_STATUS = '__ALL_DELIVERY_STATUS__'

const deliveryStatuses: Array<{ value: DeliveryStatus; label: string }> = [
  { value: 'PENDING_PREPARE', label: '待准备' },
  { value: 'PREPARING', label: '准备中' },
  { value: 'WAITING_CUSTOMER', label: '待客户确认' },
  { value: 'WAITING_DELIVERY', label: '待交付' },
  { value: 'DELIVERING', label: '交付中' },
  { value: 'SIGNED', label: '已签收' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'EXCEPTION', label: '交付异常' },
  { value: 'CANCELLED', label: '已取消' },
]
const statusFilterOptions = [{ value: ALL_STATUS, label: '全部状态' }, ...deliveryStatuses]

const loading = ref(false)
const submitting = ref(false)
const deliveryList = ref<Delivery[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterForm = ref({
  tranId: '',
  customerId: '',
  status: ALL_STATUS,
})
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const checkDialogVisible = ref(false)
const signDialogVisible = ref(false)
const exceptionDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const selectedDelivery = ref<Delivery | null>(null)
const selectedCheckItem = ref<DeliveryCheckItem | null>(null)
const checkItems = ref<DeliveryCheckItem[]>([])
const createForm = ref({
  tranId: '',
  vehicleId: '',
  plannedDeliveryTime: '',
})
const checkForm = ref<{ status: DeliveryCheckStatus; remark: string }>({
  status: 'PENDING',
  remark: '',
})
const signForm = ref({
  signerName: '',
  signedAt: '',
  signMethod: 'PAPER',
  signEvidence: '',
})
const exceptionForm = ref({
  exceptionType: '',
  reason: '',
})
const cancelForm = ref({
  reason: '',
})

onMounted(() => {
  void loadDeliveries()
})

async function loadDeliveries() {
  loading.value = true
  try {
    const params: DeliveryQuery = {
      page: currentPage.value,
      size: pageSize.value,
      tranId: parseOptionalId(filterForm.value.tranId),
      customerId: parseOptionalId(filterForm.value.customerId),
      status: filterForm.value.status === ALL_STATUS ? undefined : filterForm.value.status as DeliveryStatus,
    }
    const result = await fetchDeliveryPage(params)
    deliveryList.value = result.list ?? []
    total.value = result.total ?? 0
  } catch {
    messageTip('加载交付列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  void loadDeliveries()
}

function handleReset() {
  filterForm.value = {
    tranId: '',
    customerId: '',
    status: ALL_STATUS,
  }
  currentPage.value = 1
  void loadDeliveries()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadDeliveries()
}

function openCreate() {
  createForm.value = {
    tranId: '',
    vehicleId: '',
    plannedDeliveryTime: toDateTimeLocal(new Date()),
  }
  createDialogVisible.value = true
}

async function handleCreate() {
  const tranId = parseRequiredId(createForm.value.tranId, '交易ID')
  const vehicleId = parseRequiredId(createForm.value.vehicleId, '车辆ID')
  if (!tranId || !vehicleId) return
  if (!createForm.value.plannedDeliveryTime) {
    messageTip('请选择计划交付时间', 'warning')
    return
  }
  submitting.value = true
  try {
    await createDelivery({
      tranId,
      vehicleId,
      plannedDeliveryTime: createForm.value.plannedDeliveryTime,
    })
    createDialogVisible.value = false
    messageTip('交付记录已创建', 'success')
    void loadDeliveries()
  } catch {
    messageTip('创建交付记录失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function openDetail(delivery: Delivery) {
  selectedDelivery.value = delivery
  detailDialogVisible.value = true
  try {
    const [detail, items] = await Promise.all([
      fetchDeliveryDetail(delivery.id),
      fetchDeliveryCheckItems(delivery.id),
    ])
    selectedDelivery.value = detail
    checkItems.value = items
  } catch {
    messageTip('加载交付详情失败', 'error')
  }
}

function openCheckItem(item: DeliveryCheckItem) {
  selectedCheckItem.value = item
  checkForm.value = {
    status: item.status,
    remark: item.remark ?? '',
  }
  checkDialogVisible.value = true
}

async function handleUpdateCheck() {
  if (!selectedCheckItem.value || !selectedDelivery.value) return
  submitting.value = true
  try {
    await updateDeliveryCheckItem(selectedCheckItem.value.id, {
      status: checkForm.value.status,
      remark: checkForm.value.remark || undefined,
    })
    checkDialogVisible.value = false
    checkItems.value = await fetchDeliveryCheckItems(selectedDelivery.value.id)
    messageTip('准备项已更新', 'success')
  } catch {
    messageTip('更新准备项失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openSign(delivery: Delivery) {
  selectedDelivery.value = delivery
  signForm.value = {
    signerName: '',
    signedAt: toDateTimeLocal(new Date()),
    signMethod: 'PAPER',
    signEvidence: '',
  }
  signDialogVisible.value = true
}

async function handleSign() {
  if (!selectedDelivery.value) return
  if (!signForm.value.signerName.trim()) {
    messageTip('请输入签收人', 'warning')
    return
  }
  if (!signForm.value.signedAt) {
    messageTip('请选择签收时间', 'warning')
    return
  }
  if (!signForm.value.signMethod.trim()) {
    messageTip('请输入签收方式', 'warning')
    return
  }
  if (!signForm.value.signEvidence.trim()) {
    messageTip('请输入签收凭证', 'warning')
    return
  }
  submitting.value = true
  try {
    await signDelivery(selectedDelivery.value.id, {
      signerName: signForm.value.signerName.trim(),
      signedAt: signForm.value.signedAt,
      signMethod: signForm.value.signMethod.trim(),
      signEvidence: signForm.value.signEvidence.trim(),
    })
    signDialogVisible.value = false
    messageTip('交付签收已完成', 'success')
    void loadDeliveries()
  } catch {
    messageTip('交付签收失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openException(delivery: Delivery) {
  selectedDelivery.value = delivery
  exceptionForm.value = {
    exceptionType: '',
    reason: '',
  }
  exceptionDialogVisible.value = true
}

async function handleException() {
  if (!selectedDelivery.value) return
  if (!exceptionForm.value.exceptionType.trim() || !exceptionForm.value.reason.trim()) {
    messageTip('请输入异常类型和原因', 'warning')
    return
  }
  submitting.value = true
  try {
    await markDeliveryException(selectedDelivery.value.id, {
      exceptionType: exceptionForm.value.exceptionType.trim(),
      reason: exceptionForm.value.reason.trim(),
    })
    exceptionDialogVisible.value = false
    messageTip('交付异常已登记', 'success')
    void loadDeliveries()
  } catch {
    messageTip('登记交付异常失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openCancel(delivery: Delivery) {
  selectedDelivery.value = delivery
  cancelForm.value = { reason: '' }
  cancelDialogVisible.value = true
}

async function handleCancel() {
  if (!selectedDelivery.value) return
  if (!cancelForm.value.reason.trim()) {
    messageTip('请输入取消原因', 'warning')
    return
  }
  submitting.value = true
  try {
    await cancelDelivery(selectedDelivery.value.id, {
      reason: cancelForm.value.reason.trim(),
    })
    cancelDialogVisible.value = false
    detailDialogVisible.value = false
    messageTip('交付已取消', 'success')
    void loadDeliveries()
  } catch {
    messageTip('取消交付失败', 'error')
  } finally {
    submitting.value = false
  }
}

function parseOptionalId(value: string) {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
}

function parseRequiredId(value: string, label: string) {
  const parsed = parseOptionalId(value)
  if (!parsed) {
    messageTip(`${label}必须为正整数`, 'warning')
  }
  return parsed
}

function toDateTimeLocal(date: Date) {
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDateTime(dateTime?: string) {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').slice(0, 16)
}
</script>
