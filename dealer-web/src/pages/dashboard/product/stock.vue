<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="min-w-0">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold">库存管理</h2>
            <span
              class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]"
            >
              {{ total }} 条预警
            </span>
          </div>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
            查看低于最低库存的产品，并处理补货和库存变动记录。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <Button variant="outline" :disabled="loading" @click="loadStockAlerts">
            <RefreshCw class="h-4 w-4" />
            刷新数据
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-panel-body">
        <form class="crm-toolbar" @submit.prevent="handleSearch">
          <div class="crm-field">
            <Label class="crm-field-label">SKU</Label>
            <Input
              v-model="filterForm.sku"
              class="w-[200px]"
              placeholder="请输入SKU"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">产品名称</Label>
            <Input
              v-model="filterForm.name"
              class="w-[200px]"
              placeholder="请输入产品名称"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="crm-field">
            <Label class="crm-field-label">分类</Label>
            <Select v-model="filterForm.categoryId">
              <SelectTrigger class="w-[180px]">
                <SelectValue placeholder="请选择分类" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in categoryOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="crm-toolbar-actions">
            <Button type="submit" class="gap-2" :disabled="loading">
              <Search class="h-4 w-4" />
              查询
            </Button>
            <Button
              type="button"
              variant="outline"
              class="gap-2"
              :disabled="loading"
              @click="handleReset"
            >
              <RotateCcw class="h-4 w-4" />
              重置
            </Button>
          </div>
        </form>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <div v-if="loading" class="py-10 text-center text-[var(--crm-text-tertiary)]">
          加载中...
        </div>
        <div
          v-else-if="stockAlertList.length === 0"
          class="flex flex-col items-center py-10 text-[var(--crm-text-tertiary)]"
        >
          <MessageSquare class="h-10 w-10 mb-2.5" />
          <p>暂无符合条件的库存预警产品</p>
        </div>
        <Table v-else class="min-w-[1080px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead
                class="w-[60px]"
                sortable
                sort-key="index"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >序号</TableHead
              >
              <TableHead
                sortable
                sort-key="sku"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >SKU</TableHead
              >
              <TableHead
                sortable
                sort-key="name"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >产品名称</TableHead
              >
              <TableHead
                sortable
                sort-key="categoryName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >分类</TableHead
              >
              <TableHead
                sortable
                sort-key="specification"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >规格</TableHead
              >
              <TableHead
                class="w-[110px]"
                sortable
                sort-key="stock"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >当前库存</TableHead
              >
              <TableHead
                class="w-[100px]"
                sortable
                sort-key="minStock"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >最低库存</TableHead
              >
              <TableHead
                sortable
                sort-key="updateTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >最后更新时间</TableHead
              >
              <TableHead class="w-[100px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, idx) in displayStockAlertList" :key="row.id ?? idx">
              <TableCell class="text-[var(--crm-text-tertiary)]">{{ idx + 1 }}</TableCell>
              <TableCell class="max-w-[150px] truncate">
                <span
                  class="inline-flex rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 font-mono text-xs text-[var(--crm-text-secondary)]"
                >
                  {{ row.sku || '--' }}
                </span>
              </TableCell>
              <TableCell
                class="max-w-[200px] truncate font-semibold text-[var(--crm-text-primary)]"
                >{{ row.name || '--' }}</TableCell
              >
              <TableCell class="max-w-[150px] truncate">{{ row.categoryName || '--' }}</TableCell>
              <TableCell class="max-w-[150px] truncate">{{ row.specification || '--' }}</TableCell>
              <TableCell>
                <span
                  class="font-semibold"
                  :class="
                    row.stock < row.minStock
                      ? 'text-[var(--crm-warning)]'
                      : 'text-[var(--crm-text-primary)]'
                  "
                >
                  {{ row.stock }}
                </span>
              </TableCell>
              <TableCell class="text-[var(--crm-text-secondary)]">{{ row.minStock }}</TableCell>
              <TableCell>{{ formatDateTime(row.updateTime) }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-1">
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.stock.view"
                    label="详情"
                    @click="handleDetail(row)"
                  >
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    v-has-permission="PERMISSIONS.product.stock.adjust"
                    label="补货"
                    @click="handleRestock(row)"
                  >
                    <Plus class="h-4 w-4" />
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

    <!-- 补货对话框 -->
    <Dialog v-model:open="restockDialogVisible">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>补货</DialogTitle>
        </DialogHeader>
        <div v-if="currentProduct" class="space-y-2 py-2.5">
          <div class="flex">
            <span class="w-20 text-muted-foreground font-bold">产品:</span>
            <span class="flex-1">{{ currentProduct.name }}</span>
          </div>
          <div class="flex">
            <span class="w-20 text-muted-foreground font-bold">SKU:</span>
            <span class="flex-1">{{ currentProduct.sku }}</span>
          </div>
          <div class="flex">
            <span class="w-20 text-muted-foreground font-bold">当前库存:</span>
            <span
              class="flex-1"
              :class="{ 'text-red-500 font-bold': currentProduct.stock < currentProduct.minStock }"
            >
              {{ currentProduct.stock }}
            </span>
          </div>
          <div class="flex">
            <span class="w-20 text-muted-foreground font-bold">最低库存:</span>
            <span class="flex-1">{{ currentProduct.minStock }}</span>
          </div>
        </div>
        <Separator />
        <form class="space-y-4" @submit.prevent="handleRestockSubmit">
          <div class="space-y-2">
            <Label>补货数量</Label>
            <NumberField v-model="restockForm.quantity" :min="1" :step="1">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="restockForm.remark" :rows="3" placeholder="请输入补货备注" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="restockDialogVisible = false">取消</Button>
          <Button :disabled="restockLoading" @click="handleRestockSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 详情对话框 -->
    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[960px]">
        <DialogHeader>
          <DialogTitle>库存车辆与变动记录</DialogTitle>
        </DialogHeader>
        <div class="max-h-[62vh] space-y-5 overflow-y-auto pr-1">
          <div>
            <div class="mb-2 flex flex-wrap items-end justify-between gap-3">
              <div class="text-sm font-semibold text-[var(--crm-text-secondary)]">车辆实例</div>
              <form class="flex flex-wrap items-end gap-2" @submit.prevent="reloadDetail">
                <div class="crm-field">
                  <Label class="crm-field-label">VIN</Label>
                  <Input
                    v-model="vehicleFilterForm.vin"
                    class="w-[190px]"
                    placeholder="筛选VIN"
                    @keyup.enter="reloadDetail"
                  />
                </div>
                <div class="crm-field">
                  <Label class="crm-field-label">状态</Label>
                  <Select v-model="vehicleFilterForm.status">
                    <SelectTrigger class="w-[170px]">
                      <SelectValue placeholder="全部状态" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem
                        v-for="option in vehicleStatusOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <Button type="submit" size="sm" :disabled="detailLoading">
                  <Search class="h-4 w-4" />
                  筛选
                </Button>
              </form>
            </div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead class="w-[110px]">车辆ID</TableHead>
                  <TableHead>VIN</TableHead>
                  <TableHead class="w-[110px]">颜色</TableHead>
                  <TableHead class="w-[120px]">库位</TableHead>
                  <TableHead class="w-[130px]">状态</TableHead>
                  <TableHead class="w-[130px]">占用来源</TableHead>
                  <TableHead class="w-[150px]">预计释放</TableHead>
                  <TableHead class="w-[90px]">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-if="productVehicles.length === 0">
                  <TableCell colspan="8" class="py-6 text-center text-[var(--crm-text-tertiary)]">
                    暂无车辆实例
                  </TableCell>
                </TableRow>
                <TableRow v-for="vehicle in productVehicles" :key="vehicle.id">
                  <TableCell>{{ vehicle.id }}</TableCell>
                  <TableCell class="font-mono text-xs">{{ vehicle.vin }}</TableCell>
                  <TableCell>{{ vehicle.color || '--' }}</TableCell>
                  <TableCell>{{ vehicle.location || '--' }}</TableCell>
                  <TableCell>
                    <StatusBadge
                      :label="formatVehicleStatus(vehicle.status)"
                      :tone="getVehicleStatusTone(vehicle.status)"
                    />
                  </TableCell>
                  <TableCell>{{ formatSourceType(vehicle.sourceType, vehicle.sourceId) }}</TableCell>
                  <TableCell>{{ formatDateTime(vehicle.holdUntil) || '--' }}</TableCell>
                  <TableCell>
                    <RowActionButton
                      v-if="canReleaseVehicle(vehicle)"
                      v-has-permission="PERMISSIONS.product.stock.adjust"
                      label="释放占用"
                      @click="handleOpenRelease(vehicle)"
                    >
                      <RotateCcw class="h-4 w-4" />
                    </RowActionButton>
                    <span v-else class="text-[var(--crm-text-tertiary)]">--</span>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
          <div>
            <div class="mb-2 text-sm font-semibold text-[var(--crm-text-secondary)]">库存流水</div>
            <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[80px]">记录ID</TableHead>
              <TableHead class="w-[90px]">车辆ID</TableHead>
              <TableHead class="w-[100px]">变动数量</TableHead>
              <TableHead class="w-[100px]">类型</TableHead>
              <TableHead class="w-[120px]">来源</TableHead>
              <TableHead class="w-[150px]">状态变化</TableHead>
              <TableHead>备注</TableHead>
              <TableHead class="w-[180px]">时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="record in stockRecords" :key="record.id">
              <TableCell>{{ record.id }}</TableCell>
              <TableCell>{{ record.vehicleId ?? '--' }}</TableCell>
              <TableCell>{{ record.quantity }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="formatStockRecordType(record.type)"
                  :tone="getStockRecordTone(record.type)"
                />
              </TableCell>
              <TableCell>{{ formatSourceType(record.sourceType, record.sourceId) }}</TableCell>
              <TableCell>
                {{ formatVehicleStatus(record.beforeStatus) }} → {{ formatVehicleStatus(record.afterStatus) }}
              </TableCell>
              <TableCell>{{ record.remark }}</TableCell>
              <TableCell>{{ formatDateTime(record.createTime) }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="releaseDialogVisible">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>释放车辆占用</DialogTitle>
        </DialogHeader>
        <div v-if="releaseTarget" class="space-y-3 py-2">
          <div class="grid grid-cols-[96px_1fr] gap-2 text-sm">
            <span class="text-[var(--crm-text-tertiary)]">VIN</span>
            <span class="font-mono">{{ releaseTarget.vehicle.vin }}</span>
            <span class="text-[var(--crm-text-tertiary)]">当前状态</span>
            <span>{{ formatVehicleStatus(releaseTarget.vehicle.status) }}</span>
            <span class="text-[var(--crm-text-tertiary)]">占用来源</span>
            <span>
              {{
                formatSourceType(releaseTarget.reserveRecord.sourceType, releaseTarget.reserveRecord.sourceId)
              }}
            </span>
            <span class="text-[var(--crm-text-tertiary)]">原流水</span>
            <span>#{{ releaseTarget.reserveRecord.id }}</span>
          </div>
          <Separator />
          <div class="space-y-2">
            <Label>释放原因</Label>
            <Textarea v-model="releaseForm.reason" :rows="3" placeholder="请输入释放原因" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="releaseLoading" @click="releaseDialogVisible = false">
            取消
          </Button>
          <Button :disabled="releaseLoading" @click="handleReleaseSubmit">确认释放</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted } from 'vue'
import { messageTip } from '@/shared/utils/feedback'
import { Eye, MessageSquare, Plus, RefreshCw, RotateCcw, Search } from '@lucide/vue'
import {
  getStockAlerts,
  restockProduct,
  getStockRecords,
  getCategoryList,
  fetchProductVehicles,
  releaseProductVehicle,
} from '@/modules/product/api/product-api'
import type {
  StockAlert,
  StockRecord,
  RestockRequest,
  ProductVehicle,
  ProductVehicleStatus,
  ProductVehicleQuery,
  ReleaseProductVehicleRequest,
} from '@/modules/product/model/product.types'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import type { PageResult } from '@/shared/api/api-types'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { useClientSort } from '@/shared/utils/table-sort'

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
  NumberField,
  NumberFieldContent,
  NumberFieldDecrement,
  NumberFieldIncrement,
  NumberFieldInput,
} from '@/components/ui/number-field'
import { Separator } from '@/components/ui/separator'
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

interface CategoryOption {
  value: string
  label: string
}

interface VehicleStatusOption {
  value: ProductVehicleStatus | typeof ALL_VEHICLE_STATUS
  label: string
}

interface ReleaseTarget {
  vehicle: ProductVehicle
  reserveRecord: StockRecord
}

const ALL_CATEGORY_ID = '__ALL_CATEGORIES__'
const ALL_VEHICLE_STATUS = '__ALL_VEHICLE_STATUS__'
const stockAlertList = ref<StockAlert[]>([])
const {
  sortBy,
  sortDirection,
  sortedRows: displayStockAlertList,
  toggleSort,
} = useClientSort<StockAlert>(stockAlertList, {
  index: 'id',
  sku: 'sku',
  name: 'name',
  categoryName: 'categoryName',
  specification: 'specification',
  stock: 'stock',
  minStock: 'minStock',
  updateTime: 'updateTime',
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const restockLoading = ref(false)
const restockDialogVisible = ref(false)
const currentProduct = ref<StockAlert | null>(null)
const restockForm = ref<RestockRequest>({
  productId: 0,
  quantity: 1,
  remark: '',
})
const filterForm = ref({
  sku: '',
  name: '',
  categoryId: ALL_CATEGORY_ID,
})
const categoryOptions = ref<CategoryOption[]>([])
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailProduct = ref<StockAlert | null>(null)
const stockRecords = ref<StockRecord[]>([])
const productVehicles = ref<ProductVehicle[]>([])
const vehicleFilterForm = ref<{
  vin: string
  status: ProductVehicleStatus | typeof ALL_VEHICLE_STATUS
}>({
  vin: '',
  status: ALL_VEHICLE_STATUS,
})
const releaseDialogVisible = ref(false)
const releaseLoading = ref(false)
const releaseTarget = ref<ReleaseTarget | null>(null)
const releaseForm = ref<ReleaseProductVehicleRequest>({
  reserveRecordId: 0,
  reason: '',
})
const vehicleStatusOptions: VehicleStatusOption[] = [
  { value: ALL_VEHICLE_STATUS, label: '全部状态' },
  { value: 'AVAILABLE', label: '在库可售' },
  { value: 'ORDER_RESERVED', label: '订单占用' },
  { value: 'TEST_DRIVE_RESERVED', label: '试驾占用' },
  { value: 'SALES_LOCKED', label: '销售锁定' },
  { value: 'PENDING_DELIVERY', label: '待交付' },
  { value: 'OUTBOUND', label: '已出库' },
  { value: 'DELIVERED', label: '已交付' },
  { value: 'INVENTORY_EXCEPTION', label: '盘点异常' },
  { value: 'UNAVAILABLE', label: '不可售' },
]

const {
  run: runStockAlerts,
  cancel: cancelStockAlerts,
  loading,
} = useLatestRequest<PageResult<StockAlert>>()

function buildStockAlertQuery() {
  const params: Record<string, unknown> = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (filterForm.value.sku.trim()) {
    params.sku = filterForm.value.sku.trim()
  }
  if (filterForm.value.name.trim()) {
    params.name = filterForm.value.name.trim()
  }
  if (filterForm.value.categoryId !== ALL_CATEGORY_ID) {
    params.categoryId = filterForm.value.categoryId
  }
  return params
}

async function loadCategoryOptions() {
  try {
    const res = await getCategoryList({ page: 1, size: 100 })
    const options: CategoryOption[] = [{ value: ALL_CATEGORY_ID, label: '全部' }]
    const categoryList = res.list
      .filter((item) => item.id !== null && item.id !== undefined)
      .map((item) => ({
        value: String(item.id),
        label: item.name ?? '',
      }))
    categoryOptions.value = [...options, ...categoryList]
  } catch {
    categoryOptions.value = [{ value: ALL_CATEGORY_ID, label: '全部' }]
    messageTip('加载分类选项失败', 'error')
  }
}

async function loadStockAlerts() {
  const query = buildStockAlertQuery()
  const result = await runStockAlerts((signal) => getStockAlerts(query, signal))
  if (result) {
    stockAlertList.value = result.list ?? []
    total.value = result.total ?? 0
    pageSize.value = result.pageSize ?? pageSize.value
  }
}

function formatDateTime(dateTimeStr?: string) {
  if (!dateTimeStr) return ''
  return dateTimeStr.replace('T', ' ').split('.')[0] ?? ''
}

function formatStockRecordType(type?: string): string {
  const map: Record<string, string> = {
    INBOUND: '入库',
    IN: '入库',
    in: '入库',
    RESTOCK: '补货',
    restock: '补货',
    RESERVE: '占用',
    RELEASE: '释放',
    OUT: '出库',
    out: '出库',
    ADJUST: '调整',
    adjust: '调整',
  }
  return map[type ?? ''] ?? type ?? '--'
}

function getStockRecordTone(
  type?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  const label = formatStockRecordType(type)
  if (label === '入库' || label === '补货') return 'success'
  if (label === '释放') return 'info'
  if (label === '占用') return 'purple'
  if (label === '出库') return 'warning'
  if (label === '调整') return 'info'
  return 'muted'
}

function formatVehicleStatus(status?: string): string {
  const map: Record<string, string> = {
    PENDING_INBOUND: '待入库',
    AVAILABLE: '在库可售',
    TEST_DRIVE_RESERVED: '试驾占用',
    SALES_LOCKED: '销售锁定',
    ORDER_RESERVED: '订单占用',
    PENDING_DELIVERY: '待交付',
    OUTBOUND: '已出库',
    DELIVERED: '已交付',
    INVENTORY_EXCEPTION: '盘点异常',
    UNAVAILABLE: '不可售',
  }
  return map[status ?? ''] ?? status ?? '--'
}

function getVehicleStatusTone(
  status?: ProductVehicleStatus | string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'TEST_DRIVE_RESERVED' || status === 'SALES_LOCKED' || status === 'ORDER_RESERVED') {
    return 'purple'
  }
  if (status === 'INVENTORY_EXCEPTION' || status === 'UNAVAILABLE') return 'danger'
  if (status === 'OUTBOUND' || status === 'DELIVERED') return 'muted'
  return 'info'
}

function formatSourceType(sourceType?: string, sourceId?: string | number): string {
  if (!sourceType) return '--'
  const map: Record<string, string> = {
    ORDER: '订单',
    TEST_DRIVE: '试驾',
    SALES_LOCK: '销售锁定',
    INBOUND: '入库',
  }
  const label = map[sourceType] ?? sourceType
  return sourceId ? `${label} #${sourceId}` : label
}

function isSameEntity(left?: string | number, right?: string | number): boolean {
  return left !== undefined && right !== undefined && String(left) === String(right)
}

function isVehicleOccupied(status?: ProductVehicleStatus | string): boolean {
  return status === 'ORDER_RESERVED' || status === 'TEST_DRIVE_RESERVED' || status === 'SALES_LOCKED'
}

function findActiveReserveRecord(vehicle: ProductVehicle): StockRecord | undefined {
  const releasedRecordIds = new Set(
    stockRecords.value
      .filter((record) => record.type === 'RELEASE' && record.relatedRecordId !== undefined)
      .map((record) => String(record.relatedRecordId)),
  )
  return stockRecords.value.find(
    (record) =>
      record.type === 'RESERVE' &&
      isSameEntity(record.vehicleId, vehicle.id) &&
      !releasedRecordIds.has(String(record.id)),
  )
}

function canReleaseVehicle(vehicle: ProductVehicle): boolean {
  return isVehicleOccupied(vehicle.status) && Boolean(findActiveReserveRecord(vehicle))
}

function handleSearch() {
  currentPage.value = 1
  void loadStockAlerts()
}

function handleReset() {
  filterForm.value = {
    sku: '',
    name: '',
    categoryId: ALL_CATEGORY_ID,
  }
  currentPage.value = 1
  void loadStockAlerts()
}

function handleRestock(row: StockAlert) {
  currentProduct.value = row
  restockForm.value = {
    productId: row.id,
    quantity: 1,
    remark: '',
  }
  restockDialogVisible.value = true
}

async function handleRestockSubmit() {
  if (!restockForm.value.quantity || restockForm.value.quantity <= 0) {
    messageTip('补货数量必须大于0', 'warning')
    return
  }

  if (restockLoading.value) return
  restockLoading.value = true
  try {
    await restockProduct(restockForm.value)
    messageTip('补货成功', 'success')
    restockDialogVisible.value = false
    currentProduct.value = null
    restockForm.value = {
      productId: 0,
      quantity: 1,
      remark: '',
    }
    try {
      await loadStockAlerts()
    } catch {
      messageTip('补货已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip('补货失败', 'error')
  } finally {
    restockLoading.value = false
  }
}

function buildVehicleQuery(row: StockAlert): ProductVehicleQuery {
  const query: ProductVehicleQuery = {
    productId: row.id,
    page: 1,
    size: 100,
  }
  if (vehicleFilterForm.value.vin.trim()) {
    query.vin = vehicleFilterForm.value.vin.trim()
  }
  if (vehicleFilterForm.value.status !== ALL_VEHICLE_STATUS) {
    query.status = vehicleFilterForm.value.status
  }
  return query
}

async function loadInventoryDetail(row: StockAlert) {
  detailLoading.value = true
  try {
    const [recordsRes, vehiclesRes] = await Promise.all([
      getStockRecords(row.id, { page: 1, size: 100 }),
      fetchProductVehicles(buildVehicleQuery(row)),
    ])
    stockRecords.value = recordsRes.list ?? []
    productVehicles.value = vehiclesRes.list ?? []
  } catch {
    messageTip('加载库存详情失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

async function handleDetail(row: StockAlert) {
  detailProduct.value = row
  vehicleFilterForm.value = {
    vin: '',
    status: ALL_VEHICLE_STATUS,
  }
  await loadInventoryDetail(row)
  detailDialogVisible.value = true
}

function reloadDetail() {
  if (!detailProduct.value || detailLoading.value) return
  void loadInventoryDetail(detailProduct.value)
}

function handleOpenRelease(vehicle: ProductVehicle) {
  const reserveRecord = findActiveReserveRecord(vehicle)
  if (!reserveRecord) {
    messageTip('未找到可释放的原占用流水', 'warning')
    return
  }
  releaseTarget.value = {
    vehicle,
    reserveRecord,
  }
  releaseForm.value = {
    reserveRecordId: reserveRecord.id,
    reason: '',
  }
  releaseDialogVisible.value = true
}

async function handleReleaseSubmit() {
  if (!releaseTarget.value) return
  if (!releaseForm.value.reason.trim()) {
    messageTip('请输入释放原因', 'warning')
    return
  }
  if (releaseLoading.value) return

  releaseLoading.value = true
  try {
    await releaseProductVehicle(releaseTarget.value.vehicle.id, {
      reserveRecordId: releaseForm.value.reserveRecordId,
      reason: releaseForm.value.reason.trim(),
    })
    messageTip('释放成功', 'success')
    releaseDialogVisible.value = false
    releaseTarget.value = null
    if (detailProduct.value) {
      await loadInventoryDetail(detailProduct.value)
    }
    await loadStockAlerts()
  } catch {
    messageTip('释放失败', 'error')
  } finally {
    releaseLoading.value = false
  }
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  void loadStockAlerts()
}

onMounted(() => {
  void loadCategoryOptions()
  void loadStockAlerts()
})
</script>
