import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { Tran, TranForm, TranInvoice, TranProduct, TPayment, PaymentForm } from '@/modules/tran/model/tran.types'

export function fetchTranPage(params: TranQuery): Promise<PageResult<Tran>> {
  return httpClient.get<PageResult<Tran>>('/api/tran/list', { params })
}

export function fetchTranDetail(id: EntityId): Promise<Tran> {
  return httpClient.get<Tran>(`/api/tran/${id}`)
}

export function fetchTranProducts(id: EntityId): Promise<TranProduct[]> {
  return httpClient.get<TranProduct[]>(`/api/tran/products/${id}`)
}

export function createTran(data: TranForm): Promise<unknown> {
  return httpClient.post('/api/tran/create', data)
}

export function updateTran(data: TranForm): Promise<unknown> {
  return httpClient.put('/api/tran/update', data)
}

export function settleTran(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/tran/settle/${id}`)
}

export function approveTran(id: EntityId, data: TranForm): Promise<unknown> {
  return httpClient.put(`/api/tran/approve/${id}`, data)
}

export function getTranApprove(tranId: EntityId): Promise<TranForm> {
  return httpClient.get<TranForm>(`/api/tran/approve/info/${tranId}`)
}

export function createInvoice(data: TranForm): Promise<unknown> {
  return httpClient.post('/api/tran/invoice', data)
}

export function fetchTranInvoiceList(tranId: EntityId): Promise<TranInvoice[]> {
  return httpClient.get<TranInvoice[]>(`/api/tran/invoice/${tranId}`)
}

export function updateInvoiceStatus(invoiceId: EntityId, status: string): Promise<unknown> {
  return httpClient.put(`/api/tran/invoice/${invoiceId}/status`, { status })
}

export function deleteTran(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/tran/${id}`)
}

export function batchDeleteTran(ids: EntityId[]): Promise<unknown> {
  return httpClient.post('/api/tran/batch-delete', { ids })
}

export function recordPayment(data: PaymentForm): Promise<TPayment> {
  return httpClient.post<TPayment>('/api/tran/payment', data)
}

export function fetchTranPayments(tranId: EntityId): Promise<TPayment[]> {
  return httpClient.get<TPayment[]>(`/api/tran/payment/${tranId}`)
}

export function refundPayment(id: EntityId): Promise<TPayment> {
  return httpClient.post<TPayment>(`/api/tran/payment/${id}/refund`)
}

export const getTranList = fetchTranPage
export const getTranDetail = fetchTranDetail
export const getTranProducts = fetchTranProducts
export const getTranInvoiceList = fetchTranInvoiceList
export const getTranPayments = fetchTranPayments
