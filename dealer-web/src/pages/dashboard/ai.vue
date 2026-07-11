<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Archive, Edit3, MessageSquarePlus, Settings } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import {
  archiveAiConversation,
  createAiConversation,
  listAiConversations,
  renameAiConversation,
} from '@/modules/ai/api/ai-api'
import AiAssistantPanel from '@/modules/ai/components/AiAssistantPanel.vue'
import type { AiConversation, AiPageContext } from '@/modules/ai/model/ai.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
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
const restoredContext = ref<AiPageContext>({})
const renameDialogOpen = ref(false)
const renameTitle = ref('')
const renameSubmitting = ref(false)

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
const activeConversationContext = computed<AiPageContext>(() => {
  const selected = conversations.value.find(
    (conversation) => conversation.conversationNo === selectedConversationNo.value,
  )
  if (selected) {
    return selected.contextObjectType && selected.contextObjectId
      ? {
          objectType: selected.contextObjectType,
          objectId: selected.contextObjectId,
        }
      : {}
  }
  return restoredContext.value
})
const selectedConversationTitle = computed(
  () =>
    conversations.value.find(
      (conversation) => conversation.conversationNo === selectedConversationNo.value,
    )?.title ?? '当前会话',
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
    const conversation = await createAiConversation({
      entryPoint: 'PAGE',
      contextObjectType: activeConversationContext.value.objectType,
      contextObjectId: activeConversationContext.value.objectId,
    })
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
    await messageConfirm(`归档“${selectedConversationTitle.value}”后将从默认会话列表隐藏，是否继续？`)
  } catch {
    return
  }
  try {
    await archiveAiConversation(conversationNo)
    selectedConversationNo.value = undefined
    await router.replace({ name: 'ai-assistant', query: undefined })
    await loadConversations()
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话归档失败', 'error')
  }
}

function openRenameDialog(): void {
  if (!selectedConversationNo.value) return
  renameTitle.value = selectedConversationTitle.value
  renameDialogOpen.value = true
}

async function submitRename(): Promise<void> {
  const conversationNo = selectedConversationNo.value
  const title = renameTitle.value.trim()
  if (!conversationNo || !title || renameSubmitting.value) return
  renameSubmitting.value = true
  try {
    await renameAiConversation(conversationNo, { title })
    renameDialogOpen.value = false
    await loadConversations()
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话重命名失败', 'error')
  } finally {
    renameSubmitting.value = false
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

function handleContextChange(context: AiPageContext): void {
  restoredContext.value = context
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
  <div class="flex h-[calc(100vh-var(--crm-header-height))] min-h-0 flex-col bg-[var(--crm-bg-page)] md:flex-row">
    <aside class="flex max-h-[220px] w-full shrink-0 flex-col border-b border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] md:max-h-none md:w-[280px] md:border-b-0 md:border-r">
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
      <div class="grid grid-cols-2 gap-2 border-t border-[var(--crm-border-light)] p-3 md:block">
        <Button
          variant="outline"
          size="sm"
          class="w-full gap-2 md:mb-2"
          :disabled="!selectedConversationNo"
          @click="openRenameDialog"
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

    <div class="relative min-h-0 min-w-0 flex-1">
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
        @context-change="handleContextChange"
      />
    </div>

    <Dialog v-model:open="renameDialogOpen">
      <DialogContent class="sm:max-w-[440px]">
        <DialogHeader>
          <DialogTitle>重命名 AI 会话</DialogTitle>
          <DialogDescription>名称仅用于区分会话，不会改变已有消息和业务上下文。</DialogDescription>
        </DialogHeader>
        <form class="space-y-2" @submit.prevent="submitRename">
          <Label for="ai-conversation-title">会话名称</Label>
          <Input
            id="ai-conversation-title"
            v-model="renameTitle"
            maxlength="128"
            autocomplete="off"
          />
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="renameSubmitting" @click="renameDialogOpen = false">
            取消
          </Button>
          <Button :disabled="renameSubmitting || !renameTitle.trim()" @click="submitRename">
            保存
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
