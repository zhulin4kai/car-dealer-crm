<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h3 class="font-medium">用户变更历史</h3>
        <p class="text-sm text-muted-foreground">
          聚合账号、资料、任职、授权和会话安全的不可变审计投影。
        </p>
      </div>
      <Button
        v-if="queryControlsVisible"
        type="button"
        size="sm"
        variant="outline"
        :disabled="loading"
        @click="load(page)"
        >刷新</Button
      >
    </div>

    <form v-if="queryControlsVisible" class="grid gap-3 md:grid-cols-4" @submit.prevent="search">
      <select v-model="filters.actionCode" aria-label="历史动作" class="history-select">
        <option value="">全部动作</option>
        <option v-for="item in actionOptions" :key="item.code" :value="item.code">
          {{ safeText(item.label) }}（{{ safeText(item.code) }}）
        </option>
      </select>
      <Input v-model="filters.startTime" aria-label="历史开始时间" type="datetime-local" />
      <Input v-model="filters.endTime" aria-label="历史结束时间" type="datetime-local" />
      <div class="flex gap-2">
        <Button type="submit" :disabled="loading">查询</Button
        ><Button type="button" variant="outline" :disabled="loading" @click="reset">重置</Button>
      </div>
    </form>
    <p v-if="filterError" class="text-sm text-destructive">{{ filterError }}</p>

    <div v-if="!enabled" class="rounded-lg border p-4 text-sm text-muted-foreground">
      {{ disabledReason || '当前页面未允许查询用户历史' }}
    </div>
    <div v-else-if="loading" class="py-10 text-center text-muted-foreground">加载历史记录...</div>
    <div v-else-if="errorMessage" class="space-y-3 py-10 text-center">
      <p class="text-destructive">{{ errorMessage }}</p>
      <Button type="button" variant="outline" @click="load(page)">重新加载</Button>
    </div>
    <div
      v-else-if="!responseAllowsView"
      class="rounded-lg border p-4 text-sm text-muted-foreground"
    >
      {{ responseDeniedReason }}
    </div>
    <div
      v-else-if="!items.length"
      class="rounded-lg border py-10 text-center text-muted-foreground"
    >
      暂无符合条件的历史记录
    </div>
    <ol v-else class="space-y-4">
      <li v-for="item in items" :key="item.eventId" class="relative border-l-2 border-muted pl-5">
        <span class="absolute -left-[6px] top-1.5 h-2.5 w-2.5 rounded-full bg-primary" />
        <article class="rounded-lg border p-4">
          <div class="flex flex-wrap items-start justify-between gap-2">
            <div>
              <div class="font-medium">
                {{ safeText(item.actionName) }}
                <span class="font-mono text-xs text-muted-foreground">{{
                  safeText(item.actionCode)
                }}</span>
              </div>
              <div class="text-xs text-muted-foreground">
                {{ safeText(item.categoryName) }} · {{ safeText(item.categoryCode) }} ·
                {{ targetLabel(item) }}
              </div>
            </div>
            <div class="text-right text-xs text-muted-foreground">
              <div>{{ historyDateTime(item.occurredAt) }}</div>
              <div>{{ operatorLabel(item) }}</div>
            </div>
          </div>
          <div
            v-if="visibleBefore(item).length || visibleAfter(item).length"
            class="mt-3 grid gap-3 text-sm md:grid-cols-2"
          >
            <div class="rounded bg-muted/40 p-3">
              <div class="mb-2 text-xs font-medium text-muted-foreground">变更前</div>
              <div v-if="!visibleBefore(item).length">无</div>
              <div v-for="field in visibleBefore(item)" :key="field.code" class="break-words">
                {{ fieldLabel(field) }}
              </div>
            </div>
            <div class="rounded bg-muted/40 p-3">
              <div class="mb-2 text-xs font-medium text-muted-foreground">变更后</div>
              <div v-if="!visibleAfter(item).length">无</div>
              <div v-for="field in visibleAfter(item)" :key="field.code" class="break-words">
                {{ fieldLabel(field) }}
              </div>
            </div>
          </div>
          <div class="mt-3 grid gap-1 text-sm text-muted-foreground sm:grid-cols-2">
            <div>原因：{{ safeText(item.reason, '未填写') }}</div>
            <div>结果：{{ safeText(item.resultName) }}（{{ safeText(item.resultCode) }}）</div>
            <div v-if="item.effectiveFrom">生效：{{ historyDateTime(item.effectiveFrom) }}</div>
            <div v-if="item.effectiveTo">失效：{{ historyDateTime(item.effectiveTo) }}</div>
            <div v-if="item.batchSummary" class="sm:col-span-2">
              批次 {{ safeText(item.batchSummary.batchId) }}：共
              {{ item.batchSummary.totalCount }} 项，成功
              {{ item.batchSummary.successCount }} 项，失败
              {{ item.batchSummary.failureCount }} 项；本目标
              {{ safeText(item.batchSummary.targetResultName) }}（{{
                safeText(item.batchSummary.targetResultCode)
              }}）
            </div>
          </div>
        </article>
      </li>
    </ol>
    <DataTablePagination
      v-if="responseAllowsView"
      :page="page"
      :page-size="pageSize"
      :total="total"
      @change="load"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { fetchUserHistory } from '@/modules/user/api/user-history-api'
import {
  USER_HISTORY_ACTION,
  type UserHistoryActionOption,
  type UserHistoryCollection,
  type UserHistoryItem,
  type UserHistoryQuery,
  type UserHistoryValueField,
} from '@/modules/user/model/user-history.types'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import type { EntityId } from '@/shared/types/id'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import { formatDateTime } from '@/shared/utils/display-format'

const props = defineProps<{ userId: EntityId; enabled: boolean; disabledReason?: string }>()
const collection = ref<UserHistoryCollection | null>(null)
const items = computed(() => collection.value?.list ?? [])
const actionOptions = ref<UserHistoryActionOption[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const filterError = ref('')
const filters = reactive({ actionCode: '', startTime: '', endTime: '' })
let requestId = 0
let controller: AbortController | null = null
const responseAllowsView = computed(
  () => collection.value?.allowedActions.includes(USER_HISTORY_ACTION.VIEW) ?? false,
)
const responseDeniedReason = computed(
  () =>
    collection.value?.unavailableReasons[USER_HISTORY_ACTION.VIEW] ??
    props.disabledReason ??
    '服务端未允许查看该用户历史',
)
const queryControlsVisible = computed(
  () => props.enabled && (collection.value === null || responseAllowsView.value),
)
const FORBIDDEN_FIELD =
  /password|passwd|pwd|hash|digest|token|secret|credential|cookie|session.?id|phone|mobile|email|raw|detail|payload|context|headers?|request|response|body|ip(?:address)?|contact|address|key|signature|salt|nonce/i
const SENSITIVE_MARKER =
  /password|passwd|pwd|hash|digest|token|secret|credential|authorization\s*header|cookie|session\s*(?:id|value)|raw\s*(?:detail|payload)|(?:private|recovery|api)\s*key|signature|salt|nonce/i

function normalizeDateTime(value: string): string | undefined {
  if (!value) return undefined
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}
function query(nextPage: number): UserHistoryQuery | null {
  const startTime = normalizeDateTime(filters.startTime)
  const endTime = normalizeDateTime(filters.endTime)
  if ((filters.startTime && !startTime) || (filters.endTime && !endTime)) {
    filterError.value = '请输入有效的开始和结束时间'
    return null
  }
  if (startTime && endTime && new Date(startTime).getTime() > new Date(endTime).getTime()) {
    filterError.value = '开始时间不能晚于结束时间'
    return null
  }
  return {
    page: nextPage,
    size: pageSize.value,
    ...(filters.actionCode ? { actionCode: filters.actionCode } : {}),
    ...(startTime ? { startTime } : {}),
    ...(endTime ? { endTime } : {}),
  }
}
async function load(nextPage = page.value): Promise<void> {
  if (!props.enabled) return
  const params = query(nextPage)
  if (!params) return
  const current = ++requestId
  controller?.abort()
  const currentController = new AbortController()
  controller = currentController
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchUserHistory(props.userId, params, currentController.signal)
    if (current !== requestId || currentController.signal.aborted) return
    collection.value = result
    const allowed = result.allowedActions.includes(USER_HISTORY_ACTION.VIEW)
    actionOptions.value = allowed ? result.actionOptions : []
    page.value = result.pageNum || nextPage
    pageSize.value = result.pageSize || pageSize.value
    total.value = allowed ? result.total : 0
  } catch (error: unknown) {
    if (current !== requestId || currentController.signal.aborted) return
    collection.value = null
    actionOptions.value = []
    total.value = 0
    errorMessage.value = historyError(error)
  } finally {
    if (current === requestId) {
      loading.value = false
      controller = null
    }
  }
}
function search(): void {
  filterError.value = ''
  void load(1)
}
function reset(): void {
  Object.assign(filters, { actionCode: '', startTime: '', endTime: '' })
  filterError.value = ''
  void load(1)
}
function visible(values: UserHistoryValueField[]): UserHistoryValueField[] {
  return values.filter(
    (field) => !FORBIDDEN_FIELD.test(field.code) && !FORBIDDEN_FIELD.test(field.label),
  )
}
function visibleBefore(item: UserHistoryItem) {
  return visible(Array.isArray(item.beforeValues) ? item.beforeValues : [])
}
function visibleAfter(item: UserHistoryItem) {
  return visible(Array.isArray(item.afterValues) ? item.afterValues : [])
}
function safeText(value: string | null | undefined, fallback = '无'): string {
  if (!value) return fallback
  if (SENSITIVE_MARKER.test(value)) return '[已隐藏敏感内容]'
  return value
    .replace(/\bBearer\s+\S+/gi, '[已隐藏凭证]')
    .replace(/\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}(?:\.[A-Za-z0-9_-]{8,})?\b/g, '[已隐藏令牌]')
    .replace(/\b(?:sk|pk|api|token|secret)[-_][A-Za-z0-9_-]{12,}\b/gi, '[已隐藏凭证]')
    .replace(/\b[a-fA-F0-9]{32,}\b/g, '[已隐藏摘要]')
    .replace(/[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}/g, '[已脱敏邮箱]')
    .replace(/(^|\D)1\d{10}(?=\D|$)/g, '$1[已脱敏手机号]')
    .replace(/(^|\D)0\d{2,3}-?\d{7,8}(?=\D|$)/g, '$1[已脱敏电话]')
    .replace(/\b(?:\d{1,3}\.){3}\d{1,3}\b/g, '[已脱敏网络]')
    .replace(/\b(?:[A-Fa-f0-9]{1,4}:){2,7}[A-Fa-f0-9]{1,4}\b/g, '[已脱敏网络]')
}
function historyDateTime(value: string | null | undefined): string {
  return safeText(formatDateTime(value), '--')
}
function operatorLabel(item: UserHistoryItem): string {
  const employeeNo = item.operator.employeeNo ? ` · ${safeText(item.operator.employeeNo)}` : ''
  return `${safeText(item.operator.name, '系统')}${employeeNo}`
}
function targetLabel(item: UserHistoryItem): string {
  const code = item.target.code ? ` · ${safeText(item.target.code)}` : ''
  return `${safeText(item.target.typeName)}（${safeText(item.target.typeCode)}） · ${safeText(item.target.name, '未命名')}${code}`
}
function fieldLabel(field: UserHistoryValueField): string {
  const name = safeText(field.valueName || field.displayValue)
  const code = field.valueCode ? `（${safeText(field.valueCode)}）` : ''
  return `${safeText(field.label)}：${name}${code}`
}
function historyError(error: unknown): string {
  if (!(error instanceof ApiError)) return '加载用户历史失败'
  if (error.httpStatus === 403 || error.code === 403 || error.code === API_ERROR_CODE.ACCESS_DENIED)
    return '无审计权限或目标用户超出可管理范围'
  if (error.httpStatus === 404 || error.code === 404) return '目标用户或历史记录不存在'
  if (
    error.httpStatus === 409 ||
    error.code === API_ERROR_CODE.CONFLICT ||
    error.code === API_ERROR_CODE.ROLE_VERSION_CONFLICT
  )
    return '历史投影已变化，请刷新后重试'
  return '加载用户历史失败'
}
watch(
  () => [props.userId, props.enabled] as const,
  ([userId, enabled], previous) => {
    if (!previous || previous[0] !== userId)
      Object.assign(filters, { actionCode: '', startTime: '', endTime: '' })
    collection.value = null
    actionOptions.value = []
    page.value = 1
    total.value = 0
    loading.value = false
    errorMessage.value = ''
    filterError.value = ''
    controller?.abort()
    controller = null
    requestId += 1
    if (enabled) void load(1)
  },
  { immediate: true },
)
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.history-select {
  height: 2.25rem;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
</style>
