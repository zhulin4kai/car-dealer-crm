<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Archive, Edit3, MessageSquarePlus, Settings } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import {
  archiveAiConversation,
  createAiConversation,
  listAiConversations,
  renameAiConversation,
} from '@/modules/ai/api/ai-api'
import AiAssistantPanel from '@/modules/ai/components/AiAssistantPanel.vue'
import type { AiConversation } from '@/modules/ai/model/ai.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'AiAssistantPage',
})

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const conversations = ref<AiConversation[]>([])
const conversationsLoading = ref(false)
const selectedConversationNo = ref<string | undefined>()

const initialRunNo = computed(() => {
  const value = route.query.runNo
  return typeof value === 'string' && !selectedConversationNo.value ? value : undefined
})
const initialConversationNo = computed(() => {
  const value = route.query.conversationNo
  return typeof value === 'string' ? value : selectedConversationNo.value
})
const canViewProviderConfig = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.providerConfigView),
)

async function loadConversations(): Promise<void> {
  conversationsLoading.value = true
  try {
    conversations.value = await listAiConversations()
  } finally {
    conversationsLoading.value = false
  }
}

async function createConversation(): Promise<void> {
  try {
    const conversation = await createAiConversation({ entryPoint: 'PAGE' })
    conversations.value = [conversation, ...conversations.value]
    await selectConversation(conversation.conversationNo)
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话创建失败', 'error')
  }
}

async function archiveConversation(): Promise<void> {
  const conversationNo = selectedConversationNo.value
  if (!conversationNo) return
  try {
    await archiveAiConversation(conversationNo)
    selectedConversationNo.value = undefined
    await router.replace({ name: 'ai-assistant', query: undefined })
    await loadConversations()
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话归档失败', 'error')
  }
}

async function renameConversation(): Promise<void> {
  const conversationNo = selectedConversationNo.value
  if (!conversationNo) return
  const currentTitle =
    conversations.value.find((conversation) => conversation.conversationNo === conversationNo)?.title ?? ''
  const title = window.prompt('请输入新的会话名称', currentTitle)?.trim()
  if (!title) return
  try {
    await renameAiConversation(conversationNo, { title })
    await loadConversations()
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话重命名失败', 'error')
  }
}

async function selectConversation(conversationNo: string): Promise<void> {
  selectedConversationNo.value = conversationNo
  await router.replace({ name: 'ai-assistant', query: { conversationNo } })
}

function handleConversationChange(conversationNo: string): void {
  selectedConversationNo.value = conversationNo
  void router.replace({ name: 'ai-assistant', query: { conversationNo } })
  void loadConversations().catch(() => undefined)
}

function formatConversationTime(value?: string): string {
  if (!value) return '暂无消息'
  return value.replace('T', ' ').slice(0, 16)
}

watch(
  () => route.query.conversationNo,
  (value) => {
    selectedConversationNo.value = typeof value === 'string' ? value : undefined
  },
  { immediate: true },
)

onMounted(() => {
  void loadConversations().catch(() => undefined)
})
</script>

<template>
  <div class="flex h-[calc(100vh-var(--crm-header-height))] min-h-0 bg-[var(--crm-bg-page)]">
    <aside class="flex w-[280px] shrink-0 flex-col border-r border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)]">
      <div class="flex h-14 items-center justify-between border-b border-[var(--crm-border-light)] px-4">
        <div class="font-semibold text-[var(--crm-text-primary)]">AI 会话</div>
        <Button variant="outline" size="sm" class="gap-2" @click="createConversation">
          <MessageSquarePlus class="h-4 w-4" />
          新对话
        </Button>
      </div>
      <div class="min-h-0 flex-1 space-y-1 overflow-y-auto p-3">
        <div
          v-if="!conversations.length && !conversationsLoading"
          class="rounded-lg border border-dashed border-[var(--crm-border-light)] px-3 py-6 text-center text-sm text-[var(--crm-text-tertiary)]"
        >
          暂无会话
        </div>
        <button
          v-for="conversation in conversations"
          :key="conversation.conversationNo"
          type="button"
          class="w-full rounded-lg px-3 py-2 text-left transition-colors hover:bg-[var(--crm-bg-hover)]"
          :class="
            selectedConversationNo === conversation.conversationNo
              ? 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]'
              : 'text-[var(--crm-text-secondary)]'
          "
          @click="selectConversation(conversation.conversationNo)"
        >
          <div class="truncate text-sm font-medium">{{ conversation.title }}</div>
          <div class="mt-1 truncate text-xs text-[var(--crm-text-tertiary)]">
            {{ formatConversationTime(conversation.lastMessageTime || conversation.createTime) }}
          </div>
        </button>
      </div>
      <div class="border-t border-[var(--crm-border-light)] p-3">
        <Button
          variant="outline"
          size="sm"
          class="mb-2 w-full gap-2"
          :disabled="!selectedConversationNo"
          @click="renameConversation"
        >
          <Edit3 class="h-4 w-4" />
          重命名当前会话
        </Button>
        <Button
          variant="outline"
          size="sm"
          class="w-full gap-2"
          :disabled="!selectedConversationNo"
          @click="archiveConversation"
        >
          <Archive class="h-4 w-4" />
          归档当前会话
        </Button>
      </div>
    </aside>

    <div class="relative min-w-0 flex-1">
      <TooltipProvider v-if="canViewProviderConfig" :delay-duration="120">
        <Tooltip>
          <TooltipTrigger as-child>
            <Button
              as-child
              variant="outline"
              size="icon"
              class="absolute right-4 top-4 z-10 h-9 w-9 bg-[var(--crm-bg-surface)]"
            >
              <RouterLink to="/dashboard/ai/provider-configs" aria-label="模型配置">
                <Settings class="h-4 w-4" />
                <span class="sr-only">模型配置</span>
              </RouterLink>
            </Button>
          </TooltipTrigger>
          <TooltipContent>模型配置</TooltipContent>
        </Tooltip>
      </TooltipProvider>
      <AiAssistantPanel
        class="min-h-0 flex-1"
        entry-point="PAGE"
        :initial-conversation-no="initialConversationNo"
        :initial-run-no="initialRunNo"
        @conversation-change="handleConversationChange"
      />
    </div>
  </div>
</template>
