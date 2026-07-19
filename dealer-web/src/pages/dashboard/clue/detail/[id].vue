<template>
  <div class="p-5 bg-muted/30 min-h-screen">
    <!-- 线索详情卡片 -->
    <Card class="mb-5">
      <CardHeader class="border-b bg-muted/50 flex-row justify-between items-center">
        <CardTitle class="text-lg font-semibold">线索详情</CardTitle>
        <div class="flex gap-2">
          <Button
            v-if="canConvertClue"
            v-has-permission="PERMISSIONS.customer.transfer"
            variant="secondary"
            @click="convertCustomer"
          >
            转换客户
          </Button>
          <Button
            v-if="canConvertClue"
            v-has-permission="PERMISSIONS.clue.transfer"
            variant="outline"
            @click="openTransferDialog"
          >
            <UserRound class="size-4 mr-1" />
            转派
          </Button>
          <Button
            v-if="canCloseClue"
            v-has-permission="PERMISSIONS.clue.close"
            variant="destructive"
            @click="openLifecycleDialog('close')"
          >
            <ArchiveX class="size-4 mr-1" />
            关闭
          </Button>
          <Button
            v-if="canRestoreClue"
            v-has-permission="PERMISSIONS.clue.restore"
            variant="outline"
            @click="openLifecycleDialog('restore')"
          >
            <RotateCcw class="size-4 mr-1" />
            恢复
          </Button>
          <Button variant="secondary" @click="handleGoBack"> 返 回 </Button>
        </div>
      </CardHeader>
      <CardContent>
        <!-- 基本信息区域 -->
        <div class="mb-8 last:mb-0">
          <h4
            class="text-base font-semibold mb-5 pb-2 border-b-2 border-border relative before:absolute before:bottom-[-2px] before:left-0 before:w-[60px] before:h-[2px] before:bg-primary"
          >
            基本信息
          </h4>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >负责人：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.ownerDO?.name || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >所属活动：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.activityDO?.name || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >姓名：</span
              >
              <span class="flex-1 break-all font-semibold text-primary">{{
                clueDetail.fullName || '暂无'
              }}</span>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >称呼：</span
              >
              <span class="flex-1 break-all">{{
                clueDetail.appellationDO?.typeValue || '暂无'
              }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >手机：</span
              >
              <span class="flex-1 break-all font-semibold text-primary">{{
                clueDetail.phone || '暂无'
              }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >年龄：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.age || '暂无' }}</span>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >微信：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.weixin || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >QQ：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.qq || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >邮箱：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.email || '暂无' }}</span>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >职业：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.job || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >年收入：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.yearIncome || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >是否贷款：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.needLoanDO?.typeValue || '暂无' }}</span>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >住址：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.address || '暂无' }}</span>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >下次联系时间：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.nextContactTime || '暂无' }}</span>
            </div>
          </div>
        </div>

        <!-- 线索状态区域 -->
        <div class="mb-8 last:mb-0">
          <h4
            class="text-base font-semibold mb-5 pb-2 border-b-2 border-border relative before:absolute before:bottom-[-2px] before:left-0 before:w-[60px] before:h-[2px] before:bg-primary"
          >
            线索状态
          </h4>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >意向状态：</span
              >
              <Badge>{{ clueDetail.intentionStateDO?.typeValue || '暂无' }}</Badge>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >意向产品：</span
              >
              <Badge variant="secondary">{{ clueDetail.intentionProductDO?.name || '暂无' }}</Badge>
            </div>
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >线索状态：</span
              >
              <Badge :variant="clueDetail.state === -1 ? 'secondary' : 'outline'">
                {{ clueDetail.stateDO?.typeValue || '暂无' }}
              </Badge>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-4 mt-4">
            <div
              class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]"
                >线索来源：</span
              >
              <span class="flex-1 break-all">{{ clueDetail.sourceDO?.typeValue || '暂无' }}</span>
            </div>
          </div>
          <div v-if="clueDetail.description" class="mt-4">
            <div
              class="flex flex-col items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm"
            >
              <span class="font-semibold text-muted-foreground whitespace-nowrap mb-2"
                >线索描述：</span
              >
              <div class="mt-2 p-3 bg-muted rounded w-full leading-relaxed">
                {{ clueDetail.description }}
              </div>
            </div>
          </div>
        </div>

        <div class="mb-8 last:mb-0">
          <h4
            class="text-base font-semibold mb-5 pb-2 border-b-2 border-border relative before:absolute before:bottom-[-2px] before:left-0 before:w-[60px] before:h-[2px] before:bg-primary"
          >
            责任历史
          </h4>
          <div class="overflow-x-auto rounded-md border border-border">
            <Table class="min-w-[720px]">
              <TableHeader>
                <TableRow>
                  <TableHead>原负责人</TableHead>
                  <TableHead>新负责人</TableHead>
                  <TableHead>操作人</TableHead>
                  <TableHead>原因</TableHead>
                  <TableHead class="w-[180px]">时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-if="ownerHistoryList.length === 0">
                  <TableCell colspan="5" class="h-24 text-center text-muted-foreground">
                    暂无责任历史
                  </TableCell>
                </TableRow>
                <TableRow v-for="history in ownerHistoryList" :key="history.id">
                  <TableCell>{{ history.fromOwnerName || '初始分配' }}</TableCell>
                  <TableCell>{{ history.toOwnerName || history.toOwnerId || '暂无' }}</TableCell>
                  <TableCell>{{ history.assignedByName || history.assignedBy || '暂无' }}</TableCell>
                  <TableCell class="max-w-[260px] break-words">{{ history.reason || '暂无' }}</TableCell>
                  <TableCell>{{ history.assignedTime || '暂无' }}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 跟踪记录表单卡片 -->
    <Card class="mb-5">
      <CardHeader class="border-b bg-muted/50">
        <CardTitle class="text-lg font-semibold">添加跟踪记录</CardTitle>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="onSubmitRemark">
          <div class="space-y-4">
            <div class="space-y-2">
              <Label>跟踪方式</Label>
              <Select
                v-model="noteWay"
                @update:open="
                  (open: boolean) => {
                    if (open) loadDicValue('noteWay')
                  }
                "
              >
                <SelectTrigger class="w-[300px]">
                  <SelectValue placeholder="请选择跟踪方式" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="item in noteWayOptions" :key="item.id" :value="String(item.id)">
                    {{ item.typeValue }}
                  </SelectItem>
                </SelectContent>
              </Select>
              <p v-if="remarkErrors.noteWay" class="text-sm text-destructive">
                {{ remarkErrors.noteWay }}
              </p>
            </div>

            <div class="space-y-2">
              <Label>跟踪记录</Label>
              <Textarea
                v-model="noteContent"
                :rows="6"
                placeholder="请输入详细的跟踪记录内容..."
              />
              <p v-if="remarkErrors.noteContent" class="text-sm text-destructive">
                {{ remarkErrors.noteContent }}
              </p>
            </div>

            <div class="flex gap-2">
              <Button type="submit" :disabled="submitting">
                <Loader2 v-if="submitting" class="size-4 animate-spin mr-1" />
                <Pencil v-else class="size-4 mr-1" />
                提交记录
              </Button>
              <Button type="button" variant="outline" @click="resetRemarkForm">
                <RotateCw class="size-4 mr-1" />
                重置
              </Button>
            </div>
          </div>
        </form>
      </CardContent>
    </Card>

    <section class="crm-panel">
      <div class="border-b border-[var(--crm-border-light)] px-5 py-4">
        <h2 class="text-lg font-semibold text-[var(--crm-text-primary)]">跟踪记录列表</h2>
      </div>
      <div class="crm-table-shell">
        <Table class="min-w-[960px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[60px] text-center">序号</TableHead>
              <TableHead class="w-[120px] text-center">跟踪方式</TableHead>
              <TableHead class="min-w-[200px]">跟踪内容</TableHead>
              <TableHead class="w-[160px] text-center">跟踪时间</TableHead>
              <TableHead class="w-[100px] text-center">跟踪人</TableHead>
              <TableHead class="w-[160px] text-center">编辑时间</TableHead>
              <TableHead class="w-[100px] text-center">编辑人</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="clueRemarkList.length === 0">
              <TableCell colspan="7" class="h-32 text-center text-[var(--crm-text-tertiary)]"
                >暂无跟踪记录</TableCell
              >
            </TableRow>
            <TableRow v-for="(remark, index) in clueRemarkList" :key="remark.id">
              <TableCell class="text-center text-[var(--crm-text-tertiary)]">{{
                index + 1
              }}</TableCell>
              <TableCell class="text-center">
                <StatusBadge :label="remark.noteWayDO?.typeValue" tone="info" />
              </TableCell>
              <TableCell
                class="max-w-[300px] truncate font-medium text-[var(--crm-text-primary)]"
                >{{ remark.noteContent || '--' }}</TableCell
              >
              <TableCell class="text-center">{{ remark.createTime }}</TableCell>
              <TableCell class="text-center">{{ remark.createByDO?.name }}</TableCell>
              <TableCell class="text-center">{{ remark.editTime }}</TableCell>
              <TableCell class="text-center">{{ remark.editByDO?.name }}</TableCell>
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
  </div>

  <!-- 线索转换为客户的弹窗 -->
  <Dialog v-model:open="convertCustomerDialogVisible">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>线索转换客户</DialogTitle>
      </DialogHeader>

      <form @submit.prevent="onSubmitConvert">
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>意向产品</Label>
            <Select v-model="convertProduct">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择意向产品" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in productOptions" :key="item.id" :value="String(item.id)">
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <p v-if="convertErrors.product" class="text-sm text-destructive">
              {{ convertErrors.product }}
            </p>
          </div>

          <div class="space-y-2">
            <Label>客户描述</Label>
            <Textarea v-model="convertDescription" :rows="6" placeholder="请输入客户描述" />
            <p v-if="convertErrors.description" class="text-sm text-destructive">
              {{ convertErrors.description }}
            </p>
          </div>

          <div class="space-y-2">
            <Label>下次跟踪时间</Label>
            <Input type="datetime-local" v-model="convertNextContactTime" class="w-full" />
            <p v-if="convertErrors.nextContactTime" class="text-sm text-destructive">
              {{ convertErrors.nextContactTime }}
            </p>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" @click="convertCustomerDialogVisible = false"
              >取 消</Button
            >
            <Button type="submit" :disabled="converting">
              <Loader2 v-if="converting" class="size-4 animate-spin mr-1" />
              确认转换
            </Button>
          </DialogFooter>
        </div>
      </form>
    </DialogContent>
  </Dialog>

  <Dialog v-model:open="transferDialogVisible">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>转派线索</DialogTitle>
      </DialogHeader>

      <div class="space-y-4">
        <div class="space-y-2">
          <Label>目标负责人</Label>
          <Select v-model="transferOwnerId">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="请选择目标负责人" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem
                v-for="item in ownerOptions"
                :key="item.userId"
                :value="String(item.userId)"
              >
                {{ item.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="space-y-2">
          <Label>转派原因</Label>
          <Textarea v-model="transferReason" :rows="5" placeholder="请输入转派原因" />
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" @click="transferDialogVisible = false">
            取 消
          </Button>
          <Button type="button" :disabled="transferring" @click="submitTransferOwner">
            <Loader2 v-if="transferring" class="size-4 animate-spin mr-1" />
            确认转派
          </Button>
        </DialogFooter>
      </div>
    </DialogContent>
  </Dialog>

  <Dialog v-model:open="lifecycleDialogVisible">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>{{ lifecycleDialogTitle }}</DialogTitle>
      </DialogHeader>

      <div class="space-y-4">
        <div class="space-y-2">
          <Label>{{ lifecycleReasonLabel }}</Label>
          <Textarea v-model="lifecycleReason" :rows="5" :placeholder="lifecycleReasonPlaceholder" />
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" @click="lifecycleDialogVisible = false">
            取 消
          </Button>
          <Button
            type="button"
            :variant="lifecycleAction === 'close' ? 'destructive' : 'default'"
            :disabled="lifecycleSubmitting"
            @click="submitLifecycleAction"
          >
            <Loader2 v-if="lifecycleSubmitting" class="size-4 animate-spin mr-1" />
            {{ lifecycleSubmitText }}
          </Button>
        </DialogFooter>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { ArchiveX, Pencil, RotateCcw, RotateCw, Loader2, UserRound } from '@lucide/vue'
import { messageTip } from '@/shared/utils/feedback'
import {
  getClueDetail,
  getClueOwnerHistory,
  addClueRemark,
  getClueRemarkList,
  convertClueToCustomer,
  transferClueOwner,
  closeClue,
  restoreClue,
} from '@/modules/clue/api/clue-api'
import { getDictValueList } from '@/modules/dict/api/dict-api'
import { getProductList } from '@/modules/product/api/product-api'
import { fetchOwnerList } from '@/modules/user/api/user-api'
import {
  OWNER_QUALIFICATION_CONTEXT,
  type OwnerCandidate,
} from '@/modules/user/model/owner.types'
import type { Clue, ClueOwnerHistory, ClueRemark } from '@/modules/clue/model/clue.types'
import type { DictValue } from '@/modules/dict/model/dict.types'
import type { Product } from '@/modules/product/model/product.types'
import { fromLocalDateTimeInput } from '@/shared/datetime/local-date'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
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
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'

// 路由
const route = useRoute()
const router = useRouter()

// 线索详情对象
const clueDetail = ref<Clue>({
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {},
})

// 线索跟踪记录列表
const clueRemarkList = ref<ClueRemark[]>([])
const ownerHistoryList = ref<ClueOwnerHistory[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 跟踪方式字典
const noteWayOptions = ref<DictValue[]>([])

// 意向产品选项
const productOptions = ref<Product[]>([])
const ownerOptions = ref<OwnerCandidate[]>([])

// 加载状态
const submitting = ref(false)
const converting = ref(false)
const transferring = ref(false)
const lifecycleSubmitting = ref(false)

// 转换客户弹窗
const convertCustomerDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const transferOwnerId = ref('')
const transferReason = ref('')
type LifecycleAction = 'close' | 'restore'
const lifecycleDialogVisible = ref(false)
const lifecycleAction = ref<LifecycleAction>('close')
const lifecycleReason = ref('')
const clueStateCode = computed(() => clueDetail.value.stateDO?.valueCode)
const canConvertClue = computed(() => clueStateCode.value !== 'converted' && clueStateCode.value !== 'closed')
const canCloseClue = computed(() => clueStateCode.value !== 'converted' && clueStateCode.value !== 'closed')
const canRestoreClue = computed(() => clueStateCode.value === 'closed')
const lifecycleDialogTitle = computed(() => lifecycleAction.value === 'close' ? '关闭线索' : '恢复线索')
const lifecycleReasonLabel = computed(() => lifecycleAction.value === 'close' ? '关闭原因' : '恢复原因')
const lifecycleReasonPlaceholder = computed(() =>
  lifecycleAction.value === 'close' ? '请输入关闭原因' : '请输入恢复原因',
)
const lifecycleSubmitText = computed(() => lifecycleAction.value === 'close' ? '确认关闭' : '确认恢复')

// 跟踪记录表单 schema (zod)
const remarkFormSchema = toTypedSchema(
  z.object({
    noteContent: z.string().min(1, '跟踪记录内容不能为空'),
    noteWay: z.string().min(1, '请选择跟踪方式'),
  }),
)

const {
  handleSubmit: handleRemarkSubmit,
  errors: remarkErrors,
  resetForm: resetRemarkFields,
  defineField: defineRemarkField,
} = useForm({
  validationSchema: remarkFormSchema,
  initialValues: {
    noteContent: '',
    noteWay: '',
  },
})
const [noteContent] = defineRemarkField('noteContent')
const [noteWay] = defineRemarkField('noteWay')

// 转换客户表单 schema (zod)
const convertFormSchema = toTypedSchema(
  z.object({
    product: z.string().min(1, '请选择意向产品'),
    description: z
      .string()
      .min(5, '客户描述长度为5-255个字符')
      .max(255, '客户描述长度为5-255个字符'),
    nextContactTime: z.string().min(1, '请选择下次联系时间'),
  }),
)

const {
  handleSubmit: handleConvertSubmit,
  errors: convertErrors,
  resetForm: resetConvertFields,
  defineField: defineConvertField,
} = useForm({
  validationSchema: convertFormSchema,
  initialValues: {
    product: '',
    description: '',
    nextContactTime: '',
  },
})
const [convertProduct] = defineConvertField('product')
const [convertDescription] = defineConvertField('description')
const [convertNextContactTime] = defineConvertField('nextContactTime')

// 重置跟踪记录表单
const resetRemarkForm = () => {
  resetRemarkFields({
    values: {
      noteContent: '',
      noteWay: '',
    },
  })
}

// 加载线索详情 (严禁修改)
const loadClueDetail = async () => {
  const id = route.params.id
  try {
    clueDetail.value = await getClueDetail(id)
  } catch (error) {
    messageTip('加载线索详情失败', 'error')
  }
}

const loadOwnerHistory = async () => {
  try {
    ownerHistoryList.value = await getClueOwnerHistory(route.params.id)
  } catch (error) {
    messageTip('加载责任历史失败', 'error')
  }
}

const loadOwnerOptions = async () => {
  try {
    ownerOptions.value = await fetchOwnerList({
      permissionCode: PERMISSIONS.clue.transfer,
      qualificationContext: OWNER_QUALIFICATION_CONTEXT.CLUE_OWNER,
    })
  } catch (error) {
    messageTip('加载负责人失败', 'error')
  }
}

// 提交线索跟踪记录 (严禁修改 API 调用)
const onSubmitRemark = handleRemarkSubmit(async (formData) => {
  submitting.value = true
  try {
    if (!clueDetail.value.id) throw new Error('线索ID不存在')
    await addClueRemark(clueDetail.value.id, formData.noteContent, formData.noteWay)
    messageTip('提交成功', 'success')
    await loadClueRemarkList(1)
    resetRemarkForm()
  } catch (error) {
    messageTip('提交失败', 'error')
  } finally {
    submitting.value = false
  }
})

// 加载字典数据 (严禁修改)
const loadDicValue = async (typeCode: string) => {
  try {
    const resp = await getDictValueList({ typeCode })
    if (typeCode === 'noteWay') {
      noteWayOptions.value = resp.list
    }
  } catch (error) {
    messageTip('加载字典数据失败', 'error')
  }
}

// 查询线索跟踪记录列表数据 (严禁修改)
const loadClueRemarkList = async (current: number) => {
  try {
    currentPage.value = current
    const resp = await getClueRemarkList(current, route.params.id)
    clueRemarkList.value = resp.list || []
    pageSize.value = resp.pageSize || 10
    total.value = resp.total || 0
  } catch (error) {
    messageTip('加载跟踪记录列表失败', 'error')
  }
}

// 分页函数
const toPage = (current: number) => {
  currentPage.value = current
  loadClueRemarkList(current)
}

// 转换客户
const convertCustomer = async () => {
  // 先加载产品列表
  await ProductList()

  // 设置意向产品的默认值为当前线索的意向产品
  resetConvertFields({
    values: {
      product: String(clueDetail.value.intentionProduct ?? clueDetail.value.intentionProductDO?.id ?? ''),
      description: '',
      nextContactTime: '',
    },
  })
  convertCustomerDialogVisible.value = true
}

const openTransferDialog = async () => {
  await loadOwnerOptions()
  transferOwnerId.value = ''
  transferReason.value = ''
  transferDialogVisible.value = true
}

const submitTransferOwner = async () => {
  if (!clueDetail.value.id) {
    messageTip('线索ID不存在', 'error')
    return
  }
  if (!transferOwnerId.value) {
    messageTip('请选择目标负责人', 'warning')
    return
  }
  const reason = transferReason.value.trim()
  if (!reason) {
    messageTip('请输入转派原因', 'warning')
    return
  }
  transferring.value = true
  try {
    await transferClueOwner(clueDetail.value.id, {
      newOwnerId: Number(transferOwnerId.value),
      reason,
    })
    messageTip('转派成功', 'success')
    transferDialogVisible.value = false
    await loadClueDetail()
    await loadOwnerHistory()
  } catch (error) {
    messageTip('转派失败', 'error')
  } finally {
    transferring.value = false
  }
}

const openLifecycleDialog = (action: LifecycleAction) => {
  lifecycleAction.value = action
  lifecycleReason.value = ''
  lifecycleDialogVisible.value = true
}

const submitLifecycleAction = async () => {
  if (!clueDetail.value.id) {
    messageTip('线索ID不存在', 'error')
    return
  }
  const reason = lifecycleReason.value.trim()
  if (!reason) {
    messageTip(lifecycleAction.value === 'close' ? '请输入关闭原因' : '请输入恢复原因', 'warning')
    return
  }
  lifecycleSubmitting.value = true
  try {
    if (lifecycleAction.value === 'close') {
      await closeClue(clueDetail.value.id, { reason })
      messageTip('关闭成功', 'success')
    } else {
      await restoreClue(clueDetail.value.id, { reason })
      messageTip('恢复成功', 'success')
    }
    lifecycleDialogVisible.value = false
    await loadClueDetail()
  } catch (error) {
    messageTip(lifecycleAction.value === 'close' ? '关闭失败' : '恢复失败', 'error')
  } finally {
    lifecycleSubmitting.value = false
  }
}

// 线索转换客户 (严禁修改 API 调用)
const onSubmitConvert = handleConvertSubmit(async (formData) => {
  converting.value = true
  try {
    if (!clueDetail.value.id) throw new Error('线索ID不存在')
    const nextContactTime = fromLocalDateTimeInput(formData.nextContactTime)
    if (!nextContactTime) {
      messageTip('下次跟踪时间格式有误', 'error')
      return
    }
    await convertClueToCustomer(
      clueDetail.value.id,
      formData.product,
      formData.description,
      nextContactTime,
    )
    messageTip('转换成功', 'success')
    convertCustomerDialogVisible.value = false
    await loadClueDetail()
  } catch (error) {
    messageTip('转换失败', 'error')
  } finally {
    converting.value = false
  }
})

const ProductList = async () => {
  try {
    const resp = await getProductList({ page: 1, size: 100 })
    productOptions.value = resp.list || []
  } catch (error) {
    messageTip('加载意向产品列表失败', 'error')
  }
}

// 使用 router 返回上一页
const handleGoBack = () => {
  router.go(-1)
}

// 监听路由参数变化
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadClueDetail()
      loadOwnerHistory()
      loadClueRemarkList(1)
      ProductList()
    }
  },
)

// 组件挂载时执行
onMounted(() => {
  loadClueDetail()
  loadOwnerHistory()
  loadClueRemarkList(1)
  ProductList() // 预先加载产品列表
})
</script>
