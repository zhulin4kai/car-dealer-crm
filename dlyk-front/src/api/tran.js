import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'

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

// 结算交易
export function settleTran(id) {
    return doPut(`/api/tran/settle/${id}`)
}

// 审批交易
export function approveTran(id, data) {
    return doPut(`/api/tran/approve/${id}`, data)
}

// 获取交易审批信息
export function getTranApprove(tranId) {
    return doGet(`/api/tran/approve/info/${tranId}`)
}

// 获取交易状态
export function getTranStatus(id) {
    return doGet(`/api/tran/status/${id}`)
}

// 创建发票
export function createInvoice(data) {
    return doPost('/api/tran/invoice', data)
}

// 获取交易发票列表
export function getTranInvoiceList(tranId) {
    return doGet(`/api/tran/invoice/${tranId}`)
}

// 更新发票状态
export function updateInvoiceStatus(invoiceId, status) {
    return doPut(`/api/tran/invoice/${invoiceId}/status`, { status })
}

// 删除单个交易
export function deleteTran(id) {
    return doDelete(`/api/tran/${id}`)
}

// 批量删除交易
export function batchDeleteTran(ids) {
    return doPost('/api/tran/batch-delete', { ids })
}