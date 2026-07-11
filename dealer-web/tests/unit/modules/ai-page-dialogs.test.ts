import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AiAssistantPage from '@/pages/dashboard/ai.vue'

const apiMocks = vi.hoisted(() => ({
  archiveAiConversation: vi.fn(),
  createAiConversation: vi.fn(),
  listAiConversations: vi.fn(),
  renameAiConversation: vi.fn(),
}))

const feedbackMocks = vi.hoisted(() => ({
  messageConfirm: vi.fn(),
  messageTip: vi.fn(),
}))

vi.mock('@/modules/ai/api/ai-api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/modules/ai/api/ai-api')>()
  return {
    ...actual,
    archiveAiConversation: apiMocks.archiveAiConversation,
    createAiConversation: apiMocks.createAiConversation,
    listAiConversations: apiMocks.listAiConversations,
    renameAiConversation: apiMocks.renameAiConversation,
  }
})

vi.mock('@/shared/utils/feedback', () => feedbackMocks)

const conversation = {
  conversationNo: 'AIC1',
  title: '客户分析',
  status: 'ACTIVE' as const,
  entryPoint: 'PAGE' as const,
}

async function renderPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/dashboard/ai', name: 'ai-assistant', component: AiAssistantPage }],
  })
  await router.push('/dashboard/ai')
  await router.isReady()

  render(AiAssistantPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        AiAssistantPanel: true,
      },
    },
  })
  await flushPromises()
}

describe('ai page conversation dialogs', () => {
  beforeEach(() => {
    apiMocks.archiveAiConversation.mockReset()
    apiMocks.createAiConversation.mockReset()
    apiMocks.listAiConversations.mockReset()
    apiMocks.listAiConversations.mockResolvedValue([conversation])
    apiMocks.renameAiConversation.mockReset()
    apiMocks.renameAiConversation.mockResolvedValue(undefined)
    feedbackMocks.messageConfirm.mockReset()
    feedbackMocks.messageConfirm.mockResolvedValue('confirm')
    feedbackMocks.messageTip.mockReset()
  })

  it('renames a conversation through the project dialog form', async () => {
    await renderPage()
    await fireEvent.click(screen.getByRole('button', { name: /客户分析/ }))
    await fireEvent.click(screen.getByRole('button', { name: '重命名当前会话' }))

    const input = screen.getByLabelText('会话名称')
    expect(screen.getByText('重命名 AI 会话')).toBeTruthy()
    expect((input as HTMLInputElement).value).toBe('客户分析')
    await fireEvent.update(input, '客户年度分析')
    await fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(apiMocks.renameAiConversation).toHaveBeenCalledWith('AIC1', {
        title: '客户年度分析',
      })
    })
  })

  it('cancels conversation archive silently when confirmation is dismissed', async () => {
    feedbackMocks.messageConfirm.mockRejectedValueOnce(new Error('cancel'))
    await renderPage()
    await fireEvent.click(screen.getByRole('button', { name: /客户分析/ }))
    await fireEvent.click(screen.getByRole('button', { name: '归档当前会话' }))
    await flushPromises()

    expect(feedbackMocks.messageConfirm).toHaveBeenCalledWith(
      '归档“客户分析”后将从默认会话列表隐藏，是否继续？',
    )
    expect(apiMocks.archiveAiConversation).not.toHaveBeenCalled()
    expect(feedbackMocks.messageTip).not.toHaveBeenCalled()
  })
})
