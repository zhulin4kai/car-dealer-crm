<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
      <DialogHeader>
        <DialogTitle>{{ mode === 'roles' ? '批量调整角色' : '批量调整个人权限' }}</DialogTitle>
        <DialogDescription>
          将对 {{ details.length }} 名用户执行同一项调整。任一用户校验失败时，整批操作都会回滚。
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-4">
        <div class="rounded-md border bg-muted/20 p-3 text-sm">
          <span class="font-medium">目标用户：</span>
          {{ details.map((detail) => detail.user.name).join('、') }}
        </div>

        <template v-if="mode === 'roles'">
          <div class="space-y-2">
            <Label for="batch-role-operation">操作</Label>
            <select id="batch-role-operation" v-model="roleOperation" class="form-select">
              <option value="ASSIGN">添加角色</option>
              <option value="UNASSIGN">撤销角色</option>
            </select>
          </div>
          <fieldset class="space-y-2 rounded-md border p-3">
            <legend class="px-1 text-sm font-medium">全部目标共同可调整的角色</legend>
            <p v-if="!sharedRoleCandidates.length" class="text-sm text-muted-foreground">
              所选用户没有共同可调整的角色，请缩小选择范围。
            </p>
            <label
              v-for="role in sharedRoleCandidates"
              v-else
              :key="role.roleId"
              class="flex items-start gap-2 rounded border p-2 text-sm"
            >
              <Checkbox
                :checked="selectedRoleIds.includes(role.roleId)"
                :disabled="submitting"
                :aria-label="`批量选择角色${role.roleName}`"
                @update:checked="toggleRole(role.roleId, $event === true)"
              />
              <span>
                <span class="font-medium">{{ role.roleName }}</span>
                <code class="ml-2 text-xs text-muted-foreground">{{ role.roleCode }}</code>
              </span>
            </label>
          </fieldset>
        </template>

        <template v-else>
          <div class="space-y-2">
            <Label for="batch-permission">权限</Label>
            <select id="batch-permission" v-model="permissionId" class="form-select">
              <option value="">请选择全部目标共同可调整的权限</option>
              <option
                v-for="permission in sharedPermissions"
                :key="permission.permissionId"
                :value="String(permission.permissionId)"
              >
                {{ permission.name }}（{{ permission.code }}）
              </option>
            </select>
          </div>
          <div class="space-y-2">
            <Label for="batch-permission-state">个人状态</Label>
            <select id="batch-permission-state" v-model="permissionState" class="form-select">
              <option value="INHERIT">继承角色</option>
              <option value="GRANT">个人增加</option>
              <option value="DENY">个人拒绝</option>
            </select>
          </div>
          <div v-if="permissionState === PERSONAL_PERMISSION_STATE.GRANT" class="space-y-2">
            <Label for="batch-permission-scope">数据范围</Label>
            <select id="batch-permission-scope" v-model="scopeKey" class="form-select">
              <option value="">请选择全部目标共同允许的数据范围</option>
              <option
                v-for="scope in sharedScopes"
                :key="scope.candidateKey"
                :value="scope.candidateKey"
              >
                {{ scope.label }}
              </option>
            </select>
          </div>
          <fieldset
            v-if="permissionState === PERSONAL_PERMISSION_STATE.GRANT && scopeKey === 'CUSTOM_ORGS'"
            class="space-y-2 rounded-md border p-3"
          >
            <legend class="px-1 text-sm font-medium">共同可委派组织</legend>
            <label
              v-for="organization in sharedCustomOrganizations"
              :key="organization.id"
              class="flex items-center gap-2 rounded border p-2 text-sm"
            >
              <Checkbox
                :checked="customOrganizationIds.includes(organization.id)"
                :disabled="submitting"
                :aria-label="`批量选择组织${organization.name}`"
                @update:checked="toggleOrganization(organization.id, $event === true)"
              />
              {{ organization.name }}
            </label>
          </fieldset>
          <div
            v-if="permissionState !== PERSONAL_PERMISSION_STATE.INHERIT"
            class="grid gap-3 sm:grid-cols-2"
          >
            <label class="flex items-center gap-2 sm:col-span-2">
              <Checkbox
                :checked="immediate"
                :disabled="submitting"
                aria-label="批量权限立即生效"
                @update:checked="immediate = $event === true"
              />
              <span class="text-sm">立即生效</span>
            </label>
            <div v-if="!immediate" class="space-y-2">
              <Label for="batch-effective-from">预约生效时间</Label>
              <Input id="batch-effective-from" v-model="effectiveFrom" type="datetime-local" />
            </div>
            <div class="space-y-2">
              <Label for="batch-effective-to">失效时间（可选）</Label>
              <Input id="batch-effective-to" v-model="effectiveTo" type="datetime-local" />
            </div>
          </div>
        </template>

        <div class="space-y-2">
          <Label for="batch-authorization-reason">调整原因</Label>
          <Textarea
            id="batch-authorization-reason"
            v-model="reason"
            :rows="3"
            placeholder="请说明批量调整原因"
          />
        </div>
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>

      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">
          取消
        </Button>
        <Button :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '确认批量调整' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
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
import { Textarea } from '@/components/ui/textarea'
import {
  PERSONAL_PERMISSION_STATE,
  type BatchRoleOperation,
  type BatchUpdateUserPermissionsRequest,
  type BatchUpdateUserRolesRequest,
  type DelegableDataScopeCandidate,
  type PersonalPermissionState,
  type UserAuthorizationDetail,
} from '@/modules/access/model/user-permission.types'
import type { EntityId } from '@/shared/types/id'

const props = withDefaults(
  defineProps<{
    open: boolean
    mode: 'roles' | 'permissions'
    details: UserAuthorizationDetail[]
    submitting?: boolean
  }>(),
  { submitting: false },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  'save-roles': [request: BatchUpdateUserRolesRequest]
  'save-permissions': [request: BatchUpdateUserPermissionsRequest]
}>()

const roleOperation = ref<BatchRoleOperation>('ASSIGN')
const selectedRoleIds = ref<EntityId[]>([])
const permissionId = ref('')
const permissionState = ref<PersonalPermissionState>(PERSONAL_PERMISSION_STATE.INHERIT)
const scopeKey = ref('')
const customOrganizationIds = ref<EntityId[]>([])
const immediate = ref(true)
const effectiveFrom = ref('')
const effectiveTo = ref('')
const reason = ref('')
const errorMessage = ref('')

const targets = computed(() =>
  props.details.map((detail) => ({
    userId: detail.user.id,
    authorizationVersion: detail.authorizationVersion,
  })),
)

const sharedRoleCandidates = computed(() => {
  const first = props.details[0]?.roleCandidates ?? []
  return first.filter(
    (candidate) =>
      candidate.editable &&
      props.details.every((detail) =>
        detail.roleCandidates.some(
          (item) => String(item.roleId) === String(candidate.roleId) && item.editable,
        ),
      ),
  )
})

const sharedPermissions = computed(() => {
  const first = props.details[0]?.permissions ?? []
  return first.filter(
    (permission) =>
      permission.editable &&
      props.details.every((detail) =>
        detail.permissions.some(
          (item) => String(item.permissionId) === String(permission.permissionId) && item.editable,
        ),
      ),
  )
})

const sharedCustomOrganizations = computed(() => {
  const firstPermission = props.details[0]?.permissions.find(
    (item) => String(item.permissionId) === permissionId.value,
  )
  if (!firstPermission) return []
  const firstOptions = customOrganizationOptions(firstPermission.dataScopeCandidates)
  return firstOptions.filter((organization) =>
    props.details.every((detail) => {
      const permission = detail.permissions.find(
        (item) => String(item.permissionId) === permissionId.value,
      )
      return customOrganizationOptions(permission?.dataScopeCandidates ?? []).some(
        (item) => String(item.id) === String(organization.id),
      )
    }),
  )
})

const sharedScopes = computed<DelegableDataScopeCandidate[]>(() => {
  const selected = sharedPermissions.value.find(
    (permission) => String(permission.permissionId) === permissionId.value,
  )
  if (!selected) return []
  const choices = selected.dataScopeCandidates
    .filter((candidate) => candidate.code !== 'CUSTOM_ORGS')
    .filter((candidate) =>
      props.details.every((detail) => {
        const permission = detail.permissions.find(
          (item) => String(item.permissionId) === permissionId.value,
        )
        return permission?.dataScopeCandidates.some(
          (item) => item.candidateKey === candidate.candidateKey,
        )
      }),
    )
  if (sharedCustomOrganizations.value.length) {
    choices.push({
      candidateKey: 'CUSTOM_ORGS',
      code: 'CUSTOM_ORGS',
      label: '指定组织',
    })
  }
  return choices
})

function customOrganizationOptions(candidates: DelegableDataScopeCandidate[]) {
  const options = new Map<string, { id: EntityId; name: string }>()
  candidates
    .filter((candidate) => candidate.code === 'CUSTOM_ORGS')
    .forEach((candidate) =>
      (candidate.organizationIds ?? []).forEach((id, index) =>
        options.set(String(id), {
          id,
          name: candidate.organizationNames?.[index] ?? `组织 ${id}`,
        }),
      ),
    )
  return [...options.values()]
}

watch(
  () => [props.open, props.mode, props.details] as const,
  ([open]) => {
    if (!open) return
    roleOperation.value = 'ASSIGN'
    selectedRoleIds.value = []
    permissionId.value = ''
    permissionState.value = PERSONAL_PERMISSION_STATE.INHERIT
    scopeKey.value = ''
    customOrganizationIds.value = []
    immediate.value = true
    effectiveFrom.value = ''
    effectiveTo.value = ''
    reason.value = ''
    errorMessage.value = ''
  },
)

watch(permissionId, () => {
  scopeKey.value = ''
  customOrganizationIds.value = []
})
watch(scopeKey, () => {
  customOrganizationIds.value = []
})

function changeOpen(open: boolean): void {
  if (!props.submitting) emit('update:open', open)
}

function toggleRole(roleId: EntityId, checked: boolean): void {
  selectedRoleIds.value = checked
    ? [...selectedRoleIds.value, roleId]
    : selectedRoleIds.value.filter((item) => String(item) !== String(roleId))
}

function toggleOrganization(organizationId: EntityId, checked: boolean): void {
  customOrganizationIds.value = checked
    ? [...customOrganizationIds.value, organizationId]
    : customOrganizationIds.value.filter((item) => String(item) !== String(organizationId))
}

function submit(): void {
  errorMessage.value = ''
  if (!targets.value.length) {
    errorMessage.value = '请选择至少一个目标用户'
    return
  }
  if (!reason.value.trim()) {
    errorMessage.value = '请填写批量调整原因'
    return
  }
  if (props.mode === 'roles') {
    if (!selectedRoleIds.value.length) {
      errorMessage.value = '请至少选择一个角色'
      return
    }
    emit('save-roles', {
      targets: targets.value,
      operation: roleOperation.value,
      roleIds: selectedRoleIds.value,
      reason: reason.value.trim(),
    })
    return
  }
  const selectedPermission = sharedPermissions.value.find(
    (permission) => String(permission.permissionId) === permissionId.value,
  )
  if (!selectedPermission) {
    errorMessage.value = '请选择一个共同可调整的权限'
    return
  }
  if (permissionState.value === PERSONAL_PERMISSION_STATE.GRANT && !scopeKey.value) {
    errorMessage.value = '个人增加权限必须选择共同允许的数据范围'
    return
  }
  if (
    permissionState.value === PERSONAL_PERMISSION_STATE.GRANT &&
    scopeKey.value === 'CUSTOM_ORGS' &&
    !customOrganizationIds.value.length
  ) {
    errorMessage.value = '指定组织范围必须至少选择一个共同可委派组织'
    return
  }
  if (
    permissionState.value !== PERSONAL_PERMISSION_STATE.INHERIT &&
    !immediate.value &&
    !effectiveFrom.value
  ) {
    errorMessage.value = '预约权限必须填写生效时间'
    return
  }
  const fromTime = immediate.value ? Date.now() : Date.parse(effectiveFrom.value)
  if (effectiveTo.value && Date.parse(effectiveTo.value) <= fromTime) {
    errorMessage.value = '失效时间必须晚于生效时间'
    return
  }
  const change = {
    permissionId: selectedPermission.permissionId,
    state: permissionState.value,
    ...(permissionState.value === PERSONAL_PERMISSION_STATE.GRANT
      ? { dataScopeCandidateKey: scopeKey.value }
      : {}),
    ...(scopeKey.value === 'CUSTOM_ORGS'
      ? { customOrganizationUnitIds: customOrganizationIds.value }
      : {}),
    ...(permissionState.value !== PERSONAL_PERMISSION_STATE.INHERIT && !immediate.value
      ? { effectiveFrom: new Date(effectiveFrom.value).toISOString() }
      : {}),
    ...(permissionState.value !== PERSONAL_PERMISSION_STATE.INHERIT && effectiveTo.value
      ? { effectiveTo: new Date(effectiveTo.value).toISOString() }
      : {}),
  }
  emit('save-permissions', {
    targets: targets.value,
    changes: [change],
    reason: reason.value.trim(),
  })
}
</script>

<style scoped>
.form-select {
  height: 2.25rem;
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid var(--crm-border-light);
  background: var(--crm-bg-panel);
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
</style>
