import type { EntityId } from '@/shared/types/id'

export type AiEntryPoint = 'PAGE' | 'SIDE_PANEL'
export type AiConversationStatus = 'ACTIVE' | 'ARCHIVED'
export type AiRunStatus =
  | 'CREATED'
  | 'RUNNING'
  | 'WAITING_FOR_APPROVAL'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED'

export type AiSseEventType =
  | 'run_started'
  | 'message_delta'
  | 'message_completed'
  | 'tool_call_started'
  | 'tool_call_completed'
  | 'proposal_created'
  | 'workflow_started'
  | 'workflow_step_started'
  | 'workflow_step_completed'
  | 'workflow_waiting_user_confirmation'
  | 'workflow_paused'
  | 'workflow_resumed'
  | 'workflow_cancelled'
  | 'workflow_expired'
  | 'workflow_failed'
  | 'workflow_completed'
  | 'error'
  | 'run_completed'
  | 'run_cancelled'

export type AiRiskLevel = 'READONLY' | 'LOW' | 'MEDIUM' | 'HIGH'
export type AiProposalStatus =
  | 'PENDING_CONFIRMATION'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'EXECUTED'
  | 'FAILED'

export interface CreateAiRunRequest {
  prompt: string
  entryPoint: AiEntryPoint
  conversationNo?: string
  contextObjectType?: string
  contextObjectId?: string
}

export interface AiRun {
  runNo: string
  conversationNo?: string
  turnNo?: number
  status: AiRunStatus
  entryPoint: AiEntryPoint
  contextObjectType?: string
  contextObjectId?: string
  promptSummary?: string
  errorCode?: string
  errorMessage?: string
  createTime?: string
  expiresTime?: string
}

export interface AiTraceMessage {
  id: EntityId
  messageNo?: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  sequenceNo: number
  visibleToUser?: boolean
  contentSummary: string | null
  status?: 'ACTIVE' | 'SUPERSEDED' | 'WITHDRAWN'
  revisionNo?: number
  includedInContext?: boolean
  version?: number
  canEdit?: boolean
  canWithdraw?: boolean
  editTime?: string
  withdrawnTime?: string
  createTime?: string
}

export interface AiConversation {
  conversationNo: string
  title: string
  status: AiConversationStatus
  entryPoint: AiEntryPoint
  contextObjectType?: string
  contextObjectId?: string
  summaryText?: string
  lastRunNo?: string
  lastMessageTime?: string
  createTime?: string
  editTime?: string
}

export interface CreateAiConversationRequest {
  title?: string
  entryPoint: AiEntryPoint
  contextObjectType?: string
  contextObjectId?: string
}

export interface RenameAiConversationRequest {
  title: string
}

export interface AiConversationDetail {
  conversation: AiConversation
  messages: AiTraceMessage[]
  turns?: AiConversationTurn[]
  latestRun?: AiRun
  latestRunTrace?: AiRunTrace
}

export interface AiConversationTurn {
  run: AiRun
  turnNo?: number
  status?: AiRunStatus
  userMessage?: AiTraceMessage
  assistantMessage?: AiTraceMessage
  messages: AiTraceMessage[]
  toolResults: AiTraceToolCall[]
  proposals: AiTraceProposal[]
  approvals: AiTraceApproval[]
  executionEvents: AiTraceExecutionEvent[]
  workflows?: AiWorkflow[]
}

export interface AiTraceToolCall {
  id: EntityId
  toolName: string
  permissionCode?: string
  riskLevel?: AiRiskLevel
  inputSummary?: string
  outputSummary?: string
  objectRefs?: string
  displayPayload?: unknown
  resultStatus?: string
  errorCode?: string
  durationMs?: number
  startedTime?: string
  completedTime?: string
}

export interface AiTraceProposal {
  id: EntityId
  proposalType: string
  status: AiProposalStatus
  riskLevel: AiRiskLevel
  permissionCode: string
  relatedObjectType: string
  relatedObjectId: string
  paramsSummary: string
  impactSummary: string
  expiresTime: string
  confirmedTime?: string
  executedTime?: string
  resultSummary?: string
  errorCode?: string
  createTime?: string
}

export interface AiTraceApproval {
  id: EntityId
  proposalId: EntityId
  decision: 'CONFIRMED' | 'REJECTED' | 'EXPIRED'
  permissionSummary?: string
  reason?: string
  resultStatus?: string
  approvedTime?: string
}

export interface AiTraceExecutionEvent {
  id: EntityId
  proposalId?: EntityId
  eventType: string
  resultStatus: string
  objectType?: string
  objectId?: string
  summary: string
  errorCode?: string
  occurredTime?: string
}

export interface AiRunTrace {
  run: AiRun
  messages: AiTraceMessage[]
  toolCalls: AiTraceToolCall[]
  proposals: AiTraceProposal[]
  approvals: AiTraceApproval[]
  executionEvents: AiTraceExecutionEvent[]
  workflows?: AiWorkflow[]
}

export type AiWorkflowStatus =
  | 'CREATED'
  | 'RUNNING'
  | 'PAUSED'
  | 'WAITING_USER_CONFIRMATION'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED'

export interface AiWorkflowStep {
  id?: EntityId
  stepNo: number
  stepType: string
  title: string
  status: AiWorkflowStatus | 'PENDING'
  toolName?: string
  proposalId?: EntityId
  inputSummary?: string
  outputSummary?: string
  errorCode?: string
  startedTime?: string
  completedTime?: string
}

export interface AiWorkflow {
  workflowNo: string
  runNo?: string
  workflowType: string
  title: string
  status: AiWorkflowStatus
  currentStepNo?: number
  contextObjectType?: string
  contextObjectId?: string
  pauseReason?: string
  errorCode?: string
  errorMessage?: string
  startedTime?: string
  pausedTime?: string
  resumedTime?: string
  completedTime?: string
  expiresTime?: string
  steps: AiWorkflowStep[]
}

export interface CreateAiWorkflowRequest {
  runNo: string
  workflowType: 'CUSTOMER_FOLLOW_UP' | 'TRANSACTION_GAP_REVIEW' | 'INVENTORY_RISK_REVIEW'
  contextObjectType?: string
  contextObjectId?: string
}

export interface AiWorkflowActionRequest {
  reason?: string
}

export type AiProactiveSubscriptionType =
  | 'FOLLOW_UP_REMINDER'
  | 'TRANSACTION_EXCEPTION'
  | 'INVENTORY_ALERT'
  | 'DAILY_SUMMARY'
  | 'PERIODIC_SALES_ANALYSIS'

export type AiProactiveFrequency = 'REALTIME_LIMITED' | 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type AiProactiveSubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED'
export type AiProactiveEventStatus =
  | 'CREATED'
  | 'GENERATING'
  | 'READY'
  | 'NO_DATA'
  | 'FAILED'
  | 'SKIPPED'

export interface CreateAiProactiveSubscriptionRequest {
  subscriptionType: AiProactiveSubscriptionType
  frequency: AiProactiveFrequency
  quietStartTime?: string
  quietEndTime?: string
  dailyLimit?: number
  maxResults?: number
  duplicateWindowMinutes?: number
  configSummary?: string
}

export interface AiProactiveSubscription {
  subscriptionNo: string
  subscriptionType: AiProactiveSubscriptionType
  status: AiProactiveSubscriptionStatus
  frequency: AiProactiveFrequency
  quietStartTime?: string
  quietEndTime?: string
  dailyLimit: number
  maxResults: number
  duplicateWindowMinutes: number
  configSummary?: string
  lastTriggeredTime?: string
  nextTriggerTime?: string
}

export interface AiProactiveEvent {
  eventNo: string
  subscriptionNo?: string
  eventType: AiProactiveSubscriptionType | string
  status: AiProactiveEventStatus
  title: string
  summary: string
  detailSummary?: string
  objectType?: string
  objectId?: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | string
  generatedTime?: string
  deliveredTime?: string
  errorCode?: string
}

export interface AiSseEvent {
  eventId: string
  runNo: string
  sequence: number
  type: AiSseEventType | string
  occurredAt: string
  payload: Record<string, unknown>
}

export interface AiChatMessage {
  id: string
  messageNo?: string
  role: 'user' | 'assistant' | 'system'
  content: string
  status?: 'ACTIVE' | 'SUPERSEDED' | 'WITHDRAWN'
  revisionNo?: number
  version?: number
  canEdit?: boolean
  canWithdraw?: boolean
  pending?: boolean
  cancelled?: boolean
  error?: boolean
}

export interface EditAiMessageRequest {
  content: string
  expectedVersion: number
}

export interface WithdrawAiMessageRequest {
  expectedVersion: number
}

export type AiSafetyMode = 'STRICT' | 'STANDARD'
export type AiNetworkMode = 'DISABLED' | 'PROVIDER_ONLY'

export interface AiPolicy {
  enabledTools: boolean
  allowedToolNames: string[]
  proposalsEnabled: boolean
  maxToolCallsPerRun: number
  safetyMode: AiSafetyMode
  networkMode: AiNetworkMode
  contextMessageLimit: number
  summaryMaxChars: number
  maxRunSeconds: number
  version: number
}

export type UpdateAiPolicyRequest = AiPolicy

export type AiProviderFormat = 'OPENAI_COMPATIBLE' | 'ANTHROPIC'
export type AiProviderTestStatus = 'UNTESTED' | 'SUCCESS' | 'FAILED'

export interface AiProviderConfig {
  configNo: string
  providerName: string
  providerFormat: AiProviderFormat
  baseUrl: string
  modelName: string
  modelDisplayName: string
  hasApiKey: boolean
  maskedApiKey?: string
  enabled: boolean
  testStatus: AiProviderTestStatus
  lastTestTime?: string
  lastTestErrorCode?: string
  lastTestMessage?: string
  timeoutSeconds: number
  maxOutputTokens: number
  temperature: number
  createTime?: string
  editTime?: string
}

export interface CreateAiProviderConfigRequest {
  providerName: string
  providerFormat: AiProviderFormat
  baseUrl: string
  modelName: string
  modelDisplayName: string
  apiKey: string
  timeoutSeconds: number
  maxOutputTokens: number
  temperature: number
}

export type UpdateAiProviderConfigRequest = Omit<CreateAiProviderConfigRequest, 'apiKey'>

export interface RotateAiProviderKeyRequest {
  apiKey: string
}

export interface AiProviderConfigTestResponse {
  configNo: string
  testStatus: AiProviderTestStatus
  message?: string
  errorCode?: string
}

export interface AiToolResult {
  toolName?: string
  summary: string
  data?: unknown
  objectRefs?: string
}

export interface AiProposal {
  proposalId: EntityId
  proposalType: string
  riskLevel: AiRiskLevel
  permissionCode: string
  relatedObjectType: string
  relatedObjectId: string
  paramsSummary: string
  impactSummary: string
  expiresTime: string
  status?: AiProposalStatus
}

export interface AiProposalConfirmResponse {
  proposalId: EntityId
  status: AiProposalStatus
  resultSummary?: string
  objectType?: string
  objectId?: string
}

export interface AiPageContext {
  objectType?: string
  objectId?: string
}
