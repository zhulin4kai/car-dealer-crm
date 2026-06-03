<template>
  <!--概览统计-->
  <div class="grid grid-cols-4 gap-4 text-center">
    <div class="rounded-lg border p-6">
      <div class="text-sm text-muted-foreground">市场活动</div>
      <div class="mt-2 text-2xl font-bold">
        {{ summaryData.effectiveActivityCount }}
        <span class="text-base font-normal text-muted-foreground">/{{ summaryData.totalActivityCount }}</span>
      </div>
    </div>
    <div class="rounded-lg border p-6">
      <div class="text-sm text-muted-foreground">线索总数</div>
      <div class="mt-2 text-2xl font-bold">{{ summaryData.totalClueCount }}</div>
    </div>
    <div class="rounded-lg border p-6">
      <div class="text-sm text-muted-foreground">客户总数</div>
      <div class="mt-2 text-2xl font-bold">{{ summaryData.totalCustomerCount }}</div>
    </div>
    <div class="rounded-lg border p-6">
      <div class="text-sm text-muted-foreground">交易总额</div>
      <div class="mt-2 text-2xl font-bold">
        {{ summaryData.successTranAmount }}
        <span class="text-base font-normal text-muted-foreground">/{{ summaryData.totalTranAmount }}</span>
      </div>
    </div>
  </div>

  <!-- 销售漏斗图,为 ECharts 准备一个定义了宽高的 DOM -->
  <div id="saleFunnelChart" class="float-left m-2.5 h-[350px] w-[48%]"> 图渲染在此处 </div>

  <!-- 线索来源饼图,为 ECharts 准备一个定义了宽高的 DOM -->
  <div id="sourcePieChart" class="float-left m-2.5 h-[350px] w-[48%]"> 图渲染在此处 </div>

</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { onMounted, reactive } from 'vue'

import {
  fetchSaleFunnelData,
  fetchSourcePieData,
  fetchSummaryData,
} from '@/modules/statistic/api/statistic-api'
import type { SummaryData } from '@/modules/statistic/model/statistic.types'

defineOptions({
  name: 'StatisticView',
})

const summaryData = reactive<SummaryData>({})

async function loadSummary(): Promise<void> {
  Object.assign(summaryData, await fetchSummaryData())
}

async function loadSaleFunnelChart(): Promise<void> {
  const saleFunnelData = await fetchSaleFunnelData()
  const chartDom = document.getElementById('saleFunnelChart')
  if (!chartDom) {
    return
  }

  const myChart = echarts.init(chartDom)
  const option: EChartsOption = {
    title: {
      text: '销售漏斗图',
      left: 'center',
      top: 'bottom',
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c}',
    },
    toolbox: {
      feature: {
        dataView: { readOnly: false },
        restore: {},
        saveAsImage: {},
      },
    },
    legend: {
      data: ['线索', '客户', '交易', '成交'],
    },
    series: [
      {
        name: '销售漏斗数据统计',
        type: 'funnel',
        left: '10%',
        top: 60,
        bottom: 60,
        width: '80%',
        min: 0,
        max: 100,
        minSize: '0%',
        maxSize: '100%',
        sort: 'descending',
        gap: 2,
        label: {
          show: true,
          position: 'inside',
        },
        labelLine: {
          length: 10,
          lineStyle: {
            width: 1,
            type: 'solid',
          },
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1,
        },
        emphasis: {
          label: {
            fontSize: 20,
          },
        },
        data: saleFunnelData,
      },
    ],
  }
  myChart.setOption(option)
}

async function loadSourcePieChart(): Promise<void> {
  const sourcePieData = await fetchSourcePieData()
  const chartDom = document.getElementById('sourcePieChart')
  if (!chartDom) {
    return
  }

  const myChart = echarts.init(chartDom)
  const option: EChartsOption = {
    title: {
      text: '线索来源统计',
      left: 'center',
      top: 'bottom',
    },
    tooltip: {
      trigger: 'item',
    },
    legend: {
      orient: 'horizontal',
      left: 'center',
    },
    series: [
      {
        name: '线索来源统计',
        type: 'pie',
        radius: '60%',
        data: sourcePieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  }
  myChart.setOption(option)
}

onMounted(() => {
  void loadSummary()
  void loadSaleFunnelChart()
  void loadSourcePieChart()
})
</script>
