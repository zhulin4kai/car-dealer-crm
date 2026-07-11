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
  tables?: DisplayTable[]
}

type ResultKind =
  | 'transaction'
  | 'customer'
  | 'inventory'
  | 'followup'
  | 'product'
  | 'opportunity'
  | 'quote'
  | 'testDrive'
  | 'delivery'
  | 'businessOverview'
  | 'unknown'

type DisplayItem = {
  label: string
  value: string
}

type DisplayColumn = {
  key: string
  label: string
}

type DisplayTable = {
  title?: string
  columns: DisplayColumn[]
  rows: Array<Record<string, string>>
}

type DisplayBlock =
  | { type: 'text'; content: string }
  | { type: 'fields' | 'metrics'; items: DisplayItem[] }
  | { type: 'table'; columns: DisplayColumn[]; rows: Array<Record<string, string>> }
  | { type: 'status'; label: string; tone: string }

const TRANSACTION_NO_FIELD = `tran${'No'}`
const STAGE_LABEL_FIELD = `stage${'Label'}`
const CREATE_TIME_FIELD = `create${'Time'}`
const SINGLE_RECORD_TOOL_NAMES = new Set([
  'get_customer_profile',
  'resolve_vehicle_product',
  'get_transaction_detail',
  'get_opportunity_detail',
  'get_quote_detail',
  'get_test_drive_detail',
  'get_delivery_detail',
  'get_business_overview',
])

const records = computed(() => extractRecords(props.result.data, props.result.toolName))
const resultKind = computed<ResultKind>(() => detectKind(props.result, records.value))
const businessTitle = computed(() => titleByKind(resultKind.value, props.result.summary))
const businessCards = computed(() =>
  records.value.slice(0, 6).map((record, index) => buildCard(resultKind.value, record, index)).filter(isCard),
)
const displayBlocks = computed(() => extractDisplayBlocks(props.result.data))

function extractDisplayBlocks(value: unknown): DisplayBlock[] {
  const root = toRecord(value)
  const presentation = toRecord(root?.presentation)
  const blocks = presentation?.blocks ?? root?.blocks
  if (!Array.isArray(blocks)) return []
  return blocks.map(parseDisplayBlock).filter(isDisplayBlock)
}

function parseDisplayBlock(value: unknown): DisplayBlock | null {
  const block = toRecord(value)
  const type = readText(block ?? {}, ['type'])
  if (!block || !type) return null
  if (type === 'text') {
    const content = readText(block, ['content'])
    return content ? { type, content } : null
  }
  if (type === 'status') {
    const label = readText(block, ['label'])
    return label ? { type, label, tone: readText(block, ['tone']) || 'muted' } : null
  }
  if (type === 'fields' || type === 'metrics') {
    const items = parseDisplayItems(block.items)
    return items.length ? { type, items } : null
  }
  if (type === 'table') {
    const columns = parseDisplayColumns(block.columns)
    const rows = parseDisplayRows(block.rows, columns)
    return columns.length && rows.length ? { type, columns, rows } : null
  }
  return null
}

function parseDisplayItems(value: unknown): DisplayItem[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    const record = toRecord(item)
    if (!record) return []
    const label = readText(record, ['label'])
    const displayValue = safeDisplayValue(record.value)
    return label && displayValue ? [{ label, value: displayValue }] : []
  })
}

function parseDisplayColumns(value: unknown): DisplayColumn[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    const record = toRecord(item)
    if (!record) return []
    const key = readText(record, ['key'])
    const label = readText(record, ['label'])
    return key && label ? [{ key, label }] : []
  })
}

function parseDisplayRows(
  value: unknown,
  columns: DisplayColumn[],
): Array<Record<string, string>> {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) => {
    const record = toRecord(item)
    if (!record) return []
    const row: Record<string, string> = {}
    columns.forEach((column) => {
      row[column.key] = safeDisplayValue(record[column.key])
    })
    return [row]
  })
}

function safeDisplayValue(value: unknown): string {
  if (typeof value === 'string') return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  if (typeof value === 'boolean') return value ? '是' : '否'
  return ''
}

function isDisplayBlock(value: DisplayBlock | null): value is DisplayBlock {
  return value !== null
}

function statusToneClass(tone: string): string {
  if (tone === 'success') return 'bg-[var(--crm-success-bg)] text-[var(--crm-success)]'
  if (tone === 'danger') return 'bg-[var(--crm-danger-bg)] text-[var(--crm-danger)]'
  if (tone === 'warning') return 'bg-[var(--crm-warning-bg)] text-[var(--crm-warning)]'
  return 'bg-[var(--crm-bg-muted)] text-[var(--crm-text-secondary)]'
}

function extractRecords(value: unknown, toolName?: string): Array<Record<string, unknown>> {
  if (Array.isArray(value)) return value.map(toRecord).filter(isRecord)

  const record = toRecord(value)
  if (!record) return []
  if (toolName && SINGLE_RECORD_TOOL_NAMES.has(toolName)) return [record]

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
  if (toolName === 'get_opportunity_detail') return 'opportunity'
  if (toolName === 'get_quote_detail') return 'quote'
  if (toolName === 'get_test_drive_detail') return 'testDrive'
  if (toolName === 'get_delivery_detail') return 'delivery'
  if (toolName === 'get_business_overview') return 'businessOverview'
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
    opportunity: '商机进展',
    quote: '报价详情',
    testDrive: '试驾详情',
    delivery: '交付详情',
    businessOverview: '经营概览',
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
  if (kind === 'opportunity') return buildOpportunityCard(record, index)
  if (kind === 'quote') return buildQuoteCard(record, index)
  if (kind === 'testDrive') return buildTestDriveCard(record, index)
  if (kind === 'delivery') return buildDeliveryCard(record, index)
  if (kind === 'businessOverview') return buildBusinessOverviewCard(record, index)
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
  const status = formatBusinessStatus(
    readText(record, [STAGE_LABEL_FIELD, 'statusLabel', 'status_label', 'stateLabel', 'stage']) ||
      '--',
  )
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
  const status = formatBusinessStatus(
    readText(record, ['customerStatusName', 'customer_status_name', 'status']) || '--',
  )
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
  const status = formatBusinessStatus(
    readText(record, ['status']) || (Number(stock) <= Number(minStock) ? '库存风险' : '--'),
  )
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
  const status = formatBusinessStatus(readText(record, ['status']) || '--')
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
  const status = readText(record, ['status'])
  return {
    key: `product-${sku}-${index}`,
    title: name,
    subtitle: `SKU：${sku}`,
    status: status ? formatBusinessStatus(status) : undefined,
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

function buildOpportunityCard(record: Record<string, unknown>, index: number): BusinessCard {
  const opportunityNo = readText(record, ['opportunityNo']) || `商机 ${index + 1}`
  const stage = formatBusinessStatus(readText(record, ['stage']))
  return {
    key: `opportunity-${opportunityNo}-${index}`,
    title: opportunityNo,
    subtitle: readText(record, ['customerName']) || '商机信息',
    status: stage === '--' ? undefined : stage,
    fields: [
      { label: '商机编号', value: opportunityNo, emphasis: true },
      { label: '客户', value: readText(record, ['customerName']) || '--' },
      { label: '负责人', value: readText(record, ['ownerName']) || '--' },
      { label: '意向商品', value: readText(record, ['productName']) || '--' },
      { label: '预计金额', value: formatCurrency(readNumber(record, ['expectedAmount'])), emphasis: true },
      { label: '预计成交', value: formatDateTime(readText(record, ['expectedCloseDate'])) },
      { label: '下次行动', value: formatDateTime(readText(record, ['nextActionTime'])) },
      { label: '最近跟进', value: formatDateTime(readText(record, ['lastFollowTime'])) },
      { label: '需求摘要', value: readText(record, ['requirement']) || '--' },
      { label: '最近跟进摘要', value: readText(record, ['lastFollowSummary']) || '--' },
    ],
    action: readText(record, ['lostReason', 'resultRemark']) || '建议结合当前阶段和下次行动时间推进商机。',
  }
}

function buildQuoteCard(record: Record<string, unknown>, index: number): BusinessCard {
  const quoteNo = readText(record, ['quoteNo']) || `报价 ${index + 1}`
  const status = formatBusinessStatus(readText(record, ['status']))
  const itemRows = parseQuoteItemRows(record.items)
  return {
    key: `quote-${quoteNo}-${index}`,
    title: quoteNo,
    subtitle: `当前版本：第 ${readText(record, ['versionNo']) || '--'} 版`,
    status: status === '--' ? undefined : status,
    fields: [
      { label: '报价编号', value: quoteNo, emphasis: true },
      { label: '报价状态', value: status },
      { label: '有效期至', value: formatDateTime(readText(record, ['validUntil'])) },
      { label: '报价总额', value: formatCurrency(readNumber(record, ['totalAmount'])), emphasis: true },
      { label: '商品数量', value: readText(record, ['totalItemCount']) || String(itemRows.length) },
      { label: '备注', value: readText(record, ['remark']) || '--' },
    ],
    action: '建议在报价管理中确认版本有效期、商品价格和促销金额。',
    tables: itemRows.length
      ? [
          {
            title: '报价商品',
            columns: [
              { key: 'product', label: '商品' },
              { key: 'specification', label: '规格' },
              { key: 'unitPrice', label: '单价' },
              { key: 'quantity', label: '数量' },
              { key: 'lineAmount', label: '小计' },
              { key: 'promotion', label: '促销' },
            ],
            rows: itemRows,
          },
        ]
      : undefined,
  }
}

function parseQuoteItemRows(value: unknown): Array<Record<string, string>> {
  if (!Array.isArray(value)) return []
  return value.slice(0, 20).flatMap((item) => {
    const record = toRecord(item)
    if (!record) return []
    const promotionName = readText(record, ['promotionName'])
    const promotionAmount = formatCurrency(readNumber(record, ['promotionAmount']))
    return [
      {
        product: readText(record, ['productName']) || '--',
        specification: readText(record, ['productSpecification']) || '--',
        unitPrice: formatCurrency(readNumber(record, ['unitPrice'])),
        quantity: readText(record, ['quantity']) || '--',
        lineAmount: formatCurrency(readNumber(record, ['lineAmount'])),
        promotion: promotionName ? `${promotionName}（${promotionAmount}）` : '--',
      },
    ]
  })
}

function buildTestDriveCard(record: Record<string, unknown>, index: number): BusinessCard {
  const testDriveNo = readText(record, ['testDriveNo']) || `试驾 ${index + 1}`
  const status = formatBusinessStatus(readText(record, ['status']))
  return {
    key: `test-drive-${testDriveNo}-${index}`,
    title: testDriveNo,
    subtitle: readText(record, ['customerName']) || '试驾安排',
    status: status === '--' ? undefined : status,
    fields: [
      { label: '试驾编号', value: testDriveNo, emphasis: true },
      { label: '客户', value: readText(record, ['customerName']) || '--' },
      { label: '试驾车辆', value: readText(record, ['vehicleName']) || '--' },
      { label: '负责人', value: readText(record, ['ownerName']) || '--' },
      { label: '计划开始', value: formatDateTime(readText(record, ['plannedStartTime'])) },
      { label: '计划结束', value: formatDateTime(readText(record, ['plannedEndTime'])) },
      { label: '实际开始', value: formatDateTime(readText(record, ['actualStartTime'])) },
      { label: '实际结束', value: formatDateTime(readText(record, ['actualEndTime'])) },
      { label: '联系人', value: readText(record, ['contactName']) || '--' },
      { label: '联系电话', value: readText(record, ['contactPhoneMasked']) || '--' },
      { label: '客户反馈', value: readText(record, ['customerFeedback']) || '--' },
      { label: '下一步动作', value: readText(record, ['nextAction']) || '--' },
    ],
    action:
      readText(record, ['cancelReason', 'remark', 'result']) ||
      '建议根据试驾状态和客户反馈安排下一步跟进。',
  }
}

function buildDeliveryCard(record: Record<string, unknown>, index: number): BusinessCard {
  const status = formatBusinessStatus(readText(record, ['status']))
  const plannedTime = formatDateTime(readText(record, ['plannedDeliveryTime']))
  return {
    key: `delivery-${plannedTime}-${index}`,
    title: '车辆交付安排',
    subtitle: plannedTime === '--' ? '交付信息' : `计划交付：${plannedTime}`,
    status: status === '--' ? undefined : status,
    fields: [
      { label: '交付状态', value: status, emphasis: true },
      { label: '计划交付', value: plannedTime },
      { label: '实际交付', value: formatDateTime(readText(record, ['actualDeliveryTime'])) },
      { label: '签收人', value: readText(record, ['signerName']) || '--' },
      { label: '签收时间', value: formatDateTime(readText(record, ['signedAt'])) },
      { label: '签收方式', value: formatSignMethod(readText(record, ['signMethod'])) },
      { label: '异常类型', value: formatDeliveryException(readText(record, ['exceptionType'])) },
      { label: '异常说明', value: readText(record, ['exceptionReason']) || '--' },
    ],
    action: '建议在交付管理中核对准备清单、签收结果和异常处理进度。',
  }
}

function buildBusinessOverviewCard(record: Record<string, unknown>, index: number): BusinessCard {
  const summary = toRecord(record.summary) ?? {}
  const salesFunnel = parseNameValueRows(record.salesFunnel)
  const sourceDistribution = parseNameValueRows(record.sourceDistribution)
  const tables: DisplayTable[] = []
  if (salesFunnel.length) {
    tables.push({
      title: '销售漏斗',
      columns: [
        { key: 'name', label: '阶段' },
        { key: 'value', label: '数量' },
      ],
      rows: salesFunnel,
    })
  }
  if (sourceDistribution.length) {
    tables.push({
      title: '线索来源',
      columns: [
        { key: 'name', label: '来源' },
        { key: 'value', label: '数量' },
      ],
      rows: sourceDistribution,
    })
  }
  return {
    key: `business-overview-${index}`,
    title: '当前经营概览',
    subtitle: '数据已按当前用户权限范围统计',
    fields: [
      { label: '有效市场活动', value: readText(summary, ['effectiveActivityCount']) || '0' },
      { label: '市场活动总数', value: readText(summary, ['totalActivityCount']) || '0' },
      { label: '线索总数', value: readText(summary, ['totalClueCount']) || '0' },
      { label: '客户总数', value: readText(summary, ['totalCustomerCount']) || '0' },
      { label: '成功交易额', value: formatCurrency(readNumber(summary, ['successTranAmount'])), emphasis: true },
      { label: '交易总额', value: formatCurrency(readNumber(summary, ['totalTranAmount'])), emphasis: true },
    ],
    action: '建议结合销售漏斗和线索来源变化安排后续经营动作。',
    tables: tables.length ? tables : undefined,
  }
}

function parseNameValueRows(value: unknown): Array<Record<string, string>> {
  if (!Array.isArray(value)) return []
  return value.slice(0, 20).flatMap((item) => {
    const record = toRecord(item)
    if (!record) return []
    const name = readText(record, ['name'])
    const displayName = /^[A-Z][A-Z0-9_]*$/.test(name) ? formatMetricName(name) : name
    return displayName
      ? [{ name: displayName, value: readText(record, ['value']) || '0' }]
      : []
  })
}

function formatMetricName(value: string): string {
  const labels: Record<string, string> = {
    INITIAL_CONTACT: '初步接洽',
    NEED_ANALYSIS: '需求分析',
    PROPOSAL_QUOTE: '方案报价',
    NEGOTIATION: '商务谈判',
    WON: '已赢单',
    LOST: '已输单',
    SHELVED: '已搁置',
    ONLINE_AD: '线上广告',
    SOCIAL_MEDIA: '社交媒体',
    CUSTOMER_REFERRAL: '客户转介绍',
    OFFLINE_EVENT: '线下活动',
    STORE_VISIT: '到店咨询',
    OTHER: '其他',
  }
  return labels[value] ?? '其他'
}

function formatSignMethod(value: string): string {
  const labels: Record<string, string> = {
    PAPER: '纸质签收',
    ELECTRONIC: '电子签收',
    PHOTO: '影像留存',
  }
  if (!value) return '--'
  return labels[value] ?? (/^[A-Z][A-Z0-9_]*$/.test(value) ? '已签收' : value)
}

function formatDeliveryException(value: string): string {
  const labels: Record<string, string> = {
    VEHICLE: '车辆异常',
    DOCUMENT: '资料异常',
    PAYMENT: '款项异常',
    CUSTOMER: '客户原因',
    OTHER: '其他异常',
  }
  if (!value) return '--'
  return labels[value] ?? (/^[A-Z][A-Z0-9_]*$/.test(value) ? '交付异常' : value)
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

function formatBusinessStatus(value: string): string {
  if (!value || value === '--') return '--'
  const labels: Record<string, string> = {
    ACTIVE: '进行中',
    INACTIVE: '未启用',
    PENDING: '待处理',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    OVERDUE: '已逾期',
    IN_STOCK: '有库存',
    OUT_OF_STOCK: '无库存',
    ON_SALE: '上架',
    OFF_SALE: '下架',
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    SENT: '已发送',
    CONFIRMED: '已确认',
    EXPIRED: '已过期',
    SCHEDULED: '已预约',
    RESCHEDULED: '已改期',
    CHECKED_IN: '已签到',
    IN_PROGRESS: '进行中',
    NO_SHOW: '未到店',
    PREPARING: '准备中',
    READY: '待交付',
    PENDING_SIGN: '待签收',
    DELIVERED: '已交付',
    EXCEPTION: '存在异常',
    INITIAL_CONTACT: '初步接洽',
    NEED_ANALYSIS: '需求分析',
    PROPOSAL_QUOTE: '方案报价',
    NEGOTIATION: '商务谈判',
    WON: '已赢单',
    LOST: '已输单',
    SHELVED: '已搁置',
  }
  if (labels[value]) return labels[value]
  return /^[A-Z][A-Z0-9_]*$/.test(value) ? '状态待确认' : value
}
</script>

<template>
  <section class="border-l-2 border-[var(--crm-border-medium)] py-1 pl-4">
    <div class="min-w-0">
      <div class="text-sm font-semibold text-[var(--crm-text-primary)]">{{ businessTitle }}</div>
      <div v-if="result.summary" class="mt-1 text-xs text-[var(--crm-text-secondary)]">
        {{ result.summary }}
      </div>
    </div>

    <div v-if="displayBlocks.length" class="mt-3 space-y-3">
      <template v-for="(block, blockIndex) in displayBlocks" :key="blockIndex">
        <p v-if="block.type === 'text'" class="text-sm leading-6 text-[var(--crm-text-secondary)]">
          {{ block.content }}
        </p>
        <dl
          v-else-if="block.type === 'fields' || block.type === 'metrics'"
          class="grid gap-x-6 gap-y-2 sm:grid-cols-2"
        >
          <div v-for="item in block.items" :key="item.label" class="min-w-0">
            <dt class="text-xs text-[var(--crm-text-tertiary)]">{{ item.label }}</dt>
            <dd class="mt-0.5 truncate text-sm font-medium text-[var(--crm-text-primary)]">
              {{ item.value }}
            </dd>
          </div>
        </dl>
        <div v-else-if="block.type === 'table'" class="overflow-x-auto">
          <table class="w-full min-w-[420px] text-left text-xs">
            <thead class="text-[var(--crm-text-tertiary)]">
              <tr>
                <th v-for="column in block.columns" :key="column.key" class="pb-2 pr-4 font-medium">
                  {{ column.label }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--crm-border-light)]">
              <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
                <td v-for="column in block.columns" :key="column.key" class="py-2 pr-4">
                  {{ row[column.key] || '--' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <span
          v-else-if="block.type === 'status'"
          class="inline-flex rounded-full px-2.5 py-1 text-xs font-medium"
          :class="statusToneClass(block.tone)"
        >
          {{ block.label }}
        </span>
      </template>
    </div>

    <div v-else-if="businessCards.length" class="mt-3 divide-y divide-[var(--crm-border-light)]">
      <article
        v-for="card in businessCards"
        :key="card.key"
        class="py-3 text-xs first:pt-1 last:pb-1"
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

        <div v-if="card.tables?.length" class="mt-4 space-y-4">
          <section v-for="table in card.tables" :key="table.title" class="min-w-0">
            <div v-if="table.title" class="mb-2 font-medium text-[var(--crm-text-primary)]">
              {{ table.title }}
            </div>
            <div class="overflow-x-auto">
              <table class="w-full min-w-[420px] text-left">
                <thead class="text-[var(--crm-text-tertiary)]">
                  <tr>
                    <th
                      v-for="column in table.columns"
                      :key="column.key"
                      class="pb-2 pr-4 font-medium"
                    >
                      {{ column.label }}
                    </th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--crm-border-light)]">
                  <tr v-for="(row, rowIndex) in table.rows" :key="rowIndex">
                    <td
                      v-for="column in table.columns"
                      :key="column.key"
                      class="py-2 pr-4 text-[var(--crm-text-secondary)]"
                    >
                      {{ row[column.key] || '--' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <div class="mt-3 text-[var(--crm-primary)]">
          {{ card.action }}
        </div>
      </article>
    </div>

    <div
      v-else
      class="mt-3 text-xs text-[var(--crm-text-secondary)]"
    >
      查询已完成，当前结果暂无可展示的业务明细。
    </div>
  </section>
</template>
