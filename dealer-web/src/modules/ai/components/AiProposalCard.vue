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
      return props.proposal.riskLevel
  }
})

const typeLabel = computed(() => {
  switch (props.proposal.proposalType) {
    case 'create_communication_record_proposal':
      return '创建沟通记录'
    case 'create_follow_task_proposal':
      return '创建跟进任务'
    default:
      return props.proposal.proposalType
  }
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
  <div class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-3">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <div class="font-semibold text-[var(--crm-text-primary)]">{{ typeLabel }}</div>
        <div class="mt-1 text-sm text-[var(--crm-text-secondary)]">{{ proposal.paramsSummary }}</div>
      </div>
      <Badge variant="outline" class="shrink-0 rounded-md">{{ riskLabel }}</Badge>
    </div>
    <div class="mt-3 space-y-2 text-xs text-[var(--crm-text-tertiary)]">
      <div>关联对象：{{ proposal.relatedObjectType }} {{ proposal.relatedObjectId }}</div>
      <div>影响说明：{{ proposal.impactSummary }}</div>
      <div>过期时间：{{ proposal.expiresTime }}</div>
      <div>权限：{{ proposal.permissionCode }}</div>
    </div>
    <div v-if="proposal.status && proposal.status !== 'PENDING_CONFIRMATION'" class="mt-3 text-sm">
      状态：{{ statusLabel }}
    </div>
    <div v-else class="mt-3 flex justify-end gap-2">
      <Button variant="outline" size="sm" :disabled="loading" @click="emit('reject', proposal)">
        <X class="mr-1 h-4 w-4" />
        拒绝
      </Button>
      <Button size="sm" :disabled="loading" @click="emit('confirm', proposal)">
        <Check class="mr-1 h-4 w-4" />
        确认
      </Button>
    </div>
  </div>
</template>
