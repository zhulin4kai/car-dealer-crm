<template>
  <ScrollArea v-if="!nested" class="h-full pr-3">
    <PermissionCatalog :nodes="nodes" nested />
  </ScrollArea>
  <div v-else-if="nodes.length" class="space-y-2">
    <Collapsible v-for="node in nodes" :key="node.id" default-open class="rounded-lg border">
      <div class="flex items-start gap-3 p-3">
        <CollapsibleTrigger
          v-if="node.children.length"
          class="mt-0.5 flex h-7 w-7 items-center justify-center rounded-md hover:bg-muted"
          :aria-label="`展开或收起${node.name}`"
        >
          <ChevronRight class="h-4 w-4" />
        </CollapsibleTrigger>
        <span v-else class="block w-7" />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-medium">{{ node.name }}</span>
            <Badge variant="outline">{{ node.type === 'menu' ? '菜单' : '操作' }}</Badge>
            <Badge :variant="node.sensitivityLevel === 'NORMAL' ? 'secondary' : 'outline'">
              {{ PERMISSION_SENSITIVITY_LABEL[node.sensitivityLevel] }}
            </Badge>
            <StatusBadge
              :label="node.enabled ? '启用' : '停用'"
              :tone="node.enabled ? 'success' : 'muted'"
            />
          </div>
          <div class="mt-1 font-mono text-xs text-muted-foreground">{{ node.code }}</div>
          <p class="mt-1 text-sm text-muted-foreground">{{ node.description || '暂无说明' }}</p>
          <p class="mt-1 text-xs" :class="node.delegable ? 'text-emerald-700' : 'text-amber-700'">
            {{ node.delegable ? '允许在授权上限内委派' : '不可由普通管理者委派' }}
          </p>
        </div>
      </div>
      <CollapsibleContent v-if="node.children.length" class="border-t bg-muted/20 p-2 pl-6">
        <PermissionCatalog :nodes="node.children" nested />
      </CollapsibleContent>
    </Collapsible>
  </div>
  <div v-else class="py-12 text-center text-sm text-muted-foreground">暂无权限目录</div>
</template>

<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'

import { Badge } from '@/components/ui/badge'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  PERMISSION_SENSITIVITY_LABEL,
  type PermissionCatalogItem,
} from '@/modules/access/model/access.types'
import StatusBadge from '@/shared/ui/StatusBadge.vue'

defineOptions({ name: 'PermissionCatalog' })

withDefaults(defineProps<{ nodes: PermissionCatalogItem[]; nested?: boolean }>(), {
  nested: false,
})
</script>
