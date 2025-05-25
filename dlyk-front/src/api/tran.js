import { doGet, doPost, doPut } from '../http/httpRequest'

// 获取交易列表
export function getTranList(params) {
    return doGet('/api/tran/list', params)
}

// 获取交易详情
export function getTranDetail(id) {
    return doGet(`/api/tran/${id}`)
}

// 获取交易产品详情
export function getTranProducts(id) {
    return doGet(`/api/tran/products/${id}`)
}

// 创建新交易
export function createTran(data) {
    return doPost('/api/tran/create', data)
}

// 更新交易信息
export function updateTran(data) {
    return doPut('/api/tran/update', data)
}

// 审批交易
export function approveTran(id, data) {
    return doPut(`/api/tran/approve/${id}`, data)
}

// 获取交易状态
export function getTranStatus(id) {
    return doGet(`/api/tran/status/${id}`)
}

// 获取发票信息
export function getInvoiceInfo(tranId) {
    return doGet(`/api/tran/invoice/${tranId}`)
}

// 创建发票
export function createInvoice(data) {
    return doPost('/api/tran/invoice', data)
}