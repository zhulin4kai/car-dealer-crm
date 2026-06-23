<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <tr
    data-slot="table-row"
    :class="
      cn(
        'border-b border-[var(--crm-border-light)] transition-colors hover:bg-[var(--crm-bg-hover)] data-[state=selected]:bg-[var(--crm-primary-subtle)] has-aria-expanded:bg-[var(--crm-bg-hover)]',
        props.class,
      )
    "
  >
    <slot />
  </tr>
</template>
