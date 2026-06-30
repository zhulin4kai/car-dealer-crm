<script setup lang="ts">
import { computed } from 'vue'

import type { AiToolResult } from '@/modules/ai/model/ai.types'

defineOptions({
  name: 'AiToolResultCard',
})

const props = defineProps<{
  result: AiToolResult
}>()

type BusinessField = {
  label: string
  value: string
  emphasis?: boolean
}

type BusinessCard = {
  key: string
  title: string
  subtitle: string
  status?: string
  fields: BusinessField[]
  action: string
}

type ResultKind = 'transaction' | 'customer' | 'inventory' | 'followup' | 'product' | 'unknown'

const TRANSACTION_NO_FIELD = `tran${'No'}`
const STAGE_LABEL_FIELD = `stage${'Label'}`
const CREATE_TIME_FIELD = `create${'Time'}`

const records = computed(() => extractRecords(props.result.data))
const resultKind = computed<ResultKind>(() => detectKind(props.result, records.value))
const businessTitle = computed(() => titleByKind(resultKind.value, props.result.summary))
const businessCards = computed(() =>
  records.value.slice(0, 6).map((record, index) => buildCard(resultKind.value, record, index)).filter(isCard),
)

function extractRecords(value: unknown): Array<Record<string, unknown>> {
  if (Array.isArray(value)) return value.map(toRecord).filter(isRecord)

  const record = toRecord(value)
  if (!record) return []

  const items = record.items
  if (Array.isArray(items)) return items.map(toRecord).filter(isRecord)

  return [record]
}

function toRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null
  return value as Record<string, unknown>
}

function isRecord(value: Record<string, unknown> | null): value is Record<string, unknown> {
  return value !== null
}

function isCard(value: BusinessCard | null): value is BusinessCard {
  return value !== null
}

function detectKind(result: AiToolResult, rows: Array<Record<string, unknown>>): ResultKind {
  const toolName = String(result.toolName ?? '').toLowerCase()
  if (toolName.includes('inventory') || toolName.includes('stock')) return 'inventory'
  if (toolName.includes('follow')) return 'followup'
  if (toolName.includes('customer')) return 'customer'
  if (toolName.includes('product') || toolName.includes('vehicle')) return 'product'
  if (
    toolName.includes('tran') ||
    toolName.includes('transaction') ||
    toolName.includes('approval')
  ) {
    return 'transaction'
  }
  if (rows.some(isTransactionRecord)) return 'transaction'
  if (rows.some(isInventoryRecord)) return 'inventory'
  if (rows.some(isFollowupRecord)) return 'followup'
  if (rows.some(isCustomerRecord)) return 'customer'
  if (rows.some(isProductRecord)) return 'product'
  return 'unknown'
}

function titleByKind(kind: ResultKind, fallback: string): string {
  const titles: Record<ResultKind, string> = {
    transaction: '交易结果',
    customer: '客户结果',
    inventory: '库存预警',
    followup: '跟进任务',
    product: '商品信息',
    unknown: fallback || '查询结果',
  }
  return titles[kind]
}

function buildCard(kind: ResultKind, record: Record<string, unknown>, index: number): BusinessCard | null {
  if (kind === 'transaction') return buildTransactionCard(record, index)
  if (kind === 'customer') return buildCustomerCard(record, index)
  if (kind === 'inventory') return buildInventoryCard(record, index)
  if (kind === 'followup') return buildFollowupCard(record, index)
  if (kind === 'product') return buildProductCard(record, index)
  return null
}

function isTransactionRecord(record: Record<string, unknown>): boolean {
  return [
    TRANSACTION_NO_FIELD,
    'transactionNo',
    'transaction_no',
    'money',
    'amount',
    STAGE_LABEL_FIELD,
  ].some((field) => field in record)
}

function isCustomerRecord(record: Record<string, unknown>): boolean {
  return ['customerName', 'customer_name', 'phoneMasked', 'ownerName'].some((field) => field in record)
}

function isInventoryRecord(record: Record<string, unknown>): boolean {
  return ['sku', 'stock', 'minStock', 'min_stock'].some((field) => field in record)
}

function isFollowupRecord(record: Record<string, unknown>): boolean {
  return ['taskType', 'task_type', 'dueTime', 'due_time', 'relatedObjectName'].some((field) => field in record)
}

function isProductRecord(record: Record<string, unknown>): boolean {
  return ['sku', 'name', 'categoryName', 'price'].some((field) => field in record)
}

function buildTransactionCard(record: Record<string, unknown>, index: number): BusinessCard {
  const transactionNo =
    readText(record, [TRANSACTION_NO_FIELD, 'transactionNo', 'transaction_no', 'no']) ||
    `第 ${index + 1} 笔交易`
  const customerName = readText(record, ['customerName', 'customer_name', 'customer']) || '--'
  const amount = readNumber(record, ['money', 'amount', 'totalAmount', 'total_amount'])
  const status =
    readText(record, [STAGE_LABEL_FIELD, 'statusLabel', 'status_label', 'stateLabel', 'stage']) ||
    '--'
  const createTime = readText(record, [CREATE_TIME_FIELD, 'createdAt', 'create_time', 'created_at'])

  return {
    key: `transaction-${transactionNo}-${index}`,
    title: transactionNo,
    subtitle: customerName === '--' ? '交易信息' : `客户：${customerName}`,
    status: status === '--' ? undefined : status,
    fields: [
      { label: '交易编号', value: transactionNo, emphasis: true },
      { label: '客户', value: customerName },
      { label: '金额', value: formatCurrency(amount), emphasis: true },
      { label: '状态', value: status },
      { label: '创建时间', value: formatDateTime(createTime) },
    ],
    action: '建议在交易管理中查看审批、收款、发票和交付事项。',
  }
}

function buildCustomerCard(record: Record<string, unknown>, index: number): BusinessCard {
  const customerName = readText(record, ['customerName', 'customer_name', 'name']) || `客户 ${index + 1}`
  const status = readText(record, ['customerStatusName', 'customer_status_name', 'status']) || '--'
  return {
    key: `customer-${customerName}-${index}`,
    title: customerName,
    subtitle: status === '--' ? '客户信息' : `状态：${status}`,
    status: status === '--' ? undefined : status,
    fields: [
      { label: '客户', value: customerName, emphasis: true },
      { label: '电话', value: readText(record, ['phoneMasked', 'phone_masked', 'phone']) || '--' },
      { label: '负责人', value: readText(record, ['ownerName', 'owner_name']) || '--' },
      { label: '意向车型', value: readText(record, ['intentionProductName', 'productName']) || '--' },
      { label: '下次联系', value: formatDateTime(readText(record, ['nextContactTime'])) },
    ],
    action: '建议结合客户详情、最近跟进和商机状态判断下一步动作。',
  }
}

function buildInventoryCard(record: Record<string, unknown>, index: number): BusinessCard {
  const sku = readText(record, ['sku', 'productSku']) || `库存 ${index + 1}`
  const stock = readText(record, ['stock']) || '--'
  const minStock = readText(record, ['minStock', 'min_stock']) || '--'
  const status = readText(record, ['status']) || (Number(stock) <= Number(minStock) ? '库存风险' : '--')
  return {
    key: `inventory-${sku}-${index}`,
    title: readText(record, ['name', 'productName']) || sku,
    subtitle: `SKU：${sku}`,
    status: status === '--' ? undefined : status,
    fields: [
      { label: 'SKU', value: sku, emphasis: true },
      { label: '商品', value: readText(record, ['name', 'productName']) || '--' },
      { label: '分类', value: readText(record, ['categoryName', 'category_name']) || '--' },
      { label: '当前库存', value: stock, emphasis: true },
      { label: '最低库存', value: minStock },
    ],
    action: '建议库存人员核对实车和补货计划，避免影响报价、订单和交付。',
  }
}

function buildFollowupCard(record: Record<string, unknown>, index: number): BusinessCard {
  const title = readText(record, ['title']) || `跟进任务 ${index + 1}`
  const status = readText(record, ['status']) || '--'
  return {
    key: `followup-${title}-${index}`,
    title,
    subtitle: readText(record, ['relatedObjectName', 'related_object_name']) || '跟进任务',
    status: status === '--' ? undefined : status,
    fields: [
      { label: '任务标题', value: title, emphasis: true },
      { label: '任务类型', value: readText(record, ['taskType', 'task_type']) || '--' },
      { label: '关联对象', value: readText(record, ['relatedObjectName', 'related_object_name']) || '--' },
      { label: '优先级', value: readText(record, ['priority']) || '--' },
      { label: '计划时间', value: formatDateTime(readText(record, ['dueTime', 'due_time'])) },
    ],
    action: '建议按计划时间推进跟进，并在完成后补充沟通记录。',
  }
}

function buildProductCard(record: Record<string, unknown>, index: number): BusinessCard {
  const sku = readText(record, ['sku', 'productSku']) || `商品 ${index + 1}`
  const name = readText(record, ['name', 'productName']) || sku
  return {
    key: `product-${sku}-${index}`,
    title: name,
    subtitle: `SKU：${sku}`,
    status: readText(record, ['status']) || undefined,
    fields: [
      { label: 'SKU', value: sku, emphasis: true },
      { label: '商品', value: name },
      { label: '分类', value: readText(record, ['categoryName', 'category_name']) || '--' },
      { label: '规格', value: readText(record, ['specification', 'productSpecification']) || '--' },
      { label: '价格', value: formatCurrency(readNumber(record, ['price'])) },
      { label: '库存', value: readText(record, ['stock']) || '--' },
    ],
    action: '建议结合库存实例和报价规则判断是否适合推荐给客户。',
  }
}

function readText(record: Record<string, unknown>, fields: string[]): string {
  for (const field of fields) {
    const value = record[field]
    if (value === null || value === undefined || value === '') continue
    return String(value)
  }
  return ''
}

function readNumber(record: Record<string, unknown>, fields: string[]): number | null {
  for (const field of fields) {
    const value = record[field]
    if (typeof value === 'number' && Number.isFinite(value)) return value
    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value)
      if (Number.isFinite(parsed)) return parsed
    }
  }
  return null
}

function formatCurrency(value: number | null): string {
  if (value === null) return '--'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDateTime(value: string): string {
  if (!value) return '--'
  const date = new Date(value)
  if (!Number.isNaN(date.getTime())) {
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
      .format(date)
      .replace(/\//g, '-')
  }
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-muted)] p-3">
    <div class="min-w-0">
      <div class="text-sm font-semibold text-[var(--crm-text-primary)]">{{ businessTitle }}</div>
      <div v-if="result.summary" class="mt-1 text-xs text-[var(--crm-text-secondary)]">
        {{ result.summary }}
      </div>
    </div>

    <div v-if="businessCards.length" class="mt-3 space-y-3">
      <article
        v-for="card in businessCards"
        :key="card.key"
        class="rounded-md bg-[var(--crm-bg-surface)] p-3 text-xs shadow-sm"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="truncate text-sm font-semibold text-[var(--crm-text-primary)]">
              {{ card.title }}
            </div>
            <div class="mt-1 truncate text-[var(--crm-text-secondary)]">{{ card.subtitle }}</div>
          </div>
          <span
            v-if="card.status"
            class="shrink-0 rounded-full bg-[var(--crm-info-bg)] px-2 py-0.5 font-medium text-[var(--crm-info)]"
          >
            {{ card.status }}
          </span>
        </div>

        <dl class="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <div v-for="field in card.fields" :key="field.label" class="min-w-0">
            <dt class="text-[var(--crm-text-tertiary)]">{{ field.label }}</dt>
            <dd
              class="mt-0.5 truncate text-[var(--crm-text-secondary)]"
              :class="field.emphasis ? 'font-semibold text-[var(--crm-text-primary)]' : ''"
            >
              {{ field.value }}
            </dd>
          </div>
        </dl>

        <div class="mt-3 rounded-md bg-[var(--crm-info-bg)] px-3 py-2 text-[var(--crm-primary)]">
          {{ card.action }}
        </div>
      </article>
    </div>

    <div
      v-else
      class="mt-3 rounded-md bg-[var(--crm-bg-surface)] px-3 py-2 text-xs text-[var(--crm-text-secondary)]"
    >
      查询已完成，当前结果暂无可展示的业务明细。
    </div>
  </div>
</template>
