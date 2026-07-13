<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>员工调岗</DialogTitle>
        <DialogDescription> 关闭旧主要任职并创建新的组织、岗位和汇报关系事实。 </DialogDescription>
      </DialogHeader>
      <div v-if="loading" class="py-10 text-center text-muted-foreground">加载调岗事实...</div>
      <div v-else-if="errorMessage && !context" class="space-y-3 py-8 text-center">
        <p class="text-destructive">{{ errorMessage }}</p>
        <Button variant="outline" @click="loadContext">重新加载</Button>
      </div>
      <form v-else-if="context" class="space-y-4" @submit.prevent="submit">
        <div class="rounded-lg border p-3 text-sm">
          <div class="font-medium">当前任职</div>
          <div>{{ assignmentLabel }}</div>
          <div>
            任职状态：{{ context.employmentStatus }} · 员工版本 {{ context.employeeVersion }}
          </div>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="space-y-2 text-sm">
            目标组织
            <select v-model="form.organizationUnitId" class="field-select" @change="organizationChanged">
              <option value="">请选择</option>
              <option
                v-for="item in context.organizationCandidates"
                :key="item.id"
                :value="String(item.id)"
              >
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="space-y-2 text-sm">
            目标岗位
            <select v-model="form.positionId" class="field-select">
              <option value="">请选择</option>
              <option
                v-for="item in context.positionCandidates"
                :key="item.id"
                :value="String(item.id)"
              >
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="space-y-2 text-sm">
            直属管理者
            <select v-model="form.managerEmployeeId" class="field-select">
              <option value="">{{ managerRequired ? '请选择直属管理者' : managerOptionalReason }}</option>
              <option
                v-for="item in managerCandidates"
                :key="item.id"
                :value="String(item.id)"
              >
                {{ item.label }}{{ item.secondaryLabel ? ` · ${item.secondaryLabel}` : '' }}
              </option>
            </select>
            <span v-if="managerLoading" class="text-xs text-muted-foreground">正在加载目标组织管理者...</span>
            <span v-else-if="managerError" class="text-xs text-destructive">{{ managerError }}</span>
          </label>
          <label class="space-y-2 text-sm">
            生效时间
            <Input v-model="form.effectiveFrom" type="datetime-local" />
          </label>
        </div>
        <label class="block space-y-2 text-sm">
          调岗原因
          <Textarea v-model="form.reason" :rows="3" />
        </label>
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
        <p v-if="!canTransfer" class="text-sm text-muted-foreground">{{ actionReason }}</p>
      </form>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">
          取消
        </Button>
        <Button :disabled="submitting || loading || !canTransfer" @click="submit">
          {{ submitting ? '提交中...' : '确认调岗' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
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
import { Textarea } from '@/components/ui/textarea'
import { fetchUserLifecycleContext, transferEmployee } from '@/modules/user/api/user-lifecycle-api'
import {
  getUserLifecycleErrorMessage,
  isUserLifecycleConflict,
} from '@/modules/user/model/user-lifecycle-error'
import {
  USER_LIFECYCLE_ACTION,
  type LifecycleCandidate,
  type UserLifecycleContext,
} from '@/modules/user/model/user-lifecycle.types'
import type { EntityId } from '@/shared/types/id'

const props = defineProps<{ open: boolean; userId: EntityId }>()
const emit = defineEmits<{
  'update:open': [open: boolean]
  completed: [context: UserLifecycleContext]
}>()

const context = ref<UserLifecycleContext | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const managerCandidates = ref<LifecycleCandidate[]>([])
const managerRequired = ref(true)
const managerOptionalReason = ref('仅根公司负责人允许不设置直属管理者')
const managerLoading = ref(false)
const managerError = ref('')
const form = reactive({
  organizationUnitId: '',
  positionId: '',
  managerEmployeeId: '',
  effectiveFrom: '',
  reason: '',
})
let controller: AbortController | null = null
let managerController: AbortController | null = null
let requestId = 0
let managerRequestId = 0
let dialogEpoch = 0

const canTransfer = computed(() =>
  Boolean(
    context.value?.allowedActions.includes(USER_LIFECYCLE_ACTION.TRANSFER) &&
    context.value.statusTransitions.some(
      (item) =>
        item.action === USER_LIFECYCLE_ACTION.TRANSFER &&
        item.fromStatus === context.value?.employmentStatus &&
        !item.disabledReason,
    ),
  ),
)
const actionReason = computed(
  () =>
    context.value?.unavailableReasons[USER_LIFECYCLE_ACTION.TRANSFER] ??
    context.value?.statusTransitions.find((item) => item.action === USER_LIFECYCLE_ACTION.TRANSFER)
      ?.disabledReason ??
    '服务端未允许当前调岗迁移',
)
const assignmentLabel = computed(() => {
  const item = context.value?.currentAssignment
  return item
    ? `${item.organizationName ?? '未设置组织'} / ${item.positionName ?? '未设置岗位'} / ${item.managerName ?? '未设置管理者'}`
    : '无有效主要任职'
})

function includesCandidate(candidates: LifecycleCandidate[], value: string): boolean {
  return candidates.some((candidate) => String(candidate.id) === value)
}

function normalizedEffectiveFrom(): string | null {
  const value = new Date(form.effectiveFrom)
  return Number.isNaN(value.getTime()) ? null : value.toISOString()
}

function isCurrent(epoch: number, userId: EntityId): boolean {
  return epoch === dialogEpoch && props.open && String(props.userId) === String(userId)
}

async function organizationChanged(): Promise<void> {
  form.managerEmployeeId = ''
  managerCandidates.value = []
  managerRequired.value = true
  managerOptionalReason.value = '仅根公司负责人允许不设置直属管理者'
  managerLoading.value = false
  managerError.value = ''
  const organizationUnitId = form.organizationUnitId
  const current = ++managerRequestId
  managerController?.abort()
  if (!organizationUnitId) return
  const active = new AbortController()
  managerController = active
  managerLoading.value = true
  try {
    const result = await fetchUserLifecycleContext(props.userId, active.signal, organizationUnitId)
    if (active.signal.aborted || current !== managerRequestId || !props.open) return
    managerCandidates.value = result.managerCandidates
    managerRequired.value = result.managerRequired
    managerOptionalReason.value = result.managerOptionalReason || '根公司负责人无需直属管理者'
  } catch {
    if (!active.signal.aborted && current === managerRequestId)
      managerError.value = '加载目标组织管理者失败，请重新选择组织'
  } finally {
    if (current === managerRequestId) { managerLoading.value = false; managerController = null }
  }
}

async function loadContext(): Promise<void> {
  const current = ++requestId
  const epoch = dialogEpoch
  const userId = props.userId
  controller?.abort()
  const active = new AbortController()
  controller = active
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchUserLifecycleContext(userId, active.signal)
    if (current !== requestId || active.signal.aborted || !isCurrent(epoch, userId)) return
    context.value = result
  } catch (error) {
    if (current !== requestId || active.signal.aborted || !isCurrent(epoch, userId)) return
    context.value = null
    errorMessage.value = getUserLifecycleErrorMessage(error, '加载调岗事实失败')
  } finally {
    if (current === requestId) {
      loading.value = false
      controller = null
    }
  }
}

async function submit(): Promise<void> {
  const currentContext = context.value
  if (!currentContext || submitting.value || !canTransfer.value) return
  const effectiveFrom = normalizedEffectiveFrom()
  const organizationAllowed = includesCandidate(
    currentContext.organizationCandidates,
    form.organizationUnitId,
  )
  const positionAllowed = includesCandidate(currentContext.positionCandidates, form.positionId)
  const managerAllowed = managerRequired.value
    ? Boolean(form.managerEmployeeId) && includesCandidate(managerCandidates.value, form.managerEmployeeId)
    : !form.managerEmployeeId || includesCandidate(managerCandidates.value, form.managerEmployeeId)
  if (
    !organizationAllowed ||
    !positionAllowed ||
    !managerAllowed ||
    !effectiveFrom ||
    !form.reason.trim()
  ) {
    errorMessage.value = '请从服务端候选中完整选择组织、岗位、管理者并填写有效时间和原因'
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await transferEmployee(userId, {
      employeeVersion: currentContext.employeeVersion,
      organizationUnitId: form.organizationUnitId,
      positionId: form.positionId,
      managerEmployeeId: form.managerEmployeeId || null,
      effectiveFrom,
      reason: form.reason.trim(),
    })
    if (!isCurrent(epoch, userId)) return
    context.value = result
    emit('completed', result)
    emit('update:open', false)
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    errorMessage.value = getUserLifecycleErrorMessage(error, '调岗失败，未修改当前任职')
    if (isUserLifecycleConflict(error)) await loadContext()
  } finally {
    if (isCurrent(epoch, userId)) submitting.value = false
  }
}

function resetDialogState(): void {
  context.value = null
  loading.value = false
  submitting.value = false
  errorMessage.value = ''
  managerCandidates.value = []
  managerRequired.value = true
  managerOptionalReason.value = '仅根公司负责人允许不设置直属管理者'
  managerLoading.value = false
  managerError.value = ''
  Object.assign(form, {
    organizationUnitId: '',
    positionId: '',
    managerEmployeeId: '',
    effectiveFrom: '',
    reason: '',
  })
}

function changeOpen(open: boolean): void {
  if (!submitting.value) emit('update:open', open)
}

watch(
  () => [props.open, props.userId] as const,
  ([open]) => {
    dialogEpoch += 1
    controller?.abort()
    managerController?.abort()
    controller = null
    requestId += 1
    managerRequestId += 1
    resetDialogState()
    if (open) void loadContext()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  dialogEpoch += 1
  controller?.abort()
  managerController?.abort()
})
</script>

<style scoped>
.field-select {
  height: 2.25rem;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
}
</style>
