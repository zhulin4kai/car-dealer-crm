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
                <SelectItem :value="ALL_DICT_TYPE_CODE">全部</SelectItem>
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
            <Button v-has-permission="PERMISSIONS.dict.value.add" variant="secondary" @click="handleAdd()">新增字典值</Button>
            <Button v-has-permission="PERMISSIONS.dict.value.delete" variant="destructive" @click="handleBatchDelete()" :disabled="!selectedIds.length">批量删除</Button>
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
              <TableHead class="w-[180px]">业务编码</TableHead>
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
              <TableCell>{{ row.valueCode }}</TableCell>
              <TableCell>{{ row.order }}</TableCell>
              <TableCell>{{ row.remark }}</TableCell>
              <TableCell>
                <div class="flex gap-1">
                  <Button v-has-permission="PERMISSIONS.dict.value.edit" variant="link" size="sm" @click="handleEdit(row)">编辑</Button>
                  <Button v-has-permission="PERMISSIONS.dict.value.delete" variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
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
            <Label>业务编码</Label>
            <Input v-model="values.valueCode" placeholder="例如：wechat" />
            <p v-if="errors.valueCode" class="text-sm text-destructive">{{ errors.valueCode }}</p>
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
          <Button variant="outline" @click="dialogVisible = false" :disabled="submitting">取消</Button>
          <Button @click="onFormSubmit" :disabled="submitting">{{ submitting ? '提交中...' : '确定' }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
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
  getDictTypeList
} from '@/modules/dict/api/dict-api'
import type { DictQuery, DictValue, DictType } from '@/modules/dict/model/dict.types'
import type { PageResult } from '@/shared/api/api-types'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import type { EntityId } from '@/shared/types/id'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
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

const { run: runDictValueQuery, loading } = useLatestRequest<PageResult<DictValue>>()
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref<DictValue[]>([])
const selectedIds = ref<EntityId[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dictTypes = ref<DictType[]>([])
const ALL_DICT_TYPE_CODE = '__ALL_DICT_TYPES__'

const searchForm = reactive({
  typeCode: ALL_DICT_TYPE_CODE,
  typeValue: ''
})

// 表单验证 schema
const formSchema = toTypedSchema(z.object({
  typeCode: z.string().min(1, '请选择字典类型'),
  typeValue: z.string().min(1, '请输入字典值'),
  valueCode: z.string().min(1, '请输入业务编码').regex(/^[a-z][a-z0-9_]*$/, '业务编码只能使用小写字母、数字和下划线'),
  order: z.number({ required_error: '请输入排序' }).min(1, '排序最小为1'),
  remark: z.string().optional(),
}))

const { handleSubmit, errors, values, resetForm } = useForm({
  validationSchema: formSchema,
  initialValues: {
    typeCode: '',
    typeValue: '',
    valueCode: '',
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
    selectedIds.value = tableData.value.map(data => data.id).filter((id): id is EntityId => id !== undefined)
  } else {
    selectedIds.value = []
  }
}

const toggleRowSelection = (row: DictValue, checked: boolean) => {
  if (checked && row.id !== undefined) {
    if (!selectedIds.value.includes(row.id)) {
      selectedIds.value.push(row.id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter((id: EntityId) => id !== row.id)
  }
}

const loadDictTypes = async () => {
  try {
    const res = await getDictTypeList({ page: 1, size: 100 })
    dictTypes.value = res.list
  } catch {
    messageTip('加载字典类型选项失败', 'error')
  }
}

const getDictTypeName = (typeCode?: string) => {
  const type = dictTypes.value.find(item => item.typeCode === typeCode)
  return type?.typeName ?? typeCode ?? ''
}

function buildDictValueQuery(): DictQuery {
  const params: DictQuery = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (searchForm.typeCode !== ALL_DICT_TYPE_CODE && searchForm.typeCode.trim()) {
    params.typeCode = searchForm.typeCode.trim()
  }
  if (searchForm.typeValue.trim()) {
    params.typeValue = searchForm.typeValue.trim()
  }
  return params
}

const loadData = async () => {
  try {
    const res = await runDictValueQuery(signal => getDictValueList(buildDictValueQuery(), signal))
    if (!res) return
    tableData.value = [...res.list].sort((a, b) => Number(a.id ?? 0) - Number(b.id ?? 0))
    total.value = res.total
    selectedIds.value = []
  } catch {
    messageTip('获取数据失败', 'error')
  }
}

function handleSearch() {
  currentPage.value = 1
  void loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm({
    values: {
      typeCode: '',
      typeValue: '',
      valueCode: '',
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
const handleDelete = async (row: DictValue) => {
  try {
    await messageConfirm('确认删除该字典值吗？')
  } catch {
    messageTip('取消删除', 'warning')
    return
  }
  try {
    await deleteDictValue(row.id!)
    messageTip('删除成功', 'success')
    await loadData()
  } catch {
    messageTip('删除失败', 'error')
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请至少选择一条记录', 'warning')
    return
  }
  try {
    await messageConfirm('确定要删除选中的字典值吗?')
  } catch {
    messageTip('取消批量删除', 'warning')
    return
  }
  try {
    await batchDeleteDictValues(selectedIds.value)
    messageTip('批量删除成功', 'success')
    await loadData()
  } catch {
    messageTip('批量删除失败', 'error')
  }
}

const doSubmit = async (formData: Record<string, unknown>) => {
  if (submitting.value) return
  submitting.value = true
  try {
    const requestData = {
      typeCode: formData.typeCode,
      typeValue: formData.typeValue,
      valueCode: formData.valueCode,
      order: formData.order,
      remark: formData.remark,
    }
    if (isEdit.value) {
      await updateDictValue(values.id, requestData)
      messageTip('更新成功', 'success')
    } else {
      await createDictValue(requestData)
      messageTip('创建成功', 'success')
    }
    dialogVisible.value = false
    try {
      await loadData()
    } catch {
      messageTip('操作已成功，但列表刷新失败', 'warning')
    }
  } catch {
    messageTip(isEdit.value ? '更新失败' : '创建失败', 'error')
  } finally {
    submitting.value = false
  }
}

const onFormSubmit = handleSubmit(doSubmit)

function handleCurrentChange(val: number) {
  currentPage.value = val
  void loadData()
}

function startIndex(index: number) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

onMounted(() => {
  void loadDictTypes()
  void loadData()
})
</script>
