<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="border-b border-[var(--crm-border-light)] px-5 py-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <h2 class="text-lg font-semibold text-[var(--crm-text-primary)]">活动详情</h2>
            <span class="inline-flex rounded px-2 py-1 text-xs font-medium" :class="activityStatusTone(activityDetail.status)">
              {{ activityStatusLabel(activityDetail.status) }}
            </span>
          </div>
          <Button variant="outline" size="sm" @click="goBack">返回</Button>
        </div>
      </div>
      <div class="crm-panel-body space-y-6">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div
            v-for="item in detailItems"
            :key="item.label"
            class="rounded border border-[var(--crm-border-light)] p-3"
            :class="item.class"
          >
            <div class="mb-1 text-xs text-[var(--crm-text-secondary)]">{{ item.label }}</div>
            <div
              class="break-all text-[var(--crm-text-primary)]"
              :class="{ 'font-semibold': item.emphasis }"
            >
              {{ item.value || '--' }}
            </div>
          </div>
        </div>

        <div v-if="activityDetail.description" class="rounded border border-[var(--crm-border-light)] p-4">
          <div class="mb-2 text-sm font-medium text-[var(--crm-text-secondary)]">活动描述</div>
          <p class="leading-relaxed text-[var(--crm-text-primary)]">{{ activityDetail.description }}</p>
        </div>

        <div v-if="activityDetail.resultSummary || activityDetail.reviewConclusion" class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="rounded border border-[var(--crm-border-light)] p-4">
            <div class="mb-2 text-sm font-medium text-[var(--crm-text-secondary)]">复盘结果</div>
            <p class="leading-relaxed text-[var(--crm-text-primary)]">{{ activityDetail.resultSummary || '--' }}</p>
          </div>
          <div class="rounded border border-[var(--crm-border-light)] p-4">
            <div class="mb-2 text-sm font-medium text-[var(--crm-text-secondary)]">复盘结论</div>
            <p class="leading-relaxed text-[var(--crm-text-primary)]">{{ activityDetail.reviewConclusion || '--' }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="border-b border-[var(--crm-border-light)] px-5 py-4">
        <h2 class="text-lg font-semibold text-[var(--crm-text-primary)]">ROI 观察</h2>
      </div>
      <div class="crm-panel-body">
        <div class="grid grid-cols-2 gap-3 md:grid-cols-4 lg:grid-cols-8">
          <div
            v-for="item in metricItems"
            :key="item.label"
            class="rounded border border-[var(--crm-border-light)] p-3 text-center"
          >
            <div class="text-xs text-[var(--crm-text-secondary)]">{{ item.label }}</div>
            <div class="mt-1 text-lg font-semibold text-[var(--crm-text-primary)]">{{ item.value ?? '--' }}</div>
          </div>
        </div>
        <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
          <div
            v-for="item in roiMoneyItems"
            :key="item.label"
            class="rounded border border-[var(--crm-border-light)] p-3"
          >
            <div class="mb-1 text-xs text-[var(--crm-text-secondary)]">{{ item.label }}</div>
            <div class="break-all text-[var(--crm-text-primary)]" :class="{ 'font-semibold': item.emphasis }">
              {{ item.value || '--' }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="crm-panel">
      <div class="border-b border-[var(--crm-border-light)] px-5 py-4">
        <h2 class="text-lg font-semibold text-[var(--crm-text-primary)]">添加活动备注</h2>
      </div>
      <div class="crm-panel-body">
        <form @submit.prevent="onSubmitRemark" class="space-y-4">
          <div class="space-y-2">
            <Label>活动备注</Label>
            <Textarea v-model="noteContent" :rows="5" placeholder="请输入活动备注内容" />
            <p v-if="errors.noteContent" class="text-sm text-destructive">
              {{ errors.noteContent }}
            </p>
          </div>
          <div class="flex gap-2">
            <Button type="submit" :disabled="submitting">
              <Loader2 v-if="submitting" class="mr-1 size-4 animate-spin" />
              <Pencil v-else class="mr-1 size-4" />
              提交备注
            </Button>
            <Button type="button" variant="outline" @click="resetRemarkForm">
              <RotateCw class="mr-1 size-4" />
              重置
            </Button>
          </div>
        </form>
      </div>
    </section>

    <section class="crm-panel">
      <div class="border-b border-[var(--crm-border-light)] px-5 py-4">
        <h2 class="text-lg font-semibold text-[var(--crm-text-primary)]">活动备注记录</h2>
      </div>
      <div class="crm-table-shell">
        <Table class="min-w-[960px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead class="w-[60px] text-center">序号</TableHead>
              <TableHead class="min-w-[200px]">备注内容</TableHead>
              <TableHead class="w-[160px] text-center">备注时间</TableHead>
              <TableHead class="w-[100px] text-center">备注人</TableHead>
              <TableHead class="w-[160px] text-center">编辑时间</TableHead>
              <TableHead class="w-[100px] text-center">编辑人</TableHead>
              <TableHead class="w-[120px] text-center">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="activityRemarkList.length === 0">
              <TableCell colspan="7" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无活动备注
              </TableCell>
            </TableRow>
            <TableRow v-for="(remark, index) in activityRemarkList" :key="remark.id">
              <TableCell class="text-center text-[var(--crm-text-tertiary)]">{{ index + 1 }}</TableCell>
              <TableCell class="max-w-[300px] truncate font-medium text-[var(--crm-text-primary)]">
                {{ remark.noteContent || '--' }}
              </TableCell>
              <TableCell class="text-center">{{ remark.createTime || '--' }}</TableCell>
              <TableCell class="text-center">{{ remark.createByDO?.name || '--' }}</TableCell>
              <TableCell class="text-center">{{ remark.editTime || '--' }}</TableCell>
              <TableCell class="text-center">{{ remark.editByDO?.name || '--' }}</TableCell>
              <TableCell class="text-center">
                <div class="flex justify-center gap-1">
                  <RowActionButton label="删除" danger @click="del(remark.id)">
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { Loader2, Pencil, RotateCw, Trash2 } from '@lucide/vue'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import {
  createActivityRemark,
  deleteActivityRemark,
  fetchActivityById,
  fetchActivityRemarkPage,
  fetchActivityRoi,
} from '@/modules/activity/api/activity-api'
import {
  activityStatusLabel,
  activityStatusTone,
  type Activity,
  type ActivityRemark,
  type ActivityRoi,
} from '@/modules/activity/model/activity.types'
import { toRouteId } from '@/shared/types/id'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import RowActionButton from '@/shared/ui/RowActionButton.vue'
import { formatCurrency } from '@/shared/utils/display-format'

const route = useRoute()
const router = useRouter()
const activityDetail = ref<Activity>({ ownerDO: {}, createByDO: {}, editByDO: {}, reviewedByDO: {} })
const roiDetail = ref<ActivityRoi>({})
const activityRemarkList = ref<ActivityRemark[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const submitting = ref(false)

const actualCostText = computed(() =>
  activityDetail.value.actualCost == null
    ? '--'
    : formatCurrency(activityDetail.value.actualCost, { fractionDigits: 0 }),
)

const roiText = computed(() => {
  if (roiDetail.value.roi == null) return '--'
  const value = Number(roiDetail.value.roi)
  if (Number.isNaN(value)) return '--'
  return `${(value * 100).toFixed(1)}%`
})

const detailItems = computed(() => [
  { label: '活动ID', value: activityDetail.value.id, emphasis: true },
  { label: '负责人', value: activityDetail.value.ownerDO?.name },
  { label: '渠道', value: activityDetail.value.channel },
  { label: '活动名称', value: activityDetail.value.name, class: 'md:col-span-2' },
  { label: '目标车型', value: activityDetail.value.targetModel },
  { label: '开始时间', value: activityDetail.value.startTime },
  { label: '结束时间', value: activityDetail.value.endTime },
  { label: '预算', value: formatCurrency(activityDetail.value.cost, { fractionDigits: 0 }), emphasis: true },
  { label: '实际成本', value: actualCostText.value, emphasis: true },
  { label: '复盘人', value: activityDetail.value.reviewedByDO?.name },
  { label: '复盘时间', value: activityDetail.value.reviewedTime },
])

const metricItems = computed(() => [
  { label: '线索', value: roiDetail.value.clueCount },
  { label: '有效线索', value: roiDetail.value.validClueCount },
  { label: '客户', value: roiDetail.value.customerCount },
  { label: '商机', value: roiDetail.value.opportunityCount },
  { label: '试驾', value: roiDetail.value.testDriveCount },
  { label: '报价', value: roiDetail.value.quoteCount },
  { label: '订单', value: roiDetail.value.orderCount },
  { label: 'ROI', value: roiText.value },
])

const roiMoneyItems = computed(() => [
  { label: '预算成本', value: formatCurrency(roiDetail.value.plannedCost, { fractionDigits: 0 }) },
  {
    label: '实际成本',
    value: roiDetail.value.actualCost == null ? '--' : formatCurrency(roiDetail.value.actualCost, { fractionDigits: 0 }),
  },
  { label: '成交金额', value: formatCurrency(roiDetail.value.dealAmount, { fractionDigits: 0 }), emphasis: true },
])

const remarkSchema = toTypedSchema(
  z.object({
    noteContent: z.string().min(5, '活动备注长度为5-255个字符').max(255, '活动备注长度为5-255个字符'),
  }),
)

const { handleSubmit, errors, resetForm, defineField } = useForm({
  validationSchema: remarkSchema,
  initialValues: { noteContent: '' },
})
const [noteContent] = defineField('noteContent')

function resetRemarkForm() {
  resetForm({ values: { noteContent: '' } })
}

function goBack() {
  router.go(-1)
}

async function loadActivityDetail() {
  const id = toRouteId(route.params.id)
  if (!id) return
  try {
    const [activity, roi] = await Promise.all([fetchActivityById(id), fetchActivityRoi(id)])
    activityDetail.value = {
      ...activity,
      ownerDO: activity.ownerDO ?? {},
      createByDO: activity.createByDO ?? {},
      editByDO: activity.editByDO ?? {},
      reviewedByDO: activity.reviewedByDO ?? {},
    }
    roiDetail.value = roi
  } catch {
    messageTip('加载活动详情失败', 'error')
  }
}

const onSubmitRemark = handleSubmit(async formData => {
  submitting.value = true
  try {
    if (!activityDetail.value.id) throw new Error('活动ID不存在')
    await createActivityRemark(activityDetail.value.id, formData.noteContent)
    messageTip('提交成功', 'success')
    await loadActivityRemarkList(1)
    resetRemarkForm()
  } catch {
    messageTip('提交失败', 'error')
  } finally {
    submitting.value = false
  }
})

async function loadActivityRemarkList(current: number) {
  const activityId = toRouteId(route.params.id)
  if (!activityId) return
  try {
    currentPage.value = current
    const resp = await fetchActivityRemarkPage(current, activityId)
    activityRemarkList.value = resp.list || []
    pageSize.value = resp.pageSize || 10
    total.value = resp.total || 0
  } catch {
    messageTip('加载备注列表失败', 'error')
  }
}

function toPage(current: number) {
  void loadActivityRemarkList(current)
}

async function del(id: number | string) {
  try {
    await messageConfirm('确定要删除该备注吗？')
    await deleteActivityRemark(id)
    messageTip('删除成功', 'success')
    await loadActivityRemarkList(1)
  } catch {
    messageTip('删除未完成', 'info')
  }
}

watch(
  () => route.params.id,
  newId => {
    if (newId) {
      void loadActivityDetail()
      void loadActivityRemarkList(1)
    }
  },
)

onMounted(() => {
  void loadActivityDetail()
  void loadActivityRemarkList(1)
})
</script>
