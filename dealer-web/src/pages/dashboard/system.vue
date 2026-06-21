<template>
  <div class="p-5">
    <!-- System Monitor Dashboard -->
    <div class="grid grid-cols-1 gap-5 mb-8">
      <!-- Data Source Info Card -->
      <Card class="bg-[linear-gradient(135deg,#f8f9fa,#e9ecef)] border-0 rounded-2xl shadow-lg col-span-full">
        <CardHeader class="!bg-transparent border-0 !pb-2">
          <div class="flex items-center gap-2 text-[#495057] font-bold text-base">
            <Info class="w-5 h-5" />
            <span>系统监控状态</span>
          </div>
        </CardHeader>
        <CardContent class="!pt-2">
          <div class="flex justify-around items-center text-[#495057] flex-wrap gap-5">
            <div class="flex flex-col items-center text-center">
              <span class="text-xs opacity-90 mb-1">数据来源：</span>
              <span class="text-sm font-bold" :class="getDataSourceClass()">{{ dataSource }}</span>
            </div>
            <div class="flex flex-col items-center text-center">
              <span class="text-xs opacity-90 mb-1">刷新频率：</span>
              <span class="text-sm font-bold">{{ refreshInterval / 1000 }}秒/次</span>
            </div>
            <div class="flex flex-col items-center text-center">
              <span class="text-xs opacity-90 mb-1">监控状态：</span>
              <span class="text-sm font-bold" :class="isAutoRefresh ? 'text-[#4CAF50]' : 'text-[#9E9E9E]'">
                {{ isAutoRefresh ? '✅ 实时监控中' : '⏸️ 已暂停' }}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- Three Main Chart Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
      <!-- Memory Usage -->
      <Card class="chart-card !bg-[linear-gradient(135deg,#667eea,#764ba2)] border-0 animate-[fadeInUp_0.6s_ease_forwards] [animation-delay:0.1s]">
        <CardHeader class="!bg-transparent border-0 !pb-2">
          <div class="flex items-center gap-2 text-white font-bold text-base">
            <Cpu class="w-5 h-5" />
            <span>内存使用情况</span>
          </div>
        </CardHeader>
        <CardContent class="!pt-2">
          <div class="relative flex items-center justify-between h-[120px]">
            <div ref="memoryChartRef" class="w-[120px] h-[120px]"></div>
            <div class="text-white text-center flex-1 pl-5">
              <div class="text-[28px] font-bold mb-1">{{ systemInfo.memoryUsage }}%</div>
              <div class="text-sm opacity-90">内存使用率</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- CPU Cores -->
      <Card class="chart-card !bg-[linear-gradient(135deg,#f093fb,#f5576c)] border-0 animate-[fadeInUp_0.6s_ease_forwards] [animation-delay:0.2s]">
        <CardHeader class="!bg-transparent border-0 !pb-2">
          <div class="flex items-center gap-2 text-white font-bold text-base">
            <Settings class="w-5 h-5" />
            <span>CPU核心分布</span>
          </div>
        </CardHeader>
        <CardContent class="!pt-2">
          <div class="relative flex items-center justify-between h-[120px]">
            <div ref="cpuChartRef" class="w-[120px] h-[120px]"></div>
            <div class="text-white text-center flex-1 pl-5">
              <div class="text-[28px] font-bold mb-1">{{ systemInfo.cpus }}</div>
              <div class="text-sm opacity-90">核心数</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- System Uptime -->
      <Card class="chart-card !bg-[linear-gradient(135deg,#4facfe,#00f2fe)] border-0 animate-[fadeInUp_0.6s_ease_forwards] [animation-delay:0.3s]">
        <CardHeader class="!bg-transparent border-0 !pb-2">
          <div class="flex items-center gap-2 text-white font-bold text-base">
            <Timer class="w-5 h-5" />
            <span>系统运行时间</span>
          </div>
        </CardHeader>
        <CardContent class="!pt-2">
          <div class="relative flex items-center justify-between h-[120px]">
            <div ref="uptimeChartRef" class="w-[120px] h-[120px]"></div>
            <div class="text-white text-center flex-1 pl-5">
              <div class="text-[28px] font-bold mb-1">{{ formatUptimeHours(systemInfo.uptime) }}</div>
              <div class="text-sm opacity-90">小时</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- Detailed System Info -->
    <Card class="mb-5 rounded-xl shadow-md">
      <CardHeader>
        <div class="flex justify-between items-center font-bold">
          <span>详细系统信息</span>
          <div class="flex gap-2.5 items-center">
            <div class="bg-[linear-gradient(135deg,#667eea,#764ba2)] text-white px-2 py-1 rounded-xl text-[11px] font-bold whitespace-nowrap">
              {{ dataSource }}
            </div>
            <div class="text-xs text-muted-foreground">
              <span class="relative flex items-center gap-1">
                <span
                  v-if="isAutoRefresh"
                  class="absolute -top-1 -right-1 w-2 h-2 rounded-full bg-green-500"
                ></span>
                <RefreshCw class="w-3 h-3" :class="{ 'animate-spin': isAutoRefresh }" />
                {{ isAutoRefresh ? '实时监控中' : '监控已暂停' }}
              </span>
            </div>
            <Button size="sm" @click="refreshSystemInfo">
              <RefreshCw class="w-4 h-4 mr-1" />
              手动刷新
            </Button>
            <Button
              :variant="isAutoRefresh ? 'secondary' : 'outline'"
              size="sm"
              @click="toggleAutoRefresh"
            >
              {{ isAutoRefresh ? '自动刷新中' : '已暂停刷新' }}
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div class="border rounded-md">
          <div class="grid grid-cols-[120px_1fr_120px_1fr]">
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">操作系统</div>
            <div class="px-4 py-2 text-sm border-b border-r">
              <Badge variant="outline">{{ systemInfo.platform }}</Badge>
            </div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">系统版本</div>
            <div class="px-4 py-2 text-sm border-b">{{ systemInfo.release }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">CPU架构</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ systemInfo.arch }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">主机名</div>
            <div class="px-4 py-2 text-sm border-b">{{ systemInfo.hostname }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">CPU核心数</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ systemInfo.cpus }} 核</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">总内存</div>
            <div class="px-4 py-2 text-sm border-b">{{ systemInfo.totalMemory }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">可用内存</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ systemInfo.freeMemory }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">内存使用率</div>
            <div class="px-4 py-2 text-sm border-b">
              <Progress
                :model-value="systemInfo.memoryUsage"
                class="w-[200px]"
                :class="getProgressColorClass(systemInfo.memoryUsage)"
              />
            </div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">Node.js版本</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ systemInfo.nodeVersion }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">运行时间</div>
            <div class="px-4 py-2 text-sm border-b">{{ systemInfo.uptime }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">用户目录</div>
            <div class="px-4 py-2 text-sm border-b col-span-3">{{ systemInfo.homedir }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-r">当前工作目录</div>
            <div class="px-4 py-2 text-sm col-span-3">{{ systemInfo.cwd }}</div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- System Management -->
    <Card class="mb-5 rounded-xl shadow-md">
      <CardHeader>
        <CardTitle>系统管理信息</CardTitle>
      </CardHeader>
      <CardContent>
        <!-- Action Bar -->
        <div class="mb-5">
          <Button v-has-permission="PERMISSIONS.system.add" @click="handleAdd">新增系统信息</Button>
          <Button v-has-permission="PERMISSIONS.system.delete" variant="destructive" class="ml-2" @click="handleBatchDelete" :disabled="!selectedIds.length">批量删除</Button>
        </div>

        <!-- Data Table -->
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[55px]">
                <Checkbox
                  :checked="isAllSelected"
                  @update:checked="toggleSelectAll"
                />
              </TableHead>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead>系统代码</TableHead>
              <TableHead>系统名称</TableHead>
              <TableHead>系统标题</TableHead>
              <TableHead>系统描述</TableHead>
              <TableHead>版本</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <template v-if="loading">
              <TableRow v-for="i in 3" :key="'skel-' + i">
                <TableCell v-for="j in 9" :key="'skel-c-' + j"><Skeleton class="h-4 w-full" /></TableCell>
              </TableRow>
            </template>
            <template v-else>
              <TableRow v-for="(row, idx) in tableData" :key="row.id || idx">
                <TableCell>
                  <Checkbox
                    :checked="selectedIds.includes(row.id)"
                    @update:checked="(v) => toggleRowSelection(row.id, v)"
                  />
                </TableCell>
                <TableCell>{{ idx + 1 }}</TableCell>
                <TableCell class="truncate max-w-[150px]">{{ row.systemCode }}</TableCell>
                <TableCell class="truncate max-w-[150px]">{{ row.name }}</TableCell>
                <TableCell class="truncate max-w-[150px]">{{ row.title }}</TableCell>
                <TableCell class="truncate max-w-[200px]">{{ row.description }}</TableCell>
                <TableCell>{{ row.version }}</TableCell>
                <TableCell>
                  <Switch
                    :checked="row.isopen === 'true'"
                    @update:checked="(v) => { row.isopen = v ? 'true' : 'false'; handleStatusChange(row) }"
                  />
                </TableCell>
                <TableCell>
                  <div class="flex gap-1.5 justify-center flex-wrap">
                    <Button v-has-permission="PERMISSIONS.system.edit" variant="secondary" size="sm" @click="handleEdit(row)">编辑</Button>
                    <Button v-has-permission="PERMISSIONS.system.delete" variant="destructive" size="sm" @click="handleDelete(row)">删除</Button>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- Add/Edit Dialog -->
    <Dialog v-model:open="dialogVisible">
      <DialogContent class="max-w-[700px]">
        <DialogHeader>
          <DialogTitle>{{ isEdit ? '编辑系统信息' : '新增系统信息' }}</DialogTitle>
        </DialogHeader>

        <form class="space-y-4 mt-4" @submit.prevent="onSubmit">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>系统代码</Label>
              <Input v-model="values.systemCode" placeholder="请输入系统代码" />
              <p v-if="errors.systemCode" class="text-sm text-destructive">{{ errors.systemCode }}</p>
            </div>
            <div class="space-y-2">
              <Label>系统名称</Label>
              <Input v-model="values.name" placeholder="请输入系统名称" />
              <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>系统标题</Label>
              <Input v-model="values.title" placeholder="请输入系统标题" />
              <p v-if="errors.title" class="text-sm text-destructive">{{ errors.title }}</p>
            </div>
            <div class="space-y-2">
              <Label>系统网址</Label>
              <Input v-model="values.site" placeholder="请输入系统网址" />
              <p v-if="errors.site" class="text-sm text-destructive">{{ errors.site }}</p>
            </div>
          </div>

          <div class="space-y-2">
            <Label>系统描述</Label>
            <Textarea v-model="values.description" placeholder="请输入系统描述" />
            <p v-if="errors.description" class="text-sm text-destructive">{{ errors.description }}</p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>系统Logo</Label>
              <Input v-model="values.logo" placeholder="请输入Logo地址" />
            </div>
            <div class="space-y-2">
              <Label>快捷图标</Label>
              <Input v-model="values.shortcuticon" placeholder="请输入快捷图标地址" />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>联系电话</Label>
              <Input v-model="values.tel" placeholder="请输入联系电话" />
            </div>
            <div class="space-y-2">
              <Label>微信</Label>
              <Input v-model="values.weixin" placeholder="请输入微信号" />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>邮箱</Label>
              <Input v-model="values.email" placeholder="请输入邮箱地址" />
            </div>
            <div class="space-y-2">
              <Label>版本</Label>
              <Input v-model="values.version" placeholder="请输入系统版本" />
            </div>
          </div>

          <div class="space-y-2">
            <Label>地址</Label>
            <Input v-model="values.address" placeholder="请输入地址" />
          </div>

          <div class="space-y-2">
            <Label>关闭提示</Label>
            <Textarea v-model="values.closeMsg" placeholder="请输入系统关闭时的提示信息" />
          </div>

          <div class="flex items-center gap-2">
            <Label>系统状态</Label>
            <Switch
              :checked="isOpenChecked"
              @update:checked="(v) => isOpenChecked = v"
            />
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" type="button" @click="dialogVisible = false">取消</Button>
          <Button type="button" @click="onSubmit" :disabled="isSubmitting">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { messageTip } from '@/shared/utils/feedback'
import { messageConfirm } from '@/shared/utils/legacy-util'
import * as echarts from 'echarts'
import {
  getSystemList, createSystem, updateSystem, deleteSystem, batchDeleteSystems, toggleSystemStatus,
  getAllMonitorData, getMemoryInfo, getCpuInfo, getSystemMonitorInfo
} from '@/modules/system/api/system-api'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Switch } from '@/components/ui/switch'
import { Progress } from '@/components/ui/progress'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Info, Cpu, Settings, Timer, RefreshCw } from '@lucide/vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const selectedIds = ref([])

// Chart refs
const memoryChartRef = ref()
const cpuChartRef = ref()
const uptimeChartRef = ref()

// Chart instances
let memoryChart = null
let cpuChart = null
let uptimeChart = null

// Timer
let refreshTimer = null
const isAutoRefresh = ref(true)
const refreshInterval = 1000
const dataSource = ref('检测中...')
const connectionStatus = ref('connecting')

// Local system info
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

// Form schema
const systemSchema = toTypedSchema(z.object({
  systemCode: z.string().min(1, '请输入系统代码'),
  name: z.string().min(1, '请输入系统名称'),
  title: z.string().min(1, '请输入系统标题'),
  site: z.string().min(1, '请输入系统网址'),
  description: z.string().min(1, '请输入系统描述'),
}))

const { handleSubmit, errors, values, isSubmitting, resetForm, setValues } = useForm({
  validationSchema: systemSchema,
  initialValues: {
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
    isopen: 'true',
  },
})

// Computed for form isopen switch (boolean mapping)
const isOpenChecked = computed({
  get: () => values.isopen === 'true',
  set: (val) => { values.isopen = val ? 'true' : 'false' },
})

// Selection helpers
const isAllSelected = computed(() => {
  return tableData.value.length > 0 && selectedIds.value.length === tableData.value.length
})

const toggleSelectAll = (checked) => {
  if (checked) {
    selectedIds.value = tableData.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

const toggleRowSelection = (id, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

// Progress color class
const getProgressColorClass = (percentage) => {
  if (percentage < 50) return '[&>[data-slot=progress-indicator]]:bg-[#67c23a]'
  if (percentage < 80) return '[&>[data-slot=progress-indicator]]:bg-[#e6a23c]'
  return '[&>[data-slot=progress-indicator]]:bg-[#f56c6c]'
}

// Get local system info
const getLocalSystemInfo = async () => {
  try {
    connectionStatus.value = 'connecting'
    const response = await getAllMonitorData()
    if (true) {
      const data = response

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
    fallbackToWebAPI()
    dataSource.value = '🟡 浏览器模拟数据'
  }
}

// Retry mechanism
const getSystemInfoWithRetry = async (retries = 3) => {
  for (let i = 0; i < retries; i++) {
    try {
      await getLocalSystemInfo()
      if (connectionStatus.value === 'connected') {
        return
      }
    } catch (error) {
      console.warn(`获取系统信息失败，第${i + 1}次重试:`, error)
      if (i === retries - 1) {
        fallbackToWebAPI()
        dataSource.value = '🔴 后端连接失败，使用模拟数据'
      } else {
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
    }
  }
}

// Fallback to Web API
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

const getUserAgent = () => {
  const ua = navigator.userAgent
  if (ua.includes('Windows NT 10.0')) return 'Windows 10/11'
  if (ua.includes('Windows NT 6.3')) return 'Windows 8.1'
  if (ua.includes('Windows NT 6.1')) return 'Windows 7'
  if (ua.includes('Mac OS X')) {
    const match = ua.match(/Mac OS X ([0-9_]+)/)
    if (match) return `macOS ${match[1].replace(/_/g, '.')}`
    return 'macOS'
  }
  if (ua.includes('Linux')) return 'Linux'
  return 'Unknown OS'
}

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

const getWebMemoryUsage = () => {
  if ('memory' in performance) {
    const memory = performance.memory
    const used = memory.usedJSHeapSize
    const limit = memory.jsHeapSizeLimit

    const percentage = Math.round((used / limit) * 100)

    systemInfo.value.totalMemory = `${(limit / (1024 * 1024 * 1024)).toFixed(2)}GB (JS堆限制)`
    systemInfo.value.freeMemory = `${((limit - used) / (1024 * 1024 * 1024)).toFixed(2)}GB`

    return Math.min(percentage, 100)
  }

  const baseUsage = 35
  const timeVariation = Math.sin(Date.now() / 10000) * 10
  return Math.max(20, Math.min(70, baseUsage + timeVariation))
}

// Auto refresh
const startAutoRefresh = () => {
  if (refreshTimer) return

  refreshTimer = setInterval(async () => {
    if (isAutoRefresh.value) {
      if (connectionStatus.value === 'connected') {
        await getLocalSystemInfo()
      } else {
        await getSystemInfoWithRetry(1)
      }
      updateCharts()
    }
  }, refreshInterval)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    startAutoRefresh()
    messageTip('已开启自动刷新', 'success')
  } else {
    stopAutoRefresh()
    messageTip('已关闭自动刷新', 'info')
  }
}

const refreshSystemInfo = async () => {
  await getSystemInfoWithRetry()
  updateCharts()
  messageTip('系统信息已手动刷新', 'success')
}

// Chart initialization
const initCharts = async () => {
  await nextTick()

  if (memoryChartRef.value) {
    memoryChart = echarts.init(memoryChartRef.value)
    updateMemoryChart()
  }

  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value)
    updateCpuChart()
  }

  if (uptimeChartRef.value) {
    uptimeChart = echarts.init(uptimeChartRef.value)
    updateUptimeChart()
  }

  window.addEventListener('resize', resizeCharts)
}

const updateCharts = () => {
  updateMemoryChart()
  updateCpuChart()
  updateUptimeChart()
}

const updateMemoryChart = async () => {
  if (!memoryChart) return

  let usage = systemInfo.value.memoryUsage

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
      usage = getWebMemoryUsage()
      systemInfo.value.memoryUsage = usage
    }
  } catch (error) {
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
          itemStyle: { color: '#f0f2f5', opacity: 0.3 }
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

const updateCpuChart = async () => {
  if (!cpuChart) return

  let cores = systemInfo.value.cpus
  let data = []

  try {
    if (connectionStatus.value === 'connected') {
      const response = await getCpuInfo()
      if (true) {
        const cpuData = response
        cores = cpuData.logicalProcessors

        data = cpuData.cores.map((core, index) => ({
          value: Math.round(core.loadPercentage),
          name: `核心${index + 1}`
        }))
      }
    }
  } catch (error) {
    // CPU API failed, use fallback
  }

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
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }],
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
    animation: true,
    animationDuration: 800
  }
  cpuChart.setOption(option)
}

const updateUptimeChart = async () => {
  if (!uptimeChart) return

  let hours = formatUptimeHours(systemInfo.value.uptime)

  try {
    if (connectionStatus.value === 'connected') {
      const response = await getSystemMonitorInfo()
      if (true) {
        const sysData = response
        systemInfo.value.uptime = sysData.uptimeFormatted
        hours = Math.floor(sysData.uptime / 3600)
      }
    }
  } catch (error) {
    const currentTime = performance.now()
    systemInfo.value.uptime = formatUptime(currentTime / 1000)
    hours = formatUptimeHours(systemInfo.value.uptime)
  }

  const maxHours = 24

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
      axisLine: { lineStyle: { width: 18, color: [[1, '#e6e6e6']] } },
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

const resizeCharts = () => {
  memoryChart?.resize()
  cpuChart?.resize()
  uptimeChart?.resize()
}

const getMemoryGradientColor = (usage) => {
  if (usage < 50) return { start: '#67c23a', end: '#85ce61' }
  if (usage < 80) return { start: '#e6a23c', end: '#ebb563' }
  return { start: '#f56c6c', end: '#f89898' }
}

const formatUptimeHours = (uptimeStr) => {
  if (typeof uptimeStr === 'string') {
    const match = uptimeStr.match(/(\d+)小时/)
    if (match) return parseInt(match[1])
    const dayMatch = uptimeStr.match(/(\d+)天/)
    if (dayMatch) return parseInt(dayMatch[1]) * 24
  }
  return Math.floor(Math.random() * 12) + 1
}

const formatUptime = (ms) => {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天 ${hours % 24}小时 ${minutes % 60}分钟`
  if (hours > 0) return `${hours}小时 ${minutes % 60}分钟`
  if (minutes > 0) return `${minutes}分钟`
  return `${seconds}秒`
}

const getMemoryColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

// Data source CSS class
const getDataSourceClass = () => {
  if (dataSource.value.includes('真实系统数据')) return 'text-[#4CAF50]'
  if (dataSource.value.includes('浏览器模拟数据')) return 'text-[#FF9800]'
  if (dataSource.value.includes('连接失败')) return 'text-[#F44336]'
  return ''
}

// CRUD operations
const loadData = async () => {
  loading.value = true
  try {
    const res = await getSystemList()
    if (true) {
      tableData.value = res
    }
  } catch (error) {
    console.error('获取系统列表失败:', error)
    messageTip('获取数据失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  values.isopen = 'true'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  setValues({ ...row })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await messageConfirm('确认删除该系统信息吗？')
    const res = await deleteSystem(row.id)
    if (true) {
      messageTip('删除成功', 'success')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('删除失败', 'error')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return
  try {
    await messageConfirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`)
    const res = await batchDeleteSystems(selectedIds.value)
    if (true) {
      messageTip('批量删除成功', 'success')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      messageTip('批量删除失败', 'error')
    }
  }
}

// Submit form
const onSubmit = handleSubmit(async () => {
  try {
    const formData = { ...values }
    const res = isEdit.value
      ? await updateSystem(formData.id, formData)
      : await createSystem(formData)

    if (true) {
      messageTip(isEdit.value ? '更新成功' : '创建成功', 'success')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交系统信息失败:', error)
    messageTip(isEdit.value ? '更新失败' : '创建失败', 'error')
  }
})

// Status toggle
const handleStatusChange = async (row) => {
  try {
    const res = await toggleSystemStatus(row.id, row.isopen)
    if (true) {
      messageTip('状态更新成功', 'success')
    } else {
      row.isopen = row.isopen === 'true' ? 'false' : 'true'
      messageTip('状态更新失败', 'error')
    }
  } catch (error) {
    row.isopen = row.isopen === 'true' ? 'false' : 'true'
    messageTip('状态更新失败', 'error')
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

onMounted(async () => {
  await getSystemInfoWithRetry()
  loadData()
  await initCharts()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', resizeCharts)
  memoryChart?.dispose()
  cpuChart?.dispose()
  uptimeChart?.dispose()
})
</script>

<style scoped>
.chart-card {
  transition: all 0.3s ease;
  overflow: hidden;
}

.chart-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

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
</style>
