<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">促销管理</h2>
            <span
              class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]"
            >
              {{ total }} 条
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
            维护促销政策、适用范围、预算名额和状态流转。
          </p>
        </div>
        <Button v-has-permission="PERMISSIONS.product.promotion.add" class="gap-2" @click="handleAdd">
          <Plus class="h-4 w-4" />
          新增促销
        </Button>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="loading" class="py-10 text-center text-[var(--crm-text-tertiary)]">
          加载中...
        </div>
        <Table v-else class="min-w-[1320px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[130px]">编码</TableHead>
              <TableHead class="w-[220px]">促销名称</TableHead>
              <TableHead class="w-[120px]">类型</TableHead>
              <TableHead class="w-[130px]">优惠</TableHead>
              <TableHead class="w-[180px]">适用范围</TableHead>
              <TableHead class="w-[150px]">预算/名额</TableHead>
              <TableHead class="w-[190px]">有效期</TableHead>
              <TableHead class="w-[110px]">状态</TableHead>
              <TableHead class="w-[220px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="displayPromotionList.length === 0">
              <TableCell colspan="9" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无促销数据
              </TableCell>
            </TableRow>
            <TableRow v-for="row in displayPromotionList" :key="row.id">
              <TableCell class="font-mono text-xs">{{ row.code || '--' }}</TableCell>
              <TableCell>
                <div class="max-w-[220px] truncate font-semibold text-[var(--crm-text-primary)]">
                  {{ row.name || '--' }}
                </div>
                <div class="mt-1 max-w-[220px] truncate text-xs text-[var(--crm-text-tertiary)]">
                  {{ row.ruleSummary || '--' }}
                </div>
              </TableCell>
              <TableCell>
                <StatusBadge :label="formatPromotionType(row.type)" :tone="getPromotionTypeTone(row.type)" />
              </TableCell>
              <TableCell class="font-semibold text-[var(--crm-text-primary)]">
                {{ formatPromotionDiscount(row) }}
              </TableCell>
              <TableCell class="text-xs text-[var(--crm-text-secondary)]">
                <div>门店 {{ row.applicableStore || 'ALL' }}</div>
                <div>客户 {{ row.customerType || 'ALL' }}</div>
                <div>渠道 {{ row.applicableChannel || 'ALL' }}</div>
              </TableCell>
              <TableCell class="text-xs text-[var(--crm-text-secondary)]">
                <div>{{ formatBudget(row) }}</div>
                <div>{{ formatUsage(row) }}</div>
              </TableCell>
              <TableCell class="text-xs">
                <div>{{ formatDateTime(row.startTime) }}</div>
                <div class="text-[var(--crm-text-tertiary)]">{{ formatDateTime(row.endTime) }}</div>
              </TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatPromotionStatus(row.status)"
                  :tone="getPromotionStatusTone(row.status)"
                />
              </TableCell>
              <TableCell>
                <div class="flex flex-wrap items-center gap-1">
                  <RowActionButton
                    v-if="canPublish(row)"
                    v-has-permission="PERMISSIONS.product.promotion.status"
                    label="发布"
                    @click="handleStatusAction(row, 'publish')"
                  >
                    <Send class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canActivate(row)"
                    v-has-permission="PERMISSIONS.product.promotion.status"
                    label="生效"
                    @click="handleStatusAction(row, 'activate')"
                  >
                    <Play class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canPause(row)"
                    v-has-permission="PERMISSIONS.product.promotion.status"
                    label="暂停"
                    @click="openReasonDialog(row, 'pause')"
                  >
                    <Pause class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canEnd(row)"
                    v-has-permission="PERMISSIONS.product.promotion.status"
                    label="结束"
                    @click="openReasonDialog(row, 'end')"
                  >
                    <StopCircle class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canVoid(row)"
                    v-has-permission="PERMISSIONS.product.promotion.status"
                    label="作废"
                    danger
                    @click="openReasonDialog(row, 'void')"
                  >
                    <Ban class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="canEdit(row)"
                    v-has-permission="PERMISSIONS.product.promotion.edit"
                    label="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="row.status === PRODUCT_PROMOTION_STATUS.DRAFT"
                    v-has-permission="PERMISSIONS.product.promotion.delete"
                    label="删除"
                    danger
                    @click="handleDelete(row)"
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
          @change="handleCurrentChange"
        />
      </div>
    </section>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[760px]">
        <DialogHeader>
          <DialogTitle>{{ dialogType === 'add' ? '新增促销' : '编辑促销' }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4 md:grid-cols-2" @submit.prevent="handleSubmit">
          <div class="space-y-2">
            <Label>商品</Label>
            <Select v-model="promotionForm.productId">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择商品" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in productOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>促销编码</Label>
            <Input v-model="promotionForm.code" />
          </div>
          <div class="space-y-2">
            <Label>促销名称</Label>
            <Input v-model="promotionForm.name" />
          </div>
          <div class="space-y-2">
            <Label>促销类型</Label>
            <Select v-model="promotionForm.type">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in promotionTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>优惠值</Label>
            <NumberField
              v-model="promotionForm.discount"
              :min="0"
              :max="promotionForm.type === PRODUCT_PROMOTION_TYPE.PERCENTAGE ? 0.99 : 999999"
              :step="promotionForm.type === PRODUCT_PROMOTION_TYPE.PERCENTAGE ? 0.01 : 1"
            >
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="space-y-2">
            <Label>叠加规则</Label>
            <div class="flex h-10 items-center gap-2 rounded-md border px-3">
              <Checkbox
                :checked="promotionForm.stackable"
                @update:checked="promotionForm.stackable = Boolean($event)"
              />
              <span class="text-sm">允许与其他促销叠加</span>
            </div>
          </div>
          <div class="space-y-2 md:col-span-2">
            <Label>规则摘要</Label>
            <Textarea v-model="promotionForm.ruleSummary" rows="3" />
          </div>
          <div class="space-y-2">
            <Label>适用门店</Label>
            <Input v-model="promotionForm.applicableStore" placeholder="ALL" />
          </div>
          <div class="space-y-2">
            <Label>客户类型</Label>
            <Input v-model="promotionForm.customerType" placeholder="ALL" />
          </div>
          <div class="space-y-2">
            <Label>适用渠道</Label>
            <Input v-model="promotionForm.applicableChannel" placeholder="ALL" />
          </div>
          <div class="space-y-2">
            <Label>库存范围</Label>
            <Input v-model="promotionForm.inventoryScope" placeholder="ALL" />
          </div>
          <div class="space-y-2">
            <Label>预算上限</Label>
            <Input v-model.number="promotionForm.budgetLimit" type="number" min="0" step="1" />
          </div>
          <div class="space-y-2">
            <Label>使用名额</Label>
            <Input v-model.number="promotionForm.usageLimit" type="number" min="0" step="1" />
          </div>
          <div class="space-y-2">
            <Label>优先级</Label>
            <Input v-model.number="promotionForm.priority" type="number" min="0" step="1" />
          </div>
          <div class="space-y-2">
            <Label>开始时间</Label>
            <Input v-model="promotionForm.startTime" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>结束时间</Label>
            <Input v-model="promotionForm.endTime" type="datetime-local" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="dialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="reasonDialogVisible">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>{{ reasonDialogTitle }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-2">
          <Label>原因</Label>
          <Textarea v-model="statusReason" rows="4" />
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="statusSubmitting" @click="reasonDialogVisible = false">
            取消
          </Button>
          <Button :disabled="statusSubmitting" @click="submitReasonAction">
            {{ statusSubmitting ? '处理中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { computed, onMounted, ref } from 'vue'

import {
  activatePromotion,
  createPromotion,
  deletePromotion,
  endPromotion,
  fetchPromotionDetail,
  fetchProductPage,
  getPromotionList,
  pausePromotion,
  publishPromotion,
  updatePromotion,
  voidPromotion,
} from '@/modules/product/api/product-api'
import {
  PRODUCT_PROMOTION_STATUS,
  PRODUCT_PROMOTION_STATUS_LABEL,
  PRODUCT_PROMOTION_TYPE,
  PRODUCT_PROMOTION_TYPE_LABEL,
  toCreatePromotionRequest,
  toUpdatePromotionRequest,
  type ProductPromotion,
  type ProductPromotionStatus,
  type ProductPromotionType,
  type PromotionFormValues,
} from '@/modules/product/model/product.types'
import { fromLocalDateTimeInput, toLocalDateTimeInput } from '@/shared/datetime/local-date'
import { formatCurrency } from '@/shared/utils/display-format'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { useClientSort } from '@/shared/utils/table-sort'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
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
  NumberField,
  NumberFieldContent,
  NumberFieldDecrement,
  NumberFieldIncrement,
  NumberFieldInput,
} from '@/components/ui/number-field'
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
import { Ban, Pause, Pencil, Play, Plus, Send, StopCircle, Trash2 } from '@lucide/vue'

defineOptions({ name: 'ProductPromotionView' })

type ReasonAction = 'pause' | 'end' | 'void'
type InstantAction = 'publish' | 'activate'

const promotionList = ref<ProductPromotion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const {
  sortedRows: displayPromotionList,
} = useClientSort<ProductPromotion>(promotionList, {
  code: 'code',
  name: 'name',
  type: (row) => formatPromotionType(row.type),
  startTime: 'startTime',
  endTime: 'endTime',
  status: (row) => formatPromotionStatus(row.status),
})

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitting = ref(false)
const productOptions = ref<{ value: string; label: string }[]>([])
const editingPromotionId = ref<string | number | null>(null)

const reasonDialogVisible = ref(false)
const statusSubmitting = ref(false)
const pendingReasonAction = ref<ReasonAction | null>(null)
const pendingPromotion = ref<ProductPromotion | null>(null)
const statusReason = ref('')

const promotionTypeOptions = Object.entries(PRODUCT_PROMOTION_TYPE_LABEL).map(([value, label]) => ({
  value: value as ProductPromotionType,
  label,
}))

const promotionForm = ref<PromotionFormValues>(emptyPromotionForm())

const reasonDialogTitle = computed(() => {
  const map: Record<ReasonAction, string> = {
    pause: '暂停促销',
    end: '结束促销',
    void: '作废促销',
  }
  return pendingReasonAction.value ? map[pendingReasonAction.value] : '状态操作'
})

function emptyPromotionForm(): PromotionFormValues {
  return {
    productId: '',
    code: '',
    name: '',
    type: PRODUCT_PROMOTION_TYPE.AMOUNT,
    discount: 0,
    ruleSummary: '',
    applicableStore: 'ALL',
    customerType: 'ALL',
    applicableChannel: 'ALL',
    inventoryScope: 'ALL',
    stackable: false,
    priority: 0,
    budgetLimit: null,
    usageLimit: null,
    startTime: '',
    endTime: '',
  }
}

function getPromotionTypeTone(type: ProductPromotionType): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (type === PRODUCT_PROMOTION_TYPE.PERCENTAGE) return 'success'
  if (type === PRODUCT_PROMOTION_TYPE.GIFT || type === PRODUCT_PROMOTION_TYPE.MAINTENANCE) return 'purple'
  return 'info'
}

function getPromotionStatusTone(status: ProductPromotionStatus): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  const map: Record<ProductPromotionStatus, 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple'> = {
    DRAFT: 'muted',
    PENDING_EFFECTIVE: 'purple',
    ACTIVE: 'success',
    PAUSED: 'warning',
    ENDED: 'muted',
    VOIDED: 'danger',
    EXHAUSTED: 'info',
  }
  return map[status] ?? 'muted'
}

function formatPromotionDiscount(row: ProductPromotion): string {
  if (row.type === PRODUCT_PROMOTION_TYPE.PERCENTAGE) {
    return `${Number(row.discount ?? 0).toFixed(2)}`
  }
  if (row.type === PRODUCT_PROMOTION_TYPE.GIFT || row.type === PRODUCT_PROMOTION_TYPE.MAINTENANCE) {
    return `成本 ${formatCurrency(row.discount, { fractionDigits: 0 })}`
  }
  return formatCurrency(row.discount, { fractionDigits: 0 })
}

function formatPromotionType(type: ProductPromotionType): string {
  return PRODUCT_PROMOTION_TYPE_LABEL[type] ?? type ?? '--'
}

function formatPromotionStatus(status: ProductPromotionStatus): string {
  return PRODUCT_PROMOTION_STATUS_LABEL[status] ?? status ?? '--'
}

function formatBudget(row: ProductPromotion): string {
  if (row.budgetLimit === null || row.budgetLimit === undefined) return '预算不限'
  return `${formatCurrency(row.usedBudget ?? 0, { fractionDigits: 0 })}/${formatCurrency(row.budgetLimit, { fractionDigits: 0 })}`
}

function formatUsage(row: ProductPromotion): string {
  if (row.usageLimit === null || row.usageLimit === undefined) return '名额不限'
  return `${row.usedCount ?? 0}/${row.usageLimit} 次`
}

function formatDateTime(dateTimeStr?: string): string {
  if (!dateTimeStr) return ''
  return dateTimeStr.replace('T', ' ').split('.')[0] ?? ''
}

function canEdit(row: ProductPromotion): boolean {
  return [
    PRODUCT_PROMOTION_STATUS.DRAFT,
    PRODUCT_PROMOTION_STATUS.PENDING_EFFECTIVE,
    PRODUCT_PROMOTION_STATUS.PAUSED,
  ].includes(row.status)
}

function canPublish(row: ProductPromotion): boolean {
  return row.status === PRODUCT_PROMOTION_STATUS.DRAFT
}

function canActivate(row: ProductPromotion): boolean {
  return row.status === PRODUCT_PROMOTION_STATUS.PENDING_EFFECTIVE || row.status === PRODUCT_PROMOTION_STATUS.PAUSED
}

function canPause(row: ProductPromotion): boolean {
  return row.status === PRODUCT_PROMOTION_STATUS.ACTIVE || row.status === PRODUCT_PROMOTION_STATUS.PENDING_EFFECTIVE
}

function canEnd(row: ProductPromotion): boolean {
  return [
    PRODUCT_PROMOTION_STATUS.DRAFT,
    PRODUCT_PROMOTION_STATUS.PENDING_EFFECTIVE,
    PRODUCT_PROMOTION_STATUS.ACTIVE,
    PRODUCT_PROMOTION_STATUS.PAUSED,
    PRODUCT_PROMOTION_STATUS.EXHAUSTED,
  ].includes(row.status)
}

function canVoid(row: ProductPromotion): boolean {
  return canEnd(row)
}

async function loadProductOptions(): Promise<void> {
  try {
    const res = await fetchProductPage({ page: 1, size: 100 })
    productOptions.value = res.list
      .filter((p) => p.id !== null && p.id !== undefined)
      .map((p) => ({
        value: String(p.id),
        label: `${p.sku ?? ''} ${p.name ?? ''}`.trim(),
      }))
  } catch {
    messageTip('加载商品选项失败', 'error')
  }
}

async function loadPromotions(): Promise<void> {
  try {
    loading.value = true
    const res = await getPromotionList({ page: currentPage.value, size: pageSize.value })
    promotionList.value = res.list
    total.value = res.total
  } catch {
    messageTip('加载促销列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleAdd(): void {
  dialogType.value = 'add'
  editingPromotionId.value = null
  promotionForm.value = emptyPromotionForm()
  dialogVisible.value = true
}

async function handleEdit(row: ProductPromotion): Promise<void> {
  dialogType.value = 'edit'
  try {
    const detail = await fetchPromotionDetail(row.id)
    editingPromotionId.value = detail.id ?? row.id
    promotionForm.value = {
      productId: String(detail.productId ?? ''),
      code: detail.code ?? '',
      name: detail.name ?? '',
      type: detail.type ?? PRODUCT_PROMOTION_TYPE.AMOUNT,
      discount: Number(detail.discount ?? 0),
      ruleSummary: detail.ruleSummary ?? '',
      applicableStore: detail.applicableStore ?? 'ALL',
      customerType: detail.customerType ?? 'ALL',
      applicableChannel: detail.applicableChannel ?? 'ALL',
      inventoryScope: detail.inventoryScope ?? 'ALL',
      stackable: Boolean(detail.stackable),
      priority: Number(detail.priority ?? 0),
      budgetLimit: detail.budgetLimit === null || detail.budgetLimit === undefined ? null : Number(detail.budgetLimit),
      usageLimit: detail.usageLimit === null || detail.usageLimit === undefined ? null : Number(detail.usageLimit),
      startTime: toLocalDateTimeInput(detail.startTime),
      endTime: toLocalDateTimeInput(detail.endTime),
    }
    dialogVisible.value = true
  } catch {
    messageTip('加载促销详情失败', 'error')
  }
}

async function handleDelete(row: ProductPromotion): Promise<void> {
  try {
    await messageConfirm('确认删除该促销草稿？')
  } catch {
    return
  }
  try {
    await deletePromotion(row.id)
    messageTip('删除成功', 'success')
    await loadPromotions()
  } catch {
    messageTip('删除失败', 'error')
  }
}

function validateForm(): string | null {
  if (!promotionForm.value.productId) return '请选择商品'
  if (!promotionForm.value.code.trim()) return '请输入促销编码'
  if (!promotionForm.value.name.trim()) return '请输入促销名称'
  if (!promotionForm.value.ruleSummary.trim()) return '请输入规则摘要'
  const start = fromLocalDateTimeInput(promotionForm.value.startTime)
  const end = fromLocalDateTimeInput(promotionForm.value.endTime)
  if (!start || !end) return '请选择有效期'
  if (start >= end) return '结束时间必须晚于开始时间'
  if (promotionForm.value.type === PRODUCT_PROMOTION_TYPE.PERCENTAGE) {
    if (promotionForm.value.discount <= 0 || promotionForm.value.discount >= 1) {
      return '百分比折扣必须大于0且小于1'
    }
  } else if (
    promotionForm.value.type !== PRODUCT_PROMOTION_TYPE.GIFT &&
    promotionForm.value.type !== PRODUCT_PROMOTION_TYPE.MAINTENANCE &&
    promotionForm.value.discount <= 0
  ) {
    return '金额类促销优惠必须大于0'
  }
  if (promotionForm.value.budgetLimit !== null && promotionForm.value.budgetLimit <= 0) {
    return '预算上限必须大于0'
  }
  if (promotionForm.value.usageLimit !== null && promotionForm.value.usageLimit <= 0) {
    return '使用名额必须大于0'
  }
  return null
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) return
  const error = validateForm()
  if (error) {
    messageTip(error, 'warning')
    return
  }
  submitting.value = true
  try {
    const start = fromLocalDateTimeInput(promotionForm.value.startTime)
    const end = fromLocalDateTimeInput(promotionForm.value.endTime)
    if (!start || !end) {
      messageTip('时间格式有误', 'error')
      return
    }
    const valuesWithTime: PromotionFormValues = {
      ...promotionForm.value,
      startTime: start,
      endTime: end,
    }
    if (dialogType.value === 'add') {
      await createPromotion(toCreatePromotionRequest(valuesWithTime))
      messageTip('新增成功', 'success')
    } else {
      if (editingPromotionId.value === null) {
        messageTip('无法确定编辑目标', 'error')
        return
      }
      await updatePromotion(editingPromotionId.value, toUpdatePromotionRequest(valuesWithTime))
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadPromotions()
  } catch {
    messageTip('操作失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function handleStatusAction(row: ProductPromotion, action: InstantAction): Promise<void> {
  try {
    if (action === 'publish') {
      await publishPromotion(row.id)
      messageTip('发布成功', 'success')
    } else {
      await activatePromotion(row.id)
      messageTip('生效成功', 'success')
    }
    await loadPromotions()
  } catch {
    messageTip('状态操作失败', 'error')
  }
}

function openReasonDialog(row: ProductPromotion, action: ReasonAction): void {
  pendingPromotion.value = row
  pendingReasonAction.value = action
  statusReason.value = ''
  reasonDialogVisible.value = true
}

async function submitReasonAction(): Promise<void> {
  if (!pendingPromotion.value || !pendingReasonAction.value) return
  if (!statusReason.value.trim()) {
    messageTip('请输入原因', 'warning')
    return
  }
  statusSubmitting.value = true
  try {
    if (pendingReasonAction.value === 'pause') {
      await pausePromotion(pendingPromotion.value.id, statusReason.value.trim())
      messageTip('暂停成功', 'success')
    } else if (pendingReasonAction.value === 'end') {
      await endPromotion(pendingPromotion.value.id, statusReason.value.trim())
      messageTip('结束成功', 'success')
    } else {
      await voidPromotion(pendingPromotion.value.id, statusReason.value.trim())
      messageTip('作废成功', 'success')
    }
    reasonDialogVisible.value = false
    await loadPromotions()
  } catch {
    messageTip('状态操作失败', 'error')
  } finally {
    statusSubmitting.value = false
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadPromotions()
}

onMounted(() => {
  void loadProductOptions()
  void loadPromotions()
})
</script>
