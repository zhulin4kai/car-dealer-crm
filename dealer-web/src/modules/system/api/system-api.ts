import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'
import type { SystemConfig, SystemForm, SystemMonitorData } from '@/modules/system/model/system.types'

export function fetchSystemList(): Promise<SystemConfig[]> {
  return httpClient.get<SystemConfig[]>('/api/system/list')
}

export function fetchSystemDetail(id: EntityId): Promise<SystemConfig> {
  return httpClient.get<SystemConfig>(`/api/system/${id}`)
}

export function updateSystem(id: EntityId, data: SystemForm): Promise<unknown> {
  return httpClient.put(`/api/system/${id}`, data)
}

export function createSystem(data: SystemForm): Promise<unknown> {
  return httpClient.post('/api/system/create', data)
}

export function deleteSystem(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/system/${id}`)
}

export function batchDeleteSystems(ids: EntityId[]): Promise<unknown> {
  return httpClient.delete('/api/system/batch', ids)
}

export function toggleSystemStatus(id: EntityId, isopen: boolean | string): Promise<unknown> {
  return httpClient.put(`/api/system/${id}/status`, { isopen })
}

export function getSystemMonitorInfo(): Promise<SystemMonitorData['systemInfo']> {
  return httpClient.get('/api/monitor/system-info')
}

export function getMemoryInfo(): Promise<SystemMonitorData['memoryInfo']> {
  return httpClient.get('/api/monitor/memory-info')
}

export function getCpuInfo(): Promise<SystemMonitorData['cpuInfo']> {
  return httpClient.get('/api/monitor/cpu-info')
}

export function getDiskInfo(): Promise<SystemMonitorData['diskInfo']> {
  return httpClient.get('/api/monitor/disk-info')
}

export function getJvmInfo(): Promise<SystemMonitorData['jvmInfo']> {
  return httpClient.get('/api/monitor/jvm-info')
}

export function getNetworkInfo(): Promise<SystemMonitorData['networkInfo']> {
  return httpClient.get('/api/monitor/network-info')
}

export function getAllMonitorData(): Promise<SystemMonitorData> {
  return httpClient.get<SystemMonitorData>('/api/monitor/all')
}

export const getSystemList = fetchSystemList
export const getSystemDetail = fetchSystemDetail
