<template>
  <Card class="mb-5">
    <CardContent class="flex gap-2">
      <Button v-has-permission="PERMISSIONS.customer.export" @click="batchExportExcel">全部导出(Excel)</Button>
      <Button v-has-permission="PERMISSIONS.customer.export" @click="chooseExportExcel">选择导出(Excel)</Button>
    </CardContent>
  </Card>

  <Card class="mt-5">
    <CardContent class="p-0">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead class="w-[50px]">
              <Checkbox
                :checked="isAllSelected"
                @update:checked="toggleAllSelection"
              />
            </TableHead>
            <TableHead class="w-[80px]">序号</TableHead>
            <TableHead>负责人</TableHead>
            <TableHead>所属活动</TableHead>
            <TableHead>姓名</TableHead>
            <TableHead>称呼</TableHead>
            <TableHead>手机</TableHead>
            <TableHead>微信</TableHead>
            <TableHead>是否贷款</TableHead>
            <TableHead>意向状态</TableHead>
            <TableHead>线索状态</TableHead>
            <TableHead>线索来源</TableHead>
            <TableHead>意向产品</TableHead>
            <TableHead>下次联系时间</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="(row, index) in customerList" :key="row.id ?? index">
            <TableCell>
              <Checkbox
                :checked="selectedIds.includes(row.id)"
                @update:checked="(v) => toggleRowSelection(row, v)"
              />
            </TableCell>
            <TableCell>{{ startIndex(index) }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.ownerName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.activityName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">
              <Button v-has-permission="PERMISSIONS.customer.view" variant="link" size="sm" class="h-auto p-0" @click="handleView(row)">
                {{ row.customerName }}
              </Button>
            </TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.appellationName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.phone }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.weixin }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.needLoanName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.intentionStateName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.stateName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.sourceName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.intentionProductName }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.nextContactTime }}</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </CardContent>
  </Card>
  <div class="mt-4">
    <DataTablePagination
      :page-size="pageSize"
      :total="total"
      @change="page"
    />
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
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

const customerList = ref<Customer[]>([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const selectedIds = ref<(number | string)[]>([])
const exporting = ref(false)
const router = useRouter()

const isAllSelected = computed(() => {
  return customerList.value.length > 0 && selectedIds.value.length === customerList.value.length
})

function toggleAllSelection(checked: boolean) {
  if (checked) {
    selectedIds.value = customerList.value.map((row: Customer) => row.id)
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
