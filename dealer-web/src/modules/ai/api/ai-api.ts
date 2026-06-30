import { httpClient } from '@/shared/api/http-client'
import { env } from '@/shared/config/env'
import { readStoredToken } from '@/shared/storage/token-storage'
import type {
  AiConversation,
  AiConversationDetail,
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
  RotateAiProviderKeyRequest,
  RenameAiConversationRequest,
  UpdateAiProviderConfigRequest,
} from '@/modules/ai/model/ai.types'

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

export async function streamAiRunEvents(
  runNo: string,
  onEvent: (event: AiSseEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = readStoredToken()
  const response = await fetch(`${env.apiBaseUrl}/api/ai/runs/${encodeURIComponent(runNo)}/events`, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token.token}` } : {},
    signal,
  })

  if (!response.ok || !response.body) {
    throw new Error(`AI SSE failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() ?? ''
    chunks.forEach((chunk) => {
      const event = parseSseChunk(chunk)
      if (event) onEvent(event)
    })
  }

  const tail = parseSseChunk(buffer)
  if (tail) onEvent(tail)
}

function parseSseChunk(chunk: string): AiSseEvent | null {
  const dataLine = chunk
    .split(/\r?\n/)
    .find((line) => line.startsWith('data:'))
  if (!dataLine) return null
  const raw = dataLine.slice(5).trim()
  if (!raw) return null
  return JSON.parse(raw) as AiSseEvent
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
