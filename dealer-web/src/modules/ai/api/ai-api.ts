import { httpClient } from '@/shared/api/http-client'
import { env } from '@/shared/config/env'
import { readStoredToken } from '@/shared/storage/token-storage'
import type {
  AiConversation,
  AiConversationDetail,
  AiPolicy,
  AiProviderConfig,
  AiProviderConfigTestResponse,
  AiProposalConfirmResponse,
  AiProactiveEvent,
  AiProactiveSubscription,
  AiWorkflow,
  AiWorkflowActionRequest,
  AiRun,
  AiRunTrace,
  AiSseEvent,
  CreateAiConversationRequest,
  CreateAiProviderConfigRequest,
  CreateAiProactiveSubscriptionRequest,
  CreateAiWorkflowRequest,
  CreateAiRunRequest,
  EditAiMessageRequest,
  RotateAiProviderKeyRequest,
  RenameAiConversationRequest,
  UpdateAiPolicyRequest,
  UpdateAiProviderConfigRequest,
  WithdrawAiMessageRequest,
} from '@/modules/ai/model/ai.types'

const MAX_SSE_RECONNECTS = 2
const TERMINAL_SSE_EVENTS = new Set(['run_completed', 'run_cancelled'])

export function listAiConversations(includeArchived = false): Promise<AiConversation[]> {
  return httpClient.get<AiConversation[]>('/api/ai/conversations', {
    params: { includeArchived },
  })
}

export function createAiConversation(
  data: CreateAiConversationRequest,
): Promise<AiConversation> {
  return httpClient.post<AiConversation>('/api/ai/conversations', normalizeContextPayload(data))
}

export function fetchAiConversation(conversationNo: string): Promise<AiConversationDetail> {
  return httpClient.get<AiConversationDetail>(
    `/api/ai/conversations/${encodeURIComponent(conversationNo)}`,
  )
}

export function renameAiConversation(
  conversationNo: string,
  data: RenameAiConversationRequest,
): Promise<AiConversation> {
  return httpClient.patch<AiConversation>(
    `/api/ai/conversations/${encodeURIComponent(conversationNo)}/title`,
    data,
  )
}

export function archiveAiConversation(conversationNo: string): Promise<AiConversation> {
  return httpClient.post<AiConversation>(
    `/api/ai/conversations/${encodeURIComponent(conversationNo)}/archive`,
  )
}

export function editAiMessage(
  conversationNo: string,
  messageNo: string,
  data: EditAiMessageRequest,
): Promise<AiRun> {
  return httpClient.patch<AiRun>(
    `/api/ai/conversations/${encodeURIComponent(conversationNo)}/messages/${encodeURIComponent(messageNo)}`,
    data,
  )
}

export function withdrawAiMessage(
  conversationNo: string,
  messageNo: string,
  data: WithdrawAiMessageRequest,
): Promise<AiConversationDetail> {
  return httpClient.post<AiConversationDetail>(
    `/api/ai/conversations/${encodeURIComponent(conversationNo)}/messages/${encodeURIComponent(messageNo)}/withdraw`,
    data,
  )
}

export function createAiRun(data: CreateAiRunRequest): Promise<AiRun> {
  return httpClient.post<AiRun>('/api/ai/runs', normalizeContextPayload(data))
}

export function fetchAiRun(runNo: string): Promise<AiRun> {
  return httpClient.get<AiRun>(`/api/ai/runs/${runNo}`)
}

export function fetchAiRunTrace(runNo: string): Promise<AiRunTrace> {
  return httpClient.get<AiRunTrace>(`/api/ai/runs/${runNo}/trace`)
}

export function cancelAiRun(runNo: string, reason?: string): Promise<AiRun> {
  return httpClient.post<AiRun>(`/api/ai/runs/${runNo}/cancel`, reason ? { reason } : {})
}

export function confirmAiProposal(proposalId: string | number): Promise<AiProposalConfirmResponse> {
  return httpClient.post<AiProposalConfirmResponse>(`/api/ai/proposals/${proposalId}/confirm`)
}

export function rejectAiProposal(proposalId: string | number): Promise<AiProposalConfirmResponse> {
  return httpClient.post<AiProposalConfirmResponse>(`/api/ai/proposals/${proposalId}/reject`)
}

export function createAiWorkflow(data: CreateAiWorkflowRequest): Promise<AiWorkflow> {
  return httpClient.post<AiWorkflow>('/api/ai/workflows', normalizeContextPayload(data))
}

export function listAiWorkflows(runNo: string): Promise<AiWorkflow[]> {
  return httpClient.get<AiWorkflow[]>('/api/ai/workflows', { params: { runNo } })
}

export function fetchAiWorkflow(workflowNo: string): Promise<AiWorkflow> {
  return httpClient.get<AiWorkflow>(`/api/ai/workflows/${workflowNo}`)
}

export function pauseAiWorkflow(
  workflowNo: string,
  data: AiWorkflowActionRequest = {},
): Promise<AiWorkflow> {
  return httpClient.post<AiWorkflow>(`/api/ai/workflows/${workflowNo}/pause`, data)
}

export function resumeAiWorkflow(workflowNo: string): Promise<AiWorkflow> {
  return httpClient.post<AiWorkflow>(`/api/ai/workflows/${workflowNo}/resume`)
}

export function cancelAiWorkflow(
  workflowNo: string,
  data: AiWorkflowActionRequest = {},
): Promise<AiWorkflow> {
  return httpClient.post<AiWorkflow>(`/api/ai/workflows/${workflowNo}/cancel`, data)
}

export function failAiWorkflow(
  workflowNo: string,
  data: AiWorkflowActionRequest = {},
): Promise<AiWorkflow> {
  return httpClient.post<AiWorkflow>(`/api/ai/workflows/${workflowNo}/fail`, data)
}

export function createAiProactiveSubscription(
  data: CreateAiProactiveSubscriptionRequest,
): Promise<AiProactiveSubscription> {
  return httpClient.post<AiProactiveSubscription>('/api/ai/proactive/subscriptions', data)
}

export function listAiProactiveSubscriptions(): Promise<AiProactiveSubscription[]> {
  return httpClient.get<AiProactiveSubscription[]>('/api/ai/proactive/subscriptions')
}

export function pauseAiProactiveSubscription(
  subscriptionNo: string,
): Promise<AiProactiveSubscription> {
  return httpClient.post<AiProactiveSubscription>(
    `/api/ai/proactive/subscriptions/${subscriptionNo}/pause`,
  )
}

export function resumeAiProactiveSubscription(
  subscriptionNo: string,
): Promise<AiProactiveSubscription> {
  return httpClient.post<AiProactiveSubscription>(
    `/api/ai/proactive/subscriptions/${subscriptionNo}/resume`,
  )
}

export function cancelAiProactiveSubscription(
  subscriptionNo: string,
): Promise<AiProactiveSubscription> {
  return httpClient.post<AiProactiveSubscription>(
    `/api/ai/proactive/subscriptions/${subscriptionNo}/cancel`,
  )
}

export function listAiProactiveEvents(page = 1, size = 20): Promise<AiProactiveEvent[]> {
  return httpClient.get<AiProactiveEvent[]>('/api/ai/proactive/events', { params: { page, size } })
}

export function fetchAiProactiveEvent(eventNo: string): Promise<AiProactiveEvent> {
  return httpClient.get<AiProactiveEvent>(`/api/ai/proactive/events/${eventNo}`)
}

export function generateAiProactiveEvents(): Promise<AiProactiveEvent[]> {
  return httpClient.post<AiProactiveEvent[]>('/api/ai/proactive/events/generate')
}

export function listAiProviderConfigs(): Promise<AiProviderConfig[]> {
  return httpClient.get<AiProviderConfig[]>('/api/ai/provider-configs')
}

export function createAiProviderConfig(
  data: CreateAiProviderConfigRequest,
): Promise<AiProviderConfig> {
  return httpClient.post<AiProviderConfig>('/api/ai/provider-configs', data)
}

export function updateAiProviderConfig(
  configNo: string,
  data: UpdateAiProviderConfigRequest,
): Promise<AiProviderConfig> {
  return httpClient.put<AiProviderConfig>(`/api/ai/provider-configs/${configNo}`, data)
}

export function rotateAiProviderKey(
  configNo: string,
  data: RotateAiProviderKeyRequest,
): Promise<AiProviderConfig> {
  return httpClient.post<AiProviderConfig>(`/api/ai/provider-configs/${configNo}/rotate-key`, data)
}

export function testAiProviderConfig(configNo: string): Promise<AiProviderConfigTestResponse> {
  return httpClient.post<AiProviderConfigTestResponse>(`/api/ai/provider-configs/${configNo}/test`)
}

export function activateAiProviderConfig(configNo: string): Promise<AiProviderConfig> {
  return httpClient.post<AiProviderConfig>(`/api/ai/provider-configs/${configNo}/activate`)
}

export function disableAiProviderConfig(configNo: string): Promise<AiProviderConfig> {
  return httpClient.post<AiProviderConfig>(`/api/ai/provider-configs/${configNo}/disable`)
}

export function fetchAiPolicy(): Promise<AiPolicy> {
  return httpClient.get<AiPolicy>('/api/ai/policy')
}

export function updateAiPolicy(data: UpdateAiPolicyRequest): Promise<AiPolicy> {
  return httpClient.put<AiPolicy>('/api/ai/policy', data)
}

export interface StreamAiRunOptions {
  afterSequence?: number
  maxReconnects?: number
}

export async function streamAiRunEvents(
  runNo: string,
  onEvent: (event: AiSseEvent) => void,
  signal?: AbortSignal,
  options: StreamAiRunOptions = {},
): Promise<void> {
  const maxReconnects = options.maxReconnects ?? MAX_SSE_RECONNECTS
  let reconnectCount = 0
  let lastSequence = options.afterSequence ?? 0

  while (true) {
    if (signal?.aborted) throw createAbortError()
    const token = readStoredToken()
    const query = lastSequence > 0 ? `?afterSequence=${lastSequence}` : ''
    try {
      const response = await fetch(
        `${env.apiBaseUrl}/api/ai/runs/${encodeURIComponent(runNo)}/events${query}`,
        {
          method: 'GET',
          headers: token ? { Authorization: `Bearer ${token.token}` } : {},
          signal,
        },
      )

      if (!response.ok || !response.body) {
        throw new Error(`AI SSE failed: ${response.status}`)
      }

      const result = await decodeAiSseStream(response.body, onEvent, lastSequence)
      lastSequence = result.lastSequence
      if (result.terminal) return
    } catch (error) {
      if (signal?.aborted || isAbortError(error)) throw error
      if (reconnectCount >= maxReconnects) throw error
    }

    if (reconnectCount >= maxReconnects) {
      throw new Error('AI SSE ended before a terminal event')
    }
    reconnectCount += 1
  }
}

export async function decodeAiSseStream(
  stream: ReadableStream<Uint8Array>,
  onEvent: (event: AiSseEvent) => void,
  afterSequence = 0,
): Promise<{ lastSequence: number; terminal: boolean }> {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let lastSequence = afterSequence
  let terminal = false

  const consumeFrame = (frame: string): void => {
    if (terminal) return
    const event = parseSseFrame(frame)
    if (!event) return
    if (event.sequence > 0 && event.sequence <= lastSequence) return
    if (event.sequence > 0) lastSequence = event.sequence
    onEvent(event)
    terminal ||= TERMINAL_SSE_EVENTS.has(event.type)
  }

  while (!terminal) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split(/\r?\n\r?\n/)
    buffer = frames.pop() ?? ''
    frames.forEach(consumeFrame)
  }

  buffer += decoder.decode()
  if (!terminal && buffer.trim()) consumeFrame(buffer)
  await reader.cancel().catch(() => undefined)
  return { lastSequence, terminal }
}

export function parseSseFrame(frame: string): AiSseEvent | null {
  const data = frame
    .split(/\r?\n/)
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).replace(/^ /, ''))
    .join('\n')
  if (!data.trim()) return null
  try {
    const parsed: unknown = JSON.parse(data)
    if (!parsed || typeof parsed !== 'object') return null
    const event = parsed as Partial<AiSseEvent>
    if (typeof event.type !== 'string' || typeof event.sequence !== 'number') return null
    return parsed as AiSseEvent
  } catch {
    // 单个损坏事件不应中断后续已经到达的有效增量。
    return null
  }
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function createAbortError(): DOMException {
  return new DOMException('The operation was aborted', 'AbortError')
}

function normalizeContextPayload<
  T extends CreateAiRunRequest | CreateAiWorkflowRequest | CreateAiConversationRequest,
>(data: T): T {
  const contextObjectType = data.contextObjectType?.trim()
  const contextObjectId = data.contextObjectId?.trim()
  const payload = { ...data }
  if (contextObjectType && contextObjectId) {
    payload.contextObjectType = contextObjectType
    payload.contextObjectId = contextObjectId
    return payload
  }
  delete payload.contextObjectType
  delete payload.contextObjectId
  return payload
}
