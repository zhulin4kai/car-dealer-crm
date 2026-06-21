<template>
  <div class="p-5">
    <!-- 搜索栏 -->
    <Card class="mb-5">
      <CardContent class="pt-6">
        <div class="flex flex-wrap items-end gap-4">
          <div class="space-y-2">
            <Label>类型代码</Label>
            <Input
              v-model="searchForm.typeCode"
              placeholder="请输入类型代码"
              @keyup.enter="handleSearch()"
            />
          </div>
          <div class="space-y-2">
            <Label>类型名称</Label>
            <Input
              v-model="searchForm.typeName"
              placeholder="请输入类型名称"
              @keyup.enter="handleSearch()"
            />
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch()">查询</Button>
            <Button v-has-permission="PERMISSIONS.dict.type.add" variant="secondary" @click="handleAdd()">新增类型</Button>
            <Button v-has-permission="PERMISSIONS.dict.type.delete" variant="destructive" @click="handleBatchDelete()" :disabled="!selectedIds.length">批量删除</Button>
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
              <TableHead class="w-[180px]">类型代码</TableHead>
              <TableHead class="w-[180px]">类型名称</TableHead>
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
              <TableCell>{{ row.typeCode }}</TableCell>
              <TableCell>{{ row.typeName }}</TableCell>
              <TableCell>{{ row.remark }}</TableCell>
              <TableCell>
                <div class="flex gap-1">
                  <Button v-has-permission="PERMISSIONS.dict.type.edit" variant="link" size="sm" @click="handleEdit(row)">编辑</Button>
                  <Button v-has-permission="PERMISSIONS.dict.type.delete" variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
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
          <DialogTitle>{{ isEdit ? '编辑字典类型' : '新增字典类型' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="onFormSubmit">
          <div class="space-y-2">
            <Label>类型代码</Label>
            <Input
              v-model="values.typeCode"
              placeholder="请输入类型代码"
              @keyup.enter="onFormSubmit"
            />
            <p v-if="errors.typeCode" class="text-sm text-destructive">{{ errors.typeCode }}</p>
          </div>
          <div class="space-y-2">
            <Label>类型名称</Label>
            <Input
              v-model="values.typeName"
              placeholder="请输入类型名称"
              @keyup.enter="onFormSubmit"
            />
            <p v-if="errors.typeName" class="text-sm text-destructive">{{ errors.typeName }}</p>
          </div>
          <div class="space-y-2">
            <Label>备注</Label>
            <Textarea
              v-model="values.remark"
              placeholder="请输入备注"
              :rows="3"
              @keyup.enter="onFormSubmit"
            />
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
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, reactive, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { getDictTypeList, createDictType, updateDictType, deleteDictType, batchDeleteDictTypes, clearCache } from '@/modules/dict/api/dict-api'
import type { DictType } from '@/modules/dict/model/dict.types'
import { messageConfirm } from '@/shared/utils/legacy-util'
import { messageTip } from '@/shared/utils/feedback'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
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

const searchForm = reactive({
  typeCode: '',
  typeName: ''
})

// 表单验证 schema
const formSchema = toTypedSchema(z.object({
  typeCode: z.string().min(1, '请输入类型代码'),
  typeName: z.string().min(1, '请输入类型名称'),
  remark: z.string().optional(),
}))

const { handleSubmit, errors, values, resetForm } = useForm({
  validationSchema: formSchema,
  initialValues: {
    typeCode: '',
    typeName: '',
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

const toggleRowSelection = (row: DictType, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(row.id)) {
      selectedIds.value.push(row.id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter((id: number | string) => id !== row.id)
  }
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
    const res = await getDictTypeList(params)
    if (true) {
      // 对数据按照id升序排序
      tableData.value = res.list.sort((a, b) => a.id - b.id)
      total.value = res.total
    }
  } catch (error) {
    console.error('获取字典类型列表失败:', error)
    messageTip('获取数据失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = async () => {
  try {
    loading.value = true
    const res = await getDictTypeList(searchForm)
    if (true) {
      tableData.value = res.list
    }
  } catch (error) {
    console.error('获取字典类型列表失败:', error)
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
      typeName: '',
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
    await messageConfirm('确认删除该字典类型吗？')
    const res = await deleteDictType(row.id)
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
  if (!selectedIds.value.length) return
  try {
    await messageConfirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`)
    const res = await batchDeleteDictTypes(selectedIds.value)
    if (true) {
      messageTip('批量删除成功', 'success')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('批量删除失败', 'error')
    }
  }
}

// 提交表单
const doSubmit = async (formData: Record<string, unknown>) => {
  try {
    // 2. 准备请求数据
    const requestData = {
      typeCode: formData.typeCode,
      typeName: formData.typeName,
      remark: formData.remark
    }

    // 3. 根据编辑状态选择API并调用
    let res
    if (isEdit.value) {
      res = await updateDictType(values.id, requestData)
    } else {
      res = await createDictType(requestData)
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
    console.error('提交字典类型失败:', error)
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
  loadData()
})
</script>
