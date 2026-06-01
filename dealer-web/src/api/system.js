import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'

// 获取系统信息列表
export function getSystemList() {
    return doGet('/api/system/list')
}

// 获取系统信息详情
export function getSystemDetail(id) {
    return doGet(`/api/system/${id}`)
}

// 更新系统信息
export function updateSystem(id, data) {
    return doPut(`/api/system/${id}`, data)
}

// 创建系统信息
export function createSystem(data) {
    return doPost('/api/system/create', data)
}

// 删除系统信息
export function deleteSystem(id) {
    return doDelete(`/api/system/${id}`)
}

// 批量删除系统信息
export function batchDeleteSystems(ids) {
    return doDelete('/api/system/batch', ids)
}

// 切换系统开启状态
export function toggleSystemStatus(id, isopen) {
    return doPut(`/api/system/${id}/status`, { isopen })
}

// ================ 系统监控相关API ================

// 获取系统基本信息
export function getSystemMonitorInfo() {
    return doGet('/api/monitor/system-info')
}

// 获取内存信息
export function getMemoryInfo() {
    return doGet('/api/monitor/memory-info')
}

// 获取CPU信息
export function getCpuInfo() {
    return doGet('/api/monitor/cpu-info')
}

// 获取磁盘信息
export function getDiskInfo() {
    return doGet('/api/monitor/disk-info')
}

// 获取JVM信息
export function getJvmInfo() {
    return doGet('/api/monitor/jvm-info')
}

// 获取网络信息
export function getNetworkInfo() {
    return doGet('/api/monitor/network-info')
}

// 获取所有监控数据
export function getAllMonitorData() {
    return doGet('/api/monitor/all')
}