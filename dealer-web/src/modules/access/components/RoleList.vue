<template>
  <div class="flex h-full min-h-0 flex-col">
    <div class="flex items-center justify-between border-b p-4">
      <div>
        <h2 class="font-semibold">角色目录</h2>
        <p class="text-xs text-muted-foreground">岗位不等同于权限角色</p>
      </div>
      <Button v-if="canCreate" size="sm" @click="emit('create')">新增角色</Button>
    </div>
    <div v-if="loading" class="py-16 text-center text-muted-foreground">加载中...</div>
    <ScrollArea v-else class="min-h-0 flex-1 p-3">
      <div v-if="roles.length" class="space-y-2">
        <div
          v-for="role in roles"
          :key="role.id"
          class="rounded-lg border p-3"
          :class="String(selectedId) === String(role.id) ? 'border-primary bg-primary/5' : ''"
        >
          <button class="w-full text-left" type="button" @click="emit('select', role)">
            <div class="flex items-center gap-2">
              <span class="font-medium">{{ role.name }}</span>
              <Badge v-if="role.protectedRole" variant="outline">受保护</Badge>
              <StatusBadge
                :label="role.enabled ? '启用' : '停用'"
                :tone="role.enabled ? 'success' : 'muted'"
              />
            </div>
            <div class="mt-1 font-mono text-xs text-muted-foreground">{{ role.code }}</div>
            <div class="mt-2 text-xs text-muted-foreground">
              {{ role.memberCount }} 名成员 · {{ DATA_SCOPE_LABEL[role.defaultDataScope] }}
            </div>
          </button>
          <div class="mt-3 flex flex-wrap gap-2 border-t pt-3">
            <Button
              v-if="canEdit"
              size="xs"
              variant="outline"
              :disabled="!canRoleAction(role, ROLE_ACTION.EDIT)"
              :title="roleActionReason(role, ROLE_ACTION.EDIT)"
              @click="emit('edit', role)"
              >编辑</Button
            >
            <Button
              v-if="canCopy"
              size="xs"
              variant="outline"
              :disabled="!canRoleAction(role, ROLE_ACTION.COPY)"
              :title="roleActionReason(role, ROLE_ACTION.COPY)"
              @click="emit('copy', role)"
            >
              复制
            </Button>
            <Button
              v-if="canChangeStatus"
              size="xs"
              :variant="role.enabled ? 'destructive' : 'outline'"
              :disabled="!canRoleAction(role, ROLE_ACTION.STATUS_CHANGE)"
              :title="roleActionReason(role, ROLE_ACTION.STATUS_CHANGE)"
              @click="emit('change-status', role)"
              >{{ role.enabled ? '停用' : '启用' }}</Button
            >
          </div>
          <p
            v-if="role.protectedRole && (role.protectedReason || role.disabledReason)"
            class="mt-2 text-xs text-amber-700"
          >
            {{ role.protectedReason || role.disabledReason }}
          </p>
        </div>
      </div>
      <div v-else class="py-16 text-center text-sm text-muted-foreground">暂无角色</div>
    </ScrollArea>
  </div>
</template>

<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  DATA_SCOPE_LABEL,
  ROLE_ACTION,
  type RoleSummary,
} from '@/modules/access/model/access.types'
import type { EntityId } from '@/shared/types/id'
import StatusBadge from '@/shared/ui/StatusBadge.vue'

withDefaults(
  defineProps<{
    roles: RoleSummary[]
    selectedId?: EntityId | null
    loading?: boolean
    canCreate?: boolean
    canEdit?: boolean
    canCopy?: boolean
    canChangeStatus?: boolean
  }>(),
  {
    selectedId: null,
    loading: false,
    canCreate: false,
    canEdit: false,
    canCopy: false,
    canChangeStatus: false,
  },
)

const emit = defineEmits<{
  select: [role: RoleSummary]
  create: []
  edit: [role: RoleSummary]
  copy: [role: RoleSummary]
  'change-status': [role: RoleSummary]
}>()

function canRoleAction(role: RoleSummary, action: (typeof ROLE_ACTION)[keyof typeof ROLE_ACTION]): boolean {
  return Boolean(role.editable && role.allowedActions?.includes(action))
}

function roleActionReason(
  role: RoleSummary,
  action: (typeof ROLE_ACTION)[keyof typeof ROLE_ACTION],
): string | undefined {
  if (canRoleAction(role, action)) return undefined
  return (
    role.unavailableReasons?.[action] ||
    role.disabledReason ||
    role.protectedReason ||
    '服务端未允许当前角色操作'
  )
}
</script>
