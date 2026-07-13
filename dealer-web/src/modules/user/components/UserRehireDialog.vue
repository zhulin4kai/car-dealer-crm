<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>返聘员工</DialogTitle>
        <DialogDescription>
          返聘创建新的任职事实，不覆盖离职历史，也不恢复任何旧角色或个人授权。
        </DialogDescription>
      </DialogHeader>
      <div v-if="loading" class="py-10 text-center text-muted-foreground">加载返聘事实...</div>
      <div v-else-if="errorMessage && !context" class="space-y-3 py-8 text-center">
        <p class="text-destructive">{{ errorMessage }}</p>
        <Button variant="outline" @click="loadContext">重新加载</Button>
      </div>
      <form v-else-if="context" class="space-y-4" @submit.prevent="submit">
        <div class="rounded border p-3 text-sm">
          当前任职状态：{{ context.employmentStatus }} · 旧有效角色 {{ context.activeRoleCount }} 个
          · 旧个人权限 {{ context.activePersonalPermissionCount }} 项
          <div class="mt-1 text-amber-700">以上旧授权仅用于风险提示，本次请求不会恢复它们。</div>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="space-y-2 text-sm">
            新组织
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
            新岗位
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
                {{ item.label }}
              </option>
            </select>
            <span v-if="managerLoading" class="text-xs text-muted-foreground">正在加载目标组织管理者...</span>
            <span v-else-if="managerError" class="text-xs text-destructive">{{ managerError }}</span>
          </label>
          <label class="space-y-2 text-sm">
            生效时间
            <Input v-model="form.effectiveFrom" type="datetime-local" />
          </label>
          <label class="space-y-2 text-sm">
            账号开通方式
            <select v-model="form.accountActivationMode" class="field-select">
              <option value="INVITE">重新邀请激活</option>
              <option value="RECOVER">按服务端恢复规则开通</option>
            </select>
          </label>
        </div>
        <label class="block space-y-2 text-sm">
          返聘原因
          <Textarea v-model="form.reason" :rows="3" />
        </label>
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
        <p v-if="!canRehire" class="text-sm text-muted-foreground">{{ actionReason }}</p>
      </form>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">
          取消
        </Button>
        <Button :disabled="submitting || loading || !canRehire" @click="submit">
          {{ submitting ? '提交中...' : '确认返聘' }}
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
import { fetchUserLifecycleContext, rehireEmployee } from '@/modules/user/api/user-lifecycle-api'
import {
  getUserLifecycleErrorMessage,
  isUserLifecycleConflict,
} from '@/modules/user/model/user-lifecycle-error'
import {
  USER_LIFECYCLE_ACTION,
  type LifecycleCandidate,
  type RehireResult,
  type UserLifecycleContext,
} from '@/modules/user/model/user-lifecycle.types'
import type { EntityId } from '@/shared/types/id'

const props = defineProps<{ open: boolean; userId: EntityId }>()
const emit = defineEmits<{
  'update:open': [open: boolean]
  completed: [result: RehireResult]
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
  accountActivationMode: 'INVITE',
  reason: '',
})
let controller: AbortController | null = null
let managerController: AbortController | null = null
let requestId = 0
let managerRequestId = 0
let dialogEpoch = 0

const canRehire = computed(() =>
  Boolean(
    context.value?.allowedActions.includes(USER_LIFECYCLE_ACTION.REHIRE) &&
    context.value.statusTransitions.some(
      (item) =>
        item.action === USER_LIFECYCLE_ACTION.REHIRE &&
        item.fromStatus === context.value?.employmentStatus &&
        !item.disabledReason,
    ),
  ),
)
const actionReason = computed(
  () =>
    context.value?.unavailableReasons[USER_LIFECYCLE_ACTION.REHIRE] ??
    context.value?.statusTransitions.find((item) => item.action === USER_LIFECYCLE_ACTION.REHIRE)
      ?.disabledReason ??
    '服务端未允许当前返聘迁移',
)

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
    errorMessage.value = getUserLifecycleErrorMessage(error, '加载返聘事实失败')
  } finally {
    if (current === requestId) {
      loading.value = false
      controller = null
    }
  }
}

async function submit(): Promise<void> {
  const currentContext = context.value
  if (!currentContext || submitting.value || !canRehire.value) return
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
    !['INVITE', 'RECOVER'].includes(form.accountActivationMode) ||
    !form.reason.trim()
  ) {
    errorMessage.value = '请从服务端候选中完整选择新任职，并填写有效时间和返聘原因'
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await rehireEmployee(userId, {
      employeeVersion: currentContext.employeeVersion,
      organizationUnitId: form.organizationUnitId,
      positionId: form.positionId,
      managerEmployeeId: form.managerEmployeeId || null,
      effectiveFrom,
      accountActivationMode: form.accountActivationMode,
      reason: form.reason.trim(),
    })
    if (!isCurrent(epoch, userId)) return
    if (result.restoredLegacyAuthorizationCount !== 0) {
      errorMessage.value = '服务端返回了不应自动恢复的旧授权，返聘结果需人工复核'
      return
    }
    emit('completed', result)
    emit('update:open', false)
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    errorMessage.value = getUserLifecycleErrorMessage(error, '返聘失败，未创建新任职')
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
    accountActivationMode: 'INVITE',
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
