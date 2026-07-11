import axios from 'axios'
import fs from 'node:fs'
import path from 'node:path'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  activateAiProviderConfig,
  archiveAiConversation,
  cancelAiProactiveSubscription,
  cancelAiRun,
  cancelAiWorkflow,
  confirmAiProposal,
  createAiConversation,
  createAiProviderConfig,
  createAiRun,
  createAiProactiveSubscription,
  createAiWorkflow,
  disableAiProviderConfig,
  editAiMessage,
  fetchAiPolicy,
  fetchAiConversation,
  fetchAiRun,
  fetchAiRunTrace,
  generateAiProactiveEvents,
  listAiConversations,
  listAiProviderConfigs,
  listAiProactiveEvents,
  listAiProactiveSubscriptions,
  pauseAiProactiveSubscription,
  pauseAiWorkflow,
  rejectAiProposal,
  renameAiConversation,
  rotateAiProviderKey,
  resumeAiProactiveSubscription,
  resumeAiWorkflow,
  testAiProviderConfig,
  updateAiProviderConfig,
  updateAiPolicy,
  withdrawAiMessage,
} from '@/modules/ai/api/ai-api'

const mockedAxios = vi.mocked(axios)
const srcDir = path.resolve(__dirname, '../../../src')

function collectSourceFiles(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      return collectSourceFiles(fullPath)
    }
    return /\.(ts|vue)$/.test(entry.name) ? [fullPath] : []
  })
}

describe('ai api contract', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
  })

  it('uses Spring Boot ai endpoints', async () => {
    await createAiRun({ prompt: '查客户', entryPoint: 'PAGE' })
    await listAiConversations()
    await createAiConversation({ entryPoint: 'PAGE' })
    await fetchAiConversation('AIC1')
    await renameAiConversation('AIC1', { title: '交易分析' })
    await archiveAiConversation('AIC1')
    await editAiMessage('AIC1', 'AIM1', { content: '修改后的问题', expectedVersion: 2 })
    await withdrawAiMessage('AIC1', 'AIM1', { expectedVersion: 3 })
    await fetchAiRun('AIR1')
    await fetchAiRunTrace('AIR1')
    await cancelAiRun('AIR1', '停止生成')
    await confirmAiProposal(9)
    await rejectAiProposal(9)
    await createAiWorkflow({ runNo: 'AIR1', workflowType: 'CUSTOMER_FOLLOW_UP' })
    await pauseAiWorkflow('AIW1', { reason: '暂停检查' })
    await resumeAiWorkflow('AIW1')
    await cancelAiWorkflow('AIW1', { reason: '取消检查' })
    await createAiProactiveSubscription({
      subscriptionType: 'INVENTORY_ALERT',
      frequency: 'DAILY',
    })
    await listAiProactiveSubscriptions()
    await pauseAiProactiveSubscription('AIS1')
    await resumeAiProactiveSubscription('AIS1')
    await cancelAiProactiveSubscription('AIS1')
    await listAiProactiveEvents()
    await generateAiProactiveEvents()
    await listAiProviderConfigs()
    await createAiProviderConfig({
      providerName: 'DeepSeek V4 Pro',
      providerFormat: 'OPENAI_COMPATIBLE',
      baseUrl: 'https://api.deepseek.com',
      modelName: 'deepseek-v4-pro',
      modelDisplayName: 'DeepSeek V4 Pro',
      apiKey: 'test-key',
      timeoutSeconds: 15,
      maxOutputTokens: 64,
      temperature: 0.7,
    })
    await updateAiProviderConfig('AIPC1', {
      providerName: 'DeepSeek V4 Pro',
      providerFormat: 'OPENAI_COMPATIBLE',
      baseUrl: 'https://api.deepseek.com',
      modelName: 'deepseek-v4-pro',
      modelDisplayName: 'DeepSeek V4 Pro',
      timeoutSeconds: 15,
      maxOutputTokens: 64,
      temperature: 0.7,
    })
    await rotateAiProviderKey('AIPC1', { apiKey: 'new-test-key' })
    await testAiProviderConfig('AIPC1')
    await activateAiProviderConfig('AIPC1')
    await disableAiProviderConfig('AIPC1')
    await fetchAiPolicy()
    await updateAiPolicy({
      enabledTools: true,
      allowedToolNames: ['search_customers'],
      proposalsEnabled: true,
      maxToolCallsPerRun: 4,
      safetyMode: 'STRICT',
      networkMode: 'PROVIDER_ONLY',
      contextMessageLimit: 8,
      summaryMaxChars: 2000,
      maxRunSeconds: 60,
      version: 3,
    })

    expect(mockedAxios.request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
      ['post', '/api/ai/runs'],
      ['get', '/api/ai/conversations'],
      ['post', '/api/ai/conversations'],
      ['get', '/api/ai/conversations/AIC1'],
      ['patch', '/api/ai/conversations/AIC1/title'],
      ['post', '/api/ai/conversations/AIC1/archive'],
      ['patch', '/api/ai/conversations/AIC1/messages/AIM1'],
      ['post', '/api/ai/conversations/AIC1/messages/AIM1/withdraw'],
      ['get', '/api/ai/runs/AIR1'],
      ['get', '/api/ai/runs/AIR1/trace'],
      ['post', '/api/ai/runs/AIR1/cancel'],
      ['post', '/api/ai/proposals/9/confirm'],
      ['post', '/api/ai/proposals/9/reject'],
      ['post', '/api/ai/workflows'],
      ['post', '/api/ai/workflows/AIW1/pause'],
      ['post', '/api/ai/workflows/AIW1/resume'],
      ['post', '/api/ai/workflows/AIW1/cancel'],
      ['post', '/api/ai/proactive/subscriptions'],
      ['get', '/api/ai/proactive/subscriptions'],
      ['post', '/api/ai/proactive/subscriptions/AIS1/pause'],
      ['post', '/api/ai/proactive/subscriptions/AIS1/resume'],
      ['post', '/api/ai/proactive/subscriptions/AIS1/cancel'],
      ['get', '/api/ai/proactive/events'],
      ['post', '/api/ai/proactive/events/generate'],
      ['get', '/api/ai/provider-configs'],
      ['post', '/api/ai/provider-configs'],
      ['put', '/api/ai/provider-configs/AIPC1'],
      ['post', '/api/ai/provider-configs/AIPC1/rotate-key'],
      ['post', '/api/ai/provider-configs/AIPC1/test'],
      ['post', '/api/ai/provider-configs/AIPC1/activate'],
      ['post', '/api/ai/provider-configs/AIPC1/disable'],
      ['get', '/api/ai/policy'],
      ['put', '/api/ai/policy'],
    ])
  })

  it('omits blank page context when creating run and workflow', async () => {
    await createAiRun({
      prompt: '查询库存预警',
      entryPoint: 'SIDE_PANEL',
      contextObjectType: '',
      contextObjectId: '',
    })
    await createAiWorkflow({
      runNo: 'AIR1',
      workflowType: 'INVENTORY_RISK_REVIEW',
      contextObjectType: ' ',
      contextObjectId: '',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0].data).toEqual({
      prompt: '查询库存预警',
      entryPoint: 'SIDE_PANEL',
    })
    expect(mockedAxios.request.mock.calls[1]?.[0].data).toEqual({
      runNo: 'AIR1',
      workflowType: 'INVENTORY_RISK_REVIEW',
    })
  })

  it('does not direct-call the separate ai service from frontend source', () => {
    const offenders = collectSourceFiles(srcDir).filter((file) => {
      const content = fs.readFileSync(file, 'utf8')
      return /dealer-ai|localhost:8091|\/internal\/runs|\/internal\/ai\/tools/.test(content)
    })

    expect(offenders.map((file) => path.relative(srcDir, file))).toEqual([])
  })
})
