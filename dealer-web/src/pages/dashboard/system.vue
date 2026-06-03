<template>  <div class="system-container">    <!-- 系统监控大屏 -->
    <div class="dashboard-grid">
      <!-- 数据来源提示卡片 -->
      <el-card class="chart-card info-card" style="grid-column: 1 / -1;">
        <template #header>
          <div class="card-header">
            <i class="el-icon-info"></i>
            <span>系统监控状态</span>
          </div>
        </template>
        <div class="info-content">
          <div class="info-item">
            <span class="info-label">数据来源：</span>
            <span class="info-value" :class="getDataSourceClass()">{{ dataSource }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">刷新频率：</span>
            <span class="info-value">{{ refreshInterval / 1000 }}秒/次</span>
          </div>
          <div class="info-item">
            <span class="info-label">监控状态：</span>
            <span class="info-value" :class="isAutoRefresh ? 'status-active' : 'status-paused'">
              {{ isAutoRefresh ? '✅ 实时监控中' : '⏸️ 已暂停' }}
            </span>
          </div>
        </div>      </el-card>
    </div>

    <!-- 下方三个主要卡片 -->
    <div class="main-charts-grid">
      <!-- 内存使用情况 -->
      <el-card class="chart-card memory-card">
        <template #header>
          <div class="card-header">
            <i class="el-icon-cpu"></i>
            <span>内存使用情况</span>
          </div>
        </template>
        <div class="chart-container">
          <div ref="memoryChartRef" class="chart"></div>
          <div class="chart-info">
            <div class="usage-text">{{ systemInfo.memoryUsage }}%</div>
            <div class="usage-desc">内存使用率</div>
          </div>
        </div>
      </el-card>

      <!-- CPU核心分布 -->
      <el-card class="chart-card cpu-card">
        <template #header>
          <div class="card-header">
            <i class="el-icon-setting"></i>
            <span>CPU核心分布</span>
          </div>
        </template>
        <div class="chart-container">
          <div ref="cpuChartRef" class="chart"></div>
          <div class="chart-info">
            <div class="usage-text">{{ systemInfo.cpus }}</div>
            <div class="usage-desc">核心数</div>
          </div>
        </div>
      </el-card>

      <!-- 系统运行时间 -->
      <el-card class="chart-card uptime-card">
        <template #header>
          <div class="card-header">
            <i class="el-icon-timer"></i>
            <span>系统运行时间</span>
          </div>
        </template>
        <div class="chart-container">
          <div ref="uptimeChartRef" class="chart"></div>
          <div class="chart-info">
            <div class="usage-text">{{ formatUptimeHours(systemInfo.uptime) }}</div>
            <div class="usage-desc">小时</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 详细系统信息 -->
    <el-card class="system-info-card">
      <template #header>        <div class="card-header">
          <span>详细系统信息</span>
          <div class="header-actions">
            <div class="data-source-indicator">
              <span class="data-source-text">{{ dataSource }}</span>
            </div>
            <div class="refresh-status">
              <el-badge :is-dot="isAutoRefresh" type="success">
                <span class="refresh-indicator">
                  <i :class="isAutoRefresh ? 'el-icon-loading' : 'el-icon-remove'"></i>
                  {{ isAutoRefresh ? '实时监控中' : '监控已暂停' }}
                </span>
              </el-badge>
            </div>
            <el-button type="primary" size="small" @click="refreshSystemInfo">
              <i class="el-icon-refresh"></i>
              手动刷新
            </el-button>
            <el-button 
              :type="isAutoRefresh ? 'success' : 'info'" 
              size="small" 
              @click="toggleAutoRefresh"
            >
              <i :class="isAutoRefresh ? 'el-icon-video-play' : 'el-icon-video-pause'"></i>
              {{ isAutoRefresh ? '自动刷新中' : '已暂停刷新' }}
            </el-button>
          </div>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作系统">
          <el-tag type="info">{{ systemInfo.platform }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="系统版本">
          {{ systemInfo.release }}
        </el-descriptions-item>
        <el-descriptions-item label="CPU架构">
          {{ systemInfo.arch }}
        </el-descriptions-item>
        <el-descriptions-item label="主机名">
          {{ systemInfo.hostname }}
        </el-descriptions-item>
        <el-descriptions-item label="CPU核心数">
          {{ systemInfo.cpus }} 核
        </el-descriptions-item>
        <el-descriptions-item label="总内存">
          {{ systemInfo.totalMemory }}
        </el-descriptions-item>
        <el-descriptions-item label="可用内存">
          {{ systemInfo.freeMemory }}
        </el-descriptions-item>
        <el-descriptions-item label="内存使用率">
          <el-progress 
            :percentage="systemInfo.memoryUsage" 
            :color="getMemoryColor(systemInfo.memoryUsage)"
          />
        </el-descriptions-item>
        <el-descriptions-item label="Node.js版本">
          {{ systemInfo.nodeVersion }}
        </el-descriptions-item>
        <el-descriptions-item label="运行时间">
          {{ systemInfo.uptime }}
        </el-descriptions-item>
        <el-descriptions-item label="用户目录" :span="2">
          {{ systemInfo.homedir }}
        </el-descriptions-item>
        <el-descriptions-item label="当前工作目录" :span="2">
          {{ systemInfo.cwd }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 系统管理信息 -->
    <el-card class="management-info-card">
      <template #header>
        <div class="card-header">
          <span>系统管理信息</span>
        </div>
      </template>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增系统信息</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedIds.length">批量删除</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
        class="data-table"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column 
          type="index" 
          label="序号" 
          width="80"
          :index="(index) => index + 1"
        />
        <el-table-column prop="systemCode" label="系统代码" show-overflow-tooltip />
        <el-table-column prop="name" label="系统名称" show-overflow-tooltip />
        <el-table-column prop="title" label="系统标题" show-overflow-tooltip />
        <el-table-column prop="description" label="系统描述" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" show-overflow-tooltip />
        <el-table-column prop="isopen" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-switch
              v-model="scope.row.isopen"
              :active-value="'true'"
              :inactive-value="'false'"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <div class="operation-buttons">
              <el-button type="success" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑系统信息' : '新增系统信息'"
      width="700px"
    >
      <el-form 
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统代码" prop="systemCode">
              <el-input v-model="form.systemCode" placeholder="请输入系统代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入系统名称" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入系统标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统网址" prop="site">
              <el-input v-model="form.site" placeholder="请输入系统网址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="系统描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入系统描述" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统Logo" prop="logo">
              <el-input v-model="form.logo" placeholder="请输入Logo地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="快捷图标" prop="shortcuticon">
              <el-input v-model="form.shortcuticon" placeholder="请输入快捷图标地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信" prop="weixin">
              <el-input v-model="form.weixin" placeholder="请输入微信号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="请输入系统版本" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>

        <el-form-item label="关闭提示" prop="closeMsg">
          <el-input v-model="form.closeMsg" type="textarea" placeholder="请输入系统关闭时的提示信息" />
        </el-form-item>

        <el-form-item label="系统状态" prop="isopen">
          <el-switch
            v-model="form.isopen"
            :active-value="'true'"
            :inactive-value="'false'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { 
  getSystemList, createSystem, updateSystem, deleteSystem, batchDeleteSystems, toggleSystemStatus,
  getAllMonitorData, getMemoryInfo, getCpuInfo, getSystemMonitorInfo 
} from '@/modules/system/api/system-api'
import { messageConfirm } from '@/shared/utils/legacy-util'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const selectedIds = ref([])
const formRef = ref()

// 图表引用
const memoryChartRef = ref()
const cpuChartRef = ref()
const uptimeChartRef = ref()

// 图表实例
let memoryChart = null
let cpuChart = null
let uptimeChart = null

// 定时器
let refreshTimer = null
const isAutoRefresh = ref(true)
const refreshInterval = 1000 // 1000ms = 1秒
const dataSource = ref('检测中...') // 数据来源状态
const connectionStatus = ref('connecting') // 连接状态: connecting, connected, error

// 本机系统信息
const systemInfo = ref({
  platform: 'Unknown',
  release: 'Unknown',
  arch: 'Unknown',
  hostname: 'Unknown',
  cpus: 0,
  totalMemory: 'Unknown',
  freeMemory: 'Unknown',
  memoryUsage: 0,
  nodeVersion: 'Unknown',
  uptime: 'Unknown',
  homedir: 'Unknown',
  cwd: 'Unknown'
})

// 表单数据
const form = reactive({
  systemCode: '',
  name: '',
  site: '',
  logo: '',
  title: '',
  description: '',
  keywords: '',
  shortcuticon: '',
  tel: '',
  weixin: '',
  email: '',
  address: '',
  version: '',
  closeMsg: '',
  isopen: 'true'
})

// 表单验证规则
const rules = {
  systemCode: [{ required: true, message: '请输入系统代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  title: [{ required: true, message: '请输入系统标题', trigger: 'blur' }],
  site: [{ required: true, message: '请输入系统网址', trigger: 'blur' }],
  description: [{ required: true, message: '请输入系统描述', trigger: 'blur' }]
}

// 获取本机系统信息
const getLocalSystemInfo = async () => {
  try {
    connectionStatus.value = 'connecting'
    // 从后端API获取真实的系统监控数据
    const response = await getAllMonitorData()
    if (true) {
      const data = response
      
      // 映射后端数据到前端显示格式
      systemInfo.value = {
        platform: data.systemInfo.platform,
        release: data.systemInfo.osName,
        arch: data.systemInfo.arch,
        hostname: data.systemInfo.hostname,
        cpus: data.cpuInfo.logicalProcessors,
        totalMemory: data.memoryInfo.totalMemoryFormatted,
        freeMemory: data.memoryInfo.availableMemoryFormatted,
        memoryUsage: Math.round(data.memoryInfo.usagePercentage),
        nodeVersion: data.jvmInfo.javaVersion,
        uptime: data.systemInfo.uptimeFormatted,
        homedir: 'Java运行环境',
        cwd: window.location.href,
        // 保存原始数据用于图表更新
        _rawData: data
      }
      
      connectionStatus.value = 'connected'
      dataSource.value = '🟢 真实系统数据'
    } else {
      throw new Error('Failed to fetch system data')
    }
  } catch (error) {
    console.error('获取系统信息失败:', error)
    connectionStatus.value = 'error'
    // 如果后端调用失败，回退到浏览器API
    fallbackToWebAPI()
    dataSource.value = '🟡 浏览器模拟数据'
  }
}

// 带重试机制的数据获取
const getSystemInfoWithRetry = async (retries = 3) => {
  for (let i = 0; i < retries; i++) {
    try {
      await getLocalSystemInfo()
      if (connectionStatus.value === 'connected') {
        return // 成功获取数据，退出重试
      }
    } catch (error) {
      console.warn(`获取系统信息失败，第${i + 1}次重试:`, error)
      if (i === retries - 1) {
        // 最后一次重试失败，使用备用方案
        fallbackToWebAPI()
        dataSource.value = '🔴 后端连接失败，使用模拟数据'
      } else {
        // 等待一段时间后重试
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
    }
  }
}

// 回退到Web API获取部分信息
const fallbackToWebAPI = () => {
  const nav = window.navigator
  const perf = window.performance
  
  systemInfo.value = {
    platform: nav.platform || 'Unknown',
    release: getUserAgent(),
    arch: nav.userAgent.includes('x64') || nav.userAgent.includes('Win64') ? 'x64' : 
          nav.userAgent.includes('ARM') ? 'ARM' : 'x86',
    hostname: window.location.hostname || 'localhost',
    cpus: nav.hardwareConcurrency || 4,
    totalMemory: nav.deviceMemory ? `${nav.deviceMemory}GB` : '未知',
    freeMemory: '浏览器限制无法获取',
    memoryUsage: getWebMemoryUsage(),
    nodeVersion: getBrowserInfo(),
    uptime: formatUptime(perf.now() / 1000),
    homedir: '浏览器安全限制',
    cwd: window.location.href
  }
}

// 获取用户代理信息
const getUserAgent = () => {
  const ua = navigator.userAgent
  if (ua.includes('Windows NT 10.0')) return 'Windows 10/11'
  if (ua.includes('Windows NT 6.3')) return 'Windows 8.1'
  if (ua.includes('Windows NT 6.1')) return 'Windows 7'
  if (ua.includes('Mac OS X')) {
    const match = ua.match(/Mac OS X ([0-9_]+)/)
    if (match) {
      return `macOS ${match[1].replace(/_/g, '.')}`
    }
    return 'macOS'
  }
  if (ua.includes('Linux')) return 'Linux'
  return 'Unknown OS'
}

// 获取浏览器信息
const getBrowserInfo = () => {
  const ua = navigator.userAgent
  if (ua.includes('Chrome')) {
    const match = ua.match(/Chrome\/([0-9.]+)/)
    return match ? `Chrome ${match[1]}` : 'Chrome'
  }
  if (ua.includes('Firefox')) {
    const match = ua.match(/Firefox\/([0-9.]+)/)
    return match ? `Firefox ${match[1]}` : 'Firefox'
  }
  if (ua.includes('Safari') && !ua.includes('Chrome')) {
    const match = ua.match(/Version\/([0-9.]+)/)
    return match ? `Safari ${match[1]}` : 'Safari'
  }
  if (ua.includes('Edge')) {
    const match = ua.match(/Edge\/([0-9.]+)/)
    return match ? `Edge ${match[1]}` : 'Edge'
  }
  return 'Unknown Browser'
}

// 获取内存使用情况（基于浏览器性能API）
const getWebMemoryUsage = () => {
  if ('memory' in performance) {
    const memory = performance.memory
    const used = memory.usedJSHeapSize
    const limit = memory.jsHeapSizeLimit
    
    // 计算已使用内存占限制的百分比
    const percentage = Math.round((used / limit) * 100)
    
    // 更新内存信息显示
    systemInfo.value.totalMemory = `${(limit / (1024 * 1024 * 1024)).toFixed(2)}GB (JS堆限制)`
    systemInfo.value.freeMemory = `${((limit - used) / (1024 * 1024 * 1024)).toFixed(2)}GB`
    
    return Math.min(percentage, 100)
  }
  
  // 如果不支持memory API，返回一个基于时间的动态值
  const baseUsage = 35
  const timeVariation = Math.sin(Date.now() / 10000) * 10
  return Math.max(20, Math.min(70, baseUsage + timeVariation))
}

// 启动自动刷新
const startAutoRefresh = () => {
  if (refreshTimer) return
  
  refreshTimer = setInterval(async () => {
    if (isAutoRefresh.value) {
      // 如果是连接状态，直接获取数据；如果是错误状态，尝试重新连接
      if (connectionStatus.value === 'connected') {
        await getLocalSystemInfo()
      } else {
        await getSystemInfoWithRetry(1) // 单次重试
      }
      updateCharts()
    }
  }, refreshInterval)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 切换自动刷新状态
const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    startAutoRefresh()
    ElMessage.success('已开启自动刷新')
  } else {
    stopAutoRefresh()
    ElMessage.info('已关闭自动刷新')
  }
}

// 刷新系统信息
const refreshSystemInfo = async () => {
  await getSystemInfoWithRetry()
  updateCharts()
  ElMessage.success('系统信息已手动刷新')
}

// 初始化图表
const initCharts = async () => {
  await nextTick()
  
  // 初始化内存使用图表
  if (memoryChartRef.value) {
    memoryChart = echarts.init(memoryChartRef.value)
    updateMemoryChart()
  }

  // 初始化CPU图表
  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value)
    updateCpuChart()
  }

  // 初始化运行时间图表
  if (uptimeChartRef.value) {
    uptimeChart = echarts.init(uptimeChartRef.value)
    updateUptimeChart()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', resizeCharts)
}

// 更新所有图表
const updateCharts = () => {
  updateMemoryChart()
  updateCpuChart()
  updateUptimeChart()
}

// 更新内存使用图表
const updateMemoryChart = async () => {
  if (!memoryChart) return
  
  let usage = systemInfo.value.memoryUsage
  
  // 尝试从后端获取最新的内存数据
  try {
    if (connectionStatus.value === 'connected') {
      const response = await getMemoryInfo()
      if (true) {
        usage = Math.round(response.usagePercentage)
        systemInfo.value.memoryUsage = usage
        systemInfo.value.totalMemory = response.totalMemoryFormatted
        systemInfo.value.freeMemory = response.availableMemoryFormatted
      }
    } else {
      // 使用Web API作为备用
      usage = getWebMemoryUsage()
      systemInfo.value.memoryUsage = usage
    }
  } catch (error) {
    // 如果单独的内存API调用失败，使用Web API
    usage = getWebMemoryUsage()
    systemInfo.value.memoryUsage = usage
  }
  
  const option = {
    series: [{
      type: 'pie',
      radius: ['60%', '80%'],
      center: ['50%', '50%'],
      silent: true,
      data: [
        {
          value: usage,
          name: '已使用',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: getMemoryGradientColor(usage).start },
              { offset: 1, color: getMemoryGradientColor(usage).end }
            ])
          }
        },
        {
          value: 100 - usage,
          name: '可用',
          itemStyle: {
            color: '#f0f2f5',
            opacity: 0.3
          }
        }
      ],
      label: { show: false },
      labelLine: { show: false }
    }],
    animation: true,
    animationDuration: 800
  }
  memoryChart.setOption(option)
}

// 更新CPU图表
const updateCpuChart = async () => {
  if (!cpuChart) return
  
  let cores = systemInfo.value.cpus
  let data = []
  
  // 尝试从后端获取最新的CPU数据
  try {
    if (connectionStatus.value === 'connected') {
      const response = await getCpuInfo()
      if (true) {
        const cpuData = response
        cores = cpuData.logicalProcessors
        
        // 使用真实的CPU核心负载数据
        data = cpuData.cores.map((core, index) => ({
          value: Math.round(core.loadPercentage),
          name: `核心${index + 1}`
        }))
      }
    }
  } catch (error) {
    // CPU API调用失败，不处理，让下面的fallback处理
  }
  
  // 如果没有获取到真实数据，生成模拟数据
  if (data.length === 0) {
    data = Array.from({ length: cores }, (_, i) => {
      const timeBase = Date.now() / 1000
      const coreOffset = i * 1000
      const baseUsage = 25 + Math.sin((timeBase + coreOffset) / 10) * 15
      const noise = Math.sin((timeBase + coreOffset) / 3) * 5
      const currentUsage = Math.max(5, Math.min(75, baseUsage + noise))
      
      return {
        value: Math.floor(currentUsage),
        name: `核心${i + 1}`
      }
    })
  }
  
  const option = {
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      data: data,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }],
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
    animation: true,
    animationDuration: 800
  }
  cpuChart.setOption(option)
}

// 更新运行时间图表
const updateUptimeChart = async () => {
  if (!uptimeChart) return
  
  let hours = formatUptimeHours(systemInfo.value.uptime)
  
  // 尝试从后端获取最新的系统信息
  try {
    if (connectionStatus.value === 'connected') {
      const response = await getSystemMonitorInfo()
      if (true) {
        const sysData = response
        systemInfo.value.uptime = sysData.uptimeFormatted
        hours = Math.floor(sysData.uptime / 3600) // 将秒转换为小时
      }
    }
  } catch (error) {
    // 如果系统信息API调用失败，使用当前时间
    const currentTime = performance.now()
    systemInfo.value.uptime = formatUptime(currentTime / 1000)
    hours = formatUptimeHours(systemInfo.value.uptime)
  }
  
  const maxHours = 24 // 一天24小时
  
  const option = {
    series: [{
      type: 'gauge',
      radius: '70%',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: maxHours,
      progress: {
        show: true,
        width: 18,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#ffc107' },
            { offset: 1, color: '#ff9800' }
          ])
        }
      },
      axisLine: {
        lineStyle: {
          width: 18,
          color: [[1, '#e6e6e6']]
        }
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      title: { show: false },
      detail: { show: false },
      data: [{ value: Math.min(hours, maxHours) }]
    }],
    animation: true,
    animationDuration: 800
  }
  uptimeChart.setOption(option)
}

// 图表大小调整
const resizeCharts = () => {
  memoryChart?.resize()
  cpuChart?.resize()
  uptimeChart?.resize()
}

// 获取内存渐变色
const getMemoryGradientColor = (usage) => {
  if (usage < 50) {
    return { start: '#67c23a', end: '#85ce61' }
  } else if (usage < 80) {
    return { start: '#e6a23c', end: '#ebb563' }
  } else {
    return { start: '#f56c6c', end: '#f89898' }
  }
}

// 格式化运行时间为小时
const formatUptimeHours = (uptimeStr) => {
  if (typeof uptimeStr === 'string') {
    const match = uptimeStr.match(/(\d+)小时/)
    if (match) return parseInt(match[1])
    const dayMatch = uptimeStr.match(/(\d+)天/)
    if (dayMatch) return parseInt(dayMatch[1]) * 24
  }
  return Math.floor(Math.random() * 12) + 1 // 模拟1-12小时
}

// 格式化运行时间
const formatUptime = (ms) => {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) {
    return `${days}天 ${hours % 24}小时 ${minutes % 60}分钟`
  } else if (hours > 0) {
    return `${hours}小时 ${minutes % 60}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟`
  } else {
    return `${seconds}秒`
  }
}

// 获取内存使用率颜色
const getMemoryColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getSystemList()
    if (true) {
      tableData.value = res
    }
  } catch (error) {
    console.error('获取系统列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.keys(form).forEach(key => form[key] = '')
  form.isopen = 'true'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该系统信息吗？')
    const res = await deleteSystem(row.id)
    if (true) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return
  try {
    await messageConfirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`)
    const res = await batchDeleteSystems(selectedIds.value)
    if (true) {
      ElMessage.success('批量删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    const res = isEdit.value
      ? await updateSystem(form.id, form)
      : await createSystem(form)

    if (true) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交系统信息失败:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const res = await toggleSystemStatus(row.id, row.isopen)
    if (true) {
      ElMessage.success('状态更新成功')
    } else {
      row.isopen = row.isopen === 'true' ? 'false' : 'true' // 恢复原状态
      ElMessage.error('状态更新失败')
    }
  } catch (error) {
    row.isopen = row.isopen === 'true' ? 'false' : 'true' // 恢复原状态
    ElMessage.error('状态更新失败')
  }
}

// 获取数据来源的CSS类
const getDataSourceClass = () => {
  if (dataSource.value.includes('真实系统数据')) {
    return 'status-real'
  } else if (dataSource.value.includes('浏览器模拟数据')) {
    return 'status-browser'
  } else if (dataSource.value.includes('连接失败')) {
    return 'status-error'
  }
  return ''
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

onMounted(async () => {
  await getSystemInfoWithRetry()
  loadData()
  await initCharts()
  startAutoRefresh() // 启动自动刷新
})

// 组件卸载时清理
onUnmounted(() => {
  stopAutoRefresh() // 停止自动刷新
  window.removeEventListener('resize', resizeCharts)
  memoryChart?.dispose()
  cpuChart?.dispose()
  uptimeChart?.dispose()
})
</script>

<style scoped>
.system-container {
  padding: 20px;
}

/* 仪表盘网格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

/* 监控状态卡片独占一行 */
.info-card {
  grid-column: 1 / -1;
}

/* 下方三个主要卡片的容器 */
.main-charts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

/* 图表卡片样式 */
.chart-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 15px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.chart-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.chart-card.memory-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.chart-card.cpu-card {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.chart-card.uptime-card {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}



.chart-card.info-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #495057;
  grid-column: 1 / -1;
}

.chart-card.info-card .card-header {
  color: #495057;
}

.chart-card.info-card .info-content {
  color: #495057;
}

.info-content {
  display: flex;
  justify-content: space-around;
  align-items: center;
  color: #495057;
  flex-wrap: wrap;
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.info-label {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 5px;
}

.info-value {
  font-size: 14px;
  font-weight: bold;
}

.info-value.status-real {
  color: #4CAF50;
}

.info-value.status-browser {
  color: #FF9800;
}

.info-value.status-error {
  color: #F44336;
}

.info-value.status-active {
  color: #4CAF50;
}

.info-value.status-paused {
  color: #9E9E9E;
}

:deep(.chart-card .el-card__header) {
  background: transparent;
  border: none;
  padding: 20px 20px 10px;
}

:deep(.chart-card .el-card__body) {
  padding: 10px 20px 20px;
}

.chart-card .card-header {
  color: white;
  font-weight: bold;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-card .card-header i {
  font-size: 18px;
}

/* 图表容器 */
.chart-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 120px;
}

.chart {
  width: 120px;
  height: 120px;
}

.chart-info {
  color: white;
  text-align: center;
  flex: 1;
  padding-left: 20px;
}

.usage-text {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.usage-desc {
  font-size: 14px;
  opacity: 0.9;
}



.system-info-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.management-info-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.data-source-indicator {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: bold;
  white-space: nowrap;
}

.data-source-text {
  display: flex;
  align-items: center;
  gap: 4px;
}

.refresh-status {
  font-size: 12px;
  color: #666;
}

.refresh-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.refresh-indicator i.el-icon-loading {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.action-bar {
  margin-bottom: 20px;
}

.data-table {
  margin-top: 12px;
}

.operation-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 52px;
}

:deep(.el-table) {
  width: 100% !important;
}

:deep(.el-descriptions) {
  margin-bottom: 20px;
}

:deep(.el-descriptions__label) {
  font-weight: bold;
  color: #606266;
}

:deep(.el-progress) {
  width: 200px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-textarea__inner) {
  min-height: 80px !important;
}

:deep(.el-card__header) {
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-descriptions__body .el-descriptions__table) {
  border-collapse: collapse;
}

:deep(.el-descriptions__cell) {
  border: 1px solid #ebeef5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  
  .main-charts-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-container {
    flex-direction: column;
    height: auto;
    text-align: center;
  }
  
  .chart-info {
    padding-left: 0;
    padding-top: 10px;
  }
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chart-card {
  animation: fadeInUp 0.6s ease forwards;
}

.chart-card:nth-child(1) { animation-delay: 0.1s; }
.chart-card:nth-child(2) { animation-delay: 0.2s; }
.chart-card:nth-child(3) { animation-delay: 0.3s; }
.chart-card:nth-child(4) { animation-delay: 0.4s; }
</style>