<template>
  <span class="inline-flex">
    <TooltipProvider :delay-duration="120">
      <Tooltip>
        <TooltipTrigger as-child>
          <button
            class="inline-flex h-8 w-8 items-center justify-center rounded-md p-0 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--crm-primary)] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
            :class="buttonClass"
            type="button"
            :aria-label="label"
            :disabled="disabled"
            @click.stop="emit('click', $event)"
          >
            <slot />
          </button>
        </TooltipTrigger>
        <TooltipContent>
          {{ label }}
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'

defineOptions({
  name: 'RowActionButton',
})

const props = withDefaults(
  defineProps<{
    label: string
    danger?: boolean
    disabled?: boolean
  }>(),
  {
    danger: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClass = computed(() =>
  props.danger
    ? 'text-[var(--crm-text-tertiary)] hover:bg-[var(--crm-danger-bg)] hover:text-[var(--crm-danger)]'
    : 'text-[var(--crm-text-tertiary)] hover:bg-[var(--crm-primary-light)] hover:text-[var(--crm-primary)]',
)
</script>
