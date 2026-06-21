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
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.ownerDO?.name }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.activityDO?.name }}</TableCell>
            <TableCell class="truncate max-w-[150px]">
              <Button v-has-permission="PERMISSIONS.customer.view" variant="link" size="sm" class="h-auto p-0" @click="view(row.id)">
                {{ row.clueDO?.fullName }}
              </Button>
            </TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.appellationDO?.typeValue }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.phone }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.weixin }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.needLoanDO?.typeValue }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.intentionStateDO?.typeValue }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.stateDO?.typeValue }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.sourceDO?.typeValue }}</TableCell>
            <TableCell class="truncate max-w-[150px]">{{ row.clueDO?.intentionProductDO?.name }}</TableCell>
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
import { env } from '@/shared/config/env'
import { getToken, messageTip } from '@/shared/utils/legacy-util'
import { getCustomerList } from '@/modules/customer/api/customer-api'
import type { Customer } from '@/modules/customer/model/customer.types'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

// 响应式数据
const customerList = ref([{
  clueDO: {},
}])
const pageSize = ref(10)
const total = ref(0)
const customerIdArray = ref([])
const currentPage = ref(1)
const selectedIds = ref<(number | string)[]>([])

// 计算是否全选
const isAllSelected = computed(() => {
  return customerList.value.length > 0 && selectedIds.value.length === customerList.value.length
})

// 全选/取消全选
function toggleAllSelection(checked: boolean) {
  if (checked) {
    selectedIds.value = customerList.value.map((row: Customer) => row.id)
  } else {
    selectedIds.value = []
  }
  customerIdArray.value = [...selectedIds.value]
}

// 单行选择切换
function toggleRowSelection(row: Customer, checked: boolean) {
  if (checked) {
    selectedIds.value = [...selectedIds.value, row.id]
  } else {
    selectedIds.value = selectedIds.value.filter((id: number | string) => id !== row.id)
  }
  customerIdArray.value = [...selectedIds.value]
}

// 计算序号起始值
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 获取客户分页列表数据
const getData = async (current) => {
  try {
    const resp = await getCustomerList({ current })
    if (true) {
      customerList.value = resp.list
      pageSize.value = resp.pageSize
      total.value = resp.total
      selectedIds.value = []
      customerIdArray.value = []
    }
  } catch (error) {
    messageTip('获取客户列表失败', 'error')
  }
}

// 分页函数
const page = (number) => {
  getData(number)
  currentPage.value = number
}

// 导出 Excel
const exportExcel = (ids) => {
  const token = getToken()
  const iframe = document.createElement("iframe")
  iframe.src = ids
    ? `${env.apiBaseUrl}/api/exportExcel?Authorization=${token}&ids=${ids}`
    : `${env.apiBaseUrl}/api/exportExcel?Authorization=${token}`
  iframe.style.display = "none"
  document.body.appendChild(iframe)
}

// 批量导出
const batchExportExcel = () => {
  exportExcel(null)
}

// 选择导出
const chooseExportExcel = () => {
  if (customerIdArray.value.length <= 0) {
    messageTip("请选择要导出的数据", "warning")
    return
  }
  const ids = customerIdArray.value.join(",")
  exportExcel(ids)
}

// 组件挂载时加载数据
onMounted(() => {
  getData(1)
})
</script>
