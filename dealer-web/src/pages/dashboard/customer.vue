<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="crm-panel-body">
        <div class="crm-toolbar-actions">
          <Button
            v-has-permission="PERMISSIONS.customer.export"
            class="gap-2"
            @click="batchExportExcel"
          >
            <Download class="h-4 w-4" />
            全部导出
          </Button>
          <Button
            v-has-permission="PERMISSIONS.customer.export"
            variant="outline"
            class="gap-2"
            @click="chooseExportExcel"
          >
            <ListChecks class="h-4 w-4" />
            选择导出
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <Table class="min-w-[1500px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[50px]">
                <Checkbox :checked="isAllSelected" @update:checked="toggleAllSelection" />
              </TableHead>
              <TableHead
                class="w-[80px]"
                sortable
                sort-key="index"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >序号</TableHead
              >
              <TableHead
                sortable
                sort-key="ownerName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >负责人</TableHead
              >
              <TableHead
                sortable
                sort-key="activityName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >所属活动</TableHead
              >
              <TableHead
                sortable
                sort-key="customerName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >姓名</TableHead
              >
              <TableHead
                sortable
                sort-key="appellationName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >称呼</TableHead
              >
              <TableHead
                sortable
                sort-key="phone"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >手机</TableHead
              >
              <TableHead
                sortable
                sort-key="weixin"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >微信</TableHead
              >
              <TableHead
                sortable
                sort-key="needLoanName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >是否贷款</TableHead
              >
              <TableHead
                sortable
                sort-key="intentionStateName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >意向状态</TableHead
              >
              <TableHead
                sortable
                sort-key="stateName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >线索状态</TableHead
              >
              <TableHead
                sortable
                sort-key="sourceName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >线索来源</TableHead
              >
              <TableHead
                sortable
                sort-key="intentionProductName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >意向产品</TableHead
              >
              <TableHead
                sortable
                sort-key="nextContactTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >下次联系时间</TableHead
              >
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="displayCustomerList.length === 0">
              <TableCell colspan="14" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                >暂无客户数据</TableCell
              >
            </TableRow>
            <TableRow v-for="(row, index) in displayCustomerList" :key="row.id ?? index">
              <TableCell>
                <Checkbox
                  :checked="selectedIds.includes(row.id)"
                  @update:checked="(v) => toggleRowSelection(row, v)"
                />
              </TableCell>
              <TableCell class="text-[var(--crm-text-tertiary)]">{{ startIndex(index) }}</TableCell>
              <TableCell
                class="max-w-[150px] truncate font-medium text-[var(--crm-text-primary)]"
                >{{ row.ownerName || '--' }}</TableCell
              >
              <TableCell class="max-w-[150px] truncate">{{ row.activityName || '--' }}</TableCell>
              <TableCell class="max-w-[150px] truncate">
                <Button
                  v-has-permission="PERMISSIONS.customer.view"
                  variant="link"
                  size="sm"
                  class="h-auto p-0 font-semibold text-[var(--crm-primary)]"
                  @click="handleView(row)"
                >
                  {{ row.customerName || '--' }}
                </Button>
              </TableCell>
              <TableCell class="max-w-[120px] truncate">{{
                row.appellationName || '--'
              }}</TableCell>
              <TableCell
                class="max-w-[150px] truncate font-medium text-[var(--crm-text-primary)]"
                >{{ formatPhone(row.phone) }}</TableCell
              >
              <TableCell class="max-w-[150px] truncate">{{ row.weixin || '--' }}</TableCell>
              <TableCell class="max-w-[120px] truncate">
                <StatusBadge
                  :label="row.needLoanName"
                  :tone="row.needLoanName === '是' ? 'warning' : 'muted'"
                />
              </TableCell>
              <TableCell class="max-w-[150px] truncate">
                <StatusBadge
                  :label="row.intentionStateName"
                  :tone="getCustomerTone(row.intentionStateName)"
                />
              </TableCell>
              <TableCell class="max-w-[150px] truncate">
                <StatusBadge :label="row.stateName" :tone="getCustomerStateTone(row.stateName)" />
              </TableCell>
              <TableCell class="max-w-[150px] truncate">
                <StatusBadge :label="row.sourceName" tone="info" />
              </TableCell>
              <TableCell class="max-w-[150px] truncate">{{
                row.intentionProductName || '--'
              }}</TableCell>
              <TableCell class="max-w-[150px] truncate">{{
                formatDateTime(row.nextContactTime)
              }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="crm-table-footer">
        <DataTablePagination
          :page="currentPage"
          :page-size="pageSize"
          :total="total"
          @change="page"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageTip } from '@/shared/utils/feedback'
import { saveBlob } from '@/shared/utils/browser-download'
import { getCustomerList, exportCustomers } from '@/modules/customer/api/customer-api'
import type { Customer } from '@/modules/customer/model/customer.types'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui/table'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { formatDateTime, formatPhone } from '@/shared/utils/display-format'
import { useClientSort } from '@/shared/utils/table-sort'
import { Download, ListChecks } from '@lucide/vue'

const customerList = ref<Customer[]>([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const selectedIds = ref<(number | string)[]>([])
const exporting = ref(false)
const router = useRouter()
const {
  sortBy,
  sortDirection,
  sortedRows: displayCustomerList,
  toggleSort,
} = useClientSort<Customer>(customerList, {
  index: 'id',
  ownerName: 'ownerName',
  activityName: 'activityName',
  customerName: 'customerName',
  appellationName: 'appellationName',
  phone: 'phone',
  weixin: 'weixin',
  needLoanName: 'needLoanName',
  intentionStateName: 'intentionStateName',
  stateName: 'stateName',
  sourceName: 'sourceName',
  intentionProductName: 'intentionProductName',
  nextContactTime: 'nextContactTime',
})

const isAllSelected = computed(() => {
  return (
    displayCustomerList.value.length > 0 &&
    selectedIds.value.length === displayCustomerList.value.length
  )
})

function toggleAllSelection(checked: boolean) {
  if (checked) {
    selectedIds.value = displayCustomerList.value.map((row: Customer) => row.id)
  } else {
    selectedIds.value = []
  }
}

function toggleRowSelection(row: Customer, checked: boolean) {
  if (checked) {
    selectedIds.value = [...selectedIds.value, row.id]
  } else {
    selectedIds.value = selectedIds.value.filter((id: number | string) => id !== row.id)
  }
}

function handleView(row: Customer): void {
  void router.push({ name: 'customer-detail', params: { id: String(row.id) } })
}

const startIndex = (index: number) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

const getData = async (current: number) => {
  try {
    const resp = await getCustomerList({ current })
    customerList.value = resp.list
    pageSize.value = resp.pageSize ?? 10
    total.value = resp.total
    selectedIds.value = []
  } catch {
    messageTip('获取客户列表失败', 'error')
  }
}

const page = (number: number) => {
  getData(number)
  currentPage.value = number
}

function getCustomerTone(
  label?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (!label) return 'muted'
  if (label.includes('成交') || label.includes('高') || label.includes('有意向')) return 'info'
  if (label.includes('跟进')) return 'warning'
  if (label.includes('流失') || label.includes('无效')) return 'danger'
  if (label.includes('待')) return 'purple'
  return 'success'
}

function getCustomerStateTone(
  label?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (!label) return 'muted'
  if (label.includes('成交') || label.includes('转')) return 'success'
  if (label.includes('流失') || label.includes('无效')) return 'danger'
  if (label.includes('待') || label.includes('跟进')) return 'warning'
  return 'info'
}

async function handleExport(ids?: (number | string)[]): Promise<void> {
  if (exporting.value) {
    return
  }
  exporting.value = true
  try {
    const { blob, filename } = await exportCustomers(ids)
    saveBlob(blob, filename)
    messageTip('导出成功', 'success')
  } catch {
    messageTip('导出失败', 'error')
  } finally {
    exporting.value = false
  }
}

function batchExportExcel() {
  void handleExport()
}

function chooseExportExcel() {
  if (selectedIds.value.length <= 0) {
    messageTip('请选择要导出的数据', 'warning')
    return
  }
  void handleExport(selectedIds.value)
}

onMounted(() => {
  getData(1)
})
</script>
