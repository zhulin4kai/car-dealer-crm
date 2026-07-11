import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { AiPageContext } from '@/modules/ai/model/ai.types'
import { messageTip } from '@/shared/utils/feedback'

const CONTEXT_SWITCH_BLOCKED_MESSAGE = 'AI 正在生成，请先停止当前生成或等待完成后再切换业务对象'

export const useAiAssistantStore = defineStore('ai-assistant', () => {
  const isPanelOpen = ref(false)
  const isRunActive = ref(false)
  const context = ref<AiPageContext>({})

  function openPanel(nextContext: AiPageContext = {}): boolean {
    const normalized = normalizeContext(nextContext)
    if (
      isPanelOpen.value &&
      isRunActive.value &&
      contextIdentity(normalized) !== contextIdentity(context.value)
    ) {
      messageTip(CONTEXT_SWITCH_BLOCKED_MESSAGE, 'warning')
      return false
    }
    context.value = normalized
    isPanelOpen.value = true
    return true
  }

  function setContext(nextContext: AiPageContext = {}): boolean {
    const normalized = normalizeContext(nextContext)
    if (isRunActive.value && contextIdentity(normalized) !== contextIdentity(context.value)) {
      messageTip(CONTEXT_SWITCH_BLOCKED_MESSAGE, 'warning')
      return false
    }
    context.value = normalized
    return true
  }

  function setRunActive(active: boolean): void {
    isRunActive.value = active
  }

  function closePanel(): void {
    isPanelOpen.value = false
    isRunActive.value = false
  }

  function reset(): void {
    isPanelOpen.value = false
    isRunActive.value = false
    context.value = {}
  }

  return {
    isPanelOpen,
    isRunActive,
    context,
    openPanel,
    setContext,
    setRunActive,
    closePanel,
    reset,
  }
})

function normalizeContext(context: AiPageContext): AiPageContext {
  const objectType = context.objectType?.trim()
  const objectId = context.objectId?.trim()
  return objectType && objectId ? { objectType, objectId } : {}
}

function contextIdentity(context: AiPageContext): string {
  return `${context.objectType ?? 'GENERAL'}:${context.objectId ?? ''}`
}
