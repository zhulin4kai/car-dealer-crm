<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  CheckCircle2,
  KeyRound,
  MoreHorizontal,
  Pencil,
  PlugZap,
  Plus,
  Power,
  RefreshCcw,
  Save,
} from '@lucide/vue'

import { ApiError } from '@/shared/api/api-error'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  activateAiProviderConfig,
  createAiProviderConfig,
  disableAiProviderConfig,
  listAiProviderConfigs,
  rotateAiProviderKey,
  testAiProviderConfig,
  updateAiProviderConfig,
} from '@/modules/ai/api/ai-api'
import {
  AI_PROVIDER_PRESETS,
  findAiProviderPreset,
  type AiProviderModelPreset,
  type AiProviderPreset,
} from '@/modules/ai/constants/provider-presets'
import type {
  AiProviderConfig,
  AiProviderFormat,
  AiProviderTestStatus,
  CreateAiProviderConfigRequest,
} from '@/modules/ai/model/ai.types'
import { PERMISSIONS } from '@/shared/constants/permissions'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { formatDateTime } from '@/shared/utils/display-format'
import { messageTip } from '@/shared/utils/feedback'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'AiProviderConfigsPage',
})

interface ProviderConfigForm extends CreateAiProviderConfigRequest {
  presetKey: string
  regionKey: string
}

const permissionStore = usePermissionStore()
const router = useRouter()
const configs = ref<AiProviderConfig[]>([])
const loading = ref(false)
const configDialogOpen = ref(false)
const keyDialogOpen = ref(false)
const advancedOpen = ref(false)
const editingConfigNo = ref<string | null>(null)
const rotatingConfig = ref<AiProviderConfig | null>(null)
const rotateApiKey = ref('')
const submitError = ref('')
const rotateError = ref('')
const form = reactive<ProviderConfigForm>({
  presetKey: 'qwen',
  regionKey: 'cn-beijing',
  providerName: '',
  providerFormat: 'OPENAI_COMPATIBLE',
  baseUrl: '',
  modelName: '',
  modelDisplayName: '',
  apiKey: '',
  timeoutSeconds: 15,
  maxOutputTokens: 1024,
  temperature: 0.7,
})
const FIELD_LIMITS = {
  timeoutSeconds: { min: 1, max: 60, label: '超时时间' },
  maxOutputTokens: { min: 1, max: 4096, label: '最大输出 Token' },
  temperature: { min: 0, max: 2, label: 'Temperature' },
} as const

const canManage = computed(() => permissionStore.hasPermission(PERMISSIONS.ai.providerConfigManage))
const canRotateKey = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.ai.providerConfigRotateKey),
)
const isEditing = computed(() => editingConfigNo.value !== null)
const selectedPreset = computed(() => findAiProviderPreset(form.presetKey))
const selectedPresetModels = computed(() => selectedPreset.value.models)
const selectedPresetRegions = computed(() => selectedPreset.value.regions ?? [])
const isCustomPreset = computed(() => selectedPreset.value.custom)
const formErrors = computed(() => {
  const errors: Partial<Record<keyof ProviderConfigForm, string>> = {}
  if (!form.providerName.trim()) errors.providerName = '请输入配置名称'
  if (!form.baseUrl.trim()) errors.baseUrl = '请输入 Base URL'
  if (!form.modelName.trim()) errors.modelName = '请输入模型名称'
  if (!form.modelDisplayName.trim()) errors.modelDisplayName = '请输入模型展示名'
  if (!isEditing.value && !form.apiKey.trim()) errors.apiKey = '请输入 API Key'
  const timeoutError = validateNumberRange(
    form.timeoutSeconds,
    FIELD_LIMITS.timeoutSeconds.min,
    FIELD_LIMITS.timeoutSeconds.max,
    FIELD_LIMITS.timeoutSeconds.label,
  )
  const tokenError = validateNumberRange(
    form.maxOutputTokens,
    FIELD_LIMITS.maxOutputTokens.min,
    FIELD_LIMITS.maxOutputTokens.max,
    FIELD_LIMITS.maxOutputTokens.label,
  )
  const temperatureError = validateNumberRange(
    form.temperature,
    FIELD_LIMITS.temperature.min,
    FIELD_LIMITS.temperature.max,
    FIELD_LIMITS.temperature.label,
  )
  if (timeoutError) errors.timeoutSeconds = timeoutError
  if (tokenError) errors.maxOutputTokens = tokenError
  if (temperatureError) errors.temperature = temperatureError
  return errors
})
const formValid = computed(() => Object.keys(formErrors.value).length === 0)

async function loadConfigs(): Promise<void> {
  loading.value = true
  try {
    configs.value = await listAiProviderConfigs()
  } finally {
    loading.value = false
  }
}

function goBackToAiPage(): void {
  void router.push('/dashboard/ai')
}

function openCreateDialog(): void {
  editingConfigNo.value = null
  resetForm()
  submitError.value = ''
  configDialogOpen.value = true
}

function openEditDialog(config: AiProviderConfig): void {
  const preset = detectPreset(config)
  editingConfigNo.value = config.configNo
  form.presetKey = preset.key
  form.regionKey = detectRegionKey(preset, config.baseUrl)
  form.providerName = config.providerName
  form.providerFormat = config.providerFormat
  form.baseUrl = config.baseUrl
  form.modelName = config.modelName
  form.modelDisplayName = config.modelDisplayName
  form.apiKey = ''
  form.timeoutSeconds = config.timeoutSeconds
  form.maxOutputTokens = config.maxOutputTokens
  form.temperature = config.temperature
  advancedOpen.value = preset.custom
  submitError.value = ''
  configDialogOpen.value = true
}

function openRotateDialog(config: AiProviderConfig): void {
  rotatingConfig.value = config
  rotateApiKey.value = ''
  rotateError.value = ''
  keyDialogOpen.value = true
}

function resetForm(): void {
  applyPreset('qwen')
  form.apiKey = ''
  advancedOpen.value = false
}

function handlePresetChange(): void {
  applyPreset(form.presetKey)
  advancedOpen.value = isCustomPreset.value
}

function handleRegionChange(): void {
  const region = selectedPresetRegions.value.find((item) => item.value === form.regionKey)
  if (region) {
    form.baseUrl = region.baseUrl
  }
}

function handleModelChange(): void {
  const model = selectedPresetModels.value.find((item) => item.value === form.modelName)
  if (!model) return
  form.modelDisplayName = model.displayName
  if (!isEditing.value) {
    form.providerName = `${selectedPreset.value.providerName} ${model.displayName}`
  }
}

function applyPreset(presetKey: string): void {
  const preset = findAiProviderPreset(presetKey)
  const firstRegion = preset.regions?.[0]
  const defaultModel = preset.models.find((model) => model.value === preset.defaultModel)
    ?? preset.models[0]
  form.presetKey = preset.key
  form.regionKey = firstRegion?.value ?? ''
  form.providerFormat = preset.providerFormat
  form.baseUrl = firstRegion?.baseUrl ?? preset.baseUrl ?? ''
  form.modelName = defaultModel?.value ?? preset.defaultModel
  form.modelDisplayName = defaultModel?.displayName ?? preset.defaultModel
  form.providerName = defaultModel
    ? `${preset.providerName} ${defaultModel.displayName}`
    : preset.providerName
  form.timeoutSeconds = preset.timeoutSeconds
  form.maxOutputTokens = preset.maxOutputTokens
  form.temperature = preset.temperature
}

async function submitForm(): Promise<void> {
  if (!canManage.value) return
  submitError.value = ''
  if (!formValid.value) {
    const firstError = Object.values(formErrors.value)[0] ?? '请检查模型配置表单'
    submitError.value = firstError
    advancedOpen.value = true
    messageTip(firstError, 'error')
    return
  }
  loading.value = true
  try {
    if (editingConfigNo.value) {
      await updateAiProviderConfig(editingConfigNo.value, {
        providerName: form.providerName,
        providerFormat: form.providerFormat,
        baseUrl: form.baseUrl,
        modelName: form.modelName,
        modelDisplayName: form.modelDisplayName,
        timeoutSeconds: form.timeoutSeconds,
        maxOutputTokens: form.maxOutputTokens,
        temperature: form.temperature,
      })
    } else {
      await createAiProviderConfig({
        providerName: form.providerName,
        providerFormat: form.providerFormat,
        baseUrl: form.baseUrl,
        modelName: form.modelName,
        modelDisplayName: form.modelDisplayName,
        apiKey: form.apiKey,
        timeoutSeconds: form.timeoutSeconds,
        maxOutputTokens: form.maxOutputTokens,
        temperature: form.temperature,
      })
    }
    configDialogOpen.value = false
    resetForm()
    await loadConfigs()
  } catch (error) {
    const message = getErrorMessage(error, isEditing.value ? '模型配置保存失败' : '模型配置创建失败')
    submitError.value = message
    messageTip(message, 'error')
  } finally {
    loading.value = false
  }
}

async function testConfig(config: AiProviderConfig): Promise<void> {
  if (!canManage.value) return
  loading.value = true
  try {
    const result = await testAiProviderConfig(config.configNo)
    messageTip(result.message || '模型连接测试完成', result.testStatus === 'SUCCESS' ? 'success' : 'error')
    await loadConfigs()
  } catch (error) {
    messageTip(getErrorMessage(error, '模型连接测试失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function activateConfig(config: AiProviderConfig): Promise<void> {
  if (!canManage.value) return
  loading.value = true
  try {
    await activateAiProviderConfig(config.configNo)
    await loadConfigs()
  } catch (error) {
    messageTip(getErrorMessage(error, '模型配置启用失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function disableConfig(config: AiProviderConfig): Promise<void> {
  if (!canManage.value) return
  loading.value = true
  try {
    await disableAiProviderConfig(config.configNo)
    await loadConfigs()
  } catch (error) {
    messageTip(getErrorMessage(error, '模型配置停用失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function submitRotateKey(): Promise<void> {
  if (!canRotateKey.value || !rotatingConfig.value) return
  rotateError.value = ''
  if (!rotateApiKey.value.trim()) {
    rotateError.value = '请输入新的 API Key'
    messageTip(rotateError.value, 'error')
    return
  }
  loading.value = true
  try {
    await rotateAiProviderKey(rotatingConfig.value.configNo, { apiKey: rotateApiKey.value.trim() })
    rotateApiKey.value = ''
    keyDialogOpen.value = false
    rotatingConfig.value = null
    await loadConfigs()
  } catch (error) {
    rotateError.value = getErrorMessage(error, 'API Key 轮换失败')
    messageTip(rotateError.value, 'error')
  } finally {
    loading.value = false
  }
}

function validateNumberRange(value: unknown, min: number, max: number, label: string): string {
  const numberValue = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numberValue)) {
    return `${label}必须是数字`
  }
  if (numberValue < min || numberValue > max) {
    return `${label}必须在 ${min} 到 ${max} 之间`
  }
  return ''
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message || fallback
  }
  if (error instanceof Error) {
    return error.message || fallback
  }
  return fallback
}

function detectPreset(config: AiProviderConfig): AiProviderPreset {
  const preset = AI_PROVIDER_PRESETS.find((item) => {
    if (item.custom || item.providerFormat !== config.providerFormat) return false
    const baseUrlMatched = item.baseUrl === config.baseUrl
      || item.regions?.some((region) => region.baseUrl === config.baseUrl)
    const modelMatched = item.models.some((model) => model.value === config.modelName)
    return baseUrlMatched && modelMatched
  })
  if (preset) return preset
  return config.providerFormat === 'ANTHROPIC'
    ? findAiProviderPreset('custom-anthropic')
    : findAiProviderPreset('custom-openai')
}

function detectRegionKey(preset: AiProviderPreset, baseUrl: string): string {
  return preset.regions?.find((region) => region.baseUrl === baseUrl)?.value ?? preset.regions?.[0]?.value ?? ''
}

function formatProviderFormat(format: AiProviderFormat): string {
  return format === 'ANTHROPIC' ? 'Anthropic Messages' : 'OpenAI Compatible'
}

function formatTestStatus(status: AiProviderTestStatus): string {
  const labels: Record<AiProviderTestStatus, string> = {
    UNTESTED: '未测试',
    SUCCESS: '连接成功',
    FAILED: '连接失败',
  }
  return labels[status]
}

function testStatusTone(status: AiProviderTestStatus): 'success' | 'danger' | 'muted' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'muted'
}

function enabledStatusTone(enabled: boolean): 'success' | 'muted' {
  return enabled ? 'success' : 'muted'
}

function modelLabel(model: AiProviderModelPreset): string {
  return model.legacy ? `${model.value}（兼容旧模型，不推荐新配置使用）` : model.value
}

onMounted(() => {
  resetForm()
  void loadConfigs()
})
</script>

<template>
  <div class="crm-data-page" data-testid="ai-provider-config-page">
    <section class="crm-panel">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4"
      >
        <div class="flex min-w-0 items-center gap-3">
          <Button variant="outline" size="sm" class="gap-2" @click="goBackToAiPage">
            <ArrowLeft class="h-4 w-4" />
            返回 AI 助手
          </Button>
          <div class="min-w-0">
            <h1 class="text-lg font-semibold text-[var(--crm-text-primary)]">AI 模型配置</h1>
            <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">
              管理全局模型供应商配置，同一时间只启用一个配置。
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button variant="outline" class="gap-2" :disabled="loading" @click="loadConfigs">
            <RefreshCcw class="h-4 w-4" />
            刷新
          </Button>
          <Button v-if="canManage" class="gap-2" :disabled="loading" @click="openCreateDialog">
            <Plus class="h-4 w-4" />
            新增配置
          </Button>
        </div>
      </div>

      <div class="crm-table-shell">
        <Table class="min-w-[1120px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]">
            <TableRow>
              <TableHead>配置名称</TableHead>
              <TableHead>协议</TableHead>
              <TableHead>模型</TableHead>
              <TableHead>API Key</TableHead>
              <TableHead>启用状态</TableHead>
              <TableHead>测试状态</TableHead>
              <TableHead>最近测试</TableHead>
              <TableHead class="w-[250px] text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="!configs.length && !loading">
              <TableCell colspan="8" class="h-32 text-center text-[var(--crm-text-tertiary)]">
                暂无模型配置
              </TableCell>
            </TableRow>
            <TableRow
              v-for="config in configs"
              :key="config.configNo"
              :class="config.enabled ? 'bg-[#F7FDF9]' : ''"
            >
              <TableCell>
                <div class="flex max-w-[260px] items-center gap-2">
                  <span
                    v-if="config.enabled"
                    class="h-2 w-2 shrink-0 rounded-full bg-[var(--crm-success)]"
                    aria-hidden="true"
                  />
                  <span class="truncate font-semibold text-[var(--crm-text-primary)]">
                    {{ config.providerName }}
                  </span>
                </div>
                <div class="mt-1 max-w-[260px] truncate text-xs text-[var(--crm-text-tertiary)]">
                  {{ config.baseUrl }}
                </div>
              </TableCell>
              <TableCell>{{ formatProviderFormat(config.providerFormat) }}</TableCell>
              <TableCell>
                <div class="max-w-[180px] truncate">{{ config.modelDisplayName }}</div>
                <div class="mt-1 max-w-[180px] truncate font-mono text-xs text-[var(--crm-text-tertiary)]">
                  {{ config.modelName }}
                </div>
              </TableCell>
              <TableCell>{{ config.hasApiKey ? config.maskedApiKey : '未配置' }}</TableCell>
              <TableCell>
                <StatusBadge
                  :label="config.enabled ? '启用中' : '未启用'"
                  :tone="enabledStatusTone(config.enabled)"
                />
              </TableCell>
              <TableCell>
                <StatusBadge :label="formatTestStatus(config.testStatus)" :tone="testStatusTone(config.testStatus)" />
                <div
                  v-if="config.lastTestMessage"
                  class="mt-1 max-w-[180px] truncate text-xs text-[var(--crm-text-tertiary)]"
                >
                  {{ config.lastTestMessage }}
                </div>
              </TableCell>
              <TableCell>{{ config.lastTestTime ? formatDateTime(config.lastTestTime) : '--' }}</TableCell>
              <TableCell class="text-right">
                <div class="flex items-center justify-end gap-2">
                  <Button
                    v-if="canManage"
                    variant="outline"
                    size="sm"
                    class="border-[var(--crm-info-bg)] text-[var(--crm-info)] hover:bg-[var(--crm-info-bg)]"
                    :disabled="loading"
                    @click="testConfig(config)"
                  >
                    <PlugZap class="mr-1 h-4 w-4" />
                    测试
                  </Button>
                  <Button
                    v-if="canManage && !config.enabled"
                    variant="outline"
                    size="sm"
                    class="border-[var(--crm-success-bg)] text-[var(--crm-success)] hover:bg-[var(--crm-success-bg)]"
                    :disabled="loading"
                    @click="activateConfig(config)"
                  >
                    <CheckCircle2 class="mr-1 h-4 w-4" />
                    启用
                  </Button>
                  <Button
                    v-if="canManage && config.enabled"
                    variant="outline"
                    size="sm"
                    class="border-[var(--crm-danger-bg)] text-[var(--crm-danger)] hover:bg-[var(--crm-danger-bg)]"
                    :disabled="loading"
                    @click="disableConfig(config)"
                  >
                    <Power class="mr-1 h-4 w-4" />
                    停用
                  </Button>
                  <DropdownMenu v-if="canManage || canRotateKey">
                    <DropdownMenuTrigger as-child>
                      <Button
                        variant="outline"
                        size="sm"
                        class="h-8 w-8 p-0"
                        :disabled="loading"
                        aria-label="更多操作"
                      >
                        <MoreHorizontal class="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" class="w-36">
                      <DropdownMenuItem v-if="canManage" class="gap-2" @click="openEditDialog(config)">
                        <Pencil class="h-4 w-4" />
                        编辑
                      </DropdownMenuItem>
                      <DropdownMenuItem v-if="canRotateKey" class="gap-2" @click="openRotateDialog(config)">
                        <KeyRound class="h-4 w-4" />
                        轮换 Key
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>

      <div v-if="loading" class="border-t border-[var(--crm-border-light)] px-5 py-3 text-sm text-[var(--crm-text-tertiary)]">
        处理中...
      </div>
    </section>

    <Dialog v-model:open="configDialogOpen">
      <DialogContent class="sm:max-w-[720px]">
        <DialogHeader>
          <DialogTitle>{{ isEditing ? '编辑模型配置' : '新增模型配置' }}</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="submitForm">
          <Alert v-if="submitError" variant="destructive">
            <AlertDescription>{{ submitError }}</AlertDescription>
          </Alert>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <Label>模型提供商</Label>
              <NativeSelect v-model="form.presetKey" class="w-full min-w-[260px]" :disabled="isEditing" @change="handlePresetChange">
                <option v-for="preset in AI_PROVIDER_PRESETS" :key="preset.key" :value="preset.key">
                  {{ preset.label }}
                </option>
              </NativeSelect>
            </div>
            <div v-if="selectedPresetRegions.length" class="space-y-2">
              <Label>区域</Label>
              <NativeSelect v-model="form.regionKey" class="w-full min-w-[260px]" @change="handleRegionChange">
                <option v-for="region in selectedPresetRegions" :key="region.value" :value="region.value">
                  {{ region.label }}
                </option>
              </NativeSelect>
            </div>
            <div class="space-y-2">
              <Label>模型</Label>
              <NativeSelect
                v-if="!isCustomPreset"
                v-model="form.modelName"
                class="w-full min-w-[260px]"
                required
                @change="handleModelChange"
              >
                <option v-for="model in selectedPresetModels" :key="model.value" :value="model.value">
                  {{ modelLabel(model) }}
                </option>
              </NativeSelect>
              <Input v-else v-model="form.modelName" required maxlength="128" :aria-invalid="!!formErrors.modelName" />
              <p v-if="formErrors.modelName" class="text-xs text-destructive">{{ formErrors.modelName }}</p>
            </div>
            <div class="space-y-2">
              <Label>配置名称</Label>
              <Input v-model="form.providerName" required maxlength="64" :aria-invalid="!!formErrors.providerName" />
              <p v-if="formErrors.providerName" class="text-xs text-destructive">{{ formErrors.providerName }}</p>
            </div>
            <div v-if="isCustomPreset" class="space-y-2">
              <Label>协议格式</Label>
              <NativeSelect v-model="form.providerFormat" class="w-full min-w-[260px]" required>
                <option value="OPENAI_COMPATIBLE">OpenAI Compatible</option>
                <option value="ANTHROPIC">Anthropic Messages</option>
              </NativeSelect>
            </div>
            <div v-if="isCustomPreset" class="space-y-2">
              <Label>Base URL</Label>
              <Input v-model="form.baseUrl" required maxlength="255" :aria-invalid="!!formErrors.baseUrl" />
              <p v-if="formErrors.baseUrl" class="text-xs text-destructive">{{ formErrors.baseUrl }}</p>
            </div>
            <div v-if="isCustomPreset" class="space-y-2">
              <Label>模型展示名</Label>
              <Input
                v-model="form.modelDisplayName"
                required
                maxlength="128"
                :aria-invalid="!!formErrors.modelDisplayName"
              />
              <p v-if="formErrors.modelDisplayName" class="text-xs text-destructive">
                {{ formErrors.modelDisplayName }}
              </p>
            </div>
            <div v-if="!isEditing" class="space-y-2">
              <Label>API Key</Label>
              <Input
                v-model="form.apiKey"
                required
                type="password"
                maxlength="500"
                autocomplete="off"
                :aria-invalid="!!formErrors.apiKey"
              />
              <p v-if="formErrors.apiKey" class="text-xs text-destructive">{{ formErrors.apiKey }}</p>
            </div>
          </div>

          <Collapsible v-model:open="advancedOpen">
            <CollapsibleTrigger as-child>
              <Button type="button" variant="ghost" class="px-0 text-[var(--crm-primary)]">
                高级配置
              </Button>
            </CollapsibleTrigger>
            <CollapsibleContent class="pt-3">
              <div class="grid gap-4 md:grid-cols-2">
                <div v-if="!isCustomPreset" class="space-y-2">
                  <Label>Base URL</Label>
                  <Input v-model="form.baseUrl" required maxlength="255" :aria-invalid="!!formErrors.baseUrl" />
                  <p v-if="formErrors.baseUrl" class="text-xs text-destructive">{{ formErrors.baseUrl }}</p>
                </div>
                <div v-if="!isCustomPreset" class="space-y-2">
                  <Label>模型名称</Label>
                  <Input v-model="form.modelName" required maxlength="128" :aria-invalid="!!formErrors.modelName" />
                  <p v-if="formErrors.modelName" class="text-xs text-destructive">{{ formErrors.modelName }}</p>
                </div>
                <div v-if="!isCustomPreset" class="space-y-2">
                  <Label>模型展示名</Label>
                  <Input
                    v-model="form.modelDisplayName"
                    required
                    maxlength="128"
                    :aria-invalid="!!formErrors.modelDisplayName"
                  />
                  <p v-if="formErrors.modelDisplayName" class="text-xs text-destructive">
                    {{ formErrors.modelDisplayName }}
                  </p>
                </div>
                <div class="space-y-2">
                  <Label>超时时间（秒）</Label>
                  <Input
                    v-model.number="form.timeoutSeconds"
                    type="number"
                    :min="FIELD_LIMITS.timeoutSeconds.min"
                    :max="FIELD_LIMITS.timeoutSeconds.max"
                    :aria-invalid="!!formErrors.timeoutSeconds"
                  />
                  <p v-if="formErrors.timeoutSeconds" class="text-xs text-destructive">
                    {{ formErrors.timeoutSeconds }}
                  </p>
                  <p v-else class="text-xs text-[var(--crm-text-tertiary)]">
                    后端限制：{{ FIELD_LIMITS.timeoutSeconds.min }} 到 {{ FIELD_LIMITS.timeoutSeconds.max }} 秒。
                  </p>
                </div>
                <div class="space-y-2">
                  <Label>最大输出 Token</Label>
                  <Input
                    v-model.number="form.maxOutputTokens"
                    type="number"
                    :min="FIELD_LIMITS.maxOutputTokens.min"
                    :max="FIELD_LIMITS.maxOutputTokens.max"
                    :aria-invalid="!!formErrors.maxOutputTokens"
                  />
                  <p v-if="formErrors.maxOutputTokens" class="text-xs text-destructive">
                    {{ formErrors.maxOutputTokens }}
                  </p>
                  <p v-else class="text-xs text-[var(--crm-text-tertiary)]">
                    后端限制：{{ FIELD_LIMITS.maxOutputTokens.min }} 到 {{ FIELD_LIMITS.maxOutputTokens.max }}。
                  </p>
                </div>
                <div class="space-y-2">
                  <Label>Temperature</Label>
                  <Input
                    v-model.number="form.temperature"
                    type="number"
                    :min="FIELD_LIMITS.temperature.min"
                    :max="FIELD_LIMITS.temperature.max"
                    step="0.1"
                    :aria-invalid="!!formErrors.temperature"
                  />
                  <p v-if="formErrors.temperature" class="text-xs text-destructive">
                    {{ formErrors.temperature }}
                  </p>
                  <p v-else class="text-xs text-[var(--crm-text-tertiary)]">
                    后端限制：{{ FIELD_LIMITS.temperature.min }} 到 {{ FIELD_LIMITS.temperature.max }}。
                  </p>
                </div>
              </div>
            </CollapsibleContent>
          </Collapsible>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="loading" @click="configDialogOpen = false">取消</Button>
          <Button :disabled="loading" @click="submitForm">
            <Save class="mr-2 h-4 w-4" />
            {{ isEditing ? '保存修改' : '新增配置' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="keyDialogOpen">
      <DialogContent class="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>轮换 API Key</DialogTitle>
        </DialogHeader>
        <form class="space-y-4" @submit.prevent="submitRotateKey">
          <Alert v-if="rotateError" variant="destructive">
            <AlertDescription>{{ rotateError }}</AlertDescription>
          </Alert>
          <div class="space-y-2">
            <Label>配置名称</Label>
            <div class="text-sm text-[var(--crm-text-secondary)]">
              {{ rotatingConfig?.providerName || '--' }}
            </div>
          </div>
          <div class="space-y-2">
            <Label>新 API Key</Label>
            <Input
              v-model="rotateApiKey"
              required
              type="password"
              maxlength="500"
              autocomplete="off"
            />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" :disabled="loading" @click="keyDialogOpen = false">取消</Button>
          <Button :disabled="loading || !rotateApiKey.trim()" @click="submitRotateKey">
            <KeyRound class="mr-2 h-4 w-4" />
            确认轮换
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
