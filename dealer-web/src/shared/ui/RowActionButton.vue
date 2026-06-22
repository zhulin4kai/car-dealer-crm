<template>
  <button
    class="inline-flex h-8 w-8 items-center justify-center rounded-lg transition-colors disabled:cursor-not-allowed disabled:opacity-50"
    :class="buttonClass"
    type="button"
    :title="label"
    :aria-label="label"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({
  name: 'RowActionButton',
})

const props = withDefaults(defineProps<{
  label: string
  danger?: boolean
  disabled?: boolean
}>(), {
  danger: false,
  disabled: false,
})

defineEmits<{
  click: []
}>()

const buttonClass = computed(() =>
  props.danger
    ? 'text-[var(--crm-text-tertiary)] hover:bg-[var(--crm-danger-bg)] hover:text-[var(--crm-danger)]'
    : 'text-[var(--crm-text-tertiary)] hover:bg-[var(--crm-primary-light)] hover:text-[var(--crm-primary)]'
)
</script>
