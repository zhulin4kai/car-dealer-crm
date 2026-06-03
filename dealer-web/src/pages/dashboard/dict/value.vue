<template>
  <div class="p-5">
    <!-- 搜索栏 -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <div class="flex flex-wrap items-end gap-4">
          <div class="space-y-2">
            <Label>字典类型</Label>
            <Select v-model="searchForm.typeCode">
              <SelectTrigger class="w-[200px]">
                <SelectValue placeholder="请选择字典类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">全部</SelectItem>
                <SelectItem
                  v-for="item in dictTypes"
                  :key="item.typeCode"
                  :value="item.typeCode"
                >{{ item.typeName }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>字典值</Label>
            <Input
              v-model="searchForm.typeValue"
              placeholder="请输入字典值"
              class="w-[200px]"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch()">查询</Button>
            <Button variant="secondary" @click="handleAdd()">新增字典值</Button>
            <Button variant="destructive" @click="handleBatchDelete()" :disabled="!selectedIds.length">批量删除</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 数据表格 -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <template v-if="loading">
          <Skeleton class="h-8 w-full mb-2" v-for="i in 5" :key="i" />
        </template>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox
                  :checked="allSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead class="w-[180px]">字典类型</TableHead>
              <TableHead class="w-[180px]">字典值</TableHead>
              <TableHead class="w-[100px]">排序</TableHead>
              <TableHead>备注</TableHead>
              <TableHead class="w-[200px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, idx) in tableData" :key="row.id">
              <TableCell>
                <Checkbox
                  :checked="selectedIds.includes(row.id)"
                  @update:checked="(v: boolean) => toggleRowSelection(row, v)"
                />
              </TableCell>
              <TableCell>{{ startIndex(idx) }}</TableCell>
              <TableCell>{{ getDictTypeName(row.typeCode) }}</TableCell>
              <TableCell>{{ row.typeValue }}</TableCell>
              <TableCell>{{ row.order }}</TableCell>
              <TableCell>{{ row.remark }}</TableCell>
              <TableCell>
                <div class="flex gap-1">
                  <Button variant="link" size="sm" @click="handleEdit(row)">编辑</Button>
                  <Button variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- 分页 -->
    <DataTablePagination :page-size="pageSize" :total="total" @change="handleCurrentChange" />

    <!-- 新增/编辑弹窗 -->
    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{{ isEdit ? '编辑字典值' : '新增字典值' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="onFormSubmit">
          <div class="space-y-2">
            <Label>字典类型</Label>
            <Select v-model="values.typeCode">
              <SelectTrigger>
                <SelectValue placeholder="请选择字典类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in dictTypes"
                  :key="item.typeCode"
                  :value="item.typeCode"
                >{{ item.typeName }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.typeCode" class="text-sm text-destructive">{{ errors.typeCode }}</p>
          </div>
          <div class="space-y-2">
            <Label>字典值</Label>
            <Input v-model="values.typeValue" placeholder="请输入字典值" />
            <p v-if="errors.typeValue" class="text-sm text-destructive">{{ errors.typeValue }}</p>
          </div>
          <div class="space-y-2">
            <Label>排序</Label>
            <NumberField v-model="values.order" :min="1">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
            <p v-if="errors.order" class="text-sm text-destructive">{{ errors.order }}</p>
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea v-model="values.remark" placeholder="请输入备注" :rows="3" />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="onFormSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import {
  getDictValueList,
  createDictValue,
  updateDictValue,
  deleteDictValue,
  batchDeleteDictValues,
  clearCache,
  getDictTypeList
} from '@/modules/dict/api/dict-api'
import type { DictValue } from '@/modules/dict/model/dict.types'
import { messageConfirm } from '@/shared/utils/legacy-util'
import { messageTip } from '@/shared/utils/feedback'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { NumberField, NumberFieldContent, NumberFieldDecrement, NumberFieldIncrement, NumberFieldInput } from '@/components/ui/number-field'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dictTypes = ref([])

const searchForm = reactive({
  typeCode: '',
  typeValue: ''
})

// 表单验证 schema
const formSchema = toTypedSchema(z.object({
  typeCode: z.string().min(1, '请选择字典类型'),
  typeValue: z.string().min(1, '请输入字典值'),
  order: z.number({ required_error: '请输入排序' }).min(1, '排序最小为1'),
  remark: z.string().optional(),
}))

const { handleSubmit, errors, values, resetForm } = useForm({
  validationSchema: formSchema,
  initialValues: {
    typeCode: '',
    typeValue: '',
    order: 1,
    remark: '',
  },
})

// 全选相关
const allSelected = computed(() =>
  tableData.value.length > 0 && selectedIds.value.length === tableData.value.length
)

const toggleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedIds.value = tableData.value.map(data => data.id)
  } else {
    selectedIds.value = []
  }
}

const toggleRowSelection = (row: DictValue, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(row.id)) {
      selectedIds.value.push(row.id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter((id: number | string) => id !== row.id)
  }
}

// 加载字典类型
const loadDictTypes = async () => {
  try {
    const res = await getDictTypeList({ page: 1, size: 100 })
    if (true) {
      dictTypes.value = res.list
    }
  } catch (error) {
    console.error('获取字典类型列表失败:', error)
  }
}

// 获取字典类型名称
const getDictTypeName = (typeCode) => {
  const type = dictTypes.value.find(item => item.typeCode === typeCode)
  return type ? type.typeName : typeCode
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    const res = await getDictValueList(params)
    if (true) {
      // 对数据按照id升序排序
      tableData.value = res.list.sort((a, b) => a.id - b.id)
      total.value = res.total
    }
  } catch (error) {
    console.error('获取字典值列表失败:', error)
    messageTip('获取数据失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = async () => {
  try {
    loading.value = true
    const res = await getDictValueList(searchForm)
    if (true) {
      tableData.value = res.list
    }
  } catch (error) {
    console.error('获取字典值列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm({
    values: {
      typeCode: '',
      typeValue: '',
      order: 1,
      remark: '',
    },
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(values, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该字典值吗？')
    const res = await deleteDictValue(row.id)
    if (true) {
      messageTip('删除成功', 'success')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('删除失败', 'error')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请至少选择一条记录', 'warning');
    return;
  }

  try {
    await messageConfirm('确定要删除选中的字典值吗?');

    const res = await batchDeleteDictValues(selectedIds.value);
    if (true) {
      messageTip('批量删除成功', 'success');
      loadData();
    } else {
      loadData();
    }
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('请求失败，请检查网络或重试', 'error');
    } else {
      messageTip('已取消删除', 'info');
    }
  }
}

// 提交表单
const doSubmit = async (formData: Record<string, unknown>) => {
  try {
    // 2. 准备请求数据
    const requestData = {
      typeCode: formData.typeCode,
      typeValue: formData.typeValue,
      order: formData.order,
      remark: formData.remark
    }

    // 3. 根据编辑状态选择API并调用
    let res
    if (isEdit.value) {
      res = await updateDictValue(values.id, requestData)
    } else {
      console.log(requestData)
      res = await createDictValue(requestData)
    }
    console.log(res)

    // 4. 处理响应结果
    if (true) {
      messageTip(isEdit.value ? '更新成功' : '创建成功', 'success')
      dialogVisible.value = false
      loadData()
    } else {
      messageTip('请求失败' || (isEdit.value ? '更新失败' : '创建失败'), 'error')
    }
  } catch (error) {
    console.error('提交字典值失败:', error)
    messageTip(isEdit.value ? '更新失败' : '创建失败', 'error')
  }
}

const onFormSubmit = handleSubmit(doSubmit)

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 分页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadData()
}

// 计算序号起始值
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  loadDictTypes()
  loadData()
})
</script>
