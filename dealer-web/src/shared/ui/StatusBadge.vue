<template>
  <Badge
    variant="outline"
    class="h-6 gap-1.5 rounded-full border-transparent px-2.5 font-semibold"
    :class="toneClass"
  >
    <span class="h-1.5 w-1.5 rounded-full" :class="dotClass" />
    <span>{{ displayLabel }}</span>
  </Badge>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { Badge } from '@/components/ui/badge'

defineOptions({
  name: 'StatusBadge',
})

const props = withDefaults(
  defineProps<{
    label?: string | number | null
    tone?: 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple'
  }>(),
  {
    label: '--',
    tone: 'muted',
  },
)

const displayLabel = computed(() => {
  const label = String(props.label ?? '').trim()
  return label || '--'
})

const toneClass = computed(() => {
  switch (props.tone) {
    case 'success':
      return 'bg-[var(--crm-success-bg)] text-[var(--crm-success)]'
    case 'warning':
      return 'bg-[var(--crm-warning-bg)] text-[var(--crm-warning)]'
    case 'danger':
      return 'bg-[var(--crm-danger-bg)] text-[var(--crm-danger)]'
    case 'info':
      return 'bg-[var(--crm-info-bg)] text-[var(--crm-info)]'
    case 'purple':
      return 'bg-[var(--crm-purple-bg)] text-[var(--crm-purple)]'
    default:
      return 'bg-[var(--crm-bg-muted)] text-[var(--crm-text-tertiary)]'
  }
})

const dotClass = computed(() => {
  switch (props.tone) {
    case 'success':
      return 'bg-[var(--crm-success)]'
    case 'warning':
      return 'bg-[var(--crm-warning)]'
    case 'danger':
      return 'bg-[var(--crm-danger)]'
    case 'info':
      return 'bg-[var(--crm-info)]'
    case 'purple':
      return 'bg-[var(--crm-purple)]'
    default:
      return 'bg-[var(--crm-text-tertiary)]'
  }
})
</script>
