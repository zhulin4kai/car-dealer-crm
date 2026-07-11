import fs from 'node:fs'
import path from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AiSidePanel from '@/modules/ai/components/AiSidePanel.vue'
import { useAiAssistantStore } from '@/stores/ai-assistant.store'

const apiMocks = vi.hoisted(() => ({
  listAiConversations: vi.fn(),
}))
const feedbackMocks = vi.hoisted(() => ({
  messageTip: vi.fn(),
}))

vi.mock('@/modules/ai/api/ai-api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/modules/ai/api/ai-api')>()
  return {
    ...actual,
    listAiConversations: apiMocks.listAiConversations,
  }
})

vi.mock('@/shared/utils/feedback', () => ({
  messageTip: feedbackMocks.messageTip,
}))

const pages = [
  ['opportunity.vue', 'OPPORTUNITY'],
  ['quote.vue', 'QUOTE'],
  ['test-drive.vue', 'TEST_DRIVE'],
  ['delivery.vue', 'DELIVERY'],
  ['product/index.vue', 'PRODUCT'],
] as const
const dashboardPages = path.resolve(__dirname, '../../../src/pages/dashboard')

describe('ai contextual list entries', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    apiMocks.listAiConversations.mockReset()
    feedbackMocks.messageTip.mockReset()
    apiMocks.listAiConversations.mockResolvedValue([
      {
        conversationNo: 'AIC-OPP',
        title: '商机分析',
        status: 'ACTIVE',
        entryPoint: 'SIDE_PANEL',
        contextObjectType: 'OPPORTUNITY',
        contextObjectId: '11',
      },
      {
        conversationNo: 'AIC-QUOTE',
        title: '报价分析',
        status: 'ACTIVE',
        entryPoint: 'SIDE_PANEL',
        contextObjectType: 'QUOTE',
        contextObjectId: '22',
      },
    ])
  })

  it('keeps panel state and context in the shared store', () => {
    const store = useAiAssistantStore()

    store.openPanel({ objectType: 'OPPORTUNITY', objectId: '11' })

    expect(store.isPanelOpen).toBe(true)
    expect(store.context).toEqual({ objectType: 'OPPORTUNITY', objectId: '11' })
    store.setRunActive(true)
    expect(store.openPanel({ objectType: 'QUOTE', objectId: '22' })).toBe(false)
    expect(store.context).toEqual({ objectType: 'OPPORTUNITY', objectId: '11' })
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      'AI 正在生成，请先停止当前生成或等待完成后再切换业务对象',
      'warning',
    )
    store.closePanel()
    expect(store.isPanelOpen).toBe(false)
  })

  it.each(pages)('adds a permission-controlled AI action to %s', (file, objectType) => {
    const source = fs.readFileSync(path.join(dashboardPages, file), 'utf8')

    expect(source).toContain('PERMISSIONS.ai.assistantUse')
    expect(source).toContain('useAiAssistantStore')
    expect(source).toContain(`objectType: '${objectType}'`)
    expect(source).toContain('label="询问 AI"')
  })

  it('clears the selected conversation and filters history when object context changes', async () => {
    const view = render(AiSidePanel, {
      props: {
        open: true,
        context: { objectType: 'OPPORTUNITY', objectId: '11' },
      },
      global: {
        plugins: [createPinia()],
        stubs: { AiAssistantPanel: true },
      },
    })
    const select = screen.getByLabelText('切换 AI 会话') as HTMLSelectElement
    await waitFor(() => expect(screen.getByText('商机分析')).toBeTruthy())
    expect(screen.queryByText('报价分析')).toBeNull()
    await fireEvent.update(select, 'AIC-OPP')
    expect(select.value).toBe('AIC-OPP')

    await view.rerender({
      open: true,
      context: { objectType: 'QUOTE', objectId: '22' },
    })

    await waitFor(() => {
      expect(select.value).toBe('')
      expect(screen.getByText('报价分析')).toBeTruthy()
    })
    expect(screen.queryByText('商机分析')).toBeNull()
    expect(document.body.textContent).not.toContain('AIC-QUOTE')
    expect(document.body.textContent).not.toContain('22')
  })
})
