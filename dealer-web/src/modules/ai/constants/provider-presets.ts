import type { AiProviderFormat } from '@/modules/ai/model/ai.types'

export interface AiProviderModelPreset {
  value: string
  label: string
  displayName: string
  legacy?: boolean
}

export interface AiProviderRegionPreset {
  value: string
  label: string
  baseUrl: string
}

export interface AiProviderPreset {
  key: string
  label: string
  providerName: string
  providerFormat: AiProviderFormat
  models: AiProviderModelPreset[]
  regions?: AiProviderRegionPreset[]
  baseUrl?: string
  defaultModel: string
  timeoutSeconds: number
  maxOutputTokens: number
  temperature: number
  custom: boolean
}

export const AI_PROVIDER_PRESETS: AiProviderPreset[] = [
  {
    key: 'qwen',
    label: '千问（阿里云百炼）',
    providerName: '千问',
    providerFormat: 'OPENAI_COMPATIBLE',
    defaultModel: 'qwen-plus',
    timeoutSeconds: 15,
    maxOutputTokens: 1024,
    temperature: 0.7,
    custom: false,
    regions: [
      {
        value: 'cn-beijing',
        label: '中国北京',
        baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      },
      {
        value: 'ap-southeast-1',
        label: '新加坡',
        baseUrl: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1',
      },
      {
        value: 'us-virginia',
        label: '美国弗吉尼亚',
        baseUrl: 'https://dashscope-us.aliyuncs.com/compatible-mode/v1',
      },
      {
        value: 'cn-hongkong',
        label: '香港',
        baseUrl: 'https://dashscope-hk.aliyuncs.com/compatible-mode/v1',
      },
    ],
    models: [
      { value: 'qwen-plus', label: 'qwen-plus', displayName: 'Qwen Plus' },
      { value: 'qwen-max', label: 'qwen-max', displayName: 'Qwen Max' },
      { value: 'qwen-turbo', label: 'qwen-turbo', displayName: 'Qwen Turbo' },
    ],
  },
  {
    key: 'deepseek',
    label: '深度求索（DeepSeek）',
    providerName: 'DeepSeek',
    providerFormat: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://api.deepseek.com',
    defaultModel: 'deepseek-v4-pro',
    timeoutSeconds: 15,
    maxOutputTokens: 1024,
    temperature: 0.7,
    custom: false,
    models: [
      { value: 'deepseek-v4-pro', label: 'deepseek-v4-pro', displayName: 'DeepSeek V4 Pro' },
      { value: 'deepseek-v4-flash', label: 'deepseek-v4-flash', displayName: 'DeepSeek V4 Flash' },
      {
        value: 'deepseek-chat',
        label: 'deepseek-chat（兼容旧模型，不推荐新配置使用）',
        displayName: 'DeepSeek Chat',
        legacy: true,
      },
      {
        value: 'deepseek-reasoner',
        label: 'deepseek-reasoner（兼容旧模型，不推荐新配置使用）',
        displayName: 'DeepSeek Reasoner',
        legacy: true,
      },
    ],
  },
  {
    key: 'minimax',
    label: 'MiniMax',
    providerName: 'MiniMax',
    providerFormat: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://api.minimax.io/v1',
    defaultModel: 'MiniMax-M3',
    timeoutSeconds: 15,
    maxOutputTokens: 1024,
    temperature: 0.7,
    custom: false,
    models: [
      { value: 'MiniMax-M3', label: 'MiniMax-M3', displayName: 'MiniMax M3' },
      { value: 'MiniMax-M2.7', label: 'MiniMax-M2.7', displayName: 'MiniMax M2.7' },
      { value: 'MiniMax-M2.5', label: 'MiniMax-M2.5', displayName: 'MiniMax M2.5' },
    ],
  },
  {
    key: 'custom-openai',
    label: '自定义 OpenAI-compatible',
    providerName: '自定义 OpenAI-compatible',
    providerFormat: 'OPENAI_COMPATIBLE',
    defaultModel: '',
    timeoutSeconds: 15,
    maxOutputTokens: 1024,
    temperature: 0.7,
    custom: true,
    models: [],
  },
  {
    key: 'custom-anthropic',
    label: '自定义 Anthropic',
    providerName: '自定义 Anthropic',
    providerFormat: 'ANTHROPIC',
    defaultModel: '',
    timeoutSeconds: 15,
    maxOutputTokens: 1024,
    temperature: 0.7,
    custom: true,
    models: [],
  },
]

export function findAiProviderPreset(key: string): AiProviderPreset {
  return AI_PROVIDER_PRESETS.find((preset) => preset.key === key) ?? AI_PROVIDER_PRESETS[0]
}
