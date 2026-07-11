<script setup lang="ts">
import { computed } from 'vue'
import { Check, X } from '@lucide/vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { AiProposal } from '@/modules/ai/model/ai.types'

defineOptions({
  name: 'AiProposalCard',
})

const props = defineProps<{
  proposal: AiProposal
  loading?: boolean
  canConfirm?: boolean
}>()

const emit = defineEmits<{
  confirm: [proposal: AiProposal]
  reject: [proposal: AiProposal]
}>()

const riskLabel = computed(() => {
  switch (props.proposal.riskLevel) {
    case 'LOW':
      return '低风险'
    case 'READONLY':
      return '只读'
    case 'MEDIUM':
      return '中风险'
    case 'HIGH':
      return '高风险'
    default:
      return '需确认'
  }
})

const typeLabel = computed(() => {
  switch (props.proposal.proposalType) {
    case 'create_communication_record_proposal':
      return '创建沟通记录'
    case 'create_follow_task_proposal':
      return '创建跟进任务'
    default:
      return '待确认业务操作'
  }
})

const relatedObjectLabel = computed(() => {
  const labels: Record<string, string> = {
    CLUE: '线索',
    CUSTOMER: '客户',
    OPPORTUNITY: '商机',
    TEST_DRIVE: '试驾',
    ORDER: '订单',
    TRANSACTION: '交易',
  }
  return labels[props.proposal.relatedObjectType] ?? '业务对象'
})

const expiresTimeLabel = computed(() => {
  const date = new Date(props.proposal.expiresTime)
  if (Number.isNaN(date.getTime())) return '以系统有效期为准'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
})

const statusLabel = computed(() => {
  switch (props.proposal.status) {
    case 'CONFIRMED':
      return '已确认'
    case 'REJECTED':
      return '已拒绝'
    case 'EXPIRED':
      return '已过期'
    case 'EXECUTED':
      return '已执行'
    case 'FAILED':
      return '执行失败'
    default:
      return '待确认'
  }
})
</script>

<template>
  <div class="border-l-2 border-[var(--crm-primary)] py-1 pl-4">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <div class="font-semibold text-[var(--crm-text-primary)]">{{ typeLabel }}</div>
        <div class="mt-1 text-sm text-[var(--crm-text-secondary)]">{{ proposal.paramsSummary }}</div>
      </div>
      <Badge variant="outline" class="shrink-0 rounded-md">{{ riskLabel }}</Badge>
    </div>
    <div class="mt-3 space-y-2 text-xs text-[var(--crm-text-tertiary)]">
      <div>关联范围：{{ relatedObjectLabel }}</div>
      <div>影响说明：{{ proposal.impactSummary }}</div>
      <div>有效期至：{{ expiresTimeLabel }}</div>
    </div>
    <div v-if="proposal.status && proposal.status !== 'PENDING_CONFIRMATION'" class="mt-3 text-sm">
      状态：{{ statusLabel }}
    </div>
    <div v-else-if="canConfirm" class="mt-3 flex justify-end gap-2">
      <Button variant="outline" size="sm" :disabled="loading" @click="emit('reject', proposal)">
        <X class="mr-1 h-4 w-4" />
        拒绝
      </Button>
      <Button size="sm" :disabled="loading" @click="emit('confirm', proposal)">
        <Check class="mr-1 h-4 w-4" />
        确认
      </Button>
    </div>
    <div v-else class="mt-3 text-xs text-[var(--crm-text-tertiary)]">
      当前账号可查看该提议，但没有确认权限。
    </div>
  </div>
</template>
