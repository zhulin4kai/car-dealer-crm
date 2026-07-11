<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">报价订单</h2>
            <span
              class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]"
            >
              {{ total }} 单
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
            管理客户报价、版本快照和报价状态。
          </p>
        </div>
        <Button v-has-permission="PERMISSIONS.quote.create" class="gap-2" @click="openCreate">
          <Plus class="h-4 w-4" />
          新增报价
        </Button>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">报价单号</Label>
            <Input v-model="filterForm.quoteNo" class="w-[220px]" placeholder="请输入报价单号" />
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
        <Table v-else class="min-w-[1020px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[180px]">报价单号</TableHead>
              <TableHead class="w-[110px]">客户ID</TableHead>
              <TableHead class="w-[150px]">状态</TableHead>
              <TableHead class="w-[130px]">当前版本</TableHead>
              <TableHead class="w-[180px]">创建时间</TableHead>
              <TableHead>备注</TableHead>
              <TableHead class="w-[190px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="quoteList.length === 0">
              <TableCell colspan="7" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无报价数据
              </TableCell>
            </TableRow>
            <TableRow v-for="quote in quoteList" :key="quote.id">
              <TableCell class="font-mono text-xs">{{ quote.quoteNo }}</TableCell>
              <TableCell>{{ quote.customerId }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatQuoteStatus(quote.status)"
                  :tone="getQuoteStatusTone(quote.status)"
                />
              </TableCell>
              <TableCell>{{ quote.currentVersionId ? `#${quote.currentVersionId}` : '--' }}</TableCell>
              <TableCell>{{ formatDateTime(quote.createTime) || '--' }}</TableCell>
              <TableCell class="max-w-[260px] truncate">{{ quote.remark || '--' }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton
                    v-has-permission="PERMISSIONS.quote.view"
                    label="详情"
                    @click="openDetail(quote)"
                  >
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.ai.assistantUse"
                    label="询问 AI"
                    @click="openAiAssistant(quote.id)"
                  >
                    <Sparkles class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.quote.edit"
                    label="新版本"
                    @click="openVersion(quote)"
                  >
                    <FilePlus2 class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.quote.edit"
                    label="状态"
                    @click="openStatus(quote)"
                  >
                    <GitBranch class="h-4 w-4" />
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

    <Dialog v-model:open="formDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>{{ formMode === 'create' ? '新增报价' : '生成报价版本' }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleSubmitForm">
          <div v-if="formMode === 'create'" class="space-y-2">
            <Label>客户ID</Label>
            <Input v-model="quoteForm.customerId" placeholder="客户ID" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>产品ID</Label>
              <Input v-model="quoteForm.productId" placeholder="产品ID" />
            </div>
            <div class="space-y-2">
              <Label>数量</Label>
              <Input v-model="quoteForm.quantity" placeholder="数量" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>有效期</Label>
            <Input v-model="quoteForm.validUntil" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="quoteForm.remark" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="formDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleSubmitForm">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[860px]">
        <DialogHeader>
          <DialogTitle>报价详情</DialogTitle>
        </DialogHeader>
        <div v-if="quoteDetail" class="max-h-[64vh] space-y-5 overflow-y-auto pr-1">
          <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">报价单号</span>
            <span class="font-mono">{{ quoteDetail.quote.quoteNo }}</span>
            <span class="text-[var(--crm-text-tertiary)]">状态</span>
            <span>{{ formatQuoteStatus(quoteDetail.quote.status) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">当前版本</span>
            <span>{{ quoteDetail.currentVersion?.versionNo ?? '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">报价总额</span>
            <span>{{ formatMoney(quoteDetail.currentVersion?.totalAmount) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">有效期</span>
            <span>{{ formatDateTime(quoteDetail.currentVersion?.validUntil) || '--' }}</span>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>商品</TableHead>
                <TableHead class="w-[120px]">SKU</TableHead>
                <TableHead class="w-[100px]">数量</TableHead>
                <TableHead class="w-[130px]">单价</TableHead>
                <TableHead class="w-[130px]">金额</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="item in quoteDetail.items" :key="item.id">
                <TableCell>{{ item.productName || '--' }}</TableCell>
                <TableCell class="font-mono text-xs">{{ item.productSku || '--' }}</TableCell>
                <TableCell>{{ item.quantity }}</TableCell>
                <TableCell>{{ formatMoney(item.unitPrice) }}</TableCell>
                <TableCell>{{ formatMoney(item.lineAmount) }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="statusDialogVisible">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>报价状态</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="handleStatusSubmit">
          <div class="space-y-2">
            <Label>目标状态</Label>
            <Select v-model="statusForm.targetStatus">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="选择目标状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in quoteStatusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>原因</Label>
            <Textarea v-model="statusForm.reason" :rows="3" />
          </div>
          <div v-if="requiresConfirmationEvidence" class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>确认人</Label>
              <Input v-model="statusForm.confirmedByName" placeholder="客户或代理人姓名" />
            </div>
            <div class="space-y-2">
              <Label>确认时间</Label>
              <Input v-model="statusForm.confirmedAt" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label>确认方式</Label>
              <Select v-model="statusForm.confirmationMethod">
                <SelectTrigger class="w-full">
                  <SelectValue placeholder="选择确认方式" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem
                    v-for="option in confirmationMethodOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div v-if="statusForm.confirmationMethod === 'PROXY'" class="space-y-2">
              <Label>代确认原因</Label>
              <Input v-model="statusForm.proxyConfirmReason" placeholder="代确认原因" />
            </div>
            <div class="col-span-2 space-y-2">
              <Label>确认凭证</Label>
              <Textarea v-model="statusForm.confirmationEvidence" :rows="3" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="statusSubmitting" @click="statusDialogVisible = false">
            取消
          </Button>
          <Button :disabled="statusSubmitting" @click="handleStatusSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Eye, FilePlus2, GitBranch, Plus, RotateCcw, Search, Sparkles } from '@lucide/vue'
import { PERMISSIONS } from '@/shared/constants/permissions'
import {
  createQuote,
  createQuoteVersion,
  fetchQuoteDetail,
  fetchQuotePage,
  updateQuoteStatus,
} from '@/modules/quote/api/quote-api'
import {
  formatQuoteStatus,
  getQuoteStatusTone,
  type CreateQuoteRequest,
  type CreateQuoteVersionRequest,
  type Quote,
  type QuoteDetail,
  type QuoteQuery,
  type QuoteStatus,
  type UpdateQuoteStatusRequest,
} from '@/modules/quote/model/quote.types'
import { messageTip } from '@/shared/utils/feedback'
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

function openAiAssistant(id: string | number): void {
  aiAssistantStore.openPanel({ objectType: 'QUOTE', objectId: String(id) })
}

const ALL_STATUS = '__ALL_QUOTE_STATUS__'

type FormMode = 'create' | 'version'

interface QuoteFormState {
  customerId: string
  productId: string
  quantity: string
  validUntil: string
  remark: string
}

const quoteStatusOptions: Array<{ value: QuoteStatus; label: string }> = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING_SUBMIT', label: '待提交' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'PENDING_CUSTOMER_CONFIRMATION', label: '待客户确认' },
  { value: 'ACCEPTED', label: '已接受' },
  { value: 'REFUSED', label: '已拒绝' },
  { value: 'EXPIRED', label: '已过期' },
  { value: 'VOIDED', label: '已作废' },
  { value: 'CONVERTED_TO_ORDER', label: '已转订单' },
]

const statusFilterOptions: Array<{ value: QuoteStatus | typeof ALL_STATUS; label: string }> = [
  { value: ALL_STATUS, label: '全部状态' },
  ...quoteStatusOptions,
]
const customerDecisionStatuses = new Set<QuoteStatus>(['ACCEPTED', 'REFUSED', 'EXPIRED'])
const confirmationMethodOptions: Array<{
  value: NonNullable<UpdateQuoteStatusRequest['confirmationMethod']>
  label: string
}> = [
  { value: 'CUSTOMER_SIGNATURE', label: '客户签字' },
  { value: 'CALL_RECORD', label: '电话录音' },
  { value: 'WECHAT', label: '微信确认' },
  { value: 'EMAIL', label: '邮件确认' },
  { value: 'SYSTEM_EXPIRE', label: '系统过期' },
  { value: 'PROXY', label: '代确认' },
]

const quoteList = ref<Quote[]>([])
const quoteDetail = ref<QuoteDetail | null>(null)
const currentQuote = ref<Quote | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const statusSubmitting = ref(false)
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const statusDialogVisible = ref(false)
const formMode = ref<FormMode>('create')
const filterForm = ref({
  quoteNo: '',
  customerId: '',
  status: ALL_STATUS as QuoteStatus | typeof ALL_STATUS,
})
const quoteForm = ref<QuoteFormState>(emptyQuoteForm())
const statusForm = ref<UpdateQuoteStatusRequest>({
  expectedStatus: 'DRAFT',
  targetStatus: 'PENDING_CUSTOMER_CONFIRMATION',
  reason: '',
})
const requiresConfirmationEvidence = computed(() =>
  customerDecisionStatuses.has(statusForm.value.targetStatus),
)

function emptyQuoteForm(): QuoteFormState {
  return {
    customerId: '',
    productId: '',
    quantity: '1',
    validUntil: '',
    remark: '',
  }
}

function buildQuery(): QuoteQuery {
  const query: QuoteQuery = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (filterForm.value.quoteNo.trim()) {
    query.quoteNo = filterForm.value.quoteNo.trim()
  }
  if (filterForm.value.customerId.trim()) {
    query.customerId = Number(filterForm.value.customerId.trim())
  }
  if (filterForm.value.status !== ALL_STATUS) {
    query.status = filterForm.value.status
  }
  return query
}

async function loadQuotes() {
  loading.value = true
  try {
    const res = await fetchQuotePage(buildQuery())
    quoteList.value = res.list ?? []
    total.value = res.total ?? 0
    pageSize.value = res.pageSize ?? pageSize.value
  } catch {
    messageTip('加载报价列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function parseRequiredNumber(value: string, label: string): number | null {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    messageTip(`${label}必须为正整数`, 'warning')
    return null
  }
  return parsed
}

function normalizeDateTime(value: string): string | null {
  if (!value) {
    messageTip('请选择有效期', 'warning')
    return null
  }
  return value.replace(' ', 'T')
}

function currentLocalDateTimeInputValue() {
  const now = new Date()
  const offsetMs = now.getTimezoneOffset() * 60 * 1000
  return new Date(now.getTime() - offsetMs).toISOString().slice(0, 16)
}

function buildCreateRequest(): CreateQuoteRequest | null {
  const customerId = parseRequiredNumber(quoteForm.value.customerId, '客户ID')
  const productId = parseRequiredNumber(quoteForm.value.productId, '产品ID')
  const quantity = parseRequiredNumber(quoteForm.value.quantity, '数量')
  const validUntil = normalizeDateTime(quoteForm.value.validUntil)
  if (!customerId || !productId || !quantity || !validUntil) return null
  return {
    customerId,
    validUntil,
    remark: quoteForm.value.remark.trim(),
    items: [{ productId, quantity }],
  }
}

function buildVersionRequest(): CreateQuoteVersionRequest | null {
  const productId = parseRequiredNumber(quoteForm.value.productId, '产品ID')
  const quantity = parseRequiredNumber(quoteForm.value.quantity, '数量')
  const validUntil = normalizeDateTime(quoteForm.value.validUntil)
  if (!productId || !quantity || !validUntil) return null
  return {
    validUntil,
    remark: quoteForm.value.remark.trim(),
    items: [{ productId, quantity }],
  }
}

function formatDateTime(dateTime?: string) {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').split('.')[0] ?? ''
}

function formatMoney(value?: number | string) {
  if (value === undefined || value === null || value === '') return '--'
  const parsed = Number(value)
  if (Number.isNaN(parsed)) return String(value)
  return parsed.toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
  })
}

function handleSearch() {
  currentPage.value = 1
  void loadQuotes()
}

function handleReset() {
  filterForm.value = {
    quoteNo: '',
    customerId: '',
    status: ALL_STATUS,
  }
  currentPage.value = 1
  void loadQuotes()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadQuotes()
}

function openCreate() {
  formMode.value = 'create'
  currentQuote.value = null
  quoteForm.value = emptyQuoteForm()
  formDialogVisible.value = true
}

function openVersion(quote: Quote) {
  formMode.value = 'version'
  currentQuote.value = quote
  quoteForm.value = {
    ...emptyQuoteForm(),
    customerId: String(quote.customerId),
  }
  formDialogVisible.value = true
}

async function openDetail(quote: Quote) {
  try {
    quoteDetail.value = await fetchQuoteDetail(quote.id)
    detailDialogVisible.value = true
  } catch {
    messageTip('加载报价详情失败', 'error')
  }
}

function openStatus(quote: Quote) {
  currentQuote.value = quote
  statusForm.value = {
    expectedStatus: quote.status,
    targetStatus: 'PENDING_CUSTOMER_CONFIRMATION',
    reason: '',
    confirmedAt: currentLocalDateTimeInputValue(),
    confirmationMethod: 'CUSTOMER_SIGNATURE',
  }
  statusDialogVisible.value = true
}

async function handleSubmitForm() {
  if (submitting.value) return
  const request = formMode.value === 'create' ? buildCreateRequest() : buildVersionRequest()
  if (!request) return

  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await createQuote(request as CreateQuoteRequest)
      messageTip('报价创建成功', 'success')
    } else if (currentQuote.value) {
      await createQuoteVersion(currentQuote.value.id, request as CreateQuoteVersionRequest)
      messageTip('报价版本已生成', 'success')
    }
    formDialogVisible.value = false
    await loadQuotes()
  } catch {
    messageTip(formMode.value === 'create' ? '报价创建失败' : '报价版本生成失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function handleStatusSubmit() {
  if (!currentQuote.value || statusSubmitting.value) return
  if (!statusForm.value.reason.trim()) {
    messageTip('请输入状态变更原因', 'warning')
    return
  }
  if (requiresConfirmationEvidence.value) {
    if (!statusForm.value.confirmedByName?.trim()) {
      messageTip('请输入确认人', 'warning')
      return
    }
    if (!statusForm.value.confirmedAt) {
      messageTip('请选择确认时间', 'warning')
      return
    }
    if (!statusForm.value.confirmationMethod) {
      messageTip('请选择确认方式', 'warning')
      return
    }
    if (!statusForm.value.confirmationEvidence?.trim()) {
      messageTip('请输入确认凭证', 'warning')
      return
    }
    if (statusForm.value.confirmationMethod === 'PROXY' && !statusForm.value.proxyConfirmReason?.trim()) {
      messageTip('请输入代确认原因', 'warning')
      return
    }
  }
  statusSubmitting.value = true
  try {
    const request: UpdateQuoteStatusRequest = {
      expectedStatus: statusForm.value.expectedStatus,
      targetStatus: statusForm.value.targetStatus,
      reason: statusForm.value.reason.trim(),
    }
    if (requiresConfirmationEvidence.value) {
      request.confirmedByName = statusForm.value.confirmedByName?.trim()
      request.confirmedAt = statusForm.value.confirmedAt?.replace(' ', 'T')
      request.confirmationMethod = statusForm.value.confirmationMethod
      request.confirmationEvidence = statusForm.value.confirmationEvidence?.trim()
      request.proxyConfirmReason = statusForm.value.proxyConfirmReason?.trim()
    }
    await updateQuoteStatus(currentQuote.value.id, request)
    messageTip('报价状态已更新', 'success')
    statusDialogVisible.value = false
    await loadQuotes()
  } catch {
    messageTip('报价状态更新失败', 'error')
  } finally {
    statusSubmitting.value = false
  }
}

onMounted(() => {
  void loadQuotes()
})
</script>
