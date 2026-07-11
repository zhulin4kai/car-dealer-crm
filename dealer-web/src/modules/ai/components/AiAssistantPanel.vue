<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import {
  CalendarCheck,
  FileText,
  Loader2,
  Pencil,
  Phone,
  Send,
  Sparkles,
  Square,
  TrendingUp,
  Undo2,
  UserRound,
  Wand2,
} from '@lucide/vue'

import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import {
  cancelAiProactiveSubscription,
  cancelAiRun,
  cancelAiWorkflow,
  confirmAiProposal,
  createAiProactiveSubscription,
  createAiRun,
  editAiMessage,
  fetchAiConversation,
  fetchAiRunTrace,
  generateAiProactiveEvents,
  listAiProactiveEvents,
  listAiProactiveSubscriptions,
  pauseAiProactiveSubscription,
  pauseAiWorkflow,
  rejectAiProposal,
  resumeAiProactiveSubscription,
  resumeAiWorkflow,
  streamAiRunEvents,
  withdrawAiMessage,
} from '@/modules/ai/api/ai-api'
import AiMarkdownMessage from '@/modules/ai/components/AiMarkdownMessage.vue'
import AiProactivePanel from '@/modules/ai/components/AiProactivePanel.vue'
import AiProposalCard from '@/modules/ai/components/AiProposalCard.vue'
import AiToolResultCard from '@/modules/ai/components/AiToolResultCard.vue'
import AiWorkflowPanel from '@/modules/ai/components/AiWorkflowPanel.vue'
import type {
  AiChatMessage,
  AiConversationDetail,
  AiConversationTurn,
  AiEntryPoint,
  AiPageContext,
  AiProposal,
  AiProactiveEvent,
  AiProactiveSubscription,
  AiRunTrace,
  AiSseEvent,
  AiToolResult,
  AiWorkflow,
  AiWorkflowStep,
} from '@/modules/ai/model/ai.types'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'AiAssistantPanel',
})

const props = withDefaults(
  defineProps<{
    entryPoint?: AiEntryPoint
    context?: AiPageContext
    initialConversationNo?: string
    initialRunNo?: string
  }>(),
  {
    entryPoint: 'SIDE_PANEL',
    context: () => ({}),
    initialConversationNo: undefined,
    initialRunNo: undefined,
  },
)

const emit = defineEmits<{
  runChange: [runNo: string]
  conversationChange: [conversationNo: string]
  busyChange: [busy: boolean]
  contextChange: [context: AiPageContext]
}>()

type RecommendationItem = {
  label: string
  icon: Component
  iconClass: string
}

type ConversationTurnView = {
  id: string
  runNo?: string
  turnNo?: number
  status?: string
  userMessage?: AiChatMessage
  assistantMessage?: AiChatMessage
  toolResults: AiToolResult[]
  proposals: AiProposal[]
  workflows: AiWorkflow[]
}

type CharacterQueueState = {
  receivedContent: string
  characters: string[]
  completed: boolean
  finalContent?: string
  timer?: number
  waiters: Array<() => void>
}

const WORKFLOW_NO_FIELD = `workflow${'No'}` as keyof AiWorkflow
const STEP_TYPE_FIELD = `step${'Type'}` as keyof AiWorkflowStep
const PAYLOAD_WORKFLOW_NO_FIELD = `workflow${'No'}`
const PAYLOAD_STEP_TYPE_FIELD = `step${'Type'}`

const prompt = ref('')
const loading = ref(false)
const editingMessageId = ref<string | null>(null)
const editingContent = ref('')
const activeConversationNo = ref<string | undefined>(props.initialConversationNo)
const activeRunNo = ref<string | undefined>(props.initialRunNo)
const restoredContext = ref<AiPageContext>({})
const conversationTurns = ref<ConversationTurnView[]>([])
const proactiveSubscriptions = ref<AiProactiveSubscription[]>([])
const proactiveEvents = ref<AiProactiveEvent[]>([])
const activeController = ref<AbortController | null>(null)
const characterQueues = new Map<string, CharacterQueueState>()
const permissionStore = usePermissionStore()

const isPageEntry = computed(() => props.entryPoint === 'PAGE')
const effectiveContext = computed<AiPageContext>(() => {
  const explicit = normalizedContext(props.context)
  return explicit.objectType && explicit.objectId ? explicit : restoredContext.value
})
const canSend = computed(() => prompt.value.trim().length > 0 && !loading.value)
const hasTurns = computed(() => conversationTurns.value.length > 0)
const hasProactiveDetails = computed(
  () => proactiveSubscriptions.value.length > 0 || proactiveEvents.value.length > 0,
)
const canConfirmProposal = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.proposalConfirm),
)
const canViewWorkflow = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.workflowView),
)
const canManageWorkflow = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.workflowManage),
)
const canViewProactive = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.proactiveView),
)
const canManageProactive = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.proactiveUse),
)
const recommendations = computed<RecommendationItem[]>(() => {
  if (effectiveContext.value.objectType && effectiveContext.value.objectId) {
    const objectLabel = contextLabel(effectiveContext.value.objectType)
    return [
      {
        label: `总结这个${objectLabel}的关键风险`,
        icon: TrendingUp,
        iconClass: 'bg-[#E8F1FF] text-[#3370FF]',
      },
      {
        label: `列出这个${objectLabel}下一步可执行动作`,
        icon: CalendarCheck,
        iconClass: 'bg-[#EAF8F1] text-[#0F9F6E]',
      },
      {
        label: `为这个${objectLabel}生成跟进建议`,
        icon: Phone,
        iconClass: 'bg-[#FFF4E8] text-[#D97706]',
      },
      {
        label: `检查这个${objectLabel}是否需要主动提醒`,
        icon: Wand2,
        iconClass: 'bg-[#F0ECFF] text-[#6D5DF6]',
      },
    ]
  }
  return [
    {
      label: '查看今日跟进任务',
      icon: CalendarCheck,
      iconClass: 'bg-[#E8F1FF] text-[#3370FF]',
    },
    {
      label: '查询库存预警',
      icon: TrendingUp,
      iconClass: 'bg-[#FFF4E8] text-[#D97706]',
    },
    {
      label: '总结待处理交易',
      icon: FileText,
      iconClass: 'bg-[#EAF8F1] text-[#0F9F6E]',
    },
    {
      label: '生成经营提醒摘要',
      icon: Wand2,
      iconClass: 'bg-[#F0ECFF] text-[#6D5DF6]',
    },
  ]
})

async function sendPrompt(): Promise<void> {
  const content = prompt.value.trim()
  if (!content || loading.value) return
  prompt.value = ''
  loading.value = true

  const turn = createPendingTurn(content)
  conversationTurns.value.push(turn)
  const controller = new AbortController()
  activeController.value = controller
  let receivedEvent = false

  try {
    const run = await createAiRun({
      prompt: content,
      entryPoint: props.entryPoint,
      conversationNo: activeConversationNo.value,
      contextObjectType: effectiveContext.value.objectType,
      contextObjectId: effectiveContext.value.objectId,
    })
    turn.runNo = run.runNo
    turn.turnNo = run.turnNo
    turn.status = run.status
    activeRunNo.value = run.runNo
    if (run.conversationNo) {
      activeConversationNo.value = run.conversationNo
      emit('conversationChange', run.conversationNo)
    }
    if (run.contextObjectType && run.contextObjectId) {
      setRestoredContext(run.contextObjectType, run.contextObjectId)
    }
    emit('runChange', run.runNo)
    await streamAiRunEvents(
      run.runNo,
      (event) => {
        receivedEvent = true
        handleSseEvent(event, turn)
      },
      controller.signal,
    )
    await waitForCharacterDrain(turn.id)
    await refreshConversationAfterCompletedTurn(turn)
  } catch (error) {
    const assistantMessage = ensureAssistantMessage(turn)
    assistantMessage.pending = false
    if (isAbortError(error)) {
      assistantMessage.cancelled = true
      assistantMessage.content ||= '已停止生成'
      return
    }
    if (receivedEvent) {
      stopCharacterQueue(turn.id, assistantMessage)
      assistantMessage.error = true
      assistantMessage.content ||= '流式连接中断，请稍后重试'
      messageTip('AI 流式连接中断，已保留当前内容', 'error')
      return
    }
    assistantMessage.error = true
    assistantMessage.content = 'AI 助手暂时无法完成本次请求'
    messageTip(error instanceof Error ? error.message : 'AI 助手请求失败', 'error')
  } finally {
    loading.value = false
    activeController.value = null
    if (activeConversationNo.value) {
      emit('conversationChange', activeConversationNo.value)
    }
  }
}

async function refreshConversationAfterCompletedTurn(turn: ConversationTurnView): Promise<void> {
  if (!activeConversationNo.value || turn.status !== 'COMPLETED') return
  try {
    applyConversationDetail(await fetchAiConversation(activeConversationNo.value))
  } catch {
    // 实时结果已经可用时，恢复接口失败不覆盖用户刚收到的回答。
  }
}

function beginEditMessage(message: AiChatMessage): void {
  if (loading.value) {
    messageTip('请先停止当前生成，再编辑历史消息', 'warning')
    return
  }
  if (!message.messageNo || !message.canEdit) return
  editingMessageId.value = message.id
  editingContent.value = message.content
}

function cancelEditMessage(): void {
  editingMessageId.value = null
  editingContent.value = ''
}

async function saveEditedMessage(turn: ConversationTurnView): Promise<void> {
  const message = turn.userMessage
  const conversationNo = activeConversationNo.value
  const content = editingContent.value.trim()
  const expectedVersion = messageVersion(message)
  if (!message?.messageNo || !conversationNo || !content || expectedVersion === null) return
  if (loading.value) {
    messageTip('请先停止当前生成，再编辑历史消息', 'warning')
    return
  }

  loading.value = true
  const controller = new AbortController()
  activeController.value = controller
  const replacementTurn = createPendingTurn(content)
  const turnIndex = conversationTurns.value.findIndex((item) => item.id === turn.id)
  try {
    const run = await editAiMessage(conversationNo, message.messageNo, {
      content,
      expectedVersion,
    })
    if (turnIndex >= 0) {
      conversationTurns.value.splice(turnIndex, conversationTurns.value.length - turnIndex, replacementTurn)
    }
    cancelEditMessage()
    replacementTurn.runNo = run.runNo
    replacementTurn.turnNo = run.turnNo
    replacementTurn.status = run.status
    activeRunNo.value = run.runNo
    if (run.contextObjectType && run.contextObjectId) {
      setRestoredContext(run.contextObjectType, run.contextObjectId)
    }
    emit('runChange', run.runNo)
    await streamAiRunEvents(
      run.runNo,
      (event) => handleSseEvent(event, replacementTurn),
      controller.signal,
    )
    await waitForCharacterDrain(replacementTurn.id)
    await refreshConversationAfterCompletedTurn(replacementTurn)
  } catch (error) {
    const assistantMessage = ensureAssistantMessage(replacementTurn)
    if (isAbortError(error)) {
      assistantMessage.cancelled = true
      assistantMessage.content ||= '已停止生成'
    } else {
      assistantMessage.error = true
      assistantMessage.content ||= '消息修改后重新生成失败'
      messageTip(error instanceof Error ? error.message : '消息修改失败', 'error')
    }
  } finally {
    loading.value = false
    activeController.value = null
  }
}

async function withdrawMessage(turn: ConversationTurnView): Promise<void> {
  const message = turn.userMessage
  const conversationNo = activeConversationNo.value
  const expectedVersion = messageVersion(message)
  if (!message?.messageNo || !message.canWithdraw || !conversationNo || expectedVersion === null) return
  if (loading.value) {
    messageTip('请先停止当前生成，再撤回历史消息', 'warning')
    return
  }
  try {
    await messageConfirm('撤回只会移出后续 AI 上下文，不会撤销已经执行的业务操作。是否继续？')
  } catch {
    return
  }
  loading.value = true
  try {
    applyConversationDetail(
      await withdrawAiMessage(conversationNo, message.messageNo, { expectedVersion }),
    )
    messageTip('消息已撤回，后续回答不会再使用该内容', 'success')
  } catch (error) {
    messageTip(error instanceof Error ? error.message : '消息撤回失败', 'error')
  } finally {
    loading.value = false
  }
}

function messageVersion(message?: AiChatMessage): number | null {
  const version = message?.version ?? message?.revisionNo
  return typeof version === 'number' ? version : null
}

async function stopGeneration(): Promise<void> {
  const runNo = activeRunNo.value
  activeController.value?.abort()
  if (!runNo) {
    loading.value = false
    markActiveAssistantCancelled()
    return
  }
  try {
    await cancelAiRun(runNo, '用户停止生成')
  } catch {
    // 停止生成失败不覆盖已经收到的部分回答。
  } finally {
    loading.value = false
    markActiveAssistantCancelled()
  }
}

async function restoreRunTrace(runNo: string): Promise<void> {
  if (!runNo) return
  if (loading.value) {
    messageTip('请先停止当前生成，再切换 AI 会话', 'warning')
    return
  }
  activeRunNo.value = runNo
  emit('runChange', runNo)
  loading.value = true
  try {
    const trace = await fetchAiRunTrace(runNo)
    setRestoredContext(trace.run.contextObjectType, trace.run.contextObjectId)
    if (trace.run.conversationNo) {
      activeConversationNo.value = trace.run.conversationNo
      emit('conversationChange', trace.run.conversationNo)
    }
    conversationTurns.value = [toTurnFromTrace(trace)]
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI Run 恢复失败', 'error')
  } finally {
    loading.value = false
  }
}

async function restoreConversation(conversationNo: string): Promise<void> {
  if (!conversationNo) return
  if (loading.value) {
    messageTip('请先停止当前生成，再切换 AI 会话', 'warning')
    return
  }
  activeConversationNo.value = conversationNo
  emit('conversationChange', conversationNo)
  loading.value = true
  try {
    applyConversationDetail(await fetchAiConversation(conversationNo))
  } catch (error) {
    messageTip(error instanceof Error ? error.message : 'AI 会话恢复失败', 'error')
  } finally {
    loading.value = false
  }
}

function applyConversationDetail(detail: AiConversationDetail): void {
  activeConversationNo.value = detail.conversation.conversationNo
  setRestoredContext(
    detail.conversation.contextObjectType,
    detail.conversation.contextObjectId,
  )
  activeRunNo.value = detail.latestRun?.runNo
  if (activeRunNo.value) {
    emit('runChange', activeRunNo.value)
  }
  if (Array.isArray(detail.turns)) {
    conversationTurns.value = detail.turns.map(toTurnFromConversationTurn)
    return
  }
  if (detail.latestRunTrace) {
    conversationTurns.value = [toTurnFromTrace(detail.latestRunTrace)]
    return
  }
  conversationTurns.value = fallbackTurnsFromMessages(detail)
}

function setRestoredContext(objectType?: string, objectId?: string): void {
  restoredContext.value = normalizedContext({ objectType, objectId })
  emit('contextChange', effectiveContext.value)
}

function normalizedContext(context?: AiPageContext): AiPageContext {
  const objectType = context?.objectType?.trim()
  const objectId = context?.objectId?.trim()
  return objectType && objectId ? { objectType, objectId } : {}
}

function toTurnFromConversationTurn(turn: AiConversationTurn): ConversationTurnView {
  const fallbackTrace = {
    run: turn.run,
    messages: turn.messages ?? [],
    toolCalls: turn.toolResults ?? [],
    proposals: turn.proposals ?? [],
    approvals: turn.approvals ?? [],
    executionEvents: turn.executionEvents ?? [],
    workflows: turn.workflows ?? [],
  }
  const view = toTurnFromTrace(fallbackTrace)
  view.turnNo = turn.turnNo ?? turn.run.turnNo
  view.status = turn.status ?? turn.run.status
  if (turn.userMessage) {
    view.userMessage = toChatMessage(turn.userMessage)
  }
  if (turn.assistantMessage) {
    view.assistantMessage = toChatMessage(turn.assistantMessage)
  }
  return view
}

function toTurnFromTrace(trace: AiRunTrace): ConversationTurnView {
  const visibleMessages = trace.messages.filter((message) => message.visibleToUser !== false)
  const userMessage = visibleMessages.find((message) => message.role === 'USER')
  const assistantMessage = [...visibleMessages].reverse().find((message) => message.role === 'ASSISTANT')
  return {
    id: trace.run.runNo,
    runNo: trace.run.runNo,
    turnNo: trace.run.turnNo,
    status: trace.run.status,
    userMessage: userMessage ? toChatMessage(userMessage) : undefined,
    assistantMessage: assistantMessage ? toChatMessage(assistantMessage) : undefined,
    toolResults: trace.toolCalls
      .filter((toolCall) => !isProposalToolName(toolCall.toolName))
      .map(toToolResult),
    proposals: trace.proposals.map(toProposalFromTrace),
    workflows: trace.workflows ?? [],
  }
}

function fallbackTurnsFromMessages(detail: AiConversationDetail): ConversationTurnView[] {
  const turns: ConversationTurnView[] = []
  let currentTurn: ConversationTurnView | null = null
  for (const message of detail.messages.filter((item) => item.visibleToUser !== false)) {
    if (message.role === 'USER' || currentTurn === null) {
      currentTurn = {
        id: `message-turn-${String(message.id)}`,
        userMessage: message.role === 'USER' ? toChatMessage(message) : undefined,
        toolResults: [],
        proposals: [],
        workflows: [],
      }
      turns.push(currentTurn)
      if (message.role !== 'USER') {
        currentTurn.assistantMessage = toChatMessage(message)
      }
      continue
    }
    if (message.role === 'ASSISTANT') {
      currentTurn.assistantMessage = toChatMessage(message)
    }
  }
  return turns
}

function createPendingTurn(content: string): ConversationTurnView {
  return {
    id: `pending-turn-${Date.now()}`,
    userMessage: { id: `user-${Date.now()}`, role: 'user', content },
    assistantMessage: {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      pending: true,
    },
    toolResults: [],
    proposals: [],
    workflows: [],
  }
}

function toChatMessage(message: AiRunTrace['messages'][number]): AiChatMessage {
  const isActiveUserMessage =
    message.role === 'USER' &&
    (message.status === undefined || message.status === 'ACTIVE') &&
    typeof message.version === 'number'
  return {
    id: `trace-message-${String(message.id)}`,
    messageNo: message.messageNo,
    role: traceRoleToChatRole(message.role),
    content: message.contentSummary ?? '消息已撤回',
    status: message.status,
    revisionNo: message.revisionNo,
    version: message.version,
    canEdit: message.canEdit ?? isActiveUserMessage,
    canWithdraw: message.canWithdraw ?? isActiveUserMessage,
  }
}

function traceRoleToChatRole(role: AiRunTrace['messages'][number]['role']): AiChatMessage['role'] {
  if (role === 'USER') return 'user'
  if (role === 'ASSISTANT') return 'assistant'
  return 'system'
}

function toToolResult(toolCall: AiRunTrace['toolCalls'][number]): AiToolResult {
  return {
    toolName: toolCall.toolName,
    summary: toolCall.outputSummary || toolCall.inputSummary || '工具调用完成',
    data: toolCall.displayPayload,
    objectRefs: toolCall.objectRefs,
  }
}

function toProposalFromTrace(proposal: AiRunTrace['proposals'][number]): AiProposal {
  return {
    proposalId: proposal.id,
    proposalType: proposal.proposalType,
    riskLevel: proposal.riskLevel,
    permissionCode: proposal.permissionCode,
    relatedObjectType: proposal.relatedObjectType,
    relatedObjectId: proposal.relatedObjectId,
    paramsSummary: proposal.paramsSummary,
    impactSummary: proposal.impactSummary,
    expiresTime: proposal.expiresTime,
    status: proposal.status,
  }
}

function handleSseEvent(event: AiSseEvent, turn: ConversationTurnView): void {
  const assistantMessage = ensureAssistantMessage(turn)
  const delta = event.payload.content_delta ?? event.payload.delta
  if (event.type === 'message_delta' && typeof delta === 'string') {
    enqueueAssistantCharacters(turn, delta)
    return
  }
  if (event.type === 'message_completed') {
    completeAssistantCharacters(
      turn,
      typeof event.payload.content === 'string' ? event.payload.content : undefined,
    )
    return
  }
  if (event.type === 'tool_call_completed') {
    const toolName = String(event.payload.toolName ?? event.payload.tool_name ?? '')
    // Proposal 有独立的确认卡片，重复展示成普通业务结果会让用户误以为已经写入。
    if (isProposalToolName(toolName)) return
    turn.toolResults.push({
      toolName,
      summary: String(event.payload.outputSummary ?? event.payload.summary ?? '工具调用完成'),
      data: event.payload.data,
      objectRefs: String(event.payload.objectRefs ?? event.payload.object_refs ?? ''),
    })
    return
  }
  if (event.type === 'proposal_created') {
    turn.proposals.push(toProposal(event.payload))
    return
  }
  if (String(event.type).startsWith('workflow_')) {
    applyWorkflowEvent(event, turn)
    return
  }
  if (event.type === 'error') {
    stopCharacterQueue(turn.id, assistantMessage)
    assistantMessage.pending = false
    assistantMessage.error = true
    assistantMessage.content ||= String(event.payload.message ?? 'AI 助手请求失败')
  }
  if (event.type === 'run_completed') {
    completeAssistantCharacters(turn)
    turn.status = String(event.payload.status ?? 'COMPLETED')
  }
  if (event.type === 'run_cancelled') {
    stopCharacterQueue(turn.id, assistantMessage)
    assistantMessage.pending = false
    assistantMessage.cancelled = true
    assistantMessage.content ||= '已停止生成'
    turn.status = 'CANCELLED'
  }
}

function isProposalToolName(toolName: string): boolean {
  return toolName === 'create_communication_record_proposal' || toolName === 'create_follow_task_proposal'
}

function enqueueAssistantCharacters(turn: ConversationTurnView, content: string): void {
  if (!content) return
  const state = getCharacterQueue(turn.id)
  state.receivedContent += content
  state.characters.push(...Array.from(content))
  scheduleCharacterDrain(turn, state)
}

function completeAssistantCharacters(turn: ConversationTurnView, finalContent?: string): void {
  const state = getCharacterQueue(turn.id)
  state.completed = true
  if (finalContent && finalContent.startsWith(state.receivedContent)) {
    const suffix = finalContent.slice(state.receivedContent.length)
    state.receivedContent = finalContent
    state.characters.push(...Array.from(suffix))
  } else if (finalContent) {
    state.finalContent = finalContent
  }
  scheduleCharacterDrain(turn, state)
}

function getCharacterQueue(turnId: string): CharacterQueueState {
  const existing = characterQueues.get(turnId)
  if (existing) return existing
  const created: CharacterQueueState = {
    receivedContent: '',
    characters: [],
    completed: false,
    waiters: [],
  }
  characterQueues.set(turnId, created)
  return created
}

function scheduleCharacterDrain(
  turn: ConversationTurnView,
  state: CharacterQueueState,
): void {
  if (state.timer) return
  const drain = (): void => {
    state.timer = undefined
    const assistantMessage = ensureAssistantMessage(turn)
    const batchSize = state.completed ? Math.max(1, Math.ceil(state.characters.length / 60)) : 1
    if (state.characters.length) {
      assistantMessage.content += state.characters.splice(0, batchSize).join('')
    }
    if (state.characters.length) {
      state.timer = window.setTimeout(drain, 12)
      return
    }
    if (!state.completed) return
    if (state.finalContent) assistantMessage.content = state.finalContent
    assistantMessage.content ||= '本次处理已完成'
    assistantMessage.pending = false
    state.waiters.splice(0).forEach((resolve) => resolve())
    characterQueues.delete(turn.id)
  }
  state.timer = window.setTimeout(drain, 12)
}

function waitForCharacterDrain(turnId: string): Promise<void> {
  const state = characterQueues.get(turnId)
  if (!state || (state.completed && !state.timer && !state.characters.length)) {
    return Promise.resolve()
  }
  return new Promise((resolve) => state.waiters.push(resolve))
}

function stopCharacterQueue(turnId: string, message?: AiChatMessage): void {
  const state = characterQueues.get(turnId)
  if (!state) return
  if (state.timer) window.clearTimeout(state.timer)
  state.timer = undefined
  if (message && state.characters.length) {
    message.content += state.characters.join('')
  }
  state.characters = []
  state.waiters.splice(0).forEach((resolve) => resolve())
  characterQueues.delete(turnId)
}

function ensureAssistantMessage(turn: ConversationTurnView): AiChatMessage {
  if (!turn.assistantMessage) {
    turn.assistantMessage = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      pending: true,
    }
  }
  return turn.assistantMessage
}

function applyWorkflowEvent(event: AiSseEvent, turn: ConversationTurnView): void {
  const flowNo = String(
    event.payload[PAYLOAD_WORKFLOW_NO_FIELD] ??
      event.payload.workflow_no ??
      activeRunNo.value ??
      '',
  )
  if (!flowNo) return
  const workflow = upsertWorkflow(turn, flowNo, event.payload)
  if (event.type === 'workflow_step_started' || event.type === 'workflow_step_completed') {
    upsertWorkflowStep(workflow, event.payload)
  }
  if (typeof event.payload.status === 'string') {
    workflow.status = event.payload.status as AiWorkflow['status']
  }
}

function upsertWorkflow(
  turn: ConversationTurnView,
  flowNo: string,
  payload: Record<string, unknown>,
): AiWorkflow {
  const existing = turn.workflows.find((workflow) => getWorkflowNo(workflow) === flowNo)
  if (existing) {
    existing.title = String(payload.title ?? existing.title)
    existing.workflowType = String(
      payload.workflowType ?? payload.workflow_type ?? existing.workflowType,
    )
    return existing
  }
  const created: AiWorkflow = {
    [WORKFLOW_NO_FIELD]: flowNo,
    workflowType: String(payload.workflowType ?? payload.workflow_type ?? 'CUSTOMER_FOLLOW_UP'),
    title: String(payload.title ?? 'AI 受控工作流'),
    status: String(payload.status ?? 'RUNNING') as AiWorkflow['status'],
    steps: [],
  }
  if (turn.runNo) {
    created.runNo = turn.runNo
  }
  turn.workflows.push(created)
  return created
}

function upsertWorkflowStep(workflow: AiWorkflow, payload: Record<string, unknown>): void {
  const stepNo = Number(payload.stepNo ?? payload.step_no ?? 0)
  if (!stepNo) return
  const existing = workflow.steps.find((step) => step.stepNo === stepNo)
  const nextStep: AiWorkflowStep = {
    stepNo,
    [STEP_TYPE_FIELD]: String(payload[PAYLOAD_STEP_TYPE_FIELD] ?? payload.step_type ?? ''),
    title: String(payload.title ?? '工作流步骤'),
    status: String(payload.status ?? 'RUNNING') as AiWorkflowStep['status'],
  }
  if (typeof payload.toolName === 'string') {
    nextStep.toolName = payload.toolName
  }
  if (typeof payload.outputSummary === 'string') {
    nextStep.outputSummary = payload.outputSummary
  }
  if (existing) {
    Object.assign(existing, nextStep)
    return
  }
  workflow.steps.push(nextStep)
  workflow.steps.sort((left, right) => left.stepNo - right.stepNo)
}

function toProposal(payload: Record<string, unknown>): AiProposal {
  return {
    proposalId: String(payload.proposalId ?? payload.proposal_id ?? ''),
    proposalType: String(payload.proposalType ?? payload.proposal_type ?? ''),
    riskLevel: String(payload.riskLevel ?? payload.risk_level ?? 'LOW') as AiProposal['riskLevel'],
    permissionCode: String(payload.permissionCode ?? payload.permission_code ?? ''),
    relatedObjectType: String(payload.relatedObjectType ?? payload.related_object_type ?? ''),
    relatedObjectId: String(payload.relatedObjectId ?? payload.related_object_id ?? ''),
    paramsSummary: String(payload.paramsSummary ?? payload.params_summary ?? ''),
    impactSummary: String(payload.impactSummary ?? payload.impact_summary ?? ''),
    expiresTime: String(payload.expiresTime ?? payload.expires_time ?? ''),
    status: 'PENDING_CONFIRMATION',
  }
}

async function confirmProposal(proposal: AiProposal): Promise<void> {
  loading.value = true
  try {
    const result = await confirmAiProposal(proposal.proposalId)
    proposal.status = result.status
    messageTip(result.resultSummary || '提议已执行', 'success')
  } finally {
    loading.value = false
  }
}

async function rejectProposal(proposal: AiProposal): Promise<void> {
  loading.value = true
  try {
    const result = await rejectAiProposal(proposal.proposalId)
    proposal.status = result.status
  } finally {
    loading.value = false
  }
}

async function replaceWorkflow(updatedWorkflow: AiWorkflow): Promise<void> {
  const flowNo = getWorkflowNo(updatedWorkflow)
  for (const turn of conversationTurns.value) {
    const index = turn.workflows.findIndex((item) => getWorkflowNo(item) === flowNo)
    if (index >= 0) {
      turn.workflows.splice(index, 1, updatedWorkflow)
      return
    }
  }
}

async function pauseWorkflow(workflow: AiWorkflow): Promise<void> {
  loading.value = true
  try {
    await replaceWorkflow(
      await pauseAiWorkflow(getWorkflowNo(workflow), { reason: '用户从 AI 面板暂停' }),
    )
  } finally {
    loading.value = false
  }
}

async function resumeWorkflow(workflow: AiWorkflow): Promise<void> {
  loading.value = true
  try {
    await replaceWorkflow(await resumeAiWorkflow(getWorkflowNo(workflow)))
  } finally {
    loading.value = false
  }
}

async function cancelWorkflow(workflow: AiWorkflow): Promise<void> {
  loading.value = true
  try {
    await replaceWorkflow(
      await cancelAiWorkflow(getWorkflowNo(workflow), { reason: '用户从 AI 面板取消' }),
    )
  } finally {
    loading.value = false
  }
}

function getWorkflowNo(workflow: AiWorkflow): string {
  return String(workflow[WORKFLOW_NO_FIELD])
}

async function loadProactiveData(): Promise<void> {
  const [subscriptions, events] = await Promise.all([
    listAiProactiveSubscriptions(),
    listAiProactiveEvents(1, 20),
  ])
  proactiveSubscriptions.value = subscriptions
  proactiveEvents.value = events
}

async function createInventorySubscription(): Promise<void> {
  loading.value = true
  try {
    await createAiProactiveSubscription({
      subscriptionType: 'INVENTORY_ALERT',
      frequency: 'DAILY',
      quietStartTime: '20:00',
      quietEndTime: '08:00',
      dailyLimit: 3,
      maxResults: 10,
      duplicateWindowMinutes: 60,
      configSummary: '库存预警主动提醒',
    })
    await loadProactiveData()
  } finally {
    loading.value = false
  }
}

async function createFollowSubscription(): Promise<void> {
  loading.value = true
  try {
    await createAiProactiveSubscription({
      subscriptionType: 'FOLLOW_UP_REMINDER',
      frequency: 'DAILY',
      quietStartTime: '20:00',
      quietEndTime: '08:00',
      dailyLimit: 5,
      maxResults: 10,
      duplicateWindowMinutes: 60,
      configSummary: '跟进任务主动提醒',
    })
    await loadProactiveData()
  } finally {
    loading.value = false
  }
}

async function pauseSubscription(subscription: AiProactiveSubscription): Promise<void> {
  loading.value = true
  try {
    await pauseAiProactiveSubscription(subscription.subscriptionNo)
    await loadProactiveData()
  } finally {
    loading.value = false
  }
}

async function resumeSubscription(subscription: AiProactiveSubscription): Promise<void> {
  loading.value = true
  try {
    await resumeAiProactiveSubscription(subscription.subscriptionNo)
    await loadProactiveData()
  } finally {
    loading.value = false
  }
}

async function cancelSubscription(subscription: AiProactiveSubscription): Promise<void> {
  loading.value = true
  try {
    await cancelAiProactiveSubscription(subscription.subscriptionNo)
    await loadProactiveData()
  } finally {
    loading.value = false
  }
}

async function generateProactiveEvents(): Promise<void> {
  loading.value = true
  try {
    proactiveEvents.value = await generateAiProactiveEvents()
  } finally {
    loading.value = false
  }
}

function useRecommendation(value: string): void {
  prompt.value = value
}

function contextLabel(objectType: string): string {
  const labels: Record<string, string> = {
    CUSTOMER: '客户',
    CLUE: '线索',
    OPPORTUNITY: '商机',
    QUOTE: '报价',
    TEST_DRIVE: '试驾',
    DELIVERY: '交付',
    PRODUCT: '产品',
    TRANSACTION: '交易',
    TRAN: '交易',
  }
  return labels[objectType] ?? '对象'
}

function markActiveAssistantCancelled(): void {
  const lastTurn = [...conversationTurns.value].reverse().find((turn) => turn.assistantMessage)
  if (!lastTurn?.assistantMessage) return
  stopCharacterQueue(lastTurn.id, lastTurn.assistantMessage)
  lastTurn.assistantMessage.pending = false
  lastTurn.assistantMessage.cancelled = true
  lastTurn.assistantMessage.content ||= '已停止生成'
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function hasTurnBusinessResults(turn: ConversationTurnView): boolean {
  return turn.toolResults.length > 0
}

function hasTurnProcessDetails(turn: ConversationTurnView): boolean {
  return turn.toolResults.length > 0 || (canViewWorkflow.value && turn.workflows.length > 0)
}

function turnProcessSummaries(turn: ConversationTurnView): string[] {
  return turn.toolResults
    .map((result) => result.summary)
    .filter((summary): summary is string => Boolean(summary))
}

onBeforeUnmount(() => {
  activeController.value?.abort()
  characterQueues.forEach((_, turnId) => stopCharacterQueue(turnId))
  emit('busyChange', false)
})

onMounted(() => {
  if (canViewProactive.value) void loadProactiveData().catch(() => undefined)
})

watch(
  () => props.context,
  () => emit('contextChange', effectiveContext.value),
  { deep: true, immediate: true },
)

watch(
  loading,
  (busy) => emit('busyChange', busy),
  { immediate: true },
)

watch(
  () => props.initialConversationNo,
  (conversationNo) => {
    if (
      conversationNo &&
      (conversationNo !== activeConversationNo.value || conversationTurns.value.length === 0)
    ) {
      void restoreConversation(conversationNo)
    }
  },
  { immediate: true },
)

watch(
  () => props.initialRunNo,
  (runNo) => {
    if (!props.initialConversationNo && runNo) void restoreRunTrace(runNo)
  },
  { immediate: true },
)
</script>

<template>
  <section
    data-testid="ai-assistant-panel"
    class="flex h-full min-h-0 flex-col bg-[var(--crm-bg-surface)]"
    :class="isPageEntry ? 'bg-[var(--crm-bg-page)]' : ''"
  >
    <div
      class="min-h-0 flex-1 overflow-y-auto"
      :class="isPageEntry ? 'px-3 py-5 sm:px-6 sm:py-6' : 'px-3 py-4 sm:px-4'"
    >
      <div
        class="mx-auto flex min-h-full w-full flex-col"
        :class="isPageEntry ? 'max-w-[980px]' : 'max-w-none'"
      >
        <div
          v-if="!hasTurns"
          data-testid="ai-empty-state"
          class="ai-message-in flex flex-1 flex-col items-center justify-center py-10 text-center"
        >
          <div
            class="ai-icon-glow flex h-14 w-14 items-center justify-center rounded-2xl text-white shadow-[0_18px_32px_rgba(51,112,255,0.28)]"
          >
            <Sparkles class="h-7 w-7" />
          </div>
          <h2 class="mt-5 text-xl font-semibold text-[var(--crm-text-primary)]">
            你好，我是 AI 助手
          </h2>
          <div
            class="mt-6 grid w-full gap-3"
            :class="isPageEntry ? 'max-w-[680px] sm:grid-cols-2' : 'grid-cols-1'"
          >
            <button
              v-for="item in recommendations"
              :key="item.label"
              class="group flex items-center gap-3 rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-4 py-3 text-left text-sm text-[var(--crm-text-secondary)] shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-[var(--crm-primary)] hover:text-[var(--crm-primary)] hover:shadow-[0_12px_26px_rgba(30,41,59,0.08)]"
              type="button"
              @click="useRecommendation(item.label)"
            >
              <span
                class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
                :class="item.iconClass"
              >
                <component :is="item.icon" class="h-4 w-4" />
              </span>
              <span class="min-w-0 flex-1 truncate font-medium">{{ item.label }}</span>
            </button>
          </div>
        </div>

        <div v-else class="space-y-5 pb-4">
          <article
            v-for="turn in conversationTurns"
            :key="turn.id"
            data-testid="ai-conversation-turn"
            class="space-y-4"
          >
            <div
              v-if="turn.userMessage"
              class="ai-message-in flex justify-end gap-3"
              data-testid="ai-user-message"
            >
              <div class="group max-w-[82%]">
                <div
                  v-if="editingMessageId === turn.userMessage.id"
                  class="min-w-[280px] rounded-2xl border border-[var(--crm-primary)] bg-[var(--crm-bg-surface)] p-3 shadow-sm"
                >
                  <Textarea
                    v-model="editingContent"
                    class="min-h-20 resize-none"
                    maxlength="4000"
                    aria-label="编辑消息内容"
                  />
                  <div class="mt-2 flex justify-end gap-2">
                    <Button size="sm" variant="ghost" type="button" @click="cancelEditMessage">
                      取消
                    </Button>
                    <Button
                      size="sm"
                      type="button"
                      :disabled="!editingContent.trim()"
                      @click="saveEditedMessage(turn)"
                    >
                      保存并重新生成
                    </Button>
                  </div>
                </div>
                <div
                  v-else
                  class="rounded-2xl rounded-tr-md bg-[var(--crm-primary)] px-4 py-2.5 text-sm leading-6 text-white shadow-sm"
                >
                  {{ turn.userMessage.content }}
                </div>
                <div
                  v-if="
                    editingMessageId !== turn.userMessage.id &&
                    (turn.userMessage.canEdit || turn.userMessage.canWithdraw)
                  "
                  class="mt-1 flex justify-end gap-1 opacity-70 transition-opacity group-hover:opacity-100"
                >
                  <Button
                    v-if="turn.userMessage.canEdit"
                    variant="ghost"
                    size="sm"
                    type="button"
                    class="h-7 px-2 text-xs"
                    :disabled="loading"
                    aria-label="编辑这条消息"
                    @click="beginEditMessage(turn.userMessage)"
                  >
                    <Pencil class="mr-1 h-3.5 w-3.5" />
                    编辑
                  </Button>
                  <Button
                    v-if="turn.userMessage.canWithdraw"
                    variant="ghost"
                    size="sm"
                    type="button"
                    class="h-7 px-2 text-xs"
                    :disabled="loading"
                    aria-label="撤回这条消息"
                    @click="withdrawMessage(turn)"
                  >
                    <Undo2 class="mr-1 h-3.5 w-3.5" />
                    撤回
                  </Button>
                </div>
              </div>
              <div
                class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--crm-bg-muted)] text-[var(--crm-text-secondary)]"
              >
                <UserRound class="h-4 w-4" />
              </div>
            </div>

            <div
              v-if="turn.assistantMessage"
              class="ai-message-in flex justify-start gap-3"
              data-testid="ai-assistant-message"
            >
              <div
                class="ai-icon-glow flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-white shadow-[0_10px_22px_rgba(51,112,255,0.22)]"
              >
                <Sparkles class="h-4 w-4" />
              </div>
              <div
                class="max-w-[84%] px-1 py-1 text-sm leading-6 text-[var(--crm-text-primary)]"
              >
                <AiMarkdownMessage
                  v-if="turn.assistantMessage.content"
                  :content="turn.assistantMessage.content"
                />
                <span
                  v-if="turn.assistantMessage.pending && !turn.assistantMessage.content"
                  class="inline-flex items-center gap-1"
                >
                  <span class="ai-typing-dot" />
                  <span class="ai-typing-dot [animation-delay:0.16s]" />
                  <span class="ai-typing-dot [animation-delay:0.32s]" />
                </span>
                <Loader2
                  v-else-if="turn.assistantMessage.pending"
                  class="ml-2 inline h-4 w-4 animate-spin"
                />
                <div
                  v-if="turn.assistantMessage.cancelled"
                  class="mt-2 text-xs font-medium text-[var(--crm-text-tertiary)]"
                >
                  已停止生成，已保留当前部分内容。
                </div>
              </div>
            </div>

            <section
              v-if="hasTurnBusinessResults(turn)"
              data-testid="ai-business-results"
              class="space-y-2 sm:ml-12"
            >
              <AiToolResultCard
                v-for="(result, index) in turn.toolResults"
                :key="`${turn.id}-${result.toolName}-${index}`"
                :result="result"
              />
            </section>

            <AiProposalCard
              v-for="proposal in turn.proposals"
              :key="String(proposal.proposalId)"
              class="sm:ml-12"
              :proposal="proposal"
              :loading="loading"
              :can-confirm="canConfirmProposal"
              @confirm="confirmProposal"
              @reject="rejectProposal"
            />

            <details
              v-if="hasTurnProcessDetails(turn)"
              data-testid="ai-execution-details"
              class="rounded-lg bg-[var(--crm-bg-muted)] p-3 text-sm text-[var(--crm-text-secondary)] sm:ml-12"
            >
              <summary class="cursor-pointer text-sm font-medium text-[var(--crm-text-secondary)]">
                查看处理过程
              </summary>
              <div class="mt-3 space-y-4">
                <section v-if="turnProcessSummaries(turn).length" class="space-y-2">
                  <div class="text-xs font-semibold text-[var(--crm-text-tertiary)]">已处理事项</div>
                  <ul class="space-y-1 text-xs text-[var(--crm-text-secondary)]">
                    <li
                      v-for="(summary, index) in turnProcessSummaries(turn)"
                      :key="`${summary}-${index}`"
                      class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1.5"
                    >
                      {{ summary }}
                    </li>
                  </ul>
                </section>
                <AiWorkflowPanel
                  v-if="canViewWorkflow && turn.workflows.length"
                  :workflows="turn.workflows"
                  :loading="loading"
                  :can-manage="canManageWorkflow"
                  @pause="pauseWorkflow"
                  @resume="resumeWorkflow"
                  @cancel="cancelWorkflow"
                />
              </div>
            </details>
          </article>

          <details
            v-if="canViewProactive && hasProactiveDetails"
            data-testid="ai-proactive-details"
            class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-3 text-sm text-[var(--crm-text-secondary)]"
          >
            <summary class="cursor-pointer text-sm font-medium text-[var(--crm-text-secondary)]">
              查看主动提醒
            </summary>
            <AiProactivePanel
              class="mt-3"
              :subscriptions="proactiveSubscriptions"
              :events="proactiveEvents"
              :loading="loading"
              :can-manage="canManageProactive"
              @create-inventory="createInventorySubscription"
              @create-follow="createFollowSubscription"
              @pause="pauseSubscription"
              @resume="resumeSubscription"
              @cancel="cancelSubscription"
              @generate="generateProactiveEvents"
            />
          </details>
        </div>
      </div>
    </div>

    <form
      class="shrink-0 border-t border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)]"
      :class="isPageEntry ? 'px-3 py-4 sm:px-6' : 'p-3'"
      @submit.prevent="sendPrompt"
    >
      <div
        class="mx-auto flex items-end gap-2 rounded-2xl border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] px-3 py-2 shadow-sm transition-colors focus-within:border-[var(--crm-primary)]"
        :class="isPageEntry ? 'max-w-[980px]' : 'max-w-none'"
        data-testid="ai-composer"
      >
        <Textarea
          v-model="prompt"
          rows="1"
          class="max-h-28 min-h-10 flex-1 resize-none border-0 bg-transparent px-0 py-2 leading-6 shadow-none focus-visible:ring-0"
          maxlength="4000"
          placeholder="输入问题"
          @keydown.enter.exact.prevent="sendPrompt"
        />
        <Button
          v-if="loading"
          type="button"
          size="icon"
          class="h-9 w-9 shrink-0 rounded-full"
          aria-label="停止生成"
          @click="stopGeneration"
        >
          <Square class="h-4 w-4" />
        </Button>
        <Button
          v-else
          type="submit"
          size="icon"
          class="h-9 w-9 shrink-0 rounded-full"
          :disabled="!canSend"
          aria-label="发送"
        >
          <Send class="h-4 w-4" />
        </Button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.ai-icon-glow {
  background: linear-gradient(135deg, #3370ff 0%, #6d5df6 100%);
}

.ai-message-in {
  animation: ai-message-in 180ms ease-out both;
}

.ai-typing-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--crm-primary);
  animation: ai-typing 1.2s ease-in-out infinite;
}

@keyframes ai-message-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes ai-typing {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}
</style>
