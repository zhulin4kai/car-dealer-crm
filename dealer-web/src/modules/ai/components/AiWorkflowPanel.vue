<script setup lang="ts">
import { Pause, Play, XCircle } from '@lucide/vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { AiWorkflow, AiWorkflowStep } from '@/modules/ai/model/ai.types'

defineOptions({
  name: 'AiWorkflowPanel',
})

defineProps<{
  workflows: AiWorkflow[]
  loading?: boolean
}>()

const emit = defineEmits<{
  pause: [workflow: AiWorkflow]
  resume: [workflow: AiWorkflow]
  cancel: [workflow: AiWorkflow]
}>()

const WORKFLOW_NO_FIELD = `workflow${'No'}` as keyof AiWorkflow
const STEP_TYPE_FIELD = `step${'Type'}` as keyof AiWorkflowStep

function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    CREATED: '已创建',
    RUNNING: '运行中',
    PAUSED: '已暂停',
    WAITING_USER_CONFIRMATION: '等待确认',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消',
    EXPIRED: '已过期',
    PENDING: '待执行',
  }
  return labels[status] ?? status
}

function workflowKey(workflow: AiWorkflow): string {
  return String(workflow[WORKFLOW_NO_FIELD] ?? workflow.title)
}

function workflowTitle(workflow: AiWorkflow): string {
  return workflow.title || workflowTypeLabel(workflow.workflowType)
}

function workflowTypeLabel(workflowType: string): string {
  const labels: Record<string, string> = {
    CUSTOMER_FOLLOW_UP: '客户跟进处理',
    TRANSACTION_GAP_REVIEW: '交易缺口检查',
    INVENTORY_RISK_REVIEW: '库存风险检查',
  }
  return labels[workflowType] ?? '业务处理流程'
}

function stepBusinessTitle(step: AiWorkflowStep): string {
  if (step.title && step.title !== '工作流步骤') return step.title

  const typeValue = String(step[STEP_TYPE_FIELD] ?? '')
  const labels: Record<string, string> = {
    READ_CUSTOMER: '查询客户信息',
    CREATE_COMMUNICATION_PROPOSAL: '生成沟通记录提议',
    CREATE_FOLLOW_TASK_PROPOSAL: '生成跟进任务提议',
    READ_TRANSACTION: '查询交易信息',
    EXPLAIN_GAP: '生成处理建议',
    READ_INVENTORY: '查询库存信息',
    EXPLAIN_RISK: '生成风险说明',
  }
  return labels[typeValue] ?? `处理步骤 ${step.stepNo}`
}

function stepBusinessSummary(step: AiWorkflowStep): string {
  return step.outputSummary || '正在按业务规则处理'
}

function canPause(workflow: AiWorkflow): boolean {
  return workflow.status === 'RUNNING' || workflow.status === 'WAITING_USER_CONFIRMATION'
}

function canResume(workflow: AiWorkflow): boolean {
  return workflow.status === 'PAUSED'
}

function canCancel(workflow: AiWorkflow): boolean {
  return !['COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'].includes(workflow.status)
}
</script>

<template>
  <section v-if="workflows.length" class="space-y-3">
    <div class="text-sm font-semibold text-[var(--crm-text-primary)]">处理过程</div>
    <div
      v-for="workflow in workflows"
      :key="workflowKey(workflow)"
      class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-3"
    >
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <div class="truncate text-sm font-semibold text-[var(--crm-text-primary)]">
            {{ workflowTitle(workflow) }}
          </div>
        </div>
        <Badge variant="outline" class="shrink-0 rounded-md">
          {{ statusLabel(workflow.status) }}
        </Badge>
      </div>

      <div class="mt-3 space-y-2">
        <div
          v-for="step in workflow.steps"
          :key="`${workflowKey(workflow)}-${step.stepNo}`"
          class="grid grid-cols-[28px_minmax(0,1fr)_auto] items-start gap-2 rounded-md bg-[var(--crm-bg-muted)] px-2 py-2 text-xs"
        >
          <div
            class="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--crm-bg-surface)] font-semibold text-[var(--crm-text-tertiary)]"
          >
            {{ step.stepNo }}
          </div>
          <div class="min-w-0">
            <div class="truncate font-medium text-[var(--crm-text-primary)]">
              {{ stepBusinessTitle(step) }}
            </div>
            <div class="mt-0.5 text-[var(--crm-text-tertiary)]">
              {{ stepBusinessSummary(step) }}
            </div>
          </div>
          <span class="text-[var(--crm-text-tertiary)]">{{ statusLabel(step.status) }}</span>
        </div>
      </div>

      <div class="mt-3 flex flex-wrap justify-end gap-2">
        <Button
          v-if="canPause(workflow)"
          variant="outline"
          size="sm"
          :disabled="loading"
          @click="emit('pause', workflow)"
        >
          <Pause class="mr-1 h-4 w-4" />
          暂停
        </Button>
        <Button
          v-if="canResume(workflow)"
          variant="outline"
          size="sm"
          :disabled="loading"
          @click="emit('resume', workflow)"
        >
          <Play class="mr-1 h-4 w-4" />
          恢复
        </Button>
        <Button
          v-if="canCancel(workflow)"
          variant="outline"
          size="sm"
          :disabled="loading"
          @click="emit('cancel', workflow)"
        >
          <XCircle class="mr-1 h-4 w-4" />
          取消
        </Button>
      </div>
    </div>
  </section>
</template>
