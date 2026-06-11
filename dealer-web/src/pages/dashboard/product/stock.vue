<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button variant="outline" @click="goBack">返 回</Button>
        <Button variant="outline" :disabled="loading" @click="loadStockAlerts">
          <RefreshCw class="h-4 w-4 mr-1.5" /> 刷新数据
        </Button>
      </CardContent>
    </Card>

    <Card class="bg-muted/50">
      <CardContent class="pt-6">
        <form class="flex flex-wrap items-end gap-4" @submit.prevent="handleSearch">
          <div class="space-y-2">
            <Label>SKU</Label>
            <Input
              v-model="filterForm.sku"
              placeholder="请输入SKU"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="space-y-2">
            <Label>产品名称</Label>
            <Input
              v-model="filterForm.name"
              placeholder="请输入产品名称"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="space-y-2">
            <Label>分类</Label>
            <Select v-model="filterForm.categoryId">
              <SelectTrigger class="w-[180px]">
                <SelectValue placeholder="请选择分类" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex gap-2">
            <Button type="submit" :disabled="loading">查询</Button>
            <Button type="button" variant="outline" :disabled="loading" @click="handleReset">重置</Button>
          </div>
        </form>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <div v-if="loading" class="py-10 text-center text-muted-foreground">加载中...</div>
        <div v-else-if="stockAlertList.length === 0" class="py-10 flex flex-col items-center text-muted-foreground">
          <MessageSquare class="h-10 w-10 mb-2.5" />
          <p>暂无符合条件的库存预警产品</p>
        </div>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[60px]">序号</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>产品名称</TableHead>
              <TableHead>分类</TableHead>
              <TableHead>规格</TableHead>
              <TableHead>当前库存</TableHead>
              <TableHead>最低库存</TableHead>
              <TableHead>最后更新时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, idx) in stockAlertList" :key="row.id ?? idx">
              <TableCell>{{ idx + 1 }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.sku }}</TableCell>
              <TableCell class="truncate max-w-[200px]">{{ row.name }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.categoryName }}</TableCell>
              <TableCell class="truncate max-w-[150px]">{{ row.specification }}</TableCell>
              <TableCell>
                <span :class="{ 'text-red-500 font-bold px-2 py-0.5 rounded bg-red-500/10': row.stock < row.minStock }">
                  {{ row.stock }}
                </span>
              </TableCell>
              <TableCell>{{ row.minStock }}</TableCell>
              <TableCell>{{ formatDateTime(row.updateTime) }}</TableCell>
              <TableCell class="flex gap-2">
                <Button variant="outline" size="sm" @click="handleDetail(row)">详情</Button>
                <Button size="sm" @click="handleRestock(row)">补货</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <DataTablePagination
      :page-size="pageSize"
      :total="total"
      @change="handleCurrentChange"
    />

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
      <DialogContent class="sm:max-w-[640px]">
        <DialogHeader>
          <DialogTitle>库存变动记录</DialogTitle>
        </DialogHeader>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[80px]">记录ID</TableHead>
              <TableHead class="w-[100px]">变动数量</TableHead>
              <TableHead class="w-[100px]">类型</TableHead>
              <TableHead>备注</TableHead>
              <TableHead class="w-[180px]">时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="record in stockRecords" :key="record.id">
              <TableCell>{{ record.id }}</TableCell>
              <TableCell>{{ record.quantity }}</TableCell>
              <TableCell>{{ record.type }}</TableCell>
              <TableCell>{{ record.remark }}</TableCell>
              <TableCell>{{ formatDateTime(record.createTime) }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { messageTip } from '@/shared/utils/legacy-util'
import { useRouter } from 'vue-router'
import { MessageSquare, RefreshCw } from '@lucide/vue'
import {
  getStockAlerts,
  restockProduct,
  getStockRecords,
  getCategoryList
} from '@/modules/product/api/product-api'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
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

const router = useRouter()
const stockAlertList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const restockLoading = ref(false)
const restockDialogVisible = ref(false)
const currentProduct = ref(null)
const restockForm = ref({
  productId: null,
  quantity: 1,
  remark: ''
})
const filterForm = ref({
  sku: '',
  name: '',
    categoryId: null as number | null,
})
const categoryOptions = ref([])
const detailDialogVisible = ref(false)
const stockRecords = ref([])

// 加载分类选项
const loadCategoryOptions = async () => {
  try {
    const res = await getCategoryList({
      page: 1,
      size: 100
    })

    // 添加"全部"选项
    const options = [{ value: '', label: '全部' }]

    if (res.data && res && res.list) {
      const categoryList = res.list.map(item => ({
        value: item.id,
        label: item.name
      }))
      categoryOptions.value = [...options, ...categoryList]
    } else {
      categoryOptions.value = options
    }
  } catch (error) {
    categoryOptions.value = [{ value: '', label: '全部' }]
    messageTip('加载分类选项失败', 'error')
  }
}

// 加载库存预警列表
const loadStockAlerts = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    // 添加筛选条件
    if (filterForm.value.sku && filterForm.value.sku.trim() !== '') {
      params.sku = filterForm.value.sku.trim()
    }
    if (filterForm.value.name && filterForm.value.name.trim() !== '') {
      params.name = filterForm.value.name.trim()
    }
    if (filterForm.value.categoryId) {
      params.categoryId = filterForm.value.categoryId
    }

    const res = await getStockAlerts(params)

    if (res.data && res) {
      stockAlertList.value = res.list || []
      total.value = res.total || 0
    }
  } catch (error) {
    messageTip('加载库存预警失败', 'error')
  } finally {
    loading.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const date = new Date(dateTimeStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  loadStockAlerts()
}

// 处理重置
const handleReset = () => {
  filterForm.value = {
    sku: '',
    name: '',
  categoryId: null as number | null,
  }
  currentPage.value = 1
  loadStockAlerts()
}

// 处理补货
const handleRestock = (row) => {
  currentProduct.value = row
  restockForm.value = {
    productId: row.id,
    quantity: 1,
    remark: ''
  }
  restockDialogVisible.value = true
}

// 处理补货提交
const handleRestockSubmit = async () => {
  if (!restockForm.value.quantity || restockForm.value.quantity <= 0) {
    messageTip('补货数量必须大于0', 'warning')
    return
  }

  restockLoading.value = true
  try {
    await restockProduct(restockForm.value)
    messageTip('补货成功', 'success')
    restockDialogVisible.value = false
    loadStockAlerts()
  } catch (error) {
    messageTip('补货失败', 'error')
  } finally {
    restockLoading.value = false
  }
}

// 处理详情
const handleDetail = async (row) => {
  try {
    const res = await getStockRecords(row.id, { page: 1, size: 100 })
    stockRecords.value = res.list || []
    detailDialogVisible.value = true
  } catch (error) {
    messageTip('加载库存变动记录失败', 'error')
  }
}

// 处理分页
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadStockAlerts()
}

const goBack = () => {
  window.history.length > 1 ? window.history.back() : window.location.href = '/dashboard/product'
}

onMounted(() => {
  loadCategoryOptions()
  loadStockAlerts()
})
</script>
