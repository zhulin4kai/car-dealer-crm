<script setup lang="ts">
import type { CheckboxRootProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { CheckIcon } from '@lucide/vue'
import { computed } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { CheckboxIndicator, CheckboxRoot } from 'reka-ui'
import { cn } from '@/lib/utils'

type CheckboxValue = CheckboxRootProps['modelValue']

const props = defineProps<
  CheckboxRootProps & {
    class?: HTMLAttributes['class']
    checked?: CheckboxValue
  }
>()
const emits = defineEmits<{
  'update:modelValue': [value: CheckboxValue]
  'update:checked': [value: CheckboxValue]
}>()

const delegatedProps = reactiveOmit(props, 'class', 'checked', 'modelValue')
const checkboxValue = computed(() => props.modelValue ?? props.checked ?? false)

function handleUpdate(value: CheckboxValue) {
  emits('update:modelValue', value)
  emits('update:checked', value)
}
</script>

<template>
  <CheckboxRoot
    v-slot="slotProps"
    data-slot="checkbox"
    v-bind="delegatedProps"
    :model-value="checkboxValue"
    :class="cn('border-input dark:bg-input/30 data-checked:bg-primary data-checked:text-primary-foreground dark:data-checked:bg-primary data-checked:border-primary aria-invalid:aria-checked:border-primary aria-invalid:border-destructive dark:aria-invalid:border-destructive/50 focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 flex size-4 items-center justify-center rounded-[4px] border transition-colors group-has-disabled/field:opacity-50 focus-visible:ring-3 aria-invalid:ring-3 peer relative shrink-0 outline-none after:absolute after:-inset-x-3 after:-inset-y-2 disabled:cursor-not-allowed disabled:opacity-50', props.class)"
    @update:model-value="handleUpdate"
  >
    <CheckboxIndicator
      data-slot="checkbox-indicator"
      class="[&>svg]:size-3.5 grid place-content-center text-current transition-none"
    >
      <slot v-bind="slotProps">
        <CheckIcon />
      </slot>
    </CheckboxIndicator>
  </CheckboxRoot>
</template>
