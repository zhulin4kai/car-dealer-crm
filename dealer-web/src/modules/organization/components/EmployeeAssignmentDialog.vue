<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
      <DialogHeader>
        <DialogTitle>{{ dialogTitle }}</DialogTitle>
        <DialogDescription v-if="membership">
          {{ membership.employee.employeeNo }} · {{ membership.employee.name }}。{{
            dialogDescription
          }}
        </DialogDescription>
      </DialogHeader>

      <div v-if="loading" class="py-12 text-center text-muted-foreground">加载员工组织信息...</div>
      <div v-else-if="loadError" class="space-y-4 py-12 text-center">
        <p class="text-sm text-destructive">{{ loadError }}</p>
        <Button type="button" variant="outline" @click="emit('retry')">重新加载</Button>
      </div>
      <form v-else-if="membership" class="space-y-5" @submit.prevent="submitForm">
        <template v-if="mode === 'assignment'">
          <section class="rounded-lg border p-4">
            <h3 class="mb-3 font-medium">主要任职</h3>
            <div class="grid gap-3 sm:grid-cols-3">
              <div class="space-y-2">
                <Label for="primary-organization">主要组织</Label>
                <Select v-model="primaryOrganizationUnitId" @update:model-value="primaryOrganizationChanged">
                  <SelectTrigger id="primary-organization" class="w-full">
                    <SelectValue placeholder="选择组织" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem
                      v-for="unit in enabledOrganizationUnits"
                      :key="unit.id"
                      :value="String(unit.id)"
                    >
                      {{ unit.name }}
                    </SelectItem>
                  </SelectContent>
                </Select>
                <p v-if="errors.primaryOrganizationUnitId" class="text-sm text-destructive">
                  {{ errors.primaryOrganizationUnitId }}
                </p>
              </div>
              <div class="space-y-2">
                <Label for="primary-position">主要岗位</Label>
                <Select v-model="primaryPositionId">
                  <SelectTrigger id="primary-position" class="w-full">
                    <SelectValue placeholder="选择岗位" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem
                      v-for="position in enabledPositions"
                      :key="position.id"
                      :value="String(position.id)"
                    >
                      {{ position.name }}
                    </SelectItem>
                  </SelectContent>
                </Select>
                <p v-if="errors.primaryPositionId" class="text-sm text-destructive">
                  {{ errors.primaryPositionId }}
                </p>
              </div>
              <div class="space-y-2">
                <Label for="primary-effective-from">生效时间</Label>
                <Input
                  id="primary-effective-from"
                  v-model="primaryEffectiveFrom"
                  type="datetime-local"
                />
                <p v-if="errors.primaryEffectiveFrom" class="text-sm text-destructive">
                  {{ errors.primaryEffectiveFrom }}
                </p>
              </div>
            </div>
          </section>

          <section class="rounded-lg border p-4">
            <div class="mb-3 flex items-center justify-between">
              <div>
                <h3 class="font-medium">兼岗与代理任职</h3>
                <p class="text-xs text-muted-foreground">
                  兼岗与代理任职都必须设置明确的开始和结束时间。
                </p>
              </div>
              <Button type="button" size="sm" variant="outline" @click="addAdditionalAssignment">
                <Plus class="h-4 w-4" />新增任职
              </Button>
            </div>
            <div
              v-if="!additionalAssignments.length"
              class="py-5 text-center text-sm text-muted-foreground"
            >
              暂无兼岗或代理任职
            </div>
            <div
              v-for="(assignment, index) in additionalAssignments"
              :key="assignment.key"
              class="mb-3 grid gap-2 rounded-lg bg-muted/40 p-3 sm:grid-cols-6"
            >
              <Select v-model="assignment.assignmentType">
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="SECONDARY">兼任</SelectItem>
                  <SelectItem value="ACTING">代理任职</SelectItem>
                </SelectContent>
              </Select>
              <Select v-model="assignment.organizationUnitId">
                <SelectTrigger><SelectValue placeholder="组织" /></SelectTrigger>
                <SelectContent>
                  <SelectItem
                    v-for="unit in enabledOrganizationUnits"
                    :key="unit.id"
                    :value="String(unit.id)"
                    >{{ unit.name }}</SelectItem
                  >
                </SelectContent>
              </Select>
              <Select v-model="assignment.positionId">
                <SelectTrigger><SelectValue placeholder="岗位" /></SelectTrigger>
                <SelectContent>
                  <SelectItem
                    v-for="position in enabledPositions"
                    :key="position.id"
                    :value="String(position.id)"
                    >{{ position.name }}</SelectItem
                  >
                </SelectContent>
              </Select>
              <Input
                v-model="assignment.effectiveFrom"
                type="datetime-local"
                aria-label="任职开始时间"
              />
              <Input
                v-model="assignment.effectiveTo"
                type="datetime-local"
                aria-label="任职结束时间"
              />
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label="移除任职"
                @click="removeAdditionalAssignment(index)"
              >
                <Trash2 class="h-4 w-4" />
              </Button>
            </div>
            <p v-if="additionalError" class="text-sm text-destructive">{{ additionalError }}</p>
          </section>
        </template>

        <section v-if="mode === 'reporting' || mode === 'assignment'" class="rounded-lg border p-4">
          <h3 class="mb-3 font-medium">直属汇报关系</h3>
          <div class="grid gap-3 sm:grid-cols-2">
            <div class="space-y-2">
              <Label for="manager-employee">直属管理者</Label>
              <Select v-model="managerEmployeeId">
                <SelectTrigger id="manager-employee" class="w-full">
                  <SelectValue :placeholder="managerOptional ? '根公司负责人无需直属管理者' : '请选择直属管理者'" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-if="managerOptional" value="none">根公司负责人无需直属管理者</SelectItem>
                  <SelectItem
                    v-for="candidate in managerCandidates"
                    :key="candidate.employeeId"
                    :value="String(candidate.employeeId)"
                  >
                    {{ candidate.name }} · {{ candidate.positionName || '未设置岗位' }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label for="reporting-from">生效时间</Label>
              <Input
                id="reporting-from"
                v-model="reportingEffectiveFrom"
                type="datetime-local"
                :disabled="managerEmployeeId === 'none'"
              />
            </div>
          </div>
          <p v-if="additionalError" class="mt-2 text-sm text-destructive">
            {{ additionalError }}
          </p>
        </section>

        <div v-if="mode !== 'history'" class="space-y-2">
          <Label for="assignment-reason">调整原因</Label>
          <Textarea
            id="assignment-reason"
            v-model="reason"
            :rows="3"
            placeholder="请输入本次组织关系调整原因"
          />
          <p v-if="errors.reason" class="text-sm text-destructive">{{ errors.reason }}</p>
        </div>

        <section v-if="mode === 'history'" class="rounded-lg border p-4">
          <h3 class="mb-3 font-medium">组织关系变更历史</h3>
          <div v-if="!history.length" class="py-8 text-center text-sm text-muted-foreground">
            暂无变更历史
          </div>
          <div v-else class="space-y-3">
            <div v-for="item in history" :key="item.id" class="border-l-2 pl-3 text-sm">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-medium">{{ historyTypeLabel(item.changeType) }}</span>
                <span class="text-muted-foreground">{{ formatDateTime(item.createTime) }}</span>
                <span class="text-muted-foreground">{{ item.operatorName || '系统' }}</span>
              </div>
              <div class="mt-1 text-muted-foreground">
                {{ formatHistorySnapshot(item.beforeSummary) }} →
                {{ formatHistorySnapshot(item.afterSummary) }}
              </div>
              <div class="mt-1">原因：{{ item.reason || '未填写' }}</div>
            </div>
          </div>
        </section>
      </form>

      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="handleOpenChange(false)">
          {{ mode === 'history' ? '关闭' : '取消' }}
        </Button>
        <Button
          v-if="mode !== 'history'"
          :disabled="loading || submitting || !membership || !canSubmitMode"
          @click="submitForm"
        >
          {{ submitting ? '保存中...' : '保存调整' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { Plus, Trash2 } from '@lucide/vue'
import { useForm } from 'vee-validate'
import { computed, ref, watch } from 'vue'
import * as z from 'zod'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  ASSIGNMENT_TYPE,
  ASSIGNMENT_TYPE_LABEL,
  EMPLOYEE_ORGANIZATION_ACTION,
  ORGANIZATION_HISTORY_TYPE_LABEL,
  REPORTING_TYPE,
  flattenOrganizationTree,
  type AssignmentInput,
  type AssignmentType,
  type EmployeeOrganizationMembership,
  type ManagerCandidate,
  type OrganizationChangeHistory,
  type OrganizationHistorySnapshot,
  type OrganizationHistoryType,
  type OrganizationUnit,
  type Position,
  type ReportingInput,
  type UpdateEmployeeOrganizationRequest,
} from '@/modules/organization/model/organization.types'
import { formatDateTime } from '@/shared/utils/display-format'

export type EmployeeAssignmentDialogMode = 'assignment' | 'reporting' | 'history'

interface AssignmentFormValues {
  primaryOrganizationUnitId: string
  primaryPositionId: string
  primaryEffectiveFrom: string
  managerEmployeeId: string
  reportingEffectiveFrom: string
  reason: string
}

interface AdditionalAssignmentDraft {
  key: number
  organizationUnitId: string
  positionId: string
  assignmentType: Exclude<AssignmentType, 'PRIMARY'>
  effectiveFrom: string
  effectiveTo: string
}

const props = withDefaults(
  defineProps<{
    open: boolean
    mode: EmployeeAssignmentDialogMode
    membership?: EmployeeOrganizationMembership | null
    organizationUnits: OrganizationUnit[]
    positions: Position[]
    managerCandidates: ManagerCandidate[]
    history: OrganizationChangeHistory[]
    loadError?: string
    loading?: boolean
    submitting?: boolean
  }>(),
  { membership: null, loadError: '', loading: false, submitting: false },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  retry: []
  'organization-change': [organizationUnitId: string]
  submit: [request: UpdateEmployeeOrganizationRequest]
}>()

const dialogTitle = computed(() => {
  if (props.mode === 'reporting') return '调整员工汇报关系'
  if (props.mode === 'history') return '查看员工组织历史'
  return '调整员工任职'
})
const dialogDescription = computed(() => {
  if (props.mode === 'reporting') return '候选管理者由服务端按组织范围和汇报环路规则过滤。'
  if (props.mode === 'history') return '历史记录使用稳定业务类型展示。'
  return '主要任职、兼岗与代理任职将原子保存。'
})
const requestedAction = computed(() => {
  if (props.mode === 'reporting') return EMPLOYEE_ORGANIZATION_ACTION.REPORTING_UPDATE
  if (props.mode === 'history') return EMPLOYEE_ORGANIZATION_ACTION.HISTORY_VIEW
  return EMPLOYEE_ORGANIZATION_ACTION.ASSIGNMENT_UPDATE
})
const canSubmitMode = computed(
  () => props.membership?.allowedActions?.includes(requestedAction.value) ?? false,
)
const enabledOrganizationUnits = computed(() =>
  flattenOrganizationTree(props.organizationUnits).filter((unit) => unit.enabled),
)
const enabledPositions = computed(() => props.positions.filter((position) => position.enabled))
const managerOptional = computed(() => {
  const unit = enabledOrganizationUnits.value.find(
    (item) => String(item.id) === primaryOrganizationUnitId.value,
  )
  return Boolean(
    unit && unit.type === 'COMPANY' && unit.parentId == null &&
    String(unit.leaderEmployeeId ?? '') === String(props.membership?.employee.id ?? ''),
  )
})
const additionalAssignments = ref<AdditionalAssignmentDraft[]>([])
const additionalError = ref('')
let nextKey = 1

const schema = toTypedSchema(
  z.object({
    primaryOrganizationUnitId: z.string().min(1, '请选择主要组织'),
    primaryPositionId: z.string().min(1, '请选择主要岗位'),
    primaryEffectiveFrom: z.string().min(1, '请选择主要任职生效时间'),
    managerEmployeeId: z.string(),
    reportingEffectiveFrom: z.string(),
    reason: z.string().trim().min(1, '请输入调整原因').max(500),
  }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<AssignmentFormValues>({
  validationSchema: schema,
  initialValues: {
    primaryOrganizationUnitId: '',
    primaryPositionId: '',
    primaryEffectiveFrom: '',
    managerEmployeeId: 'none',
    reportingEffectiveFrom: '',
    reason: '',
  },
})
const [primaryOrganizationUnitId] = defineField('primaryOrganizationUnitId')
const [primaryPositionId] = defineField('primaryPositionId')
const [primaryEffectiveFrom] = defineField('primaryEffectiveFrom')
const [managerEmployeeId] = defineField('managerEmployeeId')
const [reportingEffectiveFrom] = defineField('reportingEffectiveFrom')
const [reason] = defineField('reason')

const additionalSchema = z.array(
  z
    .object({
      organizationUnitId: z.string().min(1),
      positionId: z.string().min(1),
      assignmentType: z.enum([ASSIGNMENT_TYPE.SECONDARY, ASSIGNMENT_TYPE.ACTING]),
      effectiveFrom: z.string().min(1),
      effectiveTo: z.string().min(1, '兼岗与代理任职必须设置结束时间'),
    })
    .superRefine((value, context) => {
      if (Date.parse(value.effectiveTo) <= Date.parse(value.effectiveFrom)) {
        context.addIssue({ code: 'custom', message: '任职结束时间必须晚于开始时间' })
      }
    }),
)

const submitForm = handleSubmit((values) => {
  if (!props.membership || props.mode === 'history' || !canSubmitMode.value) return
  const additionalResult = additionalSchema.safeParse(additionalAssignments.value)
  if (!additionalResult.success) {
    additionalError.value = additionalResult.error.issues[0]?.message ?? '请完善兼岗信息'
    return
  }
  additionalError.value = ''
  let reporting: ReportingInput | null = null
  if (values.managerEmployeeId === 'none' && !managerOptional.value) {
    additionalError.value = '普通员工必须选择目标组织内的直属管理者'
    return
  }
  if (values.managerEmployeeId !== 'none') {
    if (!values.reportingEffectiveFrom) {
      additionalError.value = '设置直属管理者时必须填写汇报关系生效时间'
      return
    }
    reporting = {
      managerEmployeeId: Number(values.managerEmployeeId),
      relationType: REPORTING_TYPE.DIRECT,
      effectiveFrom: toIsoDateTime(values.reportingEffectiveFrom),
    }
  }
  emit('submit', {
    expectedVersion: props.membership.version,
    primaryAssignment: {
      organizationUnitId: Number(values.primaryOrganizationUnitId),
      positionId: Number(values.primaryPositionId),
      assignmentType: ASSIGNMENT_TYPE.PRIMARY,
      effectiveFrom: toIsoDateTime(values.primaryEffectiveFrom),
    },
    additionalAssignments: additionalResult.data.map(toAssignmentInput),
    reporting,
    reason: values.reason.trim(),
  })
})

function addAdditionalAssignment(): void {
  additionalAssignments.value.push({
    key: nextKey++,
    organizationUnitId: '',
    positionId: '',
    assignmentType: ASSIGNMENT_TYPE.SECONDARY,
    effectiveFrom: nowInputValue(),
    effectiveTo: '',
  })
}

function primaryOrganizationChanged(value: unknown): void {
  managerEmployeeId.value = 'none'
  additionalError.value = ''
  if (value != null && String(value)) emit('organization-change', String(value))
}

function removeAdditionalAssignment(index: number): void {
  additionalAssignments.value.splice(index, 1)
}

function toAssignmentInput(value: Omit<AdditionalAssignmentDraft, 'key'>): AssignmentInput {
  return {
    organizationUnitId: Number(value.organizationUnitId),
    positionId: Number(value.positionId),
    assignmentType: value.assignmentType,
    effectiveFrom: toIsoDateTime(value.effectiveFrom),
    ...(value.effectiveTo ? { effectiveTo: toIsoDateTime(value.effectiveTo) } : {}),
  }
}

function nowInputValue(): string {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

function toInputValue(value?: string | null): string {
  if (!value) return nowInputValue()
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

function toIsoDateTime(value: string): string {
  return new Date(value).toISOString()
}

function historyTypeLabel(type: OrganizationHistoryType): string {
  return ORGANIZATION_HISTORY_TYPE_LABEL[type] ?? type
}

function formatHistorySnapshot(value?: string | OrganizationHistorySnapshot | null): string {
  if (!value) return '--'
  let snapshot: OrganizationHistorySnapshot | null = null
  if (typeof value === 'string') {
    try {
      snapshot = JSON.parse(value) as OrganizationHistorySnapshot
    } catch {
      return value
    }
  } else {
    snapshot = value
  }
  const parts = [
    snapshot.organizationUnitName,
    snapshot.positionName,
    snapshot.assignmentType ? ASSIGNMENT_TYPE_LABEL[snapshot.assignmentType] : null,
    snapshot.managerEmployeeName ? `管理者：${snapshot.managerEmployeeName}` : null,
    snapshot.effectiveFrom ? `自 ${formatDateTime(snapshot.effectiveFrom)}` : null,
    snapshot.effectiveTo ? `至 ${formatDateTime(snapshot.effectiveTo)}` : null,
  ].filter(Boolean)
  return parts.join(' · ') || '--'
}

function handleOpenChange(open: boolean): void {
  if (!open && props.submitting) return
  emit('update:open', open)
}

watch(
  () => [props.open, props.membership] as const,
  ([open, membership]) => {
    if (!open || !membership) return
    const primary = membership.primaryAssignment
    const reporting = membership.reporting
    resetForm({
      values: {
        primaryOrganizationUnitId: primary ? String(primary.organizationUnitId) : '',
        primaryPositionId: primary ? String(primary.positionId) : '',
        primaryEffectiveFrom: toInputValue(primary?.effectiveFrom),
        managerEmployeeId: reporting ? String(reporting.managerEmployeeId) : 'none',
        reportingEffectiveFrom: toInputValue(reporting?.effectiveFrom),
        reason: '',
      },
    })
    additionalAssignments.value = membership.additionalAssignments.map((assignment) => ({
      key: nextKey++,
      organizationUnitId: String(assignment.organizationUnitId),
      positionId: String(assignment.positionId),
      assignmentType:
        assignment.assignmentType === ASSIGNMENT_TYPE.ACTING
          ? ASSIGNMENT_TYPE.ACTING
          : ASSIGNMENT_TYPE.SECONDARY,
      effectiveFrom: toInputValue(assignment.effectiveFrom),
      effectiveTo: assignment.effectiveTo ? toInputValue(assignment.effectiveTo) : '',
    }))
    additionalError.value = ''
  },
  { immediate: true },
)
</script>
