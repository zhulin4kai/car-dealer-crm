import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AiAssistantPanel from '@/modules/ai/components/AiAssistantPanel.vue'
import type { AiConversationDetail, AiSseEvent } from '@/modules/ai/model/ai.types'

const apiMocks = vi.hoisted(() => ({
  createAiRun: vi.fn(),
  editAiMessage: vi.fn(),
  fetchAiConversation: vi.fn(),
  streamAiRunEvents: vi.fn(),
  withdrawAiMessage: vi.fn(),
}))

const feedbackMocks = vi.hoisted(() => ({
  messageConfirm: vi.fn(),
  messageTip: vi.fn(),
}))

vi.mock('@/modules/ai/api/ai-api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/modules/ai/api/ai-api')>()
  return {
    ...actual,
    createAiRun: apiMocks.createAiRun,
    editAiMessage: apiMocks.editAiMessage,
    fetchAiConversation: apiMocks.fetchAiConversation,
    streamAiRunEvents: apiMocks.streamAiRunEvents,
    withdrawAiMessage: apiMocks.withdrawAiMessage,
  }
})

vi.mock('@/shared/utils/feedback', () => feedbackMocks)

function sse(sequence: number, type: string, payload: Record<string, unknown>): AiSseEvent {
  return {
    eventId: `event-${sequence}`,
    runNo: 'AIR1',
    sequence,
    type,
    occurredAt: '2026-07-11T10:00:00+08:00',
    payload,
  }
}

function conversationDetail(): AiConversationDetail {
  return {
    conversation: {
      conversationNo: 'AIC1',
      title: '客户跟进',
      status: 'ACTIVE',
      entryPoint: 'PAGE',
    },
    messages: [],
    turns: [
      {
        run: {
          runNo: 'AIR0',
          conversationNo: 'AIC1',
          turnNo: 1,
          status: 'COMPLETED',
          entryPoint: 'PAGE',
        },
        messages: [],
        userMessage: {
          id: 1,
          messageNo: 'AIM1',
          role: 'USER',
          sequenceNo: 1,
          contentSummary: '原问题',
          status: 'ACTIVE',
          version: 2,
          canEdit: true,
          canWithdraw: true,
        },
        assistantMessage: {
          id: 2,
          messageNo: 'AIM2',
          role: 'ASSISTANT',
          sequenceNo: 2,
          contentSummary: '原回答',
        },
        toolResults: [],
        proposals: [],
        approvals: [],
        executionEvents: [],
        workflows: [],
      },
    ],
  }
}

describe('ai assistant behavior', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    apiMocks.createAiRun.mockReset()
    apiMocks.editAiMessage.mockReset()
    apiMocks.fetchAiConversation.mockReset()
    apiMocks.streamAiRunEvents.mockReset()
    apiMocks.withdrawAiMessage.mockReset()
    feedbackMocks.messageConfirm.mockReset()
    feedbackMocks.messageConfirm.mockResolvedValue('confirm')
    feedbackMocks.messageTip.mockReset()
    apiMocks.streamAiRunEvents.mockImplementation(
      async (_runNo: string, onEvent: (event: AiSseEvent) => void) => {
        onEvent(sse(1, 'message_delta', { content_delta: '你' }))
        onEvent(sse(2, 'message_delta', { content_delta: '好' }))
        onEvent(sse(3, 'message_completed', { content: '你好' }))
        onEvent(sse(4, 'run_completed', { status: 'RUNNING' }))
      },
    )
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders streamed characters through the smooth display queue', async () => {
    apiMocks.createAiRun.mockResolvedValue({
      runNo: 'AIR1',
      conversationNo: 'AIC1',
      turnNo: 1,
      status: 'RUNNING',
      entryPoint: 'PAGE',
    })
    render(AiAssistantPanel, { props: { entryPoint: 'PAGE' }, global: { plugins: [createPinia()] } })

    await fireEvent.update(screen.getByPlaceholderText('输入问题'), '你好')
    void fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await flushPromises()
    await vi.advanceTimersByTimeAsync(200)
    await flushPromises()

    expect(screen.getAllByText('你好')).toHaveLength(2)
    expect(apiMocks.streamAiRunEvents).toHaveBeenCalledTimes(1)
  })

  it('shows proposal confirmation without duplicating it as an empty business result', async () => {
    apiMocks.createAiRun.mockResolvedValue({
      runNo: 'AIR1',
      conversationNo: 'AIC1',
      turnNo: 1,
      status: 'RUNNING',
      entryPoint: 'PAGE',
    })
    apiMocks.streamAiRunEvents.mockImplementation(
      async (_runNo: string, onEvent: (event: AiSseEvent) => void) => {
        onEvent(
          sse(1, 'tool_call_completed', {
            toolName: 'create_follow_task_proposal',
            outputSummary: '已生成跟进任务提议',
            data: { proposalId: 12 },
          }),
        )
        onEvent(
          sse(2, 'proposal_created', {
            proposalId: 12,
            proposalType: 'create_follow_task_proposal',
            riskLevel: 'LOW',
            relatedObjectType: 'CUSTOMER',
            paramsSummary: '创建跟进任务：浏览器提议测试',
            impactSummary: '确认后才会创建跟进任务',
            expiresTime: '2026-07-11T18:00:00+08:00',
          }),
        )
        onEvent(sse(3, 'message_completed', { content: '当前尚未写入任何业务数据。' }))
        onEvent(sse(4, 'run_completed', { status: 'WAITING_FOR_APPROVAL' }))
      },
    )
    render(AiAssistantPanel, { props: { entryPoint: 'PAGE' }, global: { plugins: [createPinia()] } })

    await fireEvent.update(screen.getByPlaceholderText('输入问题'), '创建跟进任务')
    void fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await flushPromises()
    await vi.advanceTimersByTimeAsync(200)
    await flushPromises()

    expect(screen.getByText('创建跟进任务：浏览器提议测试')).toBeTruthy()
    expect(screen.queryByText('跟进任务 1')).toBeNull()
  })

  it('edits an owned message with optimistic version and starts a new streamed run', async () => {
    apiMocks.fetchAiConversation.mockResolvedValue(conversationDetail())
    apiMocks.editAiMessage.mockResolvedValue({
      runNo: 'AIR1',
      conversationNo: 'AIC1',
      turnNo: 2,
      status: 'RUNNING',
      entryPoint: 'PAGE',
    })
    render(AiAssistantPanel, {
      props: { entryPoint: 'PAGE', initialConversationNo: 'AIC1' },
      global: { plugins: [createPinia()] },
    })
    await flushPromises()

    await fireEvent.click(screen.getByRole('button', { name: '编辑这条消息' }))
    await fireEvent.update(screen.getByLabelText('编辑消息内容'), '修改后的问题')
    void fireEvent.click(screen.getByRole('button', { name: '保存并重新生成' }))
    await flushPromises()
    await vi.advanceTimersByTimeAsync(200)
    await flushPromises()

    await waitFor(() => {
      expect(apiMocks.editAiMessage).toHaveBeenCalledWith('AIC1', 'AIM1', {
        content: '修改后的问题',
        expectedVersion: 2,
      })
    })
    expect(screen.getByText('修改后的问题')).toBeTruthy()
  })

  it('keeps restored conversation context after expanding or refreshing the AI page', async () => {
    const detail = conversationDetail()
    detail.conversation.contextObjectType = 'OPPORTUNITY'
    detail.conversation.contextObjectId = '11'
    detail.turns = []
    apiMocks.fetchAiConversation.mockResolvedValue(detail)
    apiMocks.createAiRun.mockResolvedValue({
      runNo: 'AIR2',
      conversationNo: 'AIC1',
      turnNo: 2,
      status: 'RUNNING',
      entryPoint: 'PAGE',
      contextObjectType: 'OPPORTUNITY',
      contextObjectId: '11',
    })
    render(AiAssistantPanel, {
      props: { entryPoint: 'PAGE', initialConversationNo: 'AIC1' },
      global: { plugins: [createPinia()] },
    })
    await flushPromises()

    expect(screen.getByText('总结这个商机的关键风险')).toBeTruthy()
    await fireEvent.update(screen.getByPlaceholderText('输入问题'), '继续分析这个商机')
    void fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await flushPromises()
    await vi.advanceTimersByTimeAsync(200)
    await flushPromises()

    expect(apiMocks.createAiRun).toHaveBeenCalledWith({
      prompt: '继续分析这个商机',
      entryPoint: 'PAGE',
      conversationNo: 'AIC1',
      contextObjectType: 'OPPORTUNITY',
      contextObjectId: '11',
    })
  })

  it('does not resurrect audit messages when the active turn list is empty', async () => {
    const detail = conversationDetail()
    detail.turns = []
    detail.messages = [
      {
        id: 11,
        messageNo: 'AIM-OLD',
        role: 'USER',
        sequenceNo: 1,
        contentSummary: '已经被替代的问题',
        status: 'SUPERSEDED',
        includedInContext: false,
      },
      {
        id: 12,
        messageNo: 'AIM-WITHDRAWN',
        role: 'USER',
        sequenceNo: 2,
        contentSummary: null,
        status: 'WITHDRAWN',
        includedInContext: false,
      },
      {
        id: 13,
        messageNo: 'AIM-AUDIT',
        role: 'ASSISTANT',
        sequenceNo: 3,
        contentSummary: '仅用于审计的旧回答',
        includedInContext: false,
      },
    ]
    apiMocks.fetchAiConversation.mockResolvedValue(detail)

    render(AiAssistantPanel, {
      props: { entryPoint: 'PAGE', initialConversationNo: 'AIC1' },
      global: { plugins: [createPinia()] },
    })
    await flushPromises()

    expect(screen.queryByText('已经被替代的问题')).toBeNull()
    expect(screen.queryByText('消息已撤回')).toBeNull()
    expect(screen.queryByText('仅用于审计的旧回答')).toBeNull()
    expect(screen.getByText('你好，我是 AI 助手')).toBeTruthy()
  })

  it('cancels message withdrawal silently when the user closes the confirmation', async () => {
    apiMocks.fetchAiConversation.mockResolvedValue(conversationDetail())
    feedbackMocks.messageConfirm.mockRejectedValueOnce(new Error('cancel'))
    render(AiAssistantPanel, {
      props: { entryPoint: 'PAGE', initialConversationNo: 'AIC1' },
      global: { plugins: [createPinia()] },
    })
    await flushPromises()

    await fireEvent.click(screen.getByRole('button', { name: '撤回这条消息' }))
    await flushPromises()

    expect(feedbackMocks.messageConfirm).toHaveBeenCalledWith(
      '撤回只会移出后续 AI 上下文，不会撤销已经执行的业务操作。是否继续？',
    )
    expect(apiMocks.withdrawAiMessage).not.toHaveBeenCalled()
    expect(feedbackMocks.messageTip).not.toHaveBeenCalled()
  })
})
