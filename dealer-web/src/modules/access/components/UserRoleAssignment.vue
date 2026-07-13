<template>
  <section class="space-y-4">
    <div>
      <h2 class="text-lg font-semibold">用户角色</h2>
      <p class="text-sm text-muted-foreground">角色来源由服务端返回；这里只能选择可委派候选。</p>
    </div>

    <Alert v-if="!editable">
      <ShieldAlert class="h-4 w-4" />
      <AlertTitle>角色授权不可编辑</AlertTitle>
      <AlertDescription>{{ disabledReason || '当前用户不允许调整该账号角色。' }}</AlertDescription>
    </Alert>

    <div class="space-y-2 rounded-lg border p-4">
      <h3 class="font-medium">当前角色及来源</h3>
      <p v-if="!assignments.length" class="py-4 text-sm text-muted-foreground">暂无角色来源</p>
      <div
        v-for="assignment in assignments"
        v-else
        :key="`${assignment.roleId}-${assignment.source}`"
        class="flex flex-wrap items-center gap-2 text-sm"
      >
        <span class="font-medium">{{ assignment.roleName }}</span>
        <code class="text-xs text-muted-foreground">{{ assignment.roleCode }}</code>
        <Badge variant="outline">{{ ROLE_ASSIGNMENT_SOURCE_LABEL[assignment.source] }}</Badge>
        <span v-if="assignment.sourceDescription" class="text-muted-foreground">
          {{ assignment.sourceDescription }}
        </span>
        <span v-if="assignment.effectiveTo" class="text-muted-foreground">
          有效至 {{ formatDateTime(assignment.effectiveTo) }}
        </span>
      </div>
    </div>

    <div class="space-y-2 rounded-lg border p-4">
      <h3 class="font-medium">可分配角色候选</h3>
      <p class="text-xs text-muted-foreground">
        候选、授权级别和不可编辑原因均由服务端按管理关系与授权上限计算。
      </p>
      <p v-if="!candidates.length" class="py-4 text-sm text-muted-foreground">暂无角色候选</p>
      <label
        v-for="candidate in candidates"
        v-else
        :key="candidate.roleId"
        class="flex items-start gap-3 rounded-md border p-3"
      >
        <Checkbox
          :checked="selectedRoleIds.includes(candidate.roleId)"
          :disabled="submitting || !editable || !candidate.editable"
          :aria-label="`分配角色${candidate.roleName}`"
          @update:checked="toggleRole(candidate, $event === true)"
        />
        <span class="min-w-0 flex-1">
          <span class="flex flex-wrap items-center gap-2">
            <span class="font-medium">{{ candidate.roleName }}</span>
            <code class="text-xs text-muted-foreground">{{ candidate.roleCode }}</code>
          </span>
          <span class="mt-1 block text-xs text-muted-foreground">
            授权级别 {{ candidate.authorizationLevel }} ·
            {{ DATA_SCOPE_LABEL[candidate.defaultDataScope] }}
          </span>
          <span v-if="candidate.unavailableReason" class="mt-1 block text-xs text-amber-700">
            {{ candidate.unavailableReason }}
          </span>
        </span>
      </label>
    </div>

    <div v-if="isDirty" class="space-y-3 rounded-lg border bg-muted/20 p-4">
      <h3 class="font-medium">角色差异</h3>
      <div class="grid gap-2 text-sm sm:grid-cols-2">
        <div>新增：{{ addedRoleNames.length ? addedRoleNames.join('、') : '无' }}</div>
        <div>移除：{{ removedRoleNames.length ? removedRoleNames.join('、') : '无' }}</div>
      </div>
      <div class="space-y-2">
        <Label for="role-assignment-reason">调整原因</Label>
        <Textarea id="role-assignment-reason" v-model="reason" :rows="3" />
      </div>
      <div v-if="editable" class="flex justify-end">
        <Button :disabled="submitting || !reason.trim()" @click="submitChanges">
          {{ submitting ? '保存中...' : '保存角色调整' }}
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
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { DATA_SCOPE_LABEL } from '@/modules/access/model/access.types'
import {
  ROLE_ASSIGNMENT_SOURCE_LABEL,
  type UpdateUserRoleAssignmentsRequest,
  type UserRoleAssignmentItem,
  type UserRoleCandidate,
} from '@/modules/access/model/user-permission.types'
import type { EntityId } from '@/shared/types/id'
import { formatDateTime } from '@/shared/utils/display-format'

const props = withDefaults(
  defineProps<{
    assignments: UserRoleAssignmentItem[]
    candidates: UserRoleCandidate[]
    authorizationVersion: number
    editable: boolean
    disabledReason?: string
    submitting?: boolean
  }>(),
  { disabledReason: '', submitting: false },
)

const emit = defineEmits<{
  save: [request: UpdateUserRoleAssignmentsRequest]
}>()

const selectedRoleIds = ref<EntityId[]>([])
const reason = ref('')
const initialRoleIds = computed(() =>
  props.candidates.filter((item) => item.selected).map((item) => item.roleId),
)
const isDirty = computed(() => roleKey(selectedRoleIds.value) !== roleKey(initialRoleIds.value))
const addedRoleNames = computed(() =>
  props.candidates
    .filter(
      (candidate) =>
        selectedRoleIds.value.includes(candidate.roleId) &&
        !initialRoleIds.value.includes(candidate.roleId),
    )
    .map((candidate) => candidate.roleName),
)
const removedRoleNames = computed(() =>
  props.candidates
    .filter(
      (candidate) =>
        !selectedRoleIds.value.includes(candidate.roleId) &&
        initialRoleIds.value.includes(candidate.roleId),
    )
    .map((candidate) => candidate.roleName),
)

function toggleRole(candidate: UserRoleCandidate, checked: boolean): void {
  if (!props.editable || !candidate.editable) return
  selectedRoleIds.value = checked
    ? [...new Set([...selectedRoleIds.value, candidate.roleId])]
    : selectedRoleIds.value.filter((id) => String(id) !== String(candidate.roleId))
}

function submitChanges(): void {
  if (!props.editable || !isDirty.value || !reason.value.trim()) return
  emit('save', {
    authorizationVersion: props.authorizationVersion,
    roleIds: selectedRoleIds.value,
    reason: reason.value.trim(),
  })
}

function roleKey(ids: EntityId[]): string {
  return [...new Set(ids.map(String))].sort().join(',')
}

watch(
  () => props.candidates,
  (candidates) => {
    selectedRoleIds.value = candidates.filter((item) => item.selected).map((item) => item.roleId)
    reason.value = ''
  },
  { immediate: true },
)
</script>
