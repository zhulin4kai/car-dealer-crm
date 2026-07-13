<template>
  <ScrollArea class="h-full pr-3">
    <div v-if="nodes.length" class="space-y-1">
      <Collapsible v-for="node in nodes" :key="node.id" default-open>
        <div class="flex items-center gap-1">
          <CollapsibleTrigger
            v-if="node.children.length"
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-muted"
            :aria-label="`展开或收起${node.name}`"
          >
            <ChevronRight class="h-4 w-4 transition-transform [[data-state=open]>&]:rotate-90" />
          </CollapsibleTrigger>
          <span v-else class="block w-8 shrink-0" />
          <Button
            type="button"
            variant="ghost"
            class="min-w-0 flex-1 justify-start gap-2"
            :class="String(selectedId) === String(node.id) ? 'bg-muted text-primary' : ''"
            @click="emit('select', node)"
          >
            <Building2 class="h-4 w-4 shrink-0" />
            <span class="truncate">{{ node.name }}</span>
            <span class="ml-auto text-xs text-muted-foreground">
              {{ ORGANIZATION_UNIT_TYPE_LABEL[node.type] }}
            </span>
          </Button>
        </div>
        <CollapsibleContent v-if="node.children.length" class="ml-4 border-l pl-2">
          <OrganizationTree
            :nodes="node.children"
            :selected-id="selectedId"
            class="h-auto"
            @select="emit('select', $event)"
          />
        </CollapsibleContent>
      </Collapsible>
    </div>
    <div v-else class="px-3 py-10 text-center text-sm text-muted-foreground">暂无组织节点</div>
  </ScrollArea>
</template>

<script setup lang="ts">
import { Building2, ChevronRight } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  ORGANIZATION_UNIT_TYPE_LABEL,
  type OrganizationUnit,
} from '@/modules/organization/model/organization.types'
import type { EntityId } from '@/shared/types/id'

defineOptions({ name: 'OrganizationTree' })

defineProps<{
  nodes: OrganizationUnit[]
  selectedId?: EntityId | null
}>()

const emit = defineEmits<{
  select: [unit: OrganizationUnit]
}>()
</script>
