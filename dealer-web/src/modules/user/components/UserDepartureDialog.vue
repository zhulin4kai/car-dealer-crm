<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[92vh] overflow-y-auto sm:max-w-3xl">
      <DialogHeader>
        <DialogTitle>员工离职闭环</DialogTitle>
        <DialogDescription>
          必须依次完成预检、进入待交接、六类直接责任原子交接和最终预检，不能跳步。
        </DialogDescription>
      </DialogHeader>

      <div v-if="loading" class="py-10 text-center text-muted-foreground">加载离职事实...</div>
      <div v-else-if="errorMessage && !context" class="space-y-3 py-8 text-center">
        <p class="text-destructive">{{ errorMessage }}</p>
        <Button variant="outline" @click="loadContext">重新加载</Button>
      </div>
      <div v-else-if="context" class="space-y-5">
        <section class="grid gap-2 rounded-lg border p-4 text-sm sm:grid-cols-3">
          <div>任职状态：{{ context.employmentStatus }}</div>
          <div>
            有效授权：角色 {{ context.activeRoleCount }} / 个人
            {{ context.activePersonalPermissionCount }}
          </div>
          <div>有效会话：{{ context.activeSessionCount }}</div>
          <div>有效补充任职：{{ context.additionalAssignmentCount }}</div>
          <div>有效汇报关系：{{ context.reportingRelationCount }}</div>
          <div>员工版本：{{ context.employeeVersion }}</div>
        </section>

        <label class="block space-y-2 text-sm">
          离职及交接原因
          <Textarea v-model="reason" :rows="3" placeholder="重新预检后不要修改该原因" />
        </label>

        <section class="rounded-lg border p-4">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div>
              <h3 class="font-medium">1. 离职预检</h3>
              <p class="text-sm text-muted-foreground">
                统计责任、授权、会话和任职事实；预检标识只保存在当前对话框内存中。
              </p>
            </div>
            <Button
              size="sm"
              variant="outline"
              :disabled="busy || !canContextAction(USER_LIFECYCLE_ACTION.DEPARTURE_PRECHECK)"
              @click="runPrecheck"
            >
              {{ precheck ? '重新预检' : '执行离职预检' }}
            </Button>
          </div>

          <p
            v-if="!canContextAction(USER_LIFECYCLE_ACTION.DEPARTURE_PRECHECK)"
            class="text-sm text-muted-foreground"
          >
            {{ contextActionReason(USER_LIFECYCLE_ACTION.DEPARTURE_PRECHECK) }}
          </p>

          <div v-if="precheck" class="space-y-3 text-sm">
            <div class="grid gap-2 sm:grid-cols-3">
              <div>有效角色：{{ precheck.activeRoleCount }}</div>
              <div>个人权限：{{ precheck.activePersonalPermissionCount }}</div>
              <div>有效会话：{{ precheck.activeSessionCount }}</div>
              <div>任职关系：{{ precheck.activeAssignmentCount }}</div>
              <div>汇报关系：{{ precheck.activeReportingCount }}</div>
              <div>
                交接状态：{{
                  precheck.handoverCompleted
                    ? '已完成'
                    : precheck.handoverRequired
                      ? '需要交接'
                      : '无需交接'
                }}
              </div>
            </div>
            <p :class="precheckExpired ? 'text-destructive' : 'text-muted-foreground'">
              预检有效期至：{{ precheck.expiresAt }}
            </p>

            <div class="grid gap-2 sm:grid-cols-2">
              <article
                v-for="item in directResponsibilities"
                :key="item.resourceType"
                class="rounded bg-muted/40 p-3"
              >
                <div class="font-medium">
                  {{ item.resourceName }}（{{ item.resourceType }}）· 直接负责人责任
                </div>
                <div>
                  共 {{ item.count }}，可交接 {{ item.transferableCount }}，阻塞
                  {{ item.blockedCount }} · {{ item.statusName }}（{{ item.statusCode }}）
                </div>
                <div v-if="item.blockingReasons.length" class="mt-1 text-amber-700">
                  {{ item.blockingReasons.join('；') }}
                </div>
                <div v-if="item.conflicts.length" class="mt-2 space-y-1 text-amber-700">
                  <div v-for="conflict in item.conflicts" :key="conflict.conflictCode">
                    {{ conflict.conflictName }}（{{ conflict.conflictCode }}）×
                    {{ conflict.count }}：{{ conflict.reason }}
                  </div>
                </div>
              </article>
            </div>

            <div v-if="derivedImpacts.length" class="space-y-2 rounded border border-dashed p-3">
              <div class="font-medium">派生影响与资格检查</div>
              <div class="text-muted-foreground">
                报价和交易不具有独立可转移负责人，前端不会把它们提交为直接交接项。
              </div>
              <div v-for="item in derivedImpacts" :key="item.resourceType">
                {{ item.resourceName }}（{{ item.resourceType }}）：影响 {{ item.count }} 项 ·
                {{ item.statusName }}（{{ item.statusCode }}）
                <span v-if="item.blockingReasons.length">
                  · {{ item.blockingReasons.join('；') }}
                </span>
              </div>
            </div>

            <p v-if="matrixError" class="text-destructive">{{ matrixError }}</p>
            <p v-if="precheck.blockingReasons.length" class="text-amber-700">
              阻塞原因：{{ precheck.blockingReasons.join('；') }}
            </p>
            <p v-if="precheckReasonMismatch" class="text-destructive">
              离职原因已变化，请重新执行预检。
            </p>
          </div>
        </section>

        <section class="rounded-lg border p-4">
          <div class="mb-3">
            <h3 class="font-medium">2. 进入待交接</h3>
            <p class="text-sm text-muted-foreground">
              只有当前预检允许的服务端状态迁移才能进入待交接。
            </p>
          </div>
          <Button
            :disabled="
              busy || !precheckUsable || !canPrecheckAction(USER_LIFECYCLE_ACTION.DEPARTURE_START)
            "
            @click="beginDeparture"
          >
            进入待交接
          </Button>
        </section>

        <section class="rounded-lg border p-4">
          <div class="mb-3">
            <h3 class="font-medium">3. 六域责任交接确认</h3>
            <p class="text-sm text-muted-foreground">
              接收人及资格逐域来自服务端；活动、线索、客户、商机、跟进任务、试驾任一域失败均视为整体失败。
            </p>
          </div>
          <div v-if="precheck" class="grid gap-3 sm:grid-cols-2">
            <label
              v-for="item in responsibilitiesToTransfer"
              :key="item.resourceType"
              class="space-y-2 text-sm"
            >
              {{ item.resourceName }}（{{ item.count }}）
              <select
                v-model="targetEmployeeIds[item.resourceType]"
                :aria-label="`${item.resourceName}接收人`"
                class="field-select w-full"
              >
                <option value="">请选择接收人</option>
                <option
                  v-for="candidate in item.targetCandidates"
                  :key="candidate.id"
                  :value="String(candidate.id)"
                  :disabled="!candidate.eligible"
                >
                  {{ candidate.label
                  }}{{ candidate.secondaryLabel ? ` · ${candidate.secondaryLabel}` : '' }} ·
                  {{ candidate.qualificationName }}（{{ candidate.qualificationCode }}）{{
                    candidate.unavailableReason ? ` · ${candidate.unavailableReason}` : ''
                  }}
                </option>
              </select>
            </label>
          </div>
          <Button
            class="mt-3"
            :disabled="busy || !handoverSubmissionReady"
            @click="confirmHandover"
          >
            确认责任交接
          </Button>
          <div v-if="handoverResult" class="mt-3 space-y-2 text-sm">
            <div :class="exactHandoverSucceeded ? 'text-emerald-700' : 'text-destructive'">
              {{ handoverResult.resultName }}（{{ handoverResult.resultCode }}）
            </div>
            <div v-for="item in handoverResult.domainResults" :key="item.domainCode">
              {{ item.domainName }}：应交接 {{ item.expectedCount }}，已交接
              {{ item.transferredCount }}，{{ item.resultName }}（{{ item.resultCode }}）
            </div>
          </div>
        </section>

        <section class="rounded-lg border p-4">
          <div class="mb-3">
            <h3 class="font-medium">4. 完成离职</h3>
            <p class="text-sm text-muted-foreground">
              责任交接后必须重新预检；只有最新员工版本、最新预检和服务端迁移同时允许时才能完成。
            </p>
          </div>
          <Button
            variant="destructive"
            :disabled="
              busy ||
              !finalPrecheckReady ||
              !precheckUsable ||
              !canPrecheckAction(USER_LIFECYCLE_ACTION.DEPARTURE_COMPLETE)
            "
            @click="finishDeparture"
          >
            完成离职
          </Button>
        </section>

        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>

      <DialogFooter>
        <Button variant="outline" :disabled="busy" @click="emit('update:open', false)">关闭</Button>
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
import { Textarea } from '@/components/ui/textarea'
import {
  completeDeparture,
  confirmDepartureHandover,
  fetchUserLifecycleContext,
  precheckDeparture,
  startDeparture,
} from '@/modules/user/api/user-lifecycle-api'
import {
  getUserLifecycleErrorMessage,
  isUserLifecycleConflict,
  isUserLifecycleSnapshotExpired,
} from '@/modules/user/model/user-lifecycle-error'
import {
  DERIVED_HANDOVER_IMPACT,
  DIRECT_HANDOVER_RESOURCE_TYPES,
  HANDOVER_TRANSFER_MODE,
  USER_LIFECYCLE_ACTION,
  isDirectHandoverResourceType,
  type DeparturePrecheck,
  type DepartureResponsibilitySummary,
  type DirectHandoverResourceType,
  type HandoverResult,
  type UserLifecycleAction,
  type UserLifecycleContext,
} from '@/modules/user/model/user-lifecycle.types'
import type { EntityId } from '@/shared/types/id'

const props = defineProps<{ open: boolean; userId: EntityId }>()
const emit = defineEmits<{
  'update:open': [open: boolean]
  completed: [context: UserLifecycleContext]
}>()

const context = ref<UserLifecycleContext | null>(null)
const precheck = ref<DeparturePrecheck | null>(null)
const handoverResult = ref<HandoverResult | null>(null)
const loading = ref(false)
const busy = ref(false)
const errorMessage = ref('')
const reason = ref('')
const precheckReason = ref('')
const finalPrecheckReady = ref(false)
const currentTime = ref(Date.now())
const targetEmployeeIds = reactive<Partial<Record<DirectHandoverResourceType, string>>>({})
let controller: AbortController | null = null
let requestId = 0
let dialogEpoch = 0
let expiryTimer: ReturnType<typeof setTimeout> | null = null

function isDerivedImpact(value: string): boolean {
  return value === DERIVED_HANDOVER_IMPACT.QUOTE || value === DERIVED_HANDOVER_IMPACT.TRAN
}

const directResponsibilities = computed(() =>
  (precheck.value?.responsibilities ?? []).filter((item) =>
    isDirectHandoverResourceType(String(item.resourceType)),
  ),
)
const derivedImpacts = computed(() =>
  (precheck.value?.responsibilities ?? []).filter((item) =>
    isDerivedImpact(String(item.resourceType)),
  ),
)
const responsibilitiesToTransfer = computed(() =>
  directResponsibilities.value.filter((item) => item.count > 0),
)
const precheckExpired = computed(() => {
  const expiresAt = precheck.value ? Date.parse(precheck.value.expiresAt) : Number.NaN
  return precheck.value !== null && (!Number.isFinite(expiresAt) || expiresAt <= currentTime.value)
})
const precheckReasonMismatch = computed(
  () => precheck.value !== null && precheckReason.value !== reason.value.trim(),
)
const precheckUsable = computed(
  () => precheck.value !== null && !precheckExpired.value && !precheckReasonMismatch.value,
)
const matrixError = computed(() => {
  const responsibilities = precheck.value?.responsibilities ?? []
  const unsupported = responsibilities.filter((item) => {
    const resourceType = String(item.resourceType)
    if (isDirectHandoverResourceType(resourceType)) {
      return item.transferMode !== HANDOVER_TRANSFER_MODE.DIRECT_OWNER
    }
    if (isDerivedImpact(resourceType)) {
      return (
        item.transferMode !== HANDOVER_TRANSFER_MODE.DERIVED_IMPACT ||
        item.targetCandidates.length > 0
      )
    }
    return true
  })
  if (!unsupported.length) return ''
  return `服务端返回了不可直接交接的责任类型：${unsupported.map((item) => item.resourceType).join('、')}`
})
const precheckHasBlocking = computed(
  () =>
    (precheck.value?.blockingReasons.length ?? 0) > 0 ||
    directResponsibilities.value.some(
      (item) =>
        item.blocking ||
        item.blockedCount > 0 ||
        item.transferableCount !== item.count ||
        item.blockingReasons.length > 0,
    ) ||
    derivedImpacts.value.some((item) => item.blocking || item.blockingReasons.length > 0),
)
const allTargetsSelected = computed(
  () =>
    responsibilitiesToTransfer.value.length > 0 &&
    responsibilitiesToTransfer.value.every((item) => {
      const selected = targetEmployeeIds[item.resourceType as DirectHandoverResourceType]
      return item.targetCandidates.some(
        (candidate) => candidate.eligible && String(candidate.id) === selected,
      )
    }),
)
const handoverSubmissionReady = computed(
  () =>
    precheckUsable.value &&
    !matrixError.value &&
    !precheckHasBlocking.value &&
    allTargetsSelected.value &&
    canPrecheckAction(USER_LIFECYCLE_ACTION.HANDOVER_CONFIRM),
)
const exactHandoverSucceeded = computed(() =>
  handoverResult.value ? hasExactDomainResults(handoverResult.value) : false,
)

function hasTransition(
  actions: { action: UserLifecycleAction; fromStatus: string; disabledReason?: string | null }[],
  action: UserLifecycleAction,
  status: string,
): boolean {
  return actions.some(
    (item) => item.action === action && item.fromStatus === status && !item.disabledReason,
  )
}

function canContextAction(action: UserLifecycleAction): boolean {
  return Boolean(
    context.value?.allowedActions.includes(action) &&
    hasTransition(context.value.statusTransitions, action, context.value.employmentStatus),
  )
}

function canPrecheckAction(action: UserLifecycleAction): boolean {
  return Boolean(
    precheck.value?.allowedActions.includes(action) &&
    hasTransition(precheck.value.statusTransitions, action, precheck.value.employmentStatus),
  )
}

function contextActionReason(action: UserLifecycleAction): string {
  return (
    context.value?.unavailableReasons[action] ??
    context.value?.statusTransitions.find((item) => item.action === action)?.disabledReason ??
    '服务端未允许当前状态迁移'
  )
}

function requireReason(): boolean {
  if (reason.value.trim()) return true
  errorMessage.value = '请输入离职及交接原因'
  return false
}

function requireUsablePrecheck(): boolean {
  if (!precheck.value) return false
  currentTime.value = Date.now()
  if (precheckExpired.value) {
    errorMessage.value = '离职预检已过期，请重新执行预检'
    clearPrecheck()
    return false
  }
  if (precheckReasonMismatch.value) {
    errorMessage.value = '离职原因已变化，请重新执行预检'
    return false
  }
  return true
}

function clearTargets(): void {
  for (const resourceType of DIRECT_HANDOVER_RESOURCE_TYPES) {
    delete targetEmployeeIds[resourceType]
  }
}

function clearPrecheck(): void {
  if (expiryTimer !== null) clearTimeout(expiryTimer)
  expiryTimer = null
  precheck.value = null
  precheckReason.value = ''
  finalPrecheckReady.value = false
  clearTargets()
}

function scheduleSnapshotExpiry(expiresAt: string): void {
  currentTime.value = Date.now()
  const delay = Date.parse(expiresAt) - currentTime.value
  if (Number.isFinite(delay) && delay > 0 && delay <= 2_147_483_647) {
    expiryTimer = setTimeout(() => {
      currentTime.value = Date.now()
      expiryTimer = null
    }, delay + 5)
  }
}

function hasExactDomainResults(result: HandoverResult): boolean {
  if (!result.success || result.domainResults.length !== DIRECT_HANDOVER_RESOURCE_TYPES.length) {
    return false
  }
  const actualCodes = new Set(result.domainResults.map((item) => item.domainCode))
  if (
    actualCodes.size !== DIRECT_HANDOVER_RESOURCE_TYPES.length ||
    DIRECT_HANDOVER_RESOURCE_TYPES.some((domainCode) => !actualCodes.has(domainCode))
  ) {
    return false
  }
  const expectedCounts = new Map(
    directResponsibilities.value.map((item) => [String(item.resourceType), item.count]),
  )
  return result.domainResults.every(
    (item) =>
      item.resultCode === 'SUCCESS' &&
      item.expectedCount === (expectedCounts.get(item.domainCode) ?? 0) &&
      item.expectedCount === item.transferredCount,
  )
}

function isCurrent(epoch: number, userId: EntityId): boolean {
  return epoch === dialogEpoch && props.open && String(props.userId) === String(userId)
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
    errorMessage.value = getUserLifecycleErrorMessage(error, '加载离职事实失败')
  } finally {
    if (current === requestId) {
      loading.value = false
      controller = null
    }
  }
}

async function runPrecheck(): Promise<void> {
  if (
    !context.value ||
    busy.value ||
    !canContextAction(USER_LIFECYCLE_ACTION.DEPARTURE_PRECHECK) ||
    !requireReason()
  ) {
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  const submittedReason = reason.value.trim()
  const employeeVersion = context.value.employeeVersion
  busy.value = true
  errorMessage.value = ''
  handoverResult.value = null
  clearPrecheck()
  try {
    const result = await precheckDeparture(userId, { employeeVersion, reason: submittedReason })
    if (!isCurrent(epoch, userId)) return
    if (String(result.userId) !== String(userId)) {
      errorMessage.value = '离职预检返回了错误的目标员工，已拒绝继续'
      return
    }
    precheck.value = result
    precheckReason.value = submittedReason
    scheduleSnapshotExpiry(result.expiresAt)
    finalPrecheckReady.value =
      result.employmentStatus === 'HANDOVER' &&
      result.handoverCompleted &&
      result.readyToComplete &&
      result.allowedActions.includes(USER_LIFECYCLE_ACTION.DEPARTURE_COMPLETE) &&
      hasTransition(result.statusTransitions, USER_LIFECYCLE_ACTION.DEPARTURE_COMPLETE, 'HANDOVER')
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    errorMessage.value = getUserLifecycleErrorMessage(error, '离职预检失败')
    if (isUserLifecycleConflict(error)) await loadContext()
  } finally {
    if (isCurrent(epoch, userId)) busy.value = false
  }
}

async function beginDeparture(): Promise<void> {
  if (
    !precheck.value ||
    busy.value ||
    !requireUsablePrecheck() ||
    !canPrecheckAction(USER_LIFECYCLE_ACTION.DEPARTURE_START) ||
    !requireReason()
  ) {
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  const snapshot = precheck.value
  busy.value = true
  try {
    const result = await startDeparture(userId, {
      employeeVersion: snapshot.employeeVersion,
      snapshotToken: snapshot.snapshotToken,
      reason: reason.value.trim(),
    })
    if (!isCurrent(epoch, userId)) return
    context.value = result
    clearPrecheck()
    handoverResult.value = null
    errorMessage.value = '已进入待交接，请重新执行预检后确认责任交接'
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    await handleCommandError(error, '进入待交接失败')
  } finally {
    if (isCurrent(epoch, userId)) busy.value = false
  }
}

async function confirmHandover(): Promise<void> {
  if (
    !context.value ||
    !precheck.value ||
    busy.value ||
    !handoverSubmissionReady.value ||
    !requireUsablePrecheck() ||
    !requireReason()
  ) {
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  const snapshot = precheck.value
  busy.value = true
  errorMessage.value = ''
  try {
    const result = await confirmDepartureHandover(userId, {
      employeeVersion: snapshot.employeeVersion,
      snapshotToken: snapshot.snapshotToken,
      transfers: responsibilitiesToTransfer.value.map((item) => ({
        resourceType: item.resourceType as DirectHandoverResourceType,
        targetEmployeeId: targetEmployeeIds[item.resourceType as DirectHandoverResourceType]!,
      })),
      reason: reason.value.trim(),
    })
    if (!isCurrent(epoch, userId)) return
    handoverResult.value = result
    if (!hasExactDomainResults(result)) {
      errorMessage.value = '六域交接结果不完整、存在失败或数量不一致，未进入完成离职步骤'
      return
    }
    context.value = { ...context.value, employeeVersion: result.employeeVersion }
    clearPrecheck()
    errorMessage.value = '责任交接已完成，请重新预检确认全部事实后完成离职'
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    await handleCommandError(error, '责任交接失败，未修改离职状态')
  } finally {
    if (isCurrent(epoch, userId)) busy.value = false
  }
}

async function finishDeparture(): Promise<void> {
  if (
    !precheck.value ||
    busy.value ||
    !finalPrecheckReady.value ||
    !requireUsablePrecheck() ||
    !canPrecheckAction(USER_LIFECYCLE_ACTION.DEPARTURE_COMPLETE) ||
    !requireReason()
  ) {
    return
  }
  const epoch = dialogEpoch
  const userId = props.userId
  const snapshot = precheck.value
  busy.value = true
  try {
    const result = await completeDeparture(userId, {
      employeeVersion: snapshot.employeeVersion,
      snapshotToken: snapshot.snapshotToken,
      reason: reason.value.trim(),
    })
    if (!isCurrent(epoch, userId)) return
    context.value = result
    clearPrecheck()
    emit('completed', result)
    emit('update:open', false)
  } catch (error) {
    if (!isCurrent(epoch, userId)) return
    await handleCommandError(error, '完成离职失败，账号和任职未标记为已离职')
  } finally {
    if (isCurrent(epoch, userId)) busy.value = false
  }
}

async function handleCommandError(error: unknown, fallback: string): Promise<void> {
  errorMessage.value = getUserLifecycleErrorMessage(error, fallback)
  if (isUserLifecycleSnapshotExpired(error)) {
    clearPrecheck()
    return
  }
  if (isUserLifecycleConflict(error)) {
    clearPrecheck()
    await loadContext()
  }
}

function resetDialogState(): void {
  clearPrecheck()
  handoverResult.value = null
  context.value = null
  reason.value = ''
  errorMessage.value = ''
  loading.value = false
  busy.value = false
}

function changeOpen(open: boolean): void {
  if (!busy.value) emit('update:open', open)
}

watch(
  () => [props.open, props.userId] as const,
  ([open]) => {
    dialogEpoch += 1
    controller?.abort()
    controller = null
    requestId += 1
    resetDialogState()
    if (open) void loadContext()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  dialogEpoch += 1
  controller?.abort()
  clearPrecheck()
})
</script>

<style scoped>
.field-select {
  height: 2.25rem;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
}
</style>
