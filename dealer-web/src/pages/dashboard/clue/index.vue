<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="crm-panel-body">
        <div class="crm-toolbar-actions">
          <Button @click="addClue" v-has-permission="PERMISSIONS.clue.add" class="gap-2">
            <Plus class="h-4 w-4" />
            录入线索
          </Button>
          <Button
            variant="outline"
            @click="importExcel"
            v-has-permission="PERMISSIONS.clue.import"
            class="gap-2"
          >
            <Upload class="h-4 w-4" />
            导入线索
          </Button>
          <Button
            variant="destructive"
            @click="handleBatchDelete"
            v-has-permission="PERMISSIONS.clue.delete"
            class="gap-2"
          >
            <Trash2 class="h-4 w-4" />
            批量删除
          </Button>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="crm-table-shell">
        <Table class="min-w-[1480px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox :checked="isAllSelected" @update:checked="toggleSelectAll" />
              </TableHead>
              <TableHead
                class="w-[60px] text-center"
                sortable
                sort-key="index"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >序号</TableHead
              >
              <TableHead
                class="w-[90px]"
                sortable
                sort-key="owner"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >负责人</TableHead
              >
              <TableHead
                class="min-w-[120px] text-center"
                sortable
                sort-key="activity"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >所属活动</TableHead
              >
              <TableHead
                class="w-[90px]"
                sortable
                sort-key="fullName"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >姓名</TableHead
              >
              <TableHead
                class="w-[70px]"
                sortable
                sort-key="appellation"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >称呼</TableHead
              >
              <TableHead
                class="w-[120px]"
                sortable
                sort-key="phone"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >手机</TableHead
              >
              <TableHead
                class="w-[110px]"
                sortable
                sort-key="weixin"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >微信</TableHead
              >
              <TableHead
                class="w-[90px]"
                sortable
                sort-key="needLoan"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >是否贷款</TableHead
              >
              <TableHead
                class="w-[90px]"
                sortable
                sort-key="intentionState"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >意向状态</TableHead
              >
              <TableHead
                class="min-w-[120px] text-center"
                sortable
                sort-key="intentionProduct"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >意向产品</TableHead
              >
              <TableHead
                class="w-[90px] text-center"
                sortable
                sort-key="state"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >线索状态</TableHead
              >
              <TableHead
                class="w-[90px] text-center"
                sortable
                sort-key="source"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >线索来源</TableHead
              >
              <TableHead
                class="min-w-[150px] text-center"
                sortable
                sort-key="nextContactTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="toggleSort"
                >下次联系时间</TableHead
              >
              <TableHead class="w-[240px] text-center">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="displayClueList.length === 0">
              <TableCell colspan="15" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                >暂无线索数据</TableCell
              >
            </TableRow>
            <TableRow v-for="(row, index) in displayClueList" :key="row.id">
              <TableCell>
                <Checkbox
                  :checked="selectedIds.includes(row.id)"
                  @update:checked="(checked: boolean) => toggleSelection(row.id, checked)"
                />
              </TableCell>
              <TableCell class="text-center text-[var(--crm-text-tertiary)]">{{
                (currentPage - 1) * pageSize + index + 1
              }}</TableCell>
              <TableCell class="max-w-[90px] truncate font-medium text-[var(--crm-text-primary)]">{{
                row.ownerDO?.name || '--'
              }}</TableCell>
              <TableCell class="max-w-[130px] truncate text-center">{{
                row.activityDO?.name || '--'
              }}</TableCell>
              <TableCell class="max-w-[90px] truncate">
                <button
                  type="button"
                  @click="view(row.id)"
                  class="font-semibold text-[var(--crm-primary)] hover:underline"
                >
                  {{ row.fullName || '--' }}
                </button>
              </TableCell>
              <TableCell class="max-w-[70px] truncate">{{
                row.appellationDO?.typeValue || '--'
              }}</TableCell>
              <TableCell
                class="max-w-[120px] truncate font-medium text-[var(--crm-text-primary)]"
                >{{ formatPhone(row.phone) }}</TableCell
              >
              <TableCell class="max-w-[110px] truncate">{{ row.weixin || '--' }}</TableCell>
              <TableCell class="max-w-[90px] truncate">
                <StatusBadge
                  :label="row.needLoanDO?.typeValue"
                  :tone="row.needLoanDO?.typeValue === '是' ? 'warning' : 'muted'"
                />
              </TableCell>
              <TableCell class="max-w-[110px] truncate">
                <StatusBadge
                  :label="row.intentionStateDO?.typeValue"
                  :tone="getClueTone(row.intentionStateDO?.typeValue)"
                />
              </TableCell>
              <TableCell class="max-w-[140px] truncate text-center">{{
                row.intentionProductDO?.name || '--'
              }}</TableCell>
              <TableCell class="text-center">
                <StatusBadge
                  :label="row.stateDO?.typeValue"
                  :tone="getClueStateTone(row.stateDO?.typeValue)"
                />
              </TableCell>
              <TableCell class="max-w-[100px] truncate text-center">
                <StatusBadge :label="row.sourceDO?.typeValue" tone="info" />
              </TableCell>
              <TableCell class="max-w-[150px] truncate text-center">{{
                formatDateTime(row.nextContactTime)
              }}</TableCell>
              <TableCell class="text-center">
                <div class="flex items-center justify-center gap-1">
                  <RowActionButton
                    label="详情"
                    @click="view(row.id)"
                    v-has-permission="PERMISSIONS.clue.view"
                  >
                    <Eye class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    label="编辑"
                    @click="edit(row.id)"
                    v-has-permission="PERMISSIONS.clue.edit"
                  >
                    <Pencil class="h-4 w-4" />
                  </RowActionButton>
                  <RowActionButton
                    label="删除"
                    danger
                    @click="del(row.id)"
                    v-has-permission="PERMISSIONS.clue.delete"
                  >
                    <Trash2 class="h-4 w-4" />
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
          @change="toPage"
        />
      </div>
    </section>

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
              accept=".xlsx"
              class="hidden"
              @change="handleFileChange"
            />
            <Button variant="outline" @click="fileInputRef?.click()">选择Excel文件</Button>
            <span class="text-sm text-muted-foreground">仅支持后缀名为.xlsx的文件</span>
          </div>

          <div class="pt-2 text-sm">
            <p class="font-semibold mb-1">重要提示：</p>
            <ul class="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>上传仅支持后缀名为.xlsx的文件；</li>
              <li>给定Excel文件的第一行将视为字段名；</li>
              <li>请确认您的文件大小不超过5MB；</li>
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
    <Dialog
      v-model:open="clueDialogVisible"
      @update:open="
        (open: boolean) => {
          if (!open) handleDialogClose()
        }
      "
    >
      <DialogContent class="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
        </DialogHeader>

        <form @submit.prevent="onSubmitClue" class="space-y-4">
          <div class="space-y-2">
            <Label>负责人</Label>
            <Select v-model="ownerId" disabled>
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择负责人" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in ownerOptions" :key="item.id" :value="String(item.id)">
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>所属活动</Label>
            <Select v-model="activityId">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择所属活动" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in activityOptions" :key="item.id" :value="String(item.id)">
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>姓名</Label>
            <Input v-model="fullName" />
            <p v-if="errors.fullName" class="text-sm text-destructive">{{ errors.fullName }}</p>
          </div>

          <div class="space-y-2">
            <Label>称呼</Label>
            <Select v-model="appellation">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择称呼" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in appellationOptions" :key="item.id" :value="String(item.id)">
                  {{ item.typeValue }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <!-- 编辑模式：手机禁用 -->
          <div class="space-y-2" v-if="isEditingClue">
            <Label>手机</Label>
            <Input :model-value="originalPhone" disabled />
          </div>
          <!-- 录入模式：手机可编辑 -->
          <div class="space-y-2" v-else>
            <Label>手机</Label>
            <Input v-model="phone" />
            <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
          </div>

          <div class="space-y-2">
            <Label>微信</Label>
            <Input v-model="weixin" />
          </div>

          <div class="space-y-2">
            <Label>QQ</Label>
            <Input v-model="qq" />
            <p v-if="errors.qq" class="text-sm text-destructive">{{ errors.qq }}</p>
          </div>

          <div class="space-y-2">
            <Label>邮箱</Label>
            <Input v-model="email" />
            <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
          </div>

          <div class="space-y-2">
            <Label>年龄</Label>
            <Input v-model="age" />
            <p v-if="errors.age" class="text-sm text-destructive">{{ errors.age }}</p>
          </div>

          <div class="space-y-2">
            <Label>职业</Label>
            <Input v-model="job" />
          </div>

          <div class="space-y-2">
            <Label>年收入</Label>
            <Input v-model="yearIncome" />
            <p v-if="errors.yearIncome" class="text-sm text-destructive">{{ errors.yearIncome }}</p>
          </div>

          <div class="space-y-2">
            <Label>住址</Label>
            <Input v-model="address" />
          </div>

          <div class="space-y-2">
            <Label>贷款</Label>
            <Select v-model="needLoan">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择是否需要贷款" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in needLoanOptions" :key="item.id" :value="String(item.id)">
                  {{ item.typeValue }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>意向状态</Label>
            <Select v-model="intentionState">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择意向状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in intentionStateOptions" :key="item.id" :value="String(item.id)">
                  {{ item.typeValue }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>意向产品</Label>
            <Select v-model="intentionProduct">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择意向产品" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in productOptions" :key="item.id" :value="String(item.id)">
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>线索状态</Label>
            <Select v-model="state">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择线索状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in clueStateOptions" :key="item.id" :value="String(item.id)">
                  {{ item.typeValue }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>线索来源</Label>
            <Select v-model="source">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择线索来源" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in sourceOptions" :key="item.id" :value="String(item.id)">
                  {{ item.typeValue }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label>线索描述</Label>
            <Textarea v-model="description" :rows="5" placeholder="请输入线索描述" />
            <p v-if="errors.description" class="text-sm text-destructive">
              {{ errors.description }}
            </p>
          </div>

          <div class="space-y-2">
            <Label>下次联系时间</Label>
            <Input type="datetime-local" v-model="nextContactTime" class="w-full" />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" @click="clueDialogVisible = false"
              >取 消</Button
            >
            <Button type="submit">提 交</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted, computed, watch } from 'vue'
import { ApiError } from '@/shared/api/api-error'
import type { ApiEnvelope } from '@/shared/api/api-types'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import {
  batchDeleteCluesByIds,
  getCurrentClues,
  importExcelAPI,
  delClueById,
  isCluePhoneAvailable,
  getLoginInfo,
  getClueDetail,
  addClue as addClueAPI,
  updateClue,
} from '@/modules/clue/api/clue-api'
import { getOwnerList } from '@/modules/activity/api/activity-api'
import { getDictValueList } from '@/modules/dict/api/dict-api'
import router from '@/router'
import { useRoute } from 'vue-router'
import { getProductList } from '@/modules/product/api/product-api'
import { getActivityList } from '@/modules/activity/api/activity-api'
import type { Activity } from '@/modules/activity/model/activity.types'
import type { Clue, ImportResult } from '@/modules/clue/model/clue.types'
import type { DictValue } from '@/modules/dict/model/dict.types'
import type { Product } from '@/modules/product/model/product.types'
import type { User } from '@/modules/user/model/user.types'
import type { PageResult } from '@/shared/api/api-types'
import { fromLocalDateTimeInput, toLocalDateTimeInput } from '@/shared/datetime/local-date'

import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { formatDateTime, formatPhone } from '@/shared/utils/display-format'
import { useClientSort } from '@/shared/utils/table-sort'
import { Eye, Pencil, Plus, Trash2, Upload } from '@lucide/vue'

// 原有的线索列表相关数据 (严禁修改)
const clueList = ref<Clue[]>([])
const route = useRoute()

const pageSize = ref(10)
const total = ref(0)
const importExcelDialogVisible = ref(false)
const currentPage = ref(1)
const selectedIds = ref<(number | string)[]>([])
const {
  sortBy,
  sortDirection,
  sortedRows: displayClueList,
  toggleSort,
} = useClientSort<Clue>(clueList, {
  index: (row) => row.id ?? 0,
  owner: (row) => row.ownerDO?.name ?? '',
  activity: (row) => row.activityDO?.name ?? '',
  fullName: 'fullName',
  appellation: (row) => row.appellationDO?.typeValue ?? '',
  phone: 'phone',
  weixin: 'weixin',
  needLoan: (row) => row.needLoanDO?.typeValue ?? '',
  intentionState: (row) => row.intentionStateDO?.typeValue ?? '',
  intentionProduct: (row) => row.intentionProductDO?.name ?? '',
  state: (row) => row.stateDO?.typeValue ?? '',
  source: (row) => row.sourceDO?.typeValue ?? '',
  nextContactTime: 'nextContactTime',
})
const fileInputRef = ref<HTMLInputElement | null>(null)
let selectedFile: File | null = null
const MAX_IMPORT_FILE_SIZE = 5 * 1024 * 1024

// 线索录入/编辑相关数据
const clueDialogVisible = ref(false)
const dialogTitle = ref('录入线索')
const editingClueId = ref<number | string | null>(null)
const originalPhone = ref('')
const isEditingClue = computed(() => editingClueId.value !== null)

const normalizeCluePhone = (value: string) => value.trim().replace(/[\s\-()（）]+/g, '')
const isMainlandMobile = (value: string) => /^1[3-9]\d{9}$/.test(value)

// 加载动态数据
const activityOptions = ref<Activity[]>([])
const productOptions = ref<Product[]>([])
// 加载字典数据
const ownerOptions = ref<User[]>([])
const appellationOptions = ref<DictValue[]>([])
const needLoanOptions = ref<DictValue[]>([])
const intentionStateOptions = ref<DictValue[]>([])
const clueStateOptions = ref<DictValue[]>([])
const sourceOptions = ref<DictValue[]>([])

// 线索表单校验规则 (zod，含 checkPhone 自定义验证器 → zod refine)
const clueSchema = toTypedSchema(
  z.object({
    phone: z
      .string()
      .min(1, '请输入手机号码')
      .refine((v) => isMainlandMobile(normalizeCluePhone(v)), { message: '手机号码格式有误' })
      .refine(
        async (v) => {
          if (!v) return true
          const normalizedPhone = normalizeCluePhone(v)
          // 如果是编辑模式且手机号未变化，跳过验证
          if (editingClueId.value !== null && originalPhone.value === normalizedPhone) return true
          return isCluePhoneAvailable(normalizedPhone)
        },
        { message: '该手机号录入过了，不能再录入' },
      ),
    fullName: z
      .string()
      .refine((v) => !v || v.length >= 2, { message: '姓名至少2个汉字' })
      .refine((v) => !v || /^[\u4e00-\u9fa5]+$/.test(v), { message: '姓名必须为中文汉字' }),
    qq: z
      .string()
      .refine((v) => !v || v.length >= 5, { message: 'QQ号至少为5位' })
      .refine((v) => !v || /^\d+$/.test(v), { message: 'QQ号码必须为数字' }),
    email: z
      .string()
      .refine((v) => !v || /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(v), {
        message: '邮箱格式有误',
      }),
    age: z.string().refine((v) => !v || /^\d+$/.test(v), { message: '年龄必须为数字' }),
    yearIncome: z
      .string()
      .refine((v) => !v || /^[0-9]+(\.[0-9]{2})?$/.test(v), {
        message: '年收入必须是整数或者两位小数',
      }),
    description: z
      .string()
      .refine((v) => !v || (v.length >= 5 && v.length <= 255), {
        message: '线索描述长度为5-255个字符',
      }),
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
  }),
)

const { handleSubmit, errors, resetForm, defineField, setFieldValue } = useForm({
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
const [phone] = defineField('phone')
const [fullName] = defineField('fullName')
const [qq] = defineField('qq')
const [email] = defineField('email')
const [age] = defineField('age')
const [yearIncome] = defineField('yearIncome')
const [description] = defineField('description')
const [ownerId] = defineField('ownerId')
const [activityId] = defineField('activityId')
const [appellation] = defineField('appellation')
const [weixin] = defineField('weixin')
const [job] = defineField('job')
const [address] = defineField('address')
const [needLoan] = defineField('needLoan')
const [intentionState] = defineField('intentionState')
const [intentionProduct] = defineField('intentionProduct')
const [state] = defineField('state')
const [source] = defineField('source')
const [nextContactTime] = defineField('nextContactTime')

// 全选计算
const isAllSelected = computed(
  () => displayClueList.value.length > 0 && selectedIds.value.length === displayClueList.value.length,
)

const toggleSelectAll = (checked: boolean) => {
  selectedIds.value = checked
    ? displayClueList.value.map((item: Clue) => item.id).filter((id): id is number | string => id != null)
    : []
}

const toggleSelection = (id: number | string | undefined, checked: boolean) => {
  if (id == null) {
    return
  }
  if (checked && !selectedIds.value.includes(id)) {
    selectedIds.value.push(id)
  } else if (!checked) {
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
    clueList.value = resp.list
    pageSize.value = resp.pageSize
    total.value = resp.total
  })
  currentPage.value = current
}

const toPage = (current: number) => {
  getData(current)
}

function getClueTone(
  label?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (!label) {
    return 'muted'
  }
  if (label.includes('有意向') || label.includes('高') || label.includes('成交')) {
    return 'info'
  }
  if (label.includes('跟进')) {
    return 'warning'
  }
  if (label.includes('流失') || label.includes('无效')) {
    return 'danger'
  }
  if (label.includes('待')) {
    return 'purple'
  }
  return 'success'
}

function getClueStateTone(
  label?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (!label) {
    return 'muted'
  }
  if (label.includes('转') || label.includes('成交')) {
    return 'success'
  }
  if (label.includes('流失') || label.includes('无效')) {
    return 'danger'
  }
  if (label.includes('待') || label.includes('跟进')) {
    return 'warning'
  }
  return 'info'
}

const importExcel = () => {
  importExcelDialogVisible.value = true
}

// 文件选择处理（替代 el-upload）
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      messageTip('仅支持.xlsx格式的Excel文件', 'warning')
      target.value = ''
      selectedFile = null
      return
    }
    if (file.size > MAX_IMPORT_FILE_SIZE) {
      messageTip('文件大小不能超过5MB', 'warning')
      target.value = ''
      selectedFile = null
      return
    }
    selectedFile = file
  }
}

// 上传文件 (严禁修改 API 调用)
const uploadFile = async () => {
  if (!selectedFile) {
    messageTip('请选择要导入的Excel文件', 'warning')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedFile)
  try {
    const result = await importExcelAPI(formData)
    messageTip(formatImportResult(result, '导入成功'), 'success')
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
    selectedFile = null
    getData(currentPage.value)
    importExcelDialogVisible.value = false
  } catch (error) {
    const result = getImportResultFromError(error)
    if (result) {
      messageTip(formatImportResult(result, '导入存在错误'), 'warning')
      if (result.importedCount > 0) {
        getData(currentPage.value)
      }
      return
    }
    messageTip('导入失败，请检查文件后重试', 'error')
  }
}

const isImportResult = (value: unknown): value is ImportResult => {
  if (!value || typeof value !== 'object') return false
  const result = value as Partial<ImportResult>
  return typeof result.importedCount === 'number'
    && typeof result.failedRows === 'number'
    && typeof result.totalRows === 'number'
}

const getImportResultFromError = (error: unknown): ImportResult | null => {
  if (!(error instanceof ApiError)) return null
  const envelope = error.raw as ApiEnvelope<unknown> | null
  return isImportResult(envelope?.data) ? envelope.data : null
}

const formatImportResult = (result: ImportResult, prefix: string) => {
  const firstError = result.errors?.[0]
  const errorText = firstError
    ? `；首个错误：第${firstError.row}行${firstError.column ? ` ${firstError.column}` : ''}${firstError.reason ? ` ${firstError.reason}` : ''}`
    : ''
  return `${prefix}：成功${result.importedCount}行，失败${result.failedRows}行${errorText}`
}

// 批量删除（替换 ElMessage/ElMessageBox 直接调用）
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    messageTip('请至少选择一条记录', 'warning')
    return
  }

  try {
    await messageConfirm('确定要删除选中的线索吗?')
  } catch {
    messageTip('已取消删除', 'info')
    return
  }

  try {
    await batchDeleteCluesByIds(selectedIds.value)
    messageTip('批量删除成功', 'success')
    getData(currentPage.value)
  } catch (error) {
    messageTip('请求失败，请检查网络或重试', 'error')
  }
}

const submitExcel = () => {
  uploadFile()
}

const view = (id: number | string) => {
  router.push('/dashboard/clue/detail/' + id)
}

// del 使用 messageTip/messageConfirm 间接调用，无需修改
const del = (id: number | string) => {
  messageConfirm('您确定要删除该数据吗？')
    .then(() => {
      delClueById(id).then((resp: unknown) => {
        messageTip('删除成功', 'success')
        getData(currentPage.value)
      })
    })
    .catch(() => {
      messageTip('取消删除', 'warning')
    })
}

// 新增的线索录入/编辑相关方法
const addClue = async () => {
  dialogTitle.value = '录入线索'
  resetClueForm()
  await loadData()
  await loadOwner()
  await loadLoginUser()
  clueDialogVisible.value = true
}

const edit = async (id: number | string) => {
  dialogTitle.value = '编辑线索'
  resetClueForm()
  await loadData()
  await loadOwner()
  await loadClue(id)
  clueDialogVisible.value = true
}

const resetClueForm = () => {
  editingClueId.value = null
  originalPhone.value = ''
  resetForm({
    values: {
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
}

const handleDialogClose = () => {
  resetClueForm()
}

// 加载字典数据 (严禁修改)
const loadData = async () => {
  await Promise.all([
    loadDicValue('appellation'),
    loadDicValue('needLoan'),
    loadDicValue('intentionState'),
    loadDicValue('clueState'),
    loadDicValue('source'),
    loadActivityAndProduct(),
  ])
}

const loadActivityAndProduct = async () => {
  const param = {
    startTime: '',
    endTime: '',
  }
  const activityRes = await getActivityList(param)
  activityOptions.value = (activityRes as PageResult<Activity>).list
  const productRes = await getProductList({
    page: 1,
    size: 100,
  })
  productOptions.value = (productRes as PageResult<Product>).list
}

const loadDicValue = async (typeCode: string) => {
  await getDictValueList({ typeCode }).then((resp: PageResult<DictValue>) => {
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
  })
}

// 加载负责人 (严禁修改)
const loadOwner = async () => {
  await getOwnerList().then((resp: User[]) => {
    ownerOptions.value = resp
  })
}

// 加载当前登录用户 (严禁修改)
const loadLoginUser = async () => {
  await getLoginInfo().then((resp: User) => {
    let user = resp
    setFieldValue('ownerId', String(user.id ?? ''))
  })
}

// 加载要编辑的线索数据 (严禁修改 API 调用)
const loadClue = async (id: number | string) => {
  if (id) {
    await getClueDetail(id).then((resp: Clue) => {
      editingClueId.value = resp.id ?? id
      originalPhone.value = normalizeCluePhone(resp.phone ?? '')
      resetForm({
        values: {
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
        nextContactTime: toLocalDateTimeInput(resp.nextContactTime),
        },
      })
    })
  }
}

// 提交表单 (严禁修改 FormData + API 逻辑)
const onSubmitClue = handleSubmit((formValues) => {
  let formData = new FormData()
  for (let field in formValues) {
    // 编辑模式下排除手机号字段
    if (editingClueId.value !== null && field === 'phone') {
      continue
    }
    const rawValue = formValues[field as keyof typeof formValues]
    const value =
      field === 'nextContactTime'
        ? fromLocalDateTimeInput(String(rawValue ?? ''))
        : field === 'phone'
          ? normalizeCluePhone(String(rawValue ?? ''))
          : rawValue
    if (value) {
      formData.append(field, String(value))
    }
  }
  if (editingClueId.value !== null) {
    formData.append('id', String(editingClueId.value))
    updateClue(formData).then((resp: unknown) => {
      messageTip('编辑成功', 'success')
      clueDialogVisible.value = false
      getData(currentPage.value)
    })
  } else {
    addClueAPI(formData).then((resp: unknown) => {
      messageTip('录入成功', 'success')
      clueDialogVisible.value = false
      getData(currentPage.value)
    })
  }
})

const openCreateDialogFromRoute = async () => {
  if (route.query.create !== '1' || clueDialogVisible.value) {
    return
  }
  await addClue()
  const nextQuery = { ...route.query }
  delete nextQuery.create
  await router.replace({ path: '/dashboard/clue', query: nextQuery })
}

watch(
  () => route.query.create,
  () => {
    void openCreateDialogFromRoute()
  },
)

// 生命周期钩子
onMounted(() => {
  getData(1)
  void openCreateDialogFromRoute()
})
</script>
