import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  ApproveTranRequest,
  CreateInvoiceRequest,
  CreateTranRequest,
  InvoiceStatusCommand,
  PaymentRequest,
  SettleRequest,
  SettlementPreviewRequest,
  SettlementPreviewResponse,
  TPayment,
  Tran,
  TranApproval,
  TranInvoice,
  TranProduct,
  TranQuery,
  UpdateTranRequest,
} from '@/modules/tran/model/tran.types'

export function fetchTranPage(params: TranQuery, signal?: AbortSignal): Promise<PageResult<Tran>> {
  return httpClient.get<PageResult<Tran>>('/api/tran/list', { params, signal })
}

export function fetchTranDetail(id: EntityId): Promise<Tran> {
  return httpClient.get<Tran>(`/api/tran/${id}`)
}

export function fetchTranProducts(id: EntityId): Promise<TranProduct[]> {
  return httpClient.get<TranProduct[]>(`/api/tran/products/${id}`)
}

export function createTran(data: CreateTranRequest): Promise<number> {
  return httpClient.post<number>('/api/tran/create', data)
}

export function updateTran(data: UpdateTranRequest): Promise<boolean> {
  return httpClient.put<boolean>('/api/tran/update', data)
}

export function settleTran(id: EntityId, data: SettleRequest): Promise<SettlementPreviewResponse> {
  return httpClient.put<SettlementPreviewResponse>(`/api/tran/${id}/settle`, data)
}

export function fetchSettlementPreview(id: EntityId, data: SettlementPreviewRequest): Promise<SettlementPreviewResponse> {
  return httpClient.post<SettlementPreviewResponse>(`/api/tran/${id}/settlement-preview`, data)
}

export function approveTran(id: EntityId, data: ApproveTranRequest): Promise<boolean> {
  return httpClient.put<boolean>(`/api/tran/approve/${id}`, data)
}

export function getTranApprove(tranId: EntityId): Promise<TranApproval> {
  return httpClient.get<TranApproval>(`/api/tran/approve/info/${tranId}`)
}

export function createInvoice(data: CreateInvoiceRequest): Promise<boolean> {
  return httpClient.post<boolean>('/api/tran/invoice', data)
}

export function fetchTranInvoiceList(tranId: EntityId): Promise<TranInvoice[]> {
  return httpClient.get<TranInvoice[]>(`/api/tran/invoice/${tranId}`)
}

export function updateInvoiceStatus(invoiceId: EntityId, status: InvoiceStatusCommand): Promise<boolean> {
  return httpClient.put<boolean>(`/api/tran/invoice/${invoiceId}/status`, { status })
}

export function deleteTran(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/tran/${id}`)
}

export function batchDeleteTran(ids: EntityId[]): Promise<unknown> {
  return httpClient.post('/api/tran/batch-delete', { ids })
}

export function resubmitTran(id: EntityId): Promise<unknown> {
  return httpClient.put(`/api/tran/resubmit/${id}`)
}

export function recordPayment(data: PaymentRequest): Promise<TPayment> {
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
