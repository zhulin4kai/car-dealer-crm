<template>
  <section class="space-y-4">
    <div>
      <h2 class="text-lg font-semibold">个人权限</h2>
      <p class="text-sm text-muted-foreground">
        最终权限由角色、个人增加和个人拒绝共同计算；个人拒绝优先。
      </p>
    </div>

    <Alert v-if="!editable">
      <ShieldAlert class="h-4 w-4" />
      <AlertTitle>个人权限不可编辑</AlertTitle>
      <AlertDescription>{{ disabledReason || '当前用户不允许调整该账号权限。' }}</AlertDescription>
    </Alert>

    <div
      v-if="!permissions.length"
      class="rounded-lg border py-10 text-center text-muted-foreground"
    >
      暂无权限数据
    </div>
    <div v-else class="space-y-3">
      <article
        v-for="permission in permissions"
        :key="permission.permissionId"
        class="space-y-3 rounded-lg border p-4"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-medium">{{ permission.name }}</span>
              <code class="text-xs text-muted-foreground">{{ permission.code }}</code>
              <Badge variant="outline">{{
                PERMISSION_SENSITIVITY_LABEL[permission.sensitivityLevel]
              }}</Badge>
              <Badge :variant="permission.effective ? 'default' : 'secondary'">
                {{ permission.effective ? '最终有效' : '最终无效' }}
              </Badge>
            </div>
            <p v-if="permission.description" class="mt-1 text-sm text-muted-foreground">
              {{ permission.description }}
            </p>
          </div>
          <span v-if="permission.unavailableReason" class="text-xs text-amber-700">
            {{ permission.unavailableReason }}
          </span>
        </div>

        <div class="space-y-2 rounded-md bg-muted/30 p-3 text-sm">
          <div class="font-medium">权限来源</div>
          <div v-if="!permission.sources.length" class="text-muted-foreground">暂无有效来源</div>
          <div
            v-for="(source, index) in permission.sources"
            v-else
            :key="`${source.type}-${source.sourceId ?? index}`"
            class="flex flex-wrap items-center gap-2"
          >
            <Badge variant="outline">{{ PERMISSION_SOURCE_TYPE_LABEL[source.type] }}</Badge>
            <span>{{ source.sourceName }}</span>
            <span v-if="source.dataScopeLabel" class="text-muted-foreground">
              {{ source.dataScopeLabel }}
            </span>
            <span v-if="source.organizationNames?.length" class="text-muted-foreground">
              组织：{{ source.organizationNames.join('、') }}
            </span>
            <span v-if="source.effectiveFrom" class="text-muted-foreground">
              {{ isFutureSource(source) ? '计划于' : '生效于' }}
              {{ formatDateTime(source.effectiveFrom) }}
            </span>
            <span v-if="source.effectiveTo" class="text-muted-foreground">
              有效至 {{ formatDateTime(source.effectiveTo) }}
            </span>
            <span v-if="!source.active" class="text-muted-foreground">
              {{ isFutureSource(source) ? '待生效' : '已失效' }}
            </span>
          </div>
        </div>

        <div>
          <div class="mb-2 text-sm font-medium">个人状态</div>
          <RadioGroup
            :model-value="draftFor(permission).state"
            class="flex flex-wrap gap-4"
            :disabled="submitting || !canEditPermission(permission)"
            @update:model-value="setState(permission, $event)"
          >
            <label
              v-for="state in personalStateOptions"
              :key="state.value"
              class="flex items-center gap-2 rounded-md border px-3 py-2 text-sm"
            >
              <RadioGroupItem
                :id="`permission-${permission.permissionId}-${state.value}`"
                :value="state.value"
              />
              {{ state.label }}
            </label>
          </RadioGroup>
        </div>

        <div
          v-if="draftFor(permission).state !== PERSONAL_PERMISSION_STATE.INHERIT"
          class="grid gap-3 sm:grid-cols-3"
        >
          <div
            v-if="draftFor(permission).state === PERSONAL_PERMISSION_STATE.GRANT"
            class="space-y-2 sm:col-span-3"
          >
            <Label :for="`permission-scope-${permission.permissionId}`">数据范围</Label>
            <select
              :id="`permission-scope-${permission.permissionId}`"
              :value="draftFor(permission).dataScopeCandidateKey"
              class="scope-select"
              :disabled="submitting || !canEditPermission(permission)"
              @change="setDataScope(permission, ($event.target as HTMLSelectElement).value)"
            >
              <option value="">请选择服务端允许的数据范围</option>
              <option
                v-for="candidate in scopeChoices(permission)"
                :key="candidate.candidateKey"
                :value="candidate.candidateKey"
              >
                {{ candidate.label }}
                <template v-if="candidate.organizationNames?.length">
                  · {{ candidate.organizationNames.join('、') }}
                </template>
              </option>
            </select>
          </div>
          <fieldset
            v-if="isCustomScope(permission)"
            class="space-y-2 rounded-md border p-3 sm:col-span-3"
          >
            <legend class="px-1 text-sm font-medium">指定组织</legend>
            <p class="text-xs text-muted-foreground">
              只能选择服务端返回的可委派组织；这不是角色的适用组织范围。
            </p>
            <div class="grid gap-2 sm:grid-cols-2">
              <label
                v-for="option in customOrganizationOptions(permission)"
                :key="option.id"
                class="flex items-center gap-2 rounded border p-2 text-sm"
              >
                <Checkbox
                  :checked="draftFor(permission).customOrganizationUnitIds.includes(option.id)"
                  :disabled="submitting || !canEditPermission(permission)"
                  @update:checked="toggleCustomOrganization(permission, option.id, $event === true)"
                />
                {{ option.name }}
              </label>
            </div>
          </fieldset>
          <div class="flex items-center gap-2 sm:col-span-3">
            <Checkbox
              :id="`permission-immediate-${permission.permissionId}`"
              :checked="draftFor(permission).immediate"
              :disabled="submitting || !canEditPermission(permission)"
              @update:checked="setImmediate(permission, $event === true)"
            />
            <Label :for="`permission-immediate-${permission.permissionId}`">立即生效</Label>
          </div>
          <div class="space-y-2">
            <Label :for="`permission-from-${permission.permissionId}`">预约生效时间</Label>
            <Input
              :id="`permission-from-${permission.permissionId}`"
              :model-value="draftFor(permission).effectiveFrom"
              type="datetime-local"
              :min="minimumEffectiveFrom"
              :max="maximumEffectiveFrom"
              :disabled="
                submitting || !canEditPermission(permission) || draftFor(permission).immediate
              "
              @update:model-value="setEffectiveFrom(permission, String($event))"
            />
            <p class="text-xs text-muted-foreground">取消“立即生效”后，可预约一年内的时间。</p>
          </div>
          <div class="space-y-2">
            <Label :for="`permission-to-${permission.permissionId}`">失效时间（可选）</Label>
            <Input
              :id="`permission-to-${permission.permissionId}`"
              :model-value="draftFor(permission).effectiveTo"
              type="datetime-local"
              :disabled="submitting || !canEditPermission(permission)"
              @update:model-value="setEffectiveTo(permission, String($event))"
            />
          </div>
        </div>
        <p
          v-if="validationErrors[String(permission.permissionId)]"
          class="text-sm text-destructive"
        >
          {{ validationErrors[String(permission.permissionId)] }}
        </p>
      </article>
    </div>

    <div v-if="changes.length" class="space-y-3 rounded-lg border bg-muted/20 p-4">
      <h3 class="font-medium">个人权限差异</h3>
      <ul class="list-disc space-y-1 pl-5 text-sm">
        <li v-for="change in changeDescriptions" :key="change">{{ change }}</li>
      </ul>
      <div class="space-y-2">
        <Label for="personal-permission-reason">调整原因</Label>
        <Textarea id="personal-permission-reason" v-model="reason" :rows="3" />
      </div>
      <div v-if="editable" class="flex justify-end">
        <Button :disabled="!canSave" @click="submitChanges">
          {{ submitting ? '保存中...' : '保存个人权限' }}
        </Button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ShieldAlert } from '@lucide/vue'
import { computed, ref, watch } from 'vue'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import {
  DATA_SCOPE_CODE,
  PERMISSION_SENSITIVITY_LABEL,
  type DataScopeCode,
} from '@/modules/access/model/access.types'
import {
  PERSONAL_PERMISSION_STATE,
  PERSONAL_PERMISSION_STATE_LABEL,
  PERMISSION_SOURCE_TYPE_LABEL,
  type PersonalPermissionState,
  type UpdateUserPermissionsRequest,
  type UserPermissionAuthorizationItem,
  type UserPermissionChangeInput,
  type UserPermissionSource,
} from '@/modules/access/model/user-permission.types'
import { formatDateTime } from '@/shared/utils/display-format'

interface PermissionDraft {
  state: PersonalPermissionState
  dataScopeCandidateKey: string
  immediate: boolean
  effectiveFrom: string
  effectiveTo: string
  customOrganizationUnitIds: Array<number | string>
}

const props = withDefaults(
  defineProps<{
    permissions: UserPermissionAuthorizationItem[]
    authorizationVersion: number
    editable: boolean
    disabledReason?: string
    submitting?: boolean
  }>(),
  { disabledReason: '', submitting: false },
)

const emit = defineEmits<{
  save: [request: UpdateUserPermissionsRequest]
}>()

const drafts = ref<Record<string, PermissionDraft>>({})
const reason = ref('')
const scheduleStartedAt = new Date()
const minimumEffectiveFrom = nextMinuteInputValue(scheduleStartedAt)
const maximumEffectiveFrom = oneYearLaterInputValue(scheduleStartedAt)
const personalStateOptions = Object.values(PERSONAL_PERMISSION_STATE).map((value) => ({
  value,
  label: PERSONAL_PERMISSION_STATE_LABEL[value],
}))
const validationErrors = computed<Record<string, string>>(() => {
  const errors: Record<string, string> = {}
  props.permissions.forEach((permission) => {
    const draft = draftFor(permission)
    if (draft.state === PERSONAL_PERMISSION_STATE.INHERIT) return
    if (draft.state === PERSONAL_PERMISSION_STATE.GRANT && !draft.dataScopeCandidateKey) {
      errors[String(permission.permissionId)] = '个人增加权限必须选择服务端允许的数据范围'
      return
    }
    if (
      draft.state === PERSONAL_PERMISSION_STATE.GRANT &&
      isCustomScope(permission) &&
      !draft.customOrganizationUnitIds.length
    ) {
      errors[String(permission.permissionId)] = '指定组织范围必须至少选择一个可委派组织'
      return
    }
    if (!draft.immediate && !draft.effectiveFrom) {
      errors[String(permission.permissionId)] = '个人增加或拒绝必须设置生效时间'
      return
    }
    const effectiveFrom = draft.immediate
      ? scheduleStartedAt.getTime()
      : Date.parse(draft.effectiveFrom)
    if (!draft.immediate && effectiveFrom < scheduleStartedAt.getTime()) {
      errors[String(permission.permissionId)] = '生效时间不能早于当前时间'
      return
    }
    const maximum = new Date(scheduleStartedAt)
    maximum.setFullYear(maximum.getFullYear() + 1)
    if (effectiveFrom > maximum.getTime()) {
      errors[String(permission.permissionId)] = '预约生效时间不能超过一年'
      return
    }
    if (draft.effectiveTo && Date.parse(draft.effectiveTo) <= effectiveFrom) {
      errors[String(permission.permissionId)] = '失效时间必须晚于生效时间'
    }
  })
  return errors
})
const changes = computed<UserPermissionChangeInput[]>(() =>
  props.permissions.filter(isChanged).map(toChangeInput),
)
const changeDescriptions = computed(() =>
  props.permissions
    .filter(isChanged)
    .map(
      (permission) =>
        `${permission.name}：${PERSONAL_PERMISSION_STATE_LABEL[permission.personalState]} → ${PERSONAL_PERMISSION_STATE_LABEL[draftFor(permission).state]}`,
    ),
)
const canSave = computed(
  () =>
    props.editable &&
    changes.value.length > 0 &&
    Object.keys(validationErrors.value).length === 0 &&
    Boolean(reason.value.trim()) &&
    !props.submitting,
)

function canEditPermission(permission: UserPermissionAuthorizationItem): boolean {
  return props.editable && permission.editable
}

function draftFor(permission: UserPermissionAuthorizationItem): PermissionDraft {
  return drafts.value[String(permission.permissionId)] ?? emptyDraft()
}

function setState(permission: UserPermissionAuthorizationItem, value: unknown): void {
  if (!canEditPermission(permission) || !isPersonalPermissionState(value)) return
  const draft = draftFor(permission)
  drafts.value[String(permission.permissionId)] = {
    ...draft,
    state: value,
    immediate:
      value === PERSONAL_PERMISSION_STATE.INHERIT
        ? true
        : value === draft.state
          ? draft.immediate
          : true,
    effectiveFrom:
      value === PERSONAL_PERMISSION_STATE.INHERIT
        ? ''
        : value === draft.state
          ? draft.effectiveFrom
          : '',
    dataScopeCandidateKey:
      value === PERSONAL_PERMISSION_STATE.GRANT ? draft.dataScopeCandidateKey : '',
    effectiveTo:
      value === PERSONAL_PERMISSION_STATE.INHERIT || value !== draft.state ? '' : draft.effectiveTo,
    customOrganizationUnitIds:
      value === PERSONAL_PERMISSION_STATE.GRANT ? draft.customOrganizationUnitIds : [],
  }
}

function setDataScope(permission: UserPermissionAuthorizationItem, value: string): void {
  if (!canEditPermission(permission)) return
  drafts.value[String(permission.permissionId)] = {
    ...draftFor(permission),
    dataScopeCandidateKey: value,
    effectiveFrom: schedulableEffectiveFrom(draftFor(permission).effectiveFrom),
    customOrganizationUnitIds:
      scopeCode(permission, value) === DATA_SCOPE_CODE.CUSTOM_ORGS
        ? draftFor(permission).customOrganizationUnitIds
        : [],
  }
}

function toggleCustomOrganization(
  permission: UserPermissionAuthorizationItem,
  organizationId: number | string,
  checked: boolean,
): void {
  if (!canEditPermission(permission) || !isCustomScope(permission)) return
  const draft = draftFor(permission)
  const selected = new Map(draft.customOrganizationUnitIds.map((id) => [String(id), id] as const))
  if (checked) selected.set(String(organizationId), organizationId)
  else selected.delete(String(organizationId))
  drafts.value[String(permission.permissionId)] = {
    ...draft,
    effectiveFrom: schedulableEffectiveFrom(draft.effectiveFrom),
    customOrganizationUnitIds: [...selected.values()],
  }
}

function setEffectiveFrom(permission: UserPermissionAuthorizationItem, value: string): void {
  if (!canEditPermission(permission)) return
  drafts.value[String(permission.permissionId)] = { ...draftFor(permission), effectiveFrom: value }
}

function setImmediate(permission: UserPermissionAuthorizationItem, immediate: boolean): void {
  if (!canEditPermission(permission)) return
  drafts.value[String(permission.permissionId)] = {
    ...draftFor(permission),
    immediate,
    effectiveFrom: immediate ? '' : schedulableEffectiveFrom(draftFor(permission).effectiveFrom),
  }
}

function setEffectiveTo(permission: UserPermissionAuthorizationItem, value: string): void {
  if (!canEditPermission(permission)) return
  drafts.value[String(permission.permissionId)] = { ...draftFor(permission), effectiveTo: value }
}

function isChanged(permission: UserPermissionAuthorizationItem): boolean {
  const draft = draftFor(permission)
  if (draft.state === PERSONAL_PERMISSION_STATE.INHERIT) {
    return (
      draft.state !== permission.personalState ||
      Boolean(permission.personalDataScopeCandidateKey) ||
      Boolean(permission.personalOrganizationIds?.length) ||
      Boolean(permission.personalEffectiveFrom) ||
      Boolean(permission.personalEffectiveTo)
    )
  }
  return (
    draft.state !== permission.personalState ||
    draft.dataScopeCandidateKey !== (permission.personalDataScopeCandidateKey ?? '') ||
    draft.effectiveFrom !== toInputValue(permission.personalEffectiveFrom) ||
    draft.effectiveTo !== toInputValue(permission.personalEffectiveTo) ||
    organizationKey(draft.customOrganizationUnitIds) !==
      organizationKey(permission.personalOrganizationIds ?? [])
  )
}

function toChangeInput(permission: UserPermissionAuthorizationItem): UserPermissionChangeInput {
  const draft = draftFor(permission)
  if (draft.state === PERSONAL_PERMISSION_STATE.INHERIT) {
    return { permissionId: permission.permissionId, state: draft.state }
  }
  return {
    permissionId: permission.permissionId,
    state: draft.state,
    ...(draft.state === PERSONAL_PERMISSION_STATE.GRANT
      ? { dataScopeCandidateKey: draft.dataScopeCandidateKey }
      : {}),
    ...(draft.state === PERSONAL_PERMISSION_STATE.GRANT && isCustomScope(permission)
      ? { customOrganizationUnitIds: [...draft.customOrganizationUnitIds] }
      : {}),
    ...(!draft.immediate ? { effectiveFrom: toIsoDateTime(draft.effectiveFrom) } : {}),
    ...(draft.effectiveTo ? { effectiveTo: toIsoDateTime(draft.effectiveTo) } : {}),
  }
}

function submitChanges(): void {
  if (!canSave.value) return
  emit('save', {
    authorizationVersion: props.authorizationVersion,
    changes: changes.value,
    reason: reason.value.trim(),
  })
}

function resetDrafts(permissions: UserPermissionAuthorizationItem[]): void {
  drafts.value = Object.fromEntries(
    permissions.map((permission) => [
      String(permission.permissionId),
      {
        state: permission.personalState,
        dataScopeCandidateKey: normalizeCandidateKey(permission),
        immediate:
          !permission.personalEffectiveFrom ||
          Date.parse(permission.personalEffectiveFrom) <= scheduleStartedAt.getTime(),
        effectiveFrom: toInputValue(permission.personalEffectiveFrom),
        effectiveTo: toInputValue(permission.personalEffectiveTo),
        customOrganizationUnitIds:
          permission.personalState === PERSONAL_PERMISSION_STATE.GRANT
            ? [...(permission.personalOrganizationIds ?? [])]
            : [],
      },
    ]),
  )
  reason.value = ''
}

function emptyDraft(): PermissionDraft {
  return {
    state: PERSONAL_PERMISSION_STATE.INHERIT,
    dataScopeCandidateKey: '',
    immediate: true,
    effectiveFrom: '',
    effectiveTo: '',
    customOrganizationUnitIds: [],
  }
}

function scopeChoices(permission: UserPermissionAuthorizationItem) {
  const choices = permission.dataScopeCandidates.filter(
    (candidate) => candidate.code !== DATA_SCOPE_CODE.CUSTOM_ORGS,
  )
  if (
    permission.dataScopeCandidates.some(
      (candidate) => candidate.code === DATA_SCOPE_CODE.CUSTOM_ORGS,
    )
  ) {
    choices.push({
      candidateKey: DATA_SCOPE_CODE.CUSTOM_ORGS,
      code: DATA_SCOPE_CODE.CUSTOM_ORGS,
      label: '指定组织',
    })
  }
  return choices
}

function customOrganizationOptions(permission: UserPermissionAuthorizationItem) {
  const options = new Map<string, { id: number | string; name: string }>()
  permission.dataScopeCandidates
    .filter((candidate) => candidate.code === DATA_SCOPE_CODE.CUSTOM_ORGS)
    .forEach((candidate) => {
      ;(candidate.organizationIds ?? []).forEach((id, index) => {
        options.set(String(id), {
          id,
          name: candidate.organizationNames?.[index] ?? `组织 ${id}`,
        })
      })
    })
  return [...options.values()]
}

function scopeCode(
  permission: UserPermissionAuthorizationItem,
  candidateKey: string,
): DataScopeCode | null {
  if (candidateKey === DATA_SCOPE_CODE.CUSTOM_ORGS || candidateKey.startsWith('CUSTOM_ORGS:')) {
    return DATA_SCOPE_CODE.CUSTOM_ORGS
  }
  return (
    permission.dataScopeCandidates.find((candidate) => candidate.candidateKey === candidateKey)
      ?.code ?? null
  )
}

function isCustomScope(permission: UserPermissionAuthorizationItem): boolean {
  return (
    draftFor(permission).state === PERSONAL_PERMISSION_STATE.GRANT &&
    scopeCode(permission, draftFor(permission).dataScopeCandidateKey) ===
      DATA_SCOPE_CODE.CUSTOM_ORGS
  )
}

function normalizeCandidateKey(permission: UserPermissionAuthorizationItem): string {
  const key = permission.personalDataScopeCandidateKey ?? ''
  return key.startsWith('CUSTOM_ORGS:') ? DATA_SCOPE_CODE.CUSTOM_ORGS : key
}

function organizationKey(ids: Array<number | string>): string {
  return [...new Set(ids.map(String))].sort().join(',')
}

function isPersonalPermissionState(value: unknown): value is PersonalPermissionState {
  return Object.values(PERSONAL_PERMISSION_STATE).some((state) => state === value)
}

function isFutureSource(source: UserPermissionSource): boolean {
  return Boolean(
    source.effectiveFrom && Date.parse(source.effectiveFrom) > scheduleStartedAt.getTime(),
  )
}

function schedulableEffectiveFrom(value: string): string {
  return value && Date.parse(value) >= scheduleStartedAt.getTime() ? value : minimumEffectiveFrom
}

function nextMinuteInputValue(value: Date): string {
  const nextMinute = new Date(value)
  nextMinute.setSeconds(0, 0)
  nextMinute.setMinutes(nextMinute.getMinutes() + 1)
  return localInputValue(nextMinute)
}

function oneYearLaterInputValue(value: Date): string {
  const maximum = new Date(value)
  maximum.setFullYear(maximum.getFullYear() + 1)
  return localInputValue(maximum)
}

function localInputValue(value: Date): string {
  return new Date(value.getTime() - value.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

function toInputValue(value?: string | null): string {
  if (!value) return ''
  return localInputValue(new Date(value))
}

function toIsoDateTime(value: string): string {
  return new Date(value).toISOString()
}

watch(
  () => props.permissions,
  (permissions) => resetDrafts(permissions),
  { immediate: true },
)
</script>

<style scoped>
.scope-select {
  height: 2.25rem;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
}
</style>
