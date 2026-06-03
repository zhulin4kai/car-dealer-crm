<template>
  <!--概览统计-->
  <el-row>
    <el-col :span="6">
      <el-statistic :value="summaryData.effectiveActivityCount">
        <template #title>
          <div style="display: inline-flex; align-items: center">
            市场活动
          </div>
        </template>
        <template #suffix>/{{summaryData.totalActivityCount}}</template>
      </el-statistic>
    </el-col>

    <el-col :span="6">
      <el-statistic title="线索总数" :value="summaryData.totalClueCount" />
    </el-col>

    <el-col :span="6">
      <el-statistic title="客户总数" :value="summaryData.totalCustomerCount" />
    </el-col>

    <el-col :span="6">
      <el-statistic :value="summaryData.successTranAmount">
        <template #title>
          <div style="display: inline-flex; align-items: center">
            交易总额
          </div>
        </template>
        <template #suffix>/{{summaryData.totalTranAmount}}</template>
      </el-statistic>
    </el-col>
  </el-row>

  <!-- 销售漏斗图，为 ECharts 准备一个定义了宽高的 DOM -->
  <div id="saleFunnelChart" style="width: 48%; height:350px; margin:10px; float: left;"> 图渲染在此处 </div>

  <!-- 线索来源饼图，为 ECharts 准备一个定义了宽高的 DOM -->
  <div id="sourcePieChart" style="width: 48%; height:350px; margin:10px; float: left;"> 图渲染在此处 </div>

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

<style scoped>
.el-row {
  text-align: center;
}
</style>