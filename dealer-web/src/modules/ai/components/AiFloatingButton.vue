<script setup lang="ts">
import { Sparkles } from '@lucide/vue'

defineOptions({
  name: 'AiFloatingButton',
})

defineEmits<{
  click: []
}>()
</script>

<template>
  <button
    class="ai-floating-button fixed bottom-6 right-6 z-30 flex h-12 w-12 items-center justify-center rounded-full text-white shadow-[0_16px_32px_rgba(51,112,255,0.32)] transition-transform hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--crm-primary)] focus-visible:ring-offset-2"
    type="button"
    aria-label="打开 AI 助手"
    title="AI 助手"
    @click="$emit('click')"
  >
    <Sparkles class="h-5 w-5" />
  </button>
</template>

<style scoped>
.ai-floating-button {
  background: linear-gradient(135deg, #3370ff 0%, #6d5df6 100%);
  animation: ai-floating-pulse 2.4s ease-in-out infinite;
}

@keyframes ai-floating-pulse {
  0%,
  100% {
    box-shadow: 0 16px 32px rgba(51, 112, 255, 0.32);
  }
  50% {
    box-shadow: 0 18px 38px rgba(109, 93, 246, 0.42);
  }
}
</style>
