<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Maximize2, MessageSquarePlus, Sparkles, X } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import {
  createAiConversation,
  listAiConversations,
} from '@/modules/ai/api/ai-api'
import AiAssistantPanel from '@/modules/ai/components/AiAssistantPanel.vue'
import type { AiConversation, AiPageContext } from '@/modules/ai/model/ai.types'
import { messageTip } from '@/shared/utils/feedback'
import { useAiAssistantStore } from '@/stores/ai-assistant.store'

defineOptions({
  name: 'AiSidePanel',
})

const props = defineProps<{
  open: boolean
  context?: AiPageContext
}>()

defineEmits<{
  close: []
  expand: [payload?: { conversationNo?: string; runNo?: string }]
}>()

const currentConversationNo = ref<string | undefined>()
const currentRunNo = ref<string | undefined>()
const conversations = ref<AiConversation[]>([])
const loadingConversations = ref(false)
const aiAssistantStore = useAiAssistantStore()
const contextKey = computed(
  () => `${props.context?.objectType ?? 'GENERAL'}:${props.context?.objectId ?? ''}`,
)
const visibleConversations = computed(() => {
  const objectType = props.context?.objectType
  const objectId = props.context?.objectId
  if (!objectType || !objectId) return conversations.value
  return conversations.value.filter(
    (item) => item.contextObjectType === objectType && item.contextObjectId === objectId,
  )
})
const currentConversationTitle = computed(
  () =>
    visibleConversations.value.find((item) => item.conversationNo === currentConversationNo.value)
      ?.title ??
    '当前会话',
)

async function loadConversations(): Promise<void> {
  loadingConversations.value = true
  try {
    conversations.value = await listAiConversations()
  } finally {
    loadingConversations.value = false
  }
}

async function createConversation(): Promise<void> {
  try {
    const conversation = await createAiConversation({
      entryPoint: 'SIDE_PANEL',
      contextObjectType: props.context?.objectType,
      contextObjectId: props.context?.objectId,
    })
    conversations.value = [conversation, ...conversations.value]
    currentConversationNo.value = conversation.conversationNo
    currentRunNo.value = undefined
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话创建失败', 'error')
  }
}

function selectConversation(event: Event): void {
  const value = (event.target as HTMLSelectElement).value
  currentConversationNo.value = value || undefined
  currentRunNo.value = undefined
}

function handleConversationChange(conversationNo: string): void {
  currentConversationNo.value = conversationNo
  void loadConversations().catch(() => undefined)
}

watch(
  () => props.open,
  (open) => {
    if (open) void loadConversations().catch(() => undefined)
  },
  { immediate: true },
)

watch(
  contextKey,
  (nextKey, previousKey) => {
    if (nextKey === previousKey) return
    currentConversationNo.value = undefined
    currentRunNo.value = undefined
    if (props.open) void loadConversations().catch(() => undefined)
  },
)

onMounted(() => {
  if (props.open) void loadConversations().catch(() => undefined)
})
</script>

<template>
  <aside
    v-if="open"
    data-testid="ai-side-panel"
    class="ai-side-panel flex h-full w-full max-w-[100vw] shrink-0 flex-col border-l border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] sm:w-[420px]"
  >
    <header class="border-b border-[var(--crm-border-light)] px-4 py-3">
      <div class="flex items-center justify-between">
        <div class="flex min-w-0 items-center gap-2 font-semibold">
          <span class="ai-side-panel-icon flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-white">
            <Sparkles class="h-4 w-4" />
          </span>
          <span class="truncate">AI 助手</span>
        </div>
        <div class="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            aria-label="展开 AI 助手"
            title="展开 AI 助手"
            @click="$emit('expand', { conversationNo: currentConversationNo, runNo: currentRunNo })"
          >
            <Maximize2 class="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="icon" aria-label="关闭 AI 助手" @click="$emit('close')">
            <X class="h-4 w-4" />
          </Button>
        </div>
      </div>
      <div class="mt-3 flex items-center gap-2">
        <select
          class="min-w-0 flex-1 rounded-md border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-2 py-1.5 text-xs text-[var(--crm-text-secondary)] outline-none focus:border-[var(--crm-primary)]"
          :value="currentConversationNo || ''"
          :disabled="loadingConversations"
          aria-label="切换 AI 会话"
          @change="selectConversation"
        >
          <option value="">{{ currentConversationTitle }}</option>
          <option
            v-for="conversation in visibleConversations"
            :key="conversation.conversationNo"
            :value="conversation.conversationNo"
          >
            {{ conversation.title }}
          </option>
        </select>
        <Button variant="outline" size="icon" class="h-8 w-8 shrink-0" title="新对话" @click="createConversation">
          <MessageSquarePlus class="h-4 w-4" />
        </Button>
      </div>
    </header>
    <AiAssistantPanel
      :key="contextKey"
      class="min-h-0 flex-1"
      entry-point="SIDE_PANEL"
      :context="context"
      :initial-conversation-no="currentConversationNo"
      @conversation-change="handleConversationChange"
      @run-change="currentRunNo = $event"
      @busy-change="aiAssistantStore.setRunActive"
    />
  </aside>
</template>

<style scoped>
.ai-side-panel {
  animation: ai-side-panel-in 180ms ease-out both;
}

.ai-side-panel-icon {
  background: linear-gradient(135deg, #3370ff 0%, #6d5df6 100%);
}

@media (max-width: 639px) {
  .ai-side-panel {
    position: fixed;
    inset: 0;
    z-index: 50;
    width: 100vw;
    max-width: none;
    border-left: 0;
  }
}

@keyframes ai-side-panel-in {
  from {
    opacity: 0;
    transform: translateX(18px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
