<template>
  <Card class="mb-5">
    <CardContent>
      <div class="flex gap-2">
        <Button @click="addClue" v-hasPermission="'clue:add'">录入线索</Button>
        <Button variant="secondary" @click="importExcel" v-hasPermission="'clue:import'">导入线索(Excel)</Button>
        <Button variant="destructive" @click="handleBatchDelete" v-hasPermission="'clue:delete'">批量删除</Button>
      </div>
    </CardContent>
  </Card>

  <Card class="mb-5">
    <CardContent>
      <div class="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox
                  :checked="isAllSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="w-[60px] text-center">序号</TableHead>
              <TableHead class="w-[90px]">负责人</TableHead>
              <TableHead class="min-w-[120px] text-center">所属活动</TableHead>
              <TableHead class="w-[90px]">姓名</TableHead>
              <TableHead class="w-[70px]">称呼</TableHead>
              <TableHead class="w-[120px]">手机</TableHead>
              <TableHead class="w-[110px]">微信</TableHead>
              <TableHead class="w-[90px]">是否贷款</TableHead>
              <TableHead class="w-[90px]">意向状态</TableHead>
              <TableHead class="min-w-[120px] text-center">意向产品</TableHead>
              <TableHead class="w-[90px] text-center">线索状态</TableHead>
              <TableHead class="w-[90px] text-center">线索来源</TableHead>
              <TableHead class="min-w-[150px] text-center">下次联系时间</TableHead>
              <TableHead class="w-[240px] text-center">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(row, index) in clueList" :key="row.id">
              <TableCell>
                <Checkbox
                  :checked="selectedIds.includes(row.id)"
                  @update:checked="(checked: boolean) => toggleSelection(row.id, checked)"
                />
              </TableCell>
              <TableCell class="text-center">{{ (currentPage - 1) * pageSize + index + 1 }}</TableCell>
              <TableCell class="truncate max-w-[90px]">{{ row.ownerDO?.name }}</TableCell>
              <TableCell class="text-center truncate max-w-[120px]">{{ row.activityDO?.name }}</TableCell>
              <TableCell class="truncate max-w-[90px]">
                <a href="javascript:" @click="view(row.id)" class="text-primary hover:underline cursor-pointer">{{ row.fullName }}</a>
              </TableCell>
              <TableCell class="truncate max-w-[70px]">{{ row.appellationDO?.typeValue }}</TableCell>
              <TableCell class="truncate max-w-[120px]">{{ row.phone }}</TableCell>
              <TableCell class="truncate max-w-[110px]">{{ row.weixin }}</TableCell>
              <TableCell class="truncate max-w-[90px]">{{ row.needLoanDO?.typeValue }}</TableCell>
              <TableCell class="truncate max-w-[90px]">{{ row.intentionStateDO?.typeValue }}</TableCell>
              <TableCell class="text-center truncate max-w-[120px]">{{ row.intentionProductDO?.name }}</TableCell>
              <TableCell class="text-center">
                <Badge :variant="row.state === -1 ? 'secondary' : 'outline'">
                  {{ row.stateDO?.typeValue }}
                </Badge>
              </TableCell>
              <TableCell class="text-center truncate max-w-[90px]">{{ row.sourceDO?.typeValue }}</TableCell>
              <TableCell class="text-center truncate max-w-[150px]">{{ row.nextContactTime }}</TableCell>
              <TableCell class="text-center">
                <div class="flex gap-1 justify-center flex-wrap">
                  <Button size="sm" @click="view(row.id)" v-hasPermission="'clue:view'">详情</Button>
                  <Button size="sm" variant="secondary" @click="edit(row.id)" v-hasPermission="'clue:edit'">编辑</Button>
                  <Button size="sm" variant="destructive" @click="del(row.id)" v-hasPermission="'clue:delete'">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </CardContent>
  </Card>

  <DataTablePagination :page-size="pageSize" :total="total" @change="toPage" />

  <!-- 导入线索Excel弹窗 -->
  <Dialog v-model:open="importExcelDialogVisible">
    <DialogContent class="sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>导入线索Excel</DialogTitle>
      </DialogHeader>

      <div class="space-y-4">
        <div class="flex items-center gap-3">
          <input
            ref="fileInputRef"
            type="file"
            accept=".xlsx,.xls"
            class="hidden"
            @change="handleFileChange"
          />
          <Button variant="outline" @click="fileInputRef?.click()">选择Excel文件</Button>
          <span class="text-sm text-muted-foreground">仅支持后缀名为.xls或.xlsx的文件</span>
        </div>

        <div class="pt-2 text-sm">
          <p class="font-semibold mb-1">重要提示：</p>
          <ul class="list-disc pl-5 space-y-1 text-muted-foreground">
            <li>上传仅支持后缀名为.xls或.xlsx的文件；</li>
            <li>给定Excel文件的第一行将视为字段名；</li>
            <li>请确认您的文件大小不超过50MB；</li>
            <li>日期值以文本形式保存，必须符合yyyy-MM-dd格式；</li>
            <li>日期时间以文本形式保存，必须符合yyyy-MM-dd HH:mm:ss的格式；</li>
          </ul>
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" @click="importExcelDialogVisible = false">关 闭</Button>
        <Button @click="submitExcel">导 入</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 线索录入/编辑弹窗 -->
  <Dialog v-model:open="clueDialogVisible" @update:open="(open: boolean) => { if (!open) handleDialogClose() }">
    <DialogContent class="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>{{ dialogTitle }}</DialogTitle>
      </DialogHeader>

      <form @submit.prevent="onSubmitClue" class="space-y-4">
        <div class="space-y-2">
          <Label>负责人</Label>
          <Select v-model="values.ownerId" disabled>
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择负责人" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in ownerOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>所属活动</Label>
          <Select v-model="values.activityId">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择所属活动" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in activityOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>姓名</Label>
          <Input v-model="values.fullName" />
          <p v-if="errors.fullName" class="text-sm text-destructive">{{ errors.fullName }}</p>
        </div>

        <div class="space-y-2">
          <Label>称呼</Label>
          <Select v-model="values.appellation">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择称呼" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in appellationOptions" :key="item.id" :value="item.id">
                {{ item.typeValue }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 编辑模式：手机禁用 -->
        <div class="space-y-2" v-if="clueQuery.id > 0">
          <Label>手机</Label>
          <Input :model-value="clueQuery.phone" disabled />
        </div>
        <!-- 录入模式：手机可编辑 -->
        <div class="space-y-2" v-else>
          <Label>手机</Label>
          <Input v-model="values.phone" />
          <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
        </div>

        <div class="space-y-2">
          <Label>微信</Label>
          <Input v-model="values.weixin" />
        </div>

        <div class="space-y-2">
          <Label>QQ</Label>
          <Input v-model="values.qq" />
          <p v-if="errors.qq" class="text-sm text-destructive">{{ errors.qq }}</p>
        </div>

        <div class="space-y-2">
          <Label>邮箱</Label>
          <Input v-model="values.email" />
          <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
        </div>

        <div class="space-y-2">
          <Label>年龄</Label>
          <Input v-model="values.age" />
          <p v-if="errors.age" class="text-sm text-destructive">{{ errors.age }}</p>
        </div>

        <div class="space-y-2">
          <Label>职业</Label>
          <Input v-model="values.job" />
        </div>

        <div class="space-y-2">
          <Label>年收入</Label>
          <Input v-model="values.yearIncome" />
          <p v-if="errors.yearIncome" class="text-sm text-destructive">{{ errors.yearIncome }}</p>
        </div>

        <div class="space-y-2">
          <Label>住址</Label>
          <Input v-model="values.address" />
        </div>

        <div class="space-y-2">
          <Label>贷款</Label>
          <Select v-model="values.needLoan">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择是否需要贷款" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in needLoanOptions" :key="item.id" :value="item.id">
                {{ item.typeValue }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>意向状态</Label>
          <Select v-model="values.intentionState">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择意向状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in intentionStateOptions" :key="item.id" :value="item.id">
                {{ item.typeValue }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>意向产品</Label>
          <Select v-model="values.intentionProduct">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择意向产品" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in productOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>线索状态</Label>
          <Select v-model="values.state">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择线索状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in clueStateOptions" :key="item.id" :value="item.id">
                {{ item.typeValue }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>线索来源</Label>
          <Select v-model="values.source">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择线索来源" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in sourceOptions" :key="item.id" :value="item.id">
                {{ item.typeValue }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>线索描述</Label>
          <Textarea v-model="values.description" :rows="5" placeholder="请输入线索描述" />
          <p v-if="errors.description" class="text-sm text-destructive">{{ errors.description }}</p>
        </div>

        <div class="space-y-2">
          <Label>下次联系时间</Label>
          <Input type="datetime-local" v-model="values.nextContactTime" class="w-full" />
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" @click="clueDialogVisible = false">取 消</Button>
          <Button type="submit">提 交</Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { messageConfirm, messageTip } from '@/shared/utils/legacy-util'
import {
  batchDeleteCluesByIds,
  getCurrentClues,
  importExcelAPI,
  delClueById,
  checkPhoneIsExist,
  getLoginInfo,
  getClueDetail,
  addClue as addClueAPI,
  updateClue
} from '@/modules/clue/api/clue-api'
import { getOwnerList } from '@/modules/activity/api/activity-api'
import { getDictValueList } from '@/modules/dict/api/dict-api'
import router from '@/router'
import { getProductList } from '@/modules/product/api/product-api'
import { getActivityList } from '@/modules/activity/api/activity-api'
import type { Activity } from '@/modules/activity/model/activity.types'
import type { Clue } from '@/modules/clue/model/clue.types'
import type { DictValue } from '@/modules/dict/model/dict.types'
import type { Product } from '@/modules/product/model/product.types'
import type { User } from '@/modules/user/model/user.types'
import type { PageResult } from '@/shared/api/api-types'

import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

// 原有的线索列表相关数据 (严禁修改)
const clueList = ref([{
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {}
}])

const pageSize = ref(10)
const total = ref(0)
const importExcelDialogVisible = ref(false)
const currentPage = ref(1)
const selectedIds = ref<(number | string)[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
let selectedFile: File | null = null

// 线索录入/编辑相关数据
const clueDialogVisible = ref(false)
const dialogTitle = ref('录入线索')
const clueQuery = reactive<Record<string, unknown>>({})

// 加载动态数据
const activityOptions = ref<Activity[]>([{}])
const productOptions = ref<Product[]>([{}])
// 加载字典数据
const ownerOptions = ref<User[]>([{}])
const appellationOptions = ref<DictValue[]>([{}])
const needLoanOptions = ref<DictValue[]>([{}])
const intentionStateOptions = ref<DictValue[]>([{}])
const clueStateOptions = ref<DictValue[]>([{}])
const sourceOptions = ref<DictValue[]>([{}])

// 线索表单校验规则 (zod，含 checkPhone 自定义验证器 → zod refine)
const clueSchema = toTypedSchema(z.object({
  phone: z.string()
    .min(1, '请输入手机号码')
    .refine(v => /^1[3-9]\d{9}$/.test(v), { message: '手机号码格式有误' })
    .refine(async (v) => {
      if (!v) return true
      // 如果是编辑模式且手机号未变化，跳过验证
      if (clueQuery.id > 0 && clueQuery.phone === v) return true
      try {
        await checkPhoneIsExist(v)
      } catch { /* ignore */ }
      // 原代码 if(false) 条件永远不成立，始终验证通过
      return true
    }, { message: '该手机号录入过了，不能再录入' }),
  fullName: z.string()
    .refine(v => !v || v.length >= 2, { message: '姓名至少2个汉字' })
    .refine(v => !v || /^[\u4e00-\u9fa5]+$/.test(v), { message: '姓名必须为中文汉字' }),
  qq: z.string()
    .refine(v => !v || v.length >= 5, { message: 'QQ号至少为5位' })
    .refine(v => !v || /^\d+$/.test(v), { message: 'QQ号码必须为数字' }),
  email: z.string()
    .refine(v => !v || /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(v), { message: '邮箱格式有误' }),
  age: z.string()
    .refine(v => !v || /^\d+$/.test(v), { message: '年龄必须为数字' }),
  yearIncome: z.string()
    .refine(v => !v || /^[0-9]+(\.[0-9]{2})?$/.test(v), { message: '年收入必须是整数或者两位小数' }),
  description: z.string()
    .refine(v => !v || (v.length >= 5 && v.length <= 255), { message: '线索描述长度为5-255个字符' }),
  ownerId: z.string().optional(),
  activityId: z.string().optional(),
  appellation: z.string().optional(),
  weixin: z.string().optional(),
  job: z.string().optional(),
  address: z.string().optional(),
  needLoan: z.string().optional(),
  intentionState: z.string().optional(),
  intentionProduct: z.string().optional(),
  state: z.string().optional(),
  source: z.string().optional(),
  nextContactTime: z.string().optional(),
}))

const { handleSubmit, errors, values, resetForm, setValues } = useForm({
  validationSchema: clueSchema,
  initialValues: {
    phone: '',
    fullName: '',
    qq: '',
    email: '',
    age: '',
    yearIncome: '',
    description: '',
    ownerId: '',
    activityId: '',
    appellation: '',
    weixin: '',
    job: '',
    address: '',
    needLoan: '',
    intentionState: '',
    intentionProduct: '',
    state: '',
    source: '',
    nextContactTime: '',
  },
})

// 同步 vee-validate values 到 clueQuery（保持原有 addClueSubmit 逻辑不变）
watch(values, (newValues) => {
  Object.keys(clueQuery).forEach(key => delete clueQuery[key])
  Object.assign(clueQuery, newValues)
}, { deep: true })

// 全选计算
const isAllSelected = computed(() =>
  clueList.value.length > 0 && selectedIds.value.length === clueList.value.length
)

const toggleSelectAll = (checked: boolean) => {
  selectedIds.value = checked ? clueList.value.map((item: Clue) => item.id) : []
}

const toggleSelection = (id: number | string, checked: boolean) => {
  if (checked) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((sid: number | string) => sid !== id)
  }
}

// 计算属性 (严禁修改)
const startIndex = (index: number) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 原有的方法 (严禁修改 getData)
const getData = (current: number) => {
  getCurrentClues(current).then((resp: PageResult<Clue>) => {
    if (true) {
      clueList.value = resp.list
      pageSize.value = resp.pageSize
      total.value = resp.total
    }
  })
  currentPage.value = current
}

const toPage = (current: number) => {
  getData(current)
}

const importExcel = () => {
  importExcelDialogVisible.value = true
}

// 文件选择处理（替代 el-upload）
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile = target.files[0]
  }
}

// 上传文件 (严禁修改 API 调用)
const uploadFile = () => {
  if (!selectedFile) return
  let formData = new FormData()
  formData.append('file', selectedFile)
  importExcelAPI(formData).then((resp: unknown) => {
    if (true) {
      messageTip("导入成功", "success")
      // 清除已上传的文件
      if (fileInputRef.value) {
        fileInputRef.value.value = ''
      }
      selectedFile = null
      // 重新加载页面
      getData(currentPage.value)
      importExcelDialogVisible.value = false
    } else {
      messageTip("导入失败", "error")
    }
  })
}

const handleSelectionChange = (selection: Clue[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 批量删除（替换 ElMessage/ElMessageBox 直接调用）
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请至少选择一条记录', 'warning')
    return
  }

  try {
    await messageConfirm('确定要删除选中的线索吗?')

    const res = await batchDeleteCluesByIds(selectedIds.value)
    if (true) {
      messageTip('批量删除成功', 'success')
      getData(currentPage.value)
    } else {
      messageTip('批量删除失败', 'error')
      getData(currentPage.value)
    }
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      messageTip('请求失败，请检查网络或重试', 'error')
    } else {
      messageTip('已取消删除', 'info')
    }
  }
}

const submitExcel = () => {
  uploadFile()
}

const view = (id: number | string) => {
  router.push("/dashboard/clue/detail/" + id)
}

// del 使用 messageTip/messageConfirm 间接调用，无需修改
const del = (id: number | string) => {
  messageConfirm("您确定要删除该数据吗？").then(() => {
    delClueById(id).then((resp: unknown) => {
      if (true) {
        messageTip("删除成功", "success")
        getData(currentPage.value)
      } else {
        messageTip("删除失败，原因：" + '请求失败', "error")
      }
    })
  }).catch(() => {
    messageTip("取消删除", "warning")
  })
}

// 新增的线索录入/编辑相关方法
const addClue = async () => {
  dialogTitle.value = '录入线索'
  resetClueForm()
  await loadData()
  loadOwner()
  loadLoginUser()
  clueDialogVisible.value = true
}

const edit = async (id: number | string) => {
  dialogTitle.value = '编辑线索'
  resetClueForm()
  await loadData()
  loadOwner()
  loadClue(id)
  clueDialogVisible.value = true
}

const resetClueForm = () => {
  Object.keys(clueQuery).forEach(key => {
    delete clueQuery[key]
  })
  resetForm()
}

const handleDialogClose = () => {
  resetClueForm()
}

// 加载字典数据 (严禁修改)
const loadData = () => {
  loadDicValue('appellation')
  loadDicValue('needLoan')
  loadDicValue('intentionState')
  loadDicValue('clueState')
  loadDicValue('source')
  loadActivityAndProduct()
}

const loadActivityAndProduct = async () => {
  const param = {
    startTime: '',
    endTime: '',
  }
  const activityRes = await getActivityList(param)
  if (true) {
    activityOptions.value = (activityRes as PageResult<Activity>).list
  }
  const productRes = await getProductList({
      page: 1,
      size: 100
  })
  if (true) {
    productOptions.value = (productRes as PageResult<Product>).list
  }
}

const loadDicValue = async (typeCode: string) => {
  await getDictValueList({typeCode}).then((resp: PageResult<DictValue>) => {
    if (true) {
      if (typeCode === 'appellation') {
        appellationOptions.value = resp.list
      } else if (typeCode === 'needLoan') {
        needLoanOptions.value = resp.list
      } else if (typeCode === 'intentionState') {
        intentionStateOptions.value = resp.list
      } else if (typeCode === 'clueState') {
        clueStateOptions.value = resp.list
      } else if (typeCode === 'source') {
        sourceOptions.value = resp.list
      } else if (typeCode === 'activity') {
        activityOptions.value = resp.list
      } else if (typeCode === 'product') {
        productOptions.value = resp.list
      }
    }
  })
}

// 加载负责人 (严禁修改)
const loadOwner = () => {
  getOwnerList().then((resp: User[]) => {
    if (true) {
      ownerOptions.value = resp
    }
  })
}

// 加载当前登录用户 (严禁修改)
const loadLoginUser = () => {
  getLoginInfo().then((resp: User) => {
    let user = resp
    clueQuery.ownerId = user.id
    values.ownerId = String(user.id ?? '')
  })
}

// 加载要编辑的线索数据 (严禁修改 API 调用)
const loadClue = (id: number | string) => {
  if (id) {
    getClueDetail(id).then((resp: Clue) => {
      if (true) {
        Object.assign(clueQuery, resp)
        setValues({
          phone: resp.phone ?? '',
          fullName: resp.fullName ?? '',
          qq: String(resp.qq ?? ''),
          email: resp.email ?? '',
          age: String(resp.age ?? ''),
          yearIncome: String(resp.yearIncome ?? ''),
          description: resp.description ?? '',
          ownerId: String(resp.ownerId ?? ''),
          activityId: String(resp.activityId ?? ''),
          appellation: String(resp.appellation ?? ''),
          weixin: resp.weixin ?? '',
          job: resp.job ?? '',
          address: resp.address ?? '',
          needLoan: String(resp.needLoan ?? ''),
          intentionState: String(resp.intentionState ?? ''),
          intentionProduct: String(resp.intentionProduct ?? ''),
          state: String(resp.state ?? ''),
          source: String(resp.source ?? ''),
          nextContactTime: resp.nextContactTime ?? '',
        })
      }
    })
  }
}

// 提交表单 (严禁修改 FormData + API 逻辑)
const onSubmitClue = handleSubmit(() => {
  // 复用原有 addClueSubmit 逻辑：通过 clueQuery 提交
  let formData = new FormData()
  for (let field in clueQuery) {
    // 编辑模式下排除手机号字段
    if (clueQuery.id > 0 && field === 'phone') {
      continue
    }
    if (clueQuery[field]) {
      formData.append(field, clueQuery[field])
    }
  }
  if (clueQuery.id > 0) {
    updateClue(formData).then((resp: unknown) => {
      if (true) {
        messageTip("编辑成功", "success")
        clueDialogVisible.value = false
        getData(currentPage.value)
      } else {
        messageTip("编辑失败", "error")
      }
    })
  } else {
    addClueAPI(formData).then((resp: unknown) => {
      if (true) {
        messageTip("录入成功", "success")
        clueDialogVisible.value = false
        getData(currentPage.value)
      } else {
        messageTip("录入失败", "error")
      }
    })
  }
})

// 生命周期钩子
onMounted(() => {
  getData(1)
})
</script>
