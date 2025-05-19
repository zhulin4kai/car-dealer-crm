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
export function toggleSystemStatus(id, isOpen) {
    return doPut(`/api/system/${id}/status`, { isOpen })
} 