<template>
  <div class="flex min-h-full gap-6 p-6 max-[1199px]:flex-col">
    <section class="min-w-0 flex-1 space-y-5">
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <article
          v-for="card in metricCards"
          :key="card.label"
          class="rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-5 shadow-[var(--crm-shadow-card)]"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="space-y-3">
              <div class="text-sm text-[var(--crm-text-tertiary)]">{{ card.label }}</div>
              <div class="flex items-end gap-2">
                <span class="text-3xl font-semibold leading-none">{{ card.value }}</span>
                <span v-if="card.subValue" class="pb-0.5 text-sm text-[var(--crm-text-tertiary)]">
                  {{ card.subValue }}
                </span>
              </div>
              <div class="text-sm text-[var(--crm-text-tertiary)]">{{ card.description }}</div>
            </div>
            <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl" :class="card.iconClass">
              <component :is="card.icon" class="h-5 w-5" />
            </div>
          </div>
        </article>
      </div>

      <section class="overflow-hidden rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] shadow-[var(--crm-shadow-card)]">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border-light)] px-5 py-4">
          <div class="flex items-center gap-3">
            <h2 class="text-base font-semibold">最新线索</h2>
            <span class="rounded-md bg-[var(--crm-bg-muted)] px-2 py-1 text-sm text-[var(--crm-text-tertiary)]">
              {{ formatNumber(clueTotal) }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <Button class="gap-2 bg-[var(--crm-primary)] hover:bg-[var(--crm-primary-hover)]" @click="goToClueList">
              <Plus class="h-4 w-4" />
              录入线索
            </Button>
            <Button variant="outline" class="gap-2" @click="goToClueList">
              <ExternalLink class="h-4 w-4" />
              查看全部
            </Button>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-[960px] w-full text-left text-sm">
            <thead class="bg-[var(--crm-bg-muted)] text-xs font-semibold text-[var(--crm-text-tertiary)]">
              <tr>
                <th class="px-5 py-3">姓名</th>
                <th class="px-5 py-3">手机</th>
                <th class="px-5 py-3">负责人</th>
                <th class="px-5 py-3">所属活动</th>
                <th class="px-5 py-3">意向状态</th>
                <th class="px-5 py-3">意向产品</th>
                <th class="px-5 py-3">来源</th>
                <th class="px-5 py-3">下次联系</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--crm-border-light)]">
              <tr v-if="clueLoading">
                <td class="px-5 py-10 text-center text-[var(--crm-text-tertiary)]" colspan="8">线索加载中...</td>
              </tr>
              <tr v-else-if="clueError">
                <td class="px-5 py-10 text-center text-[var(--crm-danger)]" colspan="8">线索加载失败，请稍后重试</td>
              </tr>
              <tr v-else-if="recentClues.length === 0">
                <td class="px-5 py-10 text-center text-[var(--crm-text-tertiary)]" colspan="8">暂无线索数据</td>
              </tr>
              <template v-else>
                <tr
                  v-for="clue in recentClues"
                  :key="clue.id ?? clue.phone"
                  class="transition-colors hover:bg-[var(--crm-bg-hover)]"
                  :class="clue.id ? 'cursor-pointer' : ''"
                  @click="goToClueDetail(clue)"
                >
                  <td class="px-5 py-4 font-semibold text-[var(--crm-text-primary)]">{{ clue.fullName || '--' }}</td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">{{ formatPhone(clue.phone) }}</td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">
                    <span class="inline-flex items-center gap-2">
                      <span class="flex h-7 w-7 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-xs font-semibold text-[var(--crm-primary)]">
                        {{ getNameInitial(clue.ownerDO?.name) }}
                      </span>
                      {{ clue.ownerDO?.name || '--' }}
                    </span>
                  </td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">{{ clue.activityDO?.name || '--' }}</td>
                  <td class="px-5 py-4">
                    <StatusBadge :label="clue.intentionStateDO?.typeValue" :tone="getClueTone(clue.intentionStateDO?.typeValue)" />
                  </td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">{{ clue.intentionProductDO?.name || '--' }}</td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">{{ clue.sourceDO?.typeValue || '--' }}</td>
                  <td class="px-5 py-4 text-[var(--crm-text-secondary)]">{{ formatDateTime(clue.nextContactTime) }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>
    </section>

    <aside class="w-[var(--crm-right-panel-width)] shrink-0 space-y-5 max-[1199px]:w-full">
      <section class="rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-5 shadow-[var(--crm-shadow-card)]">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-base font-semibold">业务动态</h2>
        </div>
        <div class="flex min-h-[180px] flex-col items-center justify-center rounded-lg border border-dashed border-[var(--crm-border)] bg-[var(--crm-bg-muted)] px-6 text-center">
          <Inbox class="mb-3 h-8 w-8 text-[var(--crm-text-tertiary)]" />
          <p class="text-sm font-medium text-[var(--crm-text-secondary)]">暂无业务动态数据</p>
          <p class="mt-1 text-xs text-[var(--crm-text-tertiary)]">审计日志接口尚未开放</p>
        </div>
      </section>

      <section class="rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-5 shadow-[var(--crm-shadow-card)]">
        <div class="mb-2">
          <h2 class="text-base font-semibold">销售漏斗</h2>
          <p class="mt-1 text-sm text-[var(--crm-text-tertiary)]">线索到成交转化追踪</p>
        </div>
        <div ref="funnelChartRef" class="h-[260px] w-full" />
        <div v-if="funnelData.length === 0" class="-mt-[260px] flex h-[260px] items-center justify-center text-sm text-[var(--crm-text-tertiary)]">
          暂无漏斗数据
        </div>
        <div class="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
          <div v-for="item in funnelRates" :key="item.label">
            <div class="text-sm font-semibold text-[var(--crm-text-primary)]">{{ item.value }}</div>
            <div class="mt-1 text-[var(--crm-text-tertiary)]">{{ item.label }}</div>
          </div>
        </div>
      </section>

      <section class="rounded-[var(--crm-card-radius)] border border-[var(--crm-border-light)] bg-[var(--crm-bg-surface)] p-5 shadow-[var(--crm-shadow-card)]">
        <div class="mb-4">
          <h2 class="text-base font-semibold">线索来源分布</h2>
        </div>
        <div ref="sourceChartRef" class="h-[250px] w-full" />
        <div v-if="sourceData.length === 0" class="-mt-[250px] flex h-[250px] items-center justify-center text-sm text-[var(--crm-text-tertiary)]">
          暂无来源数据
        </div>
        <div class="space-y-2">
          <div
            v-for="(item, index) in sourceData"
            :key="item.name ?? index"
            class="flex items-center justify-between gap-3 text-sm"
          >
            <span class="flex min-w-0 items-center gap-2 text-[var(--crm-text-secondary)]">
              <span class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ backgroundColor: sourceColors[index % sourceColors.length] }" />
              <span class="truncate">{{ item.name || '--' }}</span>
            </span>
            <span class="font-semibold text-[var(--crm-text-primary)]">{{ formatNumber(item.value) }}</span>
          </div>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { Component } from 'vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { fetchCurrentClues } from '@/modules/clue/api/clue-api'
import type { Clue } from '@/modules/clue/model/clue.types'
import {
  fetchSaleFunnelData,
  fetchSourcePieData,
  fetchSummaryData,
} from '@/modules/statistic/api/statistic-api'
import type { NameValueData, SummaryData } from '@/modules/statistic/model/statistic.types'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import {
  formatCurrency,
  formatDateTime,
  formatNumber,
  formatPercent,
  formatPhone,
} from '@/shared/utils/display-format'
import { ExternalLink, Handshake, Inbox, Megaphone, Plus, UserSearch, Users } from '@lucide/vue'

defineOptions({
  name: 'StatisticView',
})

const router = useRouter()
const summaryData = reactive<SummaryData>({})
const recentClues = ref<Clue[]>([])
const clueTotal = ref(0)
const clueLoading = ref(false)
const clueError = ref(false)
const funnelData = ref<NameValueData[]>([])
const sourceData = ref<NameValueData[]>([])

const funnelChartRef = ref<HTMLElement | null>(null)
const sourceChartRef = ref<HTMLElement | null>(null)
let funnelChart: ECharts | null = null
let sourceChart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const sourceColors = ['#3370FF', '#34C759', '#FF9500', '#9B59B6', '#0A84FF', '#9CA3AF']

const metricCards = computed<Array<{
  label: string
  value: string
  subValue?: string
  description: string
  icon: Component
  iconClass: string
}>>(() => [
  {
    label: '市场活动',
    value: formatNumber(summaryData.effectiveActivityCount),
    subValue: `/ ${formatNumber(summaryData.totalActivityCount)}`,
    description: '有效/总数',
    icon: Megaphone,
    iconClass: 'bg-[var(--crm-primary-light)] text-[var(--crm-primary)]',
  },
  {
    label: '线索总数',
    value: formatNumber(summaryData.totalClueCount),
    description: '线索池总量',
    icon: UserSearch,
    iconClass: 'bg-[var(--crm-success-bg)] text-[var(--crm-success)]',
  },
  {
    label: '客户总数',
    value: formatNumber(summaryData.totalCustomerCount),
    description: '客户资产总量',
    icon: Users,
    iconClass: 'bg-[var(--crm-purple-bg)] text-[var(--crm-purple)]',
  },
  {
    label: '交易总额',
    value: formatCurrency(summaryData.successTranAmount, { fractionDigits: 1, suffix: '万' }),
    subValue: `/ ${formatCurrency(summaryData.totalTranAmount, { fractionDigits: 0, suffix: '万' })}`,
    description: '已成交/总额',
    icon: Handshake,
    iconClass: 'bg-[var(--crm-warning-bg)] text-[var(--crm-warning)]',
  },
])

const funnelRates = computed(() => {
  const values = funnelData.value.map((item) => Number(item.value ?? 0))
  return [
    { label: '线索 -> 客户', value: formatPercent(values[1] ?? 0, values[0] ?? 0) },
    { label: '客户 -> 交易', value: formatPercent(values[2] ?? 0, values[1] ?? 0) },
    { label: '交易 -> 成交', value: formatPercent(values[3] ?? 0, values[2] ?? 0) },
  ]
})

async function loadSummary(): Promise<void> {
  Object.assign(summaryData, await fetchSummaryData())
}

async function loadRecentClues(): Promise<void> {
  clueLoading.value = true
  clueError.value = false
  try {
    const result = await fetchCurrentClues(1)
    clueTotal.value = result.total ?? 0
    recentClues.value = (result.list ?? []).slice(0, 6)
  } catch {
    clueError.value = true
  } finally {
    clueLoading.value = false
  }
}

async function loadCharts(): Promise<void> {
  const [funnelResult, sourceResult] = await Promise.all([
    fetchSaleFunnelData(),
    fetchSourcePieData(),
  ])
  funnelData.value = funnelResult ?? []
  sourceData.value = sourceResult ?? []
  renderFunnelChart()
  renderSourceChart()
}

function renderFunnelChart(): void {
  if (!funnelChartRef.value) {
    return
  }

  funnelChart ??= echarts.init(funnelChartRef.value)
  const option: EChartsOption = {
    color: ['#3370FF', '#6BA1FF', '#FF9500', '#34C759'],
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'funnel',
        left: 12,
        right: 12,
        top: 18,
        bottom: 10,
        sort: 'descending',
        gap: 2,
        label: {
          color: '#ffffff',
          fontWeight: 600,
        },
        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 1,
        },
        data: funnelData.value,
      },
    ],
  }
  funnelChart.setOption(option)
}

function renderSourceChart(): void {
  if (!sourceChartRef.value) {
    return
  }

  sourceChart ??= echarts.init(sourceChartRef.value)
  const option: EChartsOption = {
    color: sourceColors,
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '48%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: sourceData.value,
      },
    ],
  }
  sourceChart.setOption(option)
}

function resizeCharts(): void {
  funnelChart?.resize()
  sourceChart?.resize()
}

function getNameInitial(name?: string): string {
  const value = name?.trim()
  return value ? value.charAt(0) : '--'
}

function getClueTone(label?: string): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (!label) {
    return 'muted'
  }
  if (label.includes('成交') || label.includes('高') || label.includes('有意向')) {
    return 'info'
  }
  if (label.includes('跟进')) {
    return 'warning'
  }
  if (label.includes('流失') || label.includes('无效')) {
    return 'danger'
  }
  if (label.includes('待')) {
    return 'purple'
  }
  return 'success'
}

function goToClueList(): void {
  void router.push('/dashboard/clue')
}

function goToClueDetail(clue: Clue): void {
  if (!clue.id) {
    return
  }
  void router.push(`/dashboard/clue/detail/${clue.id}`)
}

onMounted(() => {
  void loadSummary()
  void loadRecentClues()
  void loadCharts()

  resizeObserver = new ResizeObserver(resizeCharts)
  if (funnelChartRef.value) {
    resizeObserver.observe(funnelChartRef.value)
  }
  if (sourceChartRef.value) {
    resizeObserver.observe(sourceChartRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  funnelChart?.dispose()
  sourceChart?.dispose()
  funnelChart = null
  sourceChart = null
})
</script>
