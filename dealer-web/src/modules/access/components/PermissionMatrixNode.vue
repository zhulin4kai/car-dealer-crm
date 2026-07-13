<template>
  <Collapsible default-open class="rounded-lg border">
    <div class="flex items-start gap-3 p-3">
      <CollapsibleTrigger
        v-if="node.children.length"
        class="mt-0.5 flex h-7 w-7 items-center justify-center rounded-md hover:bg-muted"
        :aria-label="`展开或收起${node.name}`"
      >
        <ChevronRight class="h-4 w-4" />
      </CollapsibleTrigger>
      <span v-else class="block w-7" />
      <Checkbox
        class="mt-1"
        :checked="checkboxState"
        :disabled="disabled || !node.enabled || !node.assignable"
        :aria-label="`选择权限${node.name}`"
        @update:checked="emit('toggle', node, $event === true)"
      />
      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-medium">{{ node.name }}</span>
          <Badge v-if="node.sensitivityLevel !== 'NORMAL'" variant="outline">
            {{ PERMISSION_SENSITIVITY_LABEL[node.sensitivityLevel] }}
          </Badge>
          <span v-if="!node.assignable" class="text-xs text-amber-700">
            {{ node.restrictionReason || '当前操作者不可分配' }}
          </span>
        </div>
        <div class="mt-1 font-mono text-xs text-muted-foreground">{{ node.code }}</div>
      </div>
    </div>
    <CollapsibleContent v-if="node.children.length" class="space-y-2 border-t bg-muted/20 p-2 pl-6">
      <PermissionMatrixNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-ids="selectedIds"
        :disabled="disabled"
        @toggle="forwardToggle"
      />
    </CollapsibleContent>
  </Collapsible>
</template>

<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'
import { computed } from 'vue'

import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import {
  PERMISSION_SENSITIVITY_LABEL,
  type PermissionCatalogItem,
} from '@/modules/access/model/access.types'
import type { EntityId } from '@/shared/types/id'

defineOptions({ name: 'PermissionMatrixNode' })

const props = withDefaults(
  defineProps<{
    node: PermissionCatalogItem
    selectedIds: EntityId[]
    disabled?: boolean
  }>(),
  { disabled: false },
)

const emit = defineEmits<{
  toggle: [node: PermissionCatalogItem, checked: boolean]
}>()

const checkboxState = computed<boolean | 'indeterminate'>(() => {
  if (isSelected(props.node.id)) return true
  const descendants = collectDescendantIds(props.node)
  return descendants.some(isSelected) ? 'indeterminate' : false
})

function isSelected(id: EntityId): boolean {
  return props.selectedIds.some((item) => String(item) === String(id))
}

function collectDescendantIds(node: PermissionCatalogItem): EntityId[] {
  return node.children.flatMap((child) => [child.id, ...collectDescendantIds(child)])
}

function forwardToggle(node: PermissionCatalogItem, checked: boolean): void {
  emit('toggle', node, checked)
}
</script>
