<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="crm-panel-body">
        <div class="crm-toolbar">
          <div class="crm-field">
            <Label class="crm-field-label">用户姓名</Label>
            <Input
              v-model="searchForm.userName"
              class="w-[160px]"
              placeholder="请输入用户姓名"
              @keyup.enter="handleSearch()"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">动作编码</Label>
            <Input
              v-model="searchForm.actionCode"
              class="w-[180px]"
              placeholder="请输入动作编码"
              @keyup.enter="handleSearch()"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">模块</Label>
            <Input
              v-model="searchForm.moduleName"
              class="w-[160px]"
              placeholder="请输入模块"
              @keyup.enter="handleSearch()"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">结果</Label>
            <select
              v-model="searchForm.result"
              class="h-10 w-[120px] rounded-md border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-3 text-sm text-[var(--crm-text-primary)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--crm-primary)]"
            >
              <option value="">全部</option>
              <option value="SUCCESS">成功</option>
              <option value="FAILURE">失败</option>
            </select>
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">开始时间</Label>
            <Input v-model="searchForm.startTime" class="w-[190px]" type="datetime-local" />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">结束时间</Label>
            <Input v-model="searchForm.endTime" class="w-[190px]" type="datetime-local" />
          </div>
          <div class="crm-toolbar-actions">
            <Button class="gap-2" :disabled="loading" @click="handleSearch()">
              <Search class="h-4 w-4" />
              查询
            </Button>
            <Button variant="outline" class="gap-2" :disabled="loading" @click="handleReset()">
              <RotateCcw class="h-4 w-4" />
              重置
            </Button>
            <Button
              v-has-permission="PERMISSIONS.audit.operation.export"
              variant="outline"
              class="gap-2"
              :disabled="exporting"
              @click="handleExport()"
            >
              <Download class="h-4 w-4" />
              导出
            </Button>
          </div>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <template v-if="loading">
          <div class="space-y-2 p-5">
            <Skeleton v-for="i in 5" :key="i" class="h-8 w-full" />
          </div>
        </template>
        <Table v-else class="min-w-[1260px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead class="w-[140px]">用户姓名</TableHead>
              <TableHead class="w-[180px]">动作编码</TableHead>
              <TableHead class="w-[130px]">模块</TableHead>
              <TableHead class="w-[120px]">对象类型</TableHead>
              <TableHead class="w-[120px]">资源ID</TableHead>
              <TableHead class="w-[100px]">结果</TableHead>
              <TableHead class="w-[140px]">IP</TableHead>
              <TableHead class="w-[190px]">操作时间</TableHead>
              <TableHead class="w-[90px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="rows.length === 0">
              <TableCell colspan="10" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无操作记录
              </TableCell>
            </TableRow>
            <TableRow v-for="(row, idx) in rows" :key="row.id">
              <TableCell class="text-[var(--crm-text-tertiary)]">{{ startIndex(idx) }}</TableCell>
              <TableCell>{{ row.userName || '--' }}</TableCell>
              <TableCell>
                <span
                  class="inline-flex rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 font-mono text-xs text-[var(--crm-text-secondary)]"
                >
                  {{ row.actionCode || '--' }}
                </span>
              </TableCell>
              <TableCell>{{ row.moduleName || '--' }}</TableCell>
              <TableCell>{{ row.objectType || '--' }}</TableCell>
              <TableCell>{{ row.resourceId || '--' }}</TableCell>
              <TableCell>
                <StatusBadge :label="resultLabel(row.result)" :tone="resultTone(row.result)" />
              </TableCell>
              <TableCell>{{ row.ip || '--' }}</TableCell>
              <TableCell>{{ row.createTime || '--' }}</TableCell>
              <TableCell>
                <RowActionButton
                  v-has-permission="PERMISSIONS.audit.operation.detail"
                  label="详情"
                  @click="openDetail(row)"
                >
                  <Eye class="h-4 w-4" />
                </RowActionButton>
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
          @change="loadPage"
        />
      </div>
    </section>

    <Dialog v-model:open="detailVisible">
      <DialogContent class="sm:max-w-[760px]">
        <DialogHeader>
          <DialogTitle>操作记录详情</DialogTitle>
        </DialogHeader>
        <div v-if="activeRecord" class="space-y-4 text-sm">
          <div class="grid grid-cols-2 gap-4">
            <div v-for="item in detailItems" :key="item.label" class="min-w-0">
              <div class="text-xs text-[var(--crm-text-tertiary)]">{{ item.label }}</div>
              <div class="mt-1 break-words font-medium text-[var(--crm-text-primary)]">
                {{ item.value }}
              </div>
            </div>
          </div>
          <div>
            <div class="text-xs text-[var(--crm-text-tertiary)]">摘要</div>
            <pre
              class="mt-1 max-h-52 overflow-auto rounded-md bg-[var(--crm-bg-muted)] p-3 text-xs leading-5 text-[var(--crm-text-secondary)]"
              >{{ activeRecord.detail || '--' }}</pre
            >
          </div>
        </div>
        <div v-else class="py-10 text-center text-sm text-[var(--crm-text-tertiary)]">加载中</div>
        <DialogFooter>
          <Button variant="outline" @click="detailVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Eye, RotateCcw, Search } from '@lucide/vue'

import {
  exportOperationLogs,
  fetchOperationLogDetail,
  fetchOperationLogPage,
} from '@/modules/audit/api/audit-api'
import {
  AUDIT_RESULT_LABEL,
  type AuditOperationLog,
  type AuditOperationLogQuery,
  type AuditResult,
} from '@/modules/audit/model/audit.types'
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
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { PERMISSIONS } from '@/shared/constants/permissions'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { saveBlob } from '@/shared/utils/browser-download'
import { messageTip } from '@/shared/utils/feedback'

defineOptions({
  name: 'AuditOperationLogPage',
})

const pageSize = 10
const loading = ref(false)
const exporting = ref(false)
const currentPage = ref(1)
const total = ref(0)
const rows = ref<AuditOperationLog[]>([])
const detailVisible = ref(false)
const activeRecord = ref<AuditOperationLog | null>(null)

const searchForm = reactive<{
  userName: string
  actionCode: string
  moduleName: string
  result: AuditResult | ''
  startTime: string
  endTime: string
}>({
  userName: '',
  actionCode: '',
  moduleName: '',
  result: '',
  startTime: '',
  endTime: '',
})

const detailItems = computed(() => {
  const record = activeRecord.value
  if (!record) return []
  return [
    { label: '用户ID', value: record.userId ?? '--' },
    { label: '用户姓名', value: record.userName || '--' },
    { label: '动作编码', value: record.actionCode || '--' },
    { label: '模块', value: record.moduleName || '--' },
    { label: '对象类型', value: record.objectType || '--' },
    { label: '资源ID', value: record.resourceId || '--' },
    { label: '结果', value: resultLabel(record.result) },
    { label: 'IP', value: record.ip || '--' },
    { label: '请求ID', value: record.requestId || '--' },
    { label: '操作时间', value: record.createTime || '--' },
  ]
})

function buildQuery(page: number): AuditOperationLogQuery {
  return {
    page,
    size: pageSize,
    userName: trimOrUndefined(searchForm.userName),
    actionCode: trimOrUndefined(searchForm.actionCode),
    moduleName: trimOrUndefined(searchForm.moduleName),
    result: searchForm.result || undefined,
    startTime: normalizeDateTime(searchForm.startTime),
    endTime: normalizeDateTime(searchForm.endTime),
  }
}

async function loadPage(page = currentPage.value): Promise<void> {
  currentPage.value = page
  loading.value = true
  try {
    const result = await fetchOperationLogPage(buildQuery(page))
    rows.value = result.list
    total.value = result.total
    currentPage.value = result.pageNum || page
  } catch {
    messageTip('操作记录加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  void loadPage(1)
}

function handleReset(): void {
  searchForm.userName = ''
  searchForm.actionCode = ''
  searchForm.moduleName = ''
  searchForm.result = ''
  searchForm.startTime = ''
  searchForm.endTime = ''
  void loadPage(1)
}

async function handleExport(): Promise<void> {
  if (exporting.value) return
  exporting.value = true
  try {
    const { blob, filename } = await exportOperationLogs(buildQuery(currentPage.value))
    saveBlob(blob, filename)
    messageTip('导出成功', 'success')
  } catch {
    messageTip('导出失败', 'error')
  } finally {
    exporting.value = false
  }
}

async function openDetail(row: AuditOperationLog): Promise<void> {
  activeRecord.value = null
  detailVisible.value = true
  try {
    activeRecord.value = await fetchOperationLogDetail(row.id)
  } catch {
    detailVisible.value = false
    messageTip('操作记录详情加载失败', 'error')
  }
}

function startIndex(index: number): number {
  return (currentPage.value - 1) * pageSize + index + 1
}

function resultLabel(value: AuditResult | string | null | undefined): string {
  return value === 'SUCCESS' || value === 'FAILURE' ? AUDIT_RESULT_LABEL[value] : '--'
}

function resultTone(value: AuditResult | string | null | undefined): 'success' | 'danger' | 'muted' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILURE') return 'danger'
  return 'muted'
}

function trimOrUndefined(value: string): string | undefined {
  const trimmed = value.trim()
  return trimmed || undefined
}

function normalizeDateTime(value: string): string | undefined {
  if (!value) return undefined
  const normalized = value.replace('T', ' ')
  return normalized.length === 16 ? `${normalized}:00` : normalized
}

onMounted(() => {
  void loadPage(1)
})
</script>
