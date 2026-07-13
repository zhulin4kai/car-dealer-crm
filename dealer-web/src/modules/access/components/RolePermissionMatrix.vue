<template>
  <div class="flex h-full min-h-0 flex-col">
    <Alert v-if="!matrix.editable" class="mb-3">
      <ShieldAlert class="h-4 w-4" />
      <AlertTitle>当前角色不可编辑</AlertTitle>
      <AlertDescription>{{ matrix.disabledReason || '该角色受系统保护。' }}</AlertDescription>
    </Alert>

    <ScrollArea class="min-h-0 flex-1 pr-3">
      <div class="space-y-2">
        <PermissionMatrixNode
          v-for="node in catalog"
          :key="node.id"
          :node="node"
          :selected-ids="selectedPermissionIds"
          :disabled="!matrix.editable || previewing || saving"
          @toggle="togglePermission"
        />
      </div>
    </ScrollArea>

    <div class="mt-4 space-y-3 border-t pt-4">
      <div class="flex flex-wrap items-center justify-between gap-2 text-sm">
        <span>已选 {{ selectedPermissionIds.length }} 项权限</span>
        <span v-if="isDirty" class="text-amber-700">存在未预览的变更</span>
      </div>

      <section v-if="selectedPermissionIds.length" class="space-y-3 rounded-lg border p-3">
        <div>
          <h3 class="font-medium">每项权限的数据范围</h3>
          <p class="text-xs text-muted-foreground">
            指定组织属于该权限来源，不使用角色“适用组织”代替。
          </p>
        </div>
        <div
          v-for="permissionId in selectedPermissionIds"
          :key="permissionId"
          class="space-y-2 rounded-md bg-muted/30 p-3 text-sm"
        >
          <div class="flex items-center justify-between gap-2">
            <span class="font-medium">{{ permissionName(permissionId) }}</span>
            <span v-if="scopeOption(permissionId)?.unavailableReason" class="text-amber-700">
              {{ scopeOption(permissionId)?.unavailableReason }}
            </span>
          </div>
          <template v-if="scopeOption(permissionId)">
            <select
              :aria-label="`${permissionName(permissionId)}数据范围`"
              class="scope-select"
              :value="scopeAssignment(permissionId)?.dataScopeCode ?? ''"
              :disabled="!canEditScope(permissionId) || previewing || saving"
              @change="setPermissionScope(permissionId, ($event.target as HTMLSelectElement).value as DataScopeCode)"
            >
              <option value="">请选择数据范围</option>
              <option
                v-for="candidate in scopeOption(permissionId)?.dataScopeCandidates ?? []"
                :key="candidate.code"
                :value="candidate.code"
              >
                {{ candidate.label }}
              </option>
            </select>
            <div
              v-if="scopeAssignment(permissionId)?.dataScopeCode === DATA_SCOPE_CODE.CUSTOM_ORGS"
              class="grid gap-2 sm:grid-cols-2"
            >
              <label
                v-for="organization in customOrganizations(permissionId)"
                :key="organization.id"
                class="flex items-center gap-2 rounded border bg-background p-2"
              >
                <Checkbox
                  :checked="scopeAssignment(permissionId)?.organizationUnitIds.some((id) => String(id) === String(organization.id))"
                  :disabled="!canEditScope(permissionId) || previewing || saving"
                  @update:checked="toggleScopeOrganization(permissionId, organization.id, $event === true)"
                />
                {{ organization.pathName || organization.name }}
              </label>
            </div>
          </template>
          <div v-else-if="scopeAssignment(permissionId)" class="text-muted-foreground">
            {{ DATA_SCOPE_LABEL[scopeAssignment(permissionId)!.dataScopeCode] }}
            <template v-if="scopeAssignment(permissionId)!.organizationNames?.length">
              · {{ scopeAssignment(permissionId)!.organizationNames!.join('、') }}
            </template>
          </div>
        </div>
        <p v-if="scopeError" class="text-sm text-destructive">{{ scopeError }}</p>
      </section>

      <div v-if="preview" class="rounded-lg border bg-muted/30 p-3 text-sm">
        <div class="font-medium">变更影响预览</div>
        <div class="mt-2 grid gap-2 sm:grid-cols-3">
          <span>新增 {{ preview.addedPermissions.length }} 项</span>
          <span>移除 {{ preview.removedPermissions.length }} 项</span>
          <span>影响 {{ preview.affectedUserCount }} 名用户</span>
          <span>覆盖 {{ preview.affectedOrganizationCount }} 个组织</span>
          <span>撤销 {{ preview.sessionRevocationCount }} 个会话</span>
        </div>
        <div
          v-if="sensitiveDifferences.length"
          class="mt-3 rounded-md bg-amber-50 p-2 text-amber-800"
        >
          敏感变更：{{ sensitiveDifferences.map((item) => item.name).join('、') }}
        </div>
        <ul v-if="preview.warnings.length" class="mt-2 list-disc space-y-1 pl-5 text-amber-800">
          <li v-for="warning in preview.warnings" :key="warning">{{ warning }}</li>
        </ul>
        <ul v-if="preview.scopeDifferences?.length" class="mt-2 space-y-1">
          <li v-for="item in preview.scopeDifferences" :key="item.permissionId">
            {{ item.permissionName }}：{{ item.beforeDataScopeCode ? DATA_SCOPE_LABEL[item.beforeDataScopeCode] : '无' }}
            <template v-if="item.beforeOrganizationNames.length">（{{ item.beforeOrganizationNames.join('、') }}）</template>
            → {{ item.afterDataScopeCode ? DATA_SCOPE_LABEL[item.afterDataScopeCode] : '无' }}
            <template v-if="item.afterOrganizationNames.length">（{{ item.afterOrganizationNames.join('、') }}）</template>
          </li>
        </ul>
      </div>

      <div class="space-y-2">
        <Label for="matrix-reason">变更原因</Label>
        <Textarea
          id="matrix-reason"
          v-model="reason"
          :rows="3"
          placeholder="说明本次角色权限调整原因"
        />
      </div>

      <div class="flex justify-end gap-2">
        <Button
          variant="outline"
          :disabled="!isDirty || !matrix.editable || previewing || saving || Boolean(scopeError)"
          @click="requestPreview"
        >
          {{ previewing ? '预览中...' : '预览影响' }}
        </Button>
        <Button :disabled="!canSave" @click="saveMatrix">
          {{ saving ? '保存中...' : '保存矩阵' }}
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ShieldAlert } from '@lucide/vue'
import { computed, ref, watch } from 'vue'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Textarea } from '@/components/ui/textarea'
import PermissionMatrixNode from '@/modules/access/components/PermissionMatrixNode.vue'
import {
  DATA_SCOPE_CODE,
  DATA_SCOPE_LABEL,
  flattenPermissionCatalog,
  type DataScopeCode,
  type PermissionCatalogItem,
  type PreviewRolePermissionRequest,
  type RolePermissionMatrix,
  type RolePermissionPreview,
  type RolePermissionScopeAssignment,
  type UpdateRolePermissionRequest,
} from '@/modules/access/model/access.types'
import type { EntityId } from '@/shared/types/id'

const props = withDefaults(
  defineProps<{
    catalog: PermissionCatalogItem[]
    matrix: RolePermissionMatrix
    preview?: RolePermissionPreview | null
    previewing?: boolean
    saving?: boolean
  }>(),
  { preview: null, previewing: false, saving: false },
)

const emit = defineEmits<{
  preview: [request: PreviewRolePermissionRequest]
  save: [request: UpdateRolePermissionRequest]
}>()

const selectedPermissionIds = ref<EntityId[]>([])
const permissionScopes = ref<Record<string, RolePermissionScopeAssignment>>({})
const reason = ref('')
const requestedPreviewKey = ref('')
const previewedSelectionKey = ref('')
const initialSelectionKey = computed(() =>
  matrixKey(props.matrix.selectedPermissionIds, props.matrix.permissionScopes),
)
const selectionKey = computed(() =>
  matrixKey(selectedPermissionIds.value, selectedScopeAssignments.value),
)
const isDirty = computed(() => selectionKey.value !== initialSelectionKey.value)
const selectedScopeAssignments = computed(() =>
  selectedPermissionIds.value
    .map((id) => permissionScopes.value[String(id)])
    .filter((item): item is RolePermissionScopeAssignment => Boolean(item))
    .map((item) => ({
      permissionId: item.permissionId,
      dataScopeCode: item.dataScopeCode,
      organizationUnitIds: [...item.organizationUnitIds],
    })),
)
const scopeError = computed(() => {
  for (const permissionId of selectedPermissionIds.value) {
    const option = scopeOption(permissionId)
    if (!option) continue
    const assignment = scopeAssignment(permissionId)
    if (!assignment) return `${permissionName(permissionId)}必须选择数据范围`
    if (
      assignment.dataScopeCode === DATA_SCOPE_CODE.CUSTOM_ORGS &&
      !assignment.organizationUnitIds.length
    ) {
      return `${permissionName(permissionId)}的指定组织范围不能为空`
    }
  }
  return ''
})
const canSave = computed(
  () =>
    props.matrix.editable &&
    Boolean(props.preview) &&
    previewedSelectionKey.value === selectionKey.value &&
    Boolean(reason.value.trim()) &&
    !props.saving &&
    !props.previewing &&
    !scopeError.value,
)
const sensitiveDifferences = computed(() =>
  [...(props.preview?.addedPermissions ?? []), ...(props.preview?.removedPermissions ?? [])].filter(
    (item) => item.sensitivityLevel !== 'NORMAL',
  ),
)

function togglePermission(node: PermissionCatalogItem, checked: boolean): void {
  const selected = new Map(selectedPermissionIds.value.map((id) => [String(id), id]))
  const affectedNodes = [node, ...flattenPermissionCatalog(node.children)]
  if (checked) {
    affectedNodes
      .filter((item) => item.enabled && item.assignable)
      .forEach((item) => {
        selected.set(String(item.id), item.id)
      })
    const allNodes = flattenPermissionCatalog(props.catalog)
    let parentId = node.parentId
    while (parentId !== null && parentId !== undefined) {
      const parent = allNodes.find((item) => String(item.id) === String(parentId))
      if (!parent) break
      if (parent.enabled && parent.assignable) selected.set(String(parent.id), parent.id)
      parentId = parent.parentId
    }
  } else {
    affectedNodes.forEach((item) => selected.delete(String(item.id)))
  }
  selectedPermissionIds.value = [...selected.values()]
  if (checked) selectedPermissionIds.value.forEach(initializeScope)
  else affectedNodes.forEach((item) => delete permissionScopes.value[String(item.id)])
  previewedSelectionKey.value = ''
}

function requestPreview(): void {
  requestedPreviewKey.value = selectionKey.value
  emit('preview', {
    expectedVersion: props.matrix.expectedVersion,
    permissionIds: selectedPermissionIds.value,
    permissionScopes: selectedScopeAssignments.value,
  })
}

function saveMatrix(): void {
  if (!canSave.value) return
  emit('save', {
    expectedVersion: props.matrix.expectedVersion,
    permissionIds: selectedPermissionIds.value,
    permissionScopes: selectedScopeAssignments.value,
    reason: reason.value.trim(),
  })
}

function toSelectionKey(ids: EntityId[]): string {
  return [...new Set(ids.map(String))].sort().join(',')
}

function matrixKey(ids: EntityId[], scopes: RolePermissionScopeAssignment[]): string {
  const scopeKey = scopes
    .map(
      (item) =>
        `${item.permissionId}:${item.dataScopeCode}:${toSelectionKey(item.organizationUnitIds)}`,
    )
    .sort()
    .join('|')
  return `${toSelectionKey(ids)}#${scopeKey}`
}

function permissionName(permissionId: EntityId): string {
  return (
    flattenPermissionCatalog(props.catalog).find(
      (item) => String(item.id) === String(permissionId),
    )?.name ?? `权限 ${permissionId}`
  )
}

function scopeOption(permissionId: EntityId) {
  return props.matrix.permissionScopeOptions?.find(
    (item) => String(item.permissionId) === String(permissionId),
  )
}

function scopeAssignment(permissionId: EntityId) {
  return permissionScopes.value[String(permissionId)]
}

function canEditScope(permissionId: EntityId): boolean {
  return props.matrix.editable && Boolean(scopeOption(permissionId)?.editable)
}

function initializeScope(permissionId: EntityId): void {
  if (scopeAssignment(permissionId)) return
  const candidate = scopeOption(permissionId)?.dataScopeCandidates[0]
  if (!candidate) return
  permissionScopes.value[String(permissionId)] = {
    permissionId,
    dataScopeCode: candidate.code,
    organizationUnitIds: [],
  }
}

function setPermissionScope(permissionId: EntityId, code: DataScopeCode): void {
  if (!canEditScope(permissionId)) return
  const option = scopeOption(permissionId)
  if (!option?.dataScopeCandidates.some((candidate) => candidate.code === code)) return
  permissionScopes.value[String(permissionId)] = {
    permissionId,
    dataScopeCode: code,
    organizationUnitIds:
      code === DATA_SCOPE_CODE.CUSTOM_ORGS
        ? scopeAssignment(permissionId)?.organizationUnitIds ?? []
        : [],
  }
  previewedSelectionKey.value = ''
}

function customOrganizations(permissionId: EntityId) {
  return (
    scopeOption(permissionId)?.dataScopeCandidates.find(
      (candidate) => candidate.code === DATA_SCOPE_CODE.CUSTOM_ORGS,
    )?.organizationOptions ?? []
  )
}

function toggleScopeOrganization(
  permissionId: EntityId,
  organizationId: EntityId,
  checked: boolean,
): void {
  if (!canEditScope(permissionId)) return
  const assignment = scopeAssignment(permissionId)
  if (!assignment || assignment.dataScopeCode !== DATA_SCOPE_CODE.CUSTOM_ORGS) return
  const allowed = customOrganizations(permissionId).some(
    (item) => String(item.id) === String(organizationId),
  )
  if (!allowed) return
  const selected = new Map(
    assignment.organizationUnitIds.map((id) => [String(id), id] as const),
  )
  if (checked) selected.set(String(organizationId), organizationId)
  else selected.delete(String(organizationId))
  permissionScopes.value[String(permissionId)] = {
    ...assignment,
    organizationUnitIds: [...selected.values()],
  }
  previewedSelectionKey.value = ''
}

watch(
  () => props.matrix,
  (matrix) => {
    selectedPermissionIds.value = [...matrix.selectedPermissionIds]
    permissionScopes.value = Object.fromEntries(
      (matrix.permissionScopes ?? []).map((item) => [
        String(item.permissionId),
        { ...item, organizationUnitIds: [...item.organizationUnitIds] },
      ]),
    )
    selectedPermissionIds.value.forEach(initializeScope)
    reason.value = ''
    requestedPreviewKey.value = ''
    previewedSelectionKey.value = ''
  },
  { immediate: true },
)

watch(
  () => props.preview,
  (preview) => {
    if (preview) previewedSelectionKey.value = requestedPreviewKey.value
  },
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
