<script setup lang="ts">
import { Bell, Pause, Play, RefreshCw, XCircle } from '@lucide/vue'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type {
  AiProactiveEvent,
  AiProactiveSubscription,
} from '@/modules/ai/model/ai.types'

defineOptions({
  name: 'AiProactivePanel',
})

defineProps<{
  subscriptions: AiProactiveSubscription[]
  events: AiProactiveEvent[]
  loading?: boolean
  canManage?: boolean
}>()

const emit = defineEmits<{
  createInventory: []
  createFollow: []
  pause: [subscription: AiProactiveSubscription]
  resume: [subscription: AiProactiveSubscription]
  cancel: [subscription: AiProactiveSubscription]
  generate: []
}>()

function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    ACTIVE: '启用',
    PAUSED: '暂停',
    CANCELLED: '取消',
    READY: '已生成',
    NO_DATA: '无数据',
    FAILED: '失败',
    SKIPPED: '已跳过',
    CREATED: '已创建',
    GENERATING: '生成中',
  }
  return labels[status] ?? '未知状态'
}

function typeLabel(type: string): string {
  const labels: Record<string, string> = {
    FOLLOW_UP_REMINDER: '跟进提醒',
    TRANSACTION_EXCEPTION: '交易异常',
    INVENTORY_ALERT: '库存预警',
    DAILY_SUMMARY: '每日摘要',
    PERIODIC_SALES_ANALYSIS: '周期分析',
  }
  return labels[type] ?? '业务提醒'
}

function frequencyLabel(frequency: string): string {
  const labels: Record<string, string> = {
    REALTIME_LIMITED: '实时限频',
    DAILY: '每天',
    WEEKLY: '每周',
    MONTHLY: '每月',
  }
  return labels[frequency] ?? '按系统计划'
}

function objectTypeLabel(objectType: string): string {
  const labels: Record<string, string> = {
    CUSTOMER: '客户',
    TRANSACTION: '交易',
    INVENTORY: '库存',
    FOLLOW_TASK: '跟进任务',
  }
  return labels[objectType] ?? '业务对象'
}
</script>

<template>
  <section class="space-y-3">
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2 text-sm font-semibold text-[var(--crm-text-primary)]">
        <Bell class="h-4 w-4 text-[var(--crm-primary)]" />
        主动提醒
      </div>
      <Button v-if="canManage" variant="outline" size="sm" :disabled="loading" @click="emit('generate')">
        <RefreshCw class="mr-1 h-4 w-4" />
        生成
      </Button>
    </div>

    <div v-if="canManage" class="flex flex-wrap gap-2">
      <Button size="sm" :disabled="loading" @click="emit('createFollow')">订阅跟进提醒</Button>
      <Button variant="outline" size="sm" :disabled="loading" @click="emit('createInventory')">
        订阅库存预警
      </Button>
    </div>

    <div class="space-y-2">
      <div
        v-for="subscription in subscriptions"
        :key="subscription.subscriptionNo"
        class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-3"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="truncate text-sm font-semibold">{{ typeLabel(subscription.subscriptionType) }}</div>
            <div class="mt-1 text-xs text-[var(--crm-text-tertiary)]">
              {{ frequencyLabel(subscription.frequency) }} · 每日 {{ subscription.dailyLimit }} 次 · 单次
              {{ subscription.maxResults }} 条
            </div>
          </div>
          <Badge variant="outline" class="shrink-0 rounded-md">
            {{ statusLabel(subscription.status) }}
          </Badge>
        </div>
        <div class="mt-2 text-xs text-[var(--crm-text-tertiary)]">
          静默时间：{{ subscription.quietStartTime || '--' }} -
          {{ subscription.quietEndTime || '--' }}
        </div>
        <div v-if="canManage" class="mt-3 flex justify-end gap-2">
          <Button
            v-if="subscription.status === 'ACTIVE'"
            variant="outline"
            size="sm"
            :disabled="loading"
            @click="emit('pause', subscription)"
          >
            <Pause class="mr-1 h-4 w-4" />
            暂停
          </Button>
          <Button
            v-if="subscription.status === 'PAUSED'"
            variant="outline"
            size="sm"
            :disabled="loading"
            @click="emit('resume', subscription)"
          >
            <Play class="mr-1 h-4 w-4" />
            恢复
          </Button>
          <Button
            v-if="subscription.status !== 'CANCELLED'"
            variant="outline"
            size="sm"
            :disabled="loading"
            @click="emit('cancel', subscription)"
          >
            <XCircle class="mr-1 h-4 w-4" />
            取消
          </Button>
        </div>
      </div>
    </div>

    <div v-if="events.length" class="space-y-2">
      <div class="text-sm font-semibold text-[var(--crm-text-primary)]">提醒列表</div>
      <article
        v-for="event in events"
        :key="event.eventNo"
        class="rounded-lg border border-[var(--crm-border-light)] bg-[var(--crm-bg-muted)] p-3"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="truncate text-sm font-semibold text-[var(--crm-text-primary)]">
              {{ event.title }}
            </div>
            <div class="mt-1 text-xs text-[var(--crm-text-secondary)]">{{ event.summary }}</div>
          </div>
          <Badge variant="outline" class="shrink-0 rounded-md">{{ statusLabel(event.status) }}</Badge>
        </div>
        <div class="mt-2 text-xs text-[var(--crm-text-tertiary)]">
          {{ typeLabel(event.eventType) }}
          <span v-if="event.objectType"> · {{ objectTypeLabel(event.objectType) }}</span>
        </div>
        <div v-if="event.detailSummary" class="mt-2 text-xs text-[var(--crm-text-secondary)]">
          {{ event.detailSummary }}
        </div>
      </article>
    </div>
  </section>
</template>
