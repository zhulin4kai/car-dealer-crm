<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">商机管理</h2>
            <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
              {{ total }} 个
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">跟踪购车需求、销售阶段、预计金额和输赢单结果。</p>
        </div>
        <Button v-has-permission="PERMISSIONS.opportunity.create" class="gap-2" @click="openCreate">
          <Plus class="h-4 w-4" />
          新增商机
        </Button>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">关键词</Label>
            <Input v-model="filterForm.keyword" class="w-[220px]" placeholder="商机编号/客户/需求" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">客户ID</Label>
            <Input v-model="filterForm.customerId" class="w-[120px]" placeholder="客户ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">负责人ID</Label>
            <Input v-model="filterForm.ownerId" class="w-[120px]" placeholder="负责人ID" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">阶段</Label>
            <Select v-model="filterForm.stage">
              <SelectTrigger class="w-[170px]">
                <SelectValue placeholder="全部阶段" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in stageFilterOptions" :key="option.value" :value="option.value">
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
        <Table v-else class="min-w-[1180px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[170px]">商机编号</TableHead>
              <TableHead class="w-[150px]">客户</TableHead>
              <TableHead class="w-[130px]">负责人</TableHead>
              <TableHead class="w-[160px]">意向车型</TableHead>
              <TableHead class="w-[130px]">阶段</TableHead>
              <TableHead class="w-[130px]">预计金额</TableHead>
              <TableHead class="w-[130px]">预计成交</TableHead>
              <TableHead class="w-[130px]">下次动作</TableHead>
              <TableHead class="w-[250px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="opportunities.length === 0">
              <TableCell colspan="9" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无商机数据
              </TableCell>
            </TableRow>
            <TableRow v-for="opportunity in opportunities" :key="opportunity.id">
              <TableCell class="font-mono text-xs">{{ opportunity.opportunityNo }}</TableCell>
              <TableCell>{{ opportunity.customerName || `#${opportunity.customerId}` }}</TableCell>
              <TableCell>{{ opportunity.ownerName || `#${opportunity.ownerId}` }}</TableCell>
              <TableCell class="max-w-[160px] truncate">{{ opportunity.productName || productIdLabel(opportunity.productId) }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatOpportunityStage(opportunity.stage)"
                  :tone="getOpportunityStageTone(opportunity.stage)"
                />
              </TableCell>
              <TableCell>{{ formatMoney(opportunity.expectedAmount) }}</TableCell>
              <TableCell>{{ formatDate(opportunity.expectedCloseDate) }}</TableCell>
              <TableCell>{{ formatDate(opportunity.nextActionTime) }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton v-has-permission="PERMISSIONS.opportunity.view" label="详情" @click="openDetail(opportunity)">
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.ai.assistantUse"
                    label="询问 AI"
                    @click="openAiAssistant(opportunity.id)"
                  >
                    <Sparkles class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isOpportunityTerminal(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.edit"
                    label="编辑"
                    @click="openEdit(opportunity)"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isOpportunityTerminal(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.advance"
                    label="推进"
                    @click="openAdvance(opportunity)"
                  >
                    <ArrowRight class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isOpportunityTerminal(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.win"
                    label="赢单"
                    @click="openResult(opportunity, 'won')"
                  >
                    <Trophy class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isOpportunityTerminal(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.lose"
                    label="输单"
                    @click="openResult(opportunity, 'lost')"
                  >
                    <CircleX class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="!isOpportunityTerminal(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.shelve"
                    label="搁置"
                    @click="openResult(opportunity, 'shelve')"
                  >
                    <Archive class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-if="isOpportunityRestorable(opportunity.stage)"
                    v-has-permission="PERMISSIONS.opportunity.restore"
                    label="恢复"
                    @click="openResult(opportunity, 'restore')"
                  >
                    <RefreshCw class="h-4 w-4" />
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
      <DialogContent class="sm:max-w-[640px]">
        <DialogHeader>
          <DialogTitle>{{ editingOpportunity ? '编辑商机' : '新增商机' }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleSave">
          <div v-if="!editingOpportunity" class="space-y-2">
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
          <div v-else class="grid grid-cols-2 gap-3 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">客户</span>
            <span>{{ editingOpportunity.customerName || `#${editingOpportunity.customerId}` }}</span>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <Label>意向车型ID</Label>
              <Input v-model="form.productId" placeholder="可为空" />
            </div>
            <div class="space-y-2">
              <Label>来源</Label>
              <Input v-model="form.sourceType" :disabled="Boolean(editingOpportunity)" placeholder="例如 CUSTOMER_DEMAND" />
            </div>
          </div>
          <div class="space-y-2">
            <Label>购车需求</Label>
            <Textarea v-model="form.requirement" :rows="3" placeholder="车型、配置、预算或关键购买条件" />
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="space-y-2">
              <Label>预计金额</Label>
              <Input v-model="form.expectedAmount" inputmode="decimal" placeholder="0.00" />
            </div>
            <div class="space-y-2">
              <Label>预计成交日期</Label>
              <Input v-model="form.expectedCloseDate" type="date" />
            </div>
            <div class="space-y-2">
              <Label>下一步日期</Label>
              <Input v-model="form.nextActionTime" type="date" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="formDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleSave">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[920px]">
        <DialogHeader>
          <DialogTitle>商机详情</DialogTitle>
        </DialogHeader>
        <div v-if="selectedOpportunity" class="max-h-[68vh] space-y-5 overflow-y-auto pr-1">
          <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">商机编号</span>
            <span>{{ selectedOpportunity.opportunityNo }}</span>
            <span class="text-[var(--crm-text-tertiary)]">客户</span>
            <span>{{ selectedOpportunity.customerName || `#${selectedOpportunity.customerId}` }}</span>
            <span class="text-[var(--crm-text-tertiary)]">负责人</span>
            <span>{{ selectedOpportunity.ownerName || `#${selectedOpportunity.ownerId}` }}</span>
            <span class="text-[var(--crm-text-tertiary)]">阶段</span>
            <span>{{ formatOpportunityStage(selectedOpportunity.stage) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">预计金额</span>
            <span>{{ formatMoney(selectedOpportunity.expectedAmount) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">结果原因</span>
            <span>{{ selectedOpportunity.lostReason || selectedOpportunity.resultRemark || '--' }}</span>
            <span class="text-[var(--crm-text-tertiary)]">赢单交易</span>
            <span>{{ selectedOpportunity.orderTranId ? `#${selectedOpportunity.orderTranId}` : '--' }}</span>
          </div>
          <div class="rounded-md border border-[var(--crm-border-light)] p-3 text-sm">
            <div class="mb-2 font-medium">购车需求</div>
            <p class="whitespace-pre-wrap text-[var(--crm-text-secondary)]">{{ selectedOpportunity.requirement }}</p>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>原阶段</TableHead>
                <TableHead>目标阶段</TableHead>
                <TableHead>原因</TableHead>
                <TableHead class="w-[170px]">操作时间</TableHead>
                <TableHead class="w-[90px]">操作人</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="stageHistory.length === 0">
                <TableCell colspan="5" class="h-24 text-center text-[var(--crm-text-tertiary)]">
                  暂无阶段历史
                </TableCell>
              </TableRow>
              <TableRow v-for="item in stageHistory" :key="item.id">
                <TableCell>{{ formatOpportunityStage(item.fromStage) }}</TableCell>
                <TableCell>{{ formatOpportunityStage(item.toStage) }}</TableCell>
                <TableCell class="max-w-[320px] truncate">{{ item.reason }}</TableCell>
                <TableCell>{{ formatDateTime(item.operateTime) }}</TableCell>
                <TableCell>#{{ item.operateBy }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="advanceDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>推进商机阶段</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleAdvance">
          <div class="grid grid-cols-2 gap-3 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">当前阶段</span>
            <span>{{ formatOpportunityStage(selectedOpportunity?.stage) }}</span>
          </div>
          <div class="space-y-2">
            <Label>目标阶段</Label>
            <Select v-model="advanceForm.targetStage">
              <SelectTrigger>
                <SelectValue placeholder="选择目标阶段" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in advanceStageOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>推进原因</Label>
            <Textarea v-model="advanceForm.reason" :rows="3" />
          </div>
          <div class="space-y-2">
            <Label>下一步日期</Label>
            <Input v-model="advanceForm.nextActionTime" type="date" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="advanceDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleAdvance">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="resultDialogVisible">
      <DialogContent class="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle>{{ resultDialogTitle }}</DialogTitle>
        </DialogHeader>
        <form class="grid gap-4" @submit.prevent="handleResultAction">
          <div v-if="resultMode === 'won'" class="space-y-2">
            <Label>关联交易ID</Label>
            <Input v-model="resultForm.orderTranId" placeholder="已成立交易ID" />
          </div>
          <div class="space-y-2">
            <Label>{{ resultMode === 'restore' ? '恢复原因' : '原因' }}</Label>
            <Textarea v-model="resultForm.reason" :rows="3" />
          </div>
          <div v-if="resultMode === 'lost'" class="space-y-2">
            <Label>竞品或客户反馈</Label>
            <Input v-model="resultForm.competitor" placeholder="竞品/反馈摘要" />
          </div>
          <div v-if="resultMode === 'shelve' || resultMode === 'restore'" class="space-y-2">
            <Label>下一步日期</Label>
            <Input v-model="resultForm.nextActionTime" type="date" />
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="resultForm.remark" :rows="2" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="resultDialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleResultAction">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Archive,
  ArrowRight,
  CircleX,
  Eye,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  Trophy,
} from '@lucide/vue'
import {
  advanceOpportunityStage,
  createOpportunity,
  fetchOpportunityDetail,
  fetchOpportunityPage,
  fetchOpportunityStageHistory,
  markOpportunityLost,
  markOpportunityWon,
  restoreOpportunity,
  shelveOpportunity,
  updateOpportunity,
} from '@/modules/opportunity/api/opportunity-api'
import {
  formatOpportunityStage,
  getOpportunityStageTone,
  isOpportunityRestorable,
  isOpportunityTerminal,
  OPPORTUNITY_STAGE_OPTIONS,
  type Opportunity,
  type OpportunityQuery,
  type OpportunityResultRequest,
  type OpportunityStage,
  type OpportunityStageHistory,
} from '@/modules/opportunity/model/opportunity.types'
import { fetchCustomerOptions } from '@/modules/customer/api/customer-api'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import { useAiAssistantStore } from '@/stores/ai-assistant.store'
import type { SelectOption } from '@/shared/types/common'
import type { EntityId } from '@/shared/types/id'
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

const aiAssistantStore = useAiAssistantStore()

function openAiAssistant(id: EntityId): void {
  aiAssistantStore.openPanel({ objectType: 'OPPORTUNITY', objectId: String(id) })
}

type CustomerOption = SelectOption & {
  customerId?: EntityId
  customerName?: string
  clueId?: EntityId
}

type ResultMode = 'won' | 'lost' | 'shelve' | 'restore'

const ALL_STAGE = '__ALL_OPPORTUNITY_STAGE__'
const stageFilterOptions = [{ value: ALL_STAGE, label: '全部阶段' }, ...OPPORTUNITY_STAGE_OPTIONS]
const advanceStageOptions = OPPORTUNITY_STAGE_OPTIONS.filter(
  option => !['WON', 'LOST', 'SHELVED', 'CLOSED'].includes(option.value),
)

const loading = ref(false)
const submitting = ref(false)
const opportunities = ref<Opportunity[]>([])
const stageHistory = ref<OpportunityStageHistory[]>([])
const customerOptions = ref<CustomerOption[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedOpportunity = ref<Opportunity | null>(null)
const editingOpportunity = ref<Opportunity | null>(null)
const resultMode = ref<ResultMode>('lost')

const filterForm = ref({
  keyword: '',
  customerId: '',
  ownerId: '',
  stage: ALL_STAGE,
})
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const advanceDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const form = ref({
  customerId: '',
  productId: '',
  sourceType: '',
  requirement: '',
  expectedAmount: '',
  expectedCloseDate: '',
  nextActionTime: '',
})
const advanceForm = ref({
  targetStage: 'NEEDS_ANALYSIS' as OpportunityStage,
  reason: '',
  nextActionTime: '',
})
const resultForm = ref({
  orderTranId: '',
  reason: '',
  competitor: '',
  remark: '',
  nextActionTime: '',
})

const resultDialogTitle = computed(() => {
  const map: Record<ResultMode, string> = {
    won: '标记赢单',
    lost: '标记输单',
    shelve: '搁置商机',
    restore: '恢复商机',
  }
  return map[resultMode.value]
})

onMounted(() => {
  void Promise.all([loadOpportunities(), loadCustomerOptions()])
})

async function loadOpportunities() {
  loading.value = true
  try {
    const params: OpportunityQuery = {
      page: currentPage.value,
      size: pageSize.value,
    }
    const keyword = filterForm.value.keyword.trim()
    const customerId = parseOptionalId(filterForm.value.customerId)
    const ownerId = parseOptionalId(filterForm.value.ownerId)
    if (keyword) params.keyword = keyword
    if (customerId) params.customerId = customerId
    if (ownerId) params.ownerId = ownerId
    if (filterForm.value.stage !== ALL_STAGE) {
      params.stage = filterForm.value.stage as OpportunityStage
    }
    const result = await fetchOpportunityPage(params)
    opportunities.value = result.list ?? []
    total.value = result.total ?? 0
  } catch {
    messageTip('加载商机列表失败', 'error')
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

function handleSearch() {
  currentPage.value = 1
  void loadOpportunities()
}

function handleReset() {
  filterForm.value = {
    keyword: '',
    customerId: '',
    ownerId: '',
    stage: ALL_STAGE,
  }
  currentPage.value = 1
  void loadOpportunities()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  void loadOpportunities()
}

function openCreate() {
  editingOpportunity.value = null
  form.value = {
    customerId: '',
    productId: '',
    sourceType: 'CUSTOMER_DEMAND',
    requirement: '',
    expectedAmount: '',
    expectedCloseDate: '',
    nextActionTime: '',
  }
  formDialogVisible.value = true
}

function openEdit(opportunity: Opportunity) {
  editingOpportunity.value = opportunity
  form.value = {
    customerId: String(opportunity.customerId),
    productId: optionalString(opportunity.productId),
    sourceType: opportunity.sourceType ?? '',
    requirement: opportunity.requirement ?? '',
    expectedAmount: optionalString(opportunity.expectedAmount),
    expectedCloseDate: opportunity.expectedCloseDate ?? '',
    nextActionTime: opportunity.nextActionTime ?? '',
  }
  formDialogVisible.value = true
}

async function handleSave() {
  const requirement = form.value.requirement.trim()
  if (!requirement) {
    messageTip('请填写购车需求', 'warning')
    return
  }
  submitting.value = true
  try {
    const productId = parseOptionalId(form.value.productId)
    const expectedAmount = parseOptionalAmount(form.value.expectedAmount)
    if (expectedAmount === null) return
    if (editingOpportunity.value) {
      await updateOpportunity(editingOpportunity.value.id, {
        id: editingOpportunity.value.id,
        requirement,
        ...(productId ? { productId } : {}),
        ...(expectedAmount ? { expectedAmount } : {}),
        ...(form.value.expectedCloseDate ? { expectedCloseDate: form.value.expectedCloseDate } : {}),
        ...(form.value.nextActionTime ? { nextActionTime: form.value.nextActionTime } : {}),
      })
      messageTip('商机已更新', 'success')
    } else {
      const customerId = parseRequiredId(form.value.customerId, '客户')
      if (!customerId) return
      await createOpportunity({
        customerId,
        requirement,
        ...(productId ? { productId } : {}),
        ...(form.value.sourceType.trim() ? { sourceType: form.value.sourceType.trim() } : {}),
        ...(expectedAmount ? { expectedAmount } : {}),
        ...(form.value.expectedCloseDate ? { expectedCloseDate: form.value.expectedCloseDate } : {}),
        ...(form.value.nextActionTime ? { nextActionTime: form.value.nextActionTime } : {}),
      })
      messageTip('商机已创建', 'success')
    }
    formDialogVisible.value = false
    void loadOpportunities()
  } catch {
    messageTip('保存商机失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function openDetail(opportunity: Opportunity) {
  selectedOpportunity.value = opportunity
  detailDialogVisible.value = true
  try {
    const [detail, history] = await Promise.all([
      fetchOpportunityDetail(opportunity.id),
      fetchOpportunityStageHistory(opportunity.id),
    ])
    selectedOpportunity.value = detail
    stageHistory.value = history
  } catch {
    messageTip('加载商机详情失败', 'error')
  }
}

function openAdvance(opportunity: Opportunity) {
  selectedOpportunity.value = opportunity
  advanceForm.value = {
    targetStage: nextStage(opportunity.stage),
    reason: '',
    nextActionTime: opportunity.nextActionTime ?? '',
  }
  advanceDialogVisible.value = true
}

async function handleAdvance() {
  if (!selectedOpportunity.value) return
  const reason = advanceForm.value.reason.trim()
  if (!reason) {
    messageTip('请填写推进原因', 'warning')
    return
  }
  submitting.value = true
  try {
    await advanceOpportunityStage(selectedOpportunity.value.id, {
      expectedStage: selectedOpportunity.value.stage,
      targetStage: advanceForm.value.targetStage,
      reason,
      ...(advanceForm.value.nextActionTime ? { nextActionTime: advanceForm.value.nextActionTime } : {}),
    })
    messageTip('商机阶段已推进', 'success')
    advanceDialogVisible.value = false
    void loadOpportunities()
  } catch {
    messageTip('推进商机失败', 'error')
  } finally {
    submitting.value = false
  }
}

function openResult(opportunity: Opportunity, mode: ResultMode) {
  selectedOpportunity.value = opportunity
  resultMode.value = mode
  resultForm.value = {
    orderTranId: '',
    reason: '',
    competitor: '',
    remark: '',
    nextActionTime: opportunity.nextActionTime ?? '',
  }
  resultDialogVisible.value = true
}

async function handleResultAction() {
  if (!selectedOpportunity.value) return
  const reason = resultForm.value.reason.trim()
  if (!reason) {
    messageTip('请填写原因', 'warning')
    return
  }
  const request: OpportunityResultRequest = { reason }
  const orderTranId = parseOptionalId(resultForm.value.orderTranId)
  if (resultMode.value === 'won') {
    if (!orderTranId) {
      messageTip('请填写已成立交易ID', 'warning')
      return
    }
    request.orderTranId = orderTranId
  }
  if (resultForm.value.competitor.trim()) request.competitor = resultForm.value.competitor.trim()
  if (resultForm.value.remark.trim()) request.remark = resultForm.value.remark.trim()
  if (resultForm.value.nextActionTime) request.nextActionTime = resultForm.value.nextActionTime

  submitting.value = true
  try {
    if (resultMode.value === 'won') {
      await markOpportunityWon(selectedOpportunity.value.id, request)
    } else if (resultMode.value === 'lost') {
      await markOpportunityLost(selectedOpportunity.value.id, request)
    } else if (resultMode.value === 'shelve') {
      if (!request.nextActionTime) {
        messageTip('搁置必须填写下一步日期', 'warning')
        return
      }
      await shelveOpportunity(selectedOpportunity.value.id, request)
    } else {
      await restoreOpportunity(selectedOpportunity.value.id, request)
    }
    messageTip('商机状态已更新', 'success')
    resultDialogVisible.value = false
    void loadOpportunities()
  } catch {
    messageTip('处理商机失败', 'error')
  } finally {
    submitting.value = false
  }
}

function nextStage(stage: OpportunityStage): OpportunityStage {
  const map: Partial<Record<OpportunityStage, OpportunityStage>> = {
    INITIAL_CONTACT: 'NEEDS_ANALYSIS',
    NEEDS_ANALYSIS: 'VEHICLE_MATCHING',
    VEHICLE_MATCHING: 'TEST_DRIVE_INVITED',
    TEST_DRIVE_INVITED: 'QUOTING',
    QUOTING: 'NEGOTIATION',
    NEGOTIATION: 'PENDING_APPROVAL',
    SHELVED: 'NEEDS_ANALYSIS',
  }
  return map[stage] ?? 'NEEDS_ANALYSIS'
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
    messageTip(`请填写${label}`, 'warning')
    return null
  }
  return id
}

function parseOptionalAmount(value: string): string | null | undefined {
  const trimmed = value.trim()
  if (!trimmed) return undefined
  const parsed = Number(trimmed)
  if (!Number.isFinite(parsed) || parsed < 0) {
    messageTip('预计金额不能为负数', 'warning')
    return null
  }
  return trimmed
}

function optionalString(value: unknown): string {
  return value === null || value === undefined ? '' : String(value)
}

function productIdLabel(value?: EntityId): string {
  return value ? `#${value}` : '--'
}

function formatMoney(value?: number | string): string {
  if (value === null || value === undefined || value === '') return '--'
  const num = Number(value)
  return Number.isFinite(num) ? `￥${num.toLocaleString('zh-CN')}` : String(value)
}

function formatDate(value?: string): string {
  return value || '--'
}

function formatDateTime(value?: string): string {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 19)
}
</script>
