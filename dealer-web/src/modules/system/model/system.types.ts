import type { LooseRecord } from '@/shared/types/common'

export interface SystemConfig extends LooseRecord {
  id?: number | string
  name?: string
  isopen?: boolean | string
}

export interface MemoryInfo extends LooseRecord {
  totalMemoryFormatted?: string
  availableMemoryFormatted?: string
  usagePercentage?: number
}

export interface CpuInfo extends LooseRecord {
  usagePercentage?: number
}

export interface SystemMonitorData extends LooseRecord {
  memoryInfo?: MemoryInfo
  cpuInfo?: CpuInfo
  systemInfo?: LooseRecord
  diskInfo?: LooseRecord
  jvmInfo?: LooseRecord
  networkInfo?: LooseRecord
  timestamp?: number
}

export type SystemForm = LooseRecord
