import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  ApproveTranRequest,
  ApproveRefundRequest,
  ConfirmPaymentRequest,
  CreateInvoiceRequest,
  CreateRefundRequest,
  CreateTranRequest,
  ExecuteRefundRequest,
  InvoiceStatusCommand,
  PaymentRequest,
  RedReverseInvoiceRequest,
  ReissueInvoiceRequest,
  SettleRequest,
  SettlementPromotionOption,
  SettlementPreviewRequest,
  SettlementPreviewResponse,
  TRefundRequest,
  TPayment,
  Tran,
  TranApproval,
  TranInvoice,
  TransactionLifecycleRequest,
  TranProduct,
  TranQuery,
  UpdateInvoiceStatusRequest,
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
  return httpClient.post<number>('/api/transactions', data)
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

export function fetchAvailableSettlementPromotions(id: EntityId): Promise<SettlementPromotionOption[]> {
  return httpClient.get<SettlementPromotionOption[]>(`/api/tran/${id}/available-promotions`)
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

export function updateInvoiceStatus(
  invoiceId: EntityId,
  data: UpdateInvoiceStatusRequest | InvoiceStatusCommand,
): Promise<boolean> {
  const request = typeof data === 'string' ? { status: data } : data
  return httpClient.put<boolean>(`/api/tran/invoice/${invoiceId}/status`, request)
}

export function redReverseInvoice(invoiceId: EntityId, data: RedReverseInvoiceRequest): Promise<TranInvoice> {
  return httpClient.post<TranInvoice>(`/api/tran/invoice/${invoiceId}/red-reversal`, data)
}

export function reissueInvoice(invoiceId: EntityId, data: ReissueInvoiceRequest): Promise<TranInvoice> {
  return httpClient.post<TranInvoice>(`/api/tran/invoice/${invoiceId}/reissue`, data)
}

export function deleteTran(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/tran/${id}`)
}

export function cancelTran(id: EntityId, data: TransactionLifecycleRequest): Promise<boolean> {
  return httpClient.put<boolean>(`/api/tran/${id}/cancel`, data)
}

export function closeTran(id: EntityId, data: TransactionLifecycleRequest): Promise<boolean> {
  return httpClient.put<boolean>(`/api/tran/${id}/close`, data)
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

export function confirmPayment(id: EntityId, data: ConfirmPaymentRequest): Promise<TPayment> {
  return httpClient.put<TPayment>(`/api/tran/payment/${id}/confirm`, data)
}

export function fetchTranPayments(tranId: EntityId): Promise<TPayment[]> {
  return httpClient.get<TPayment[]>(`/api/tran/payment/${tranId}`)
}

export function fetchTranRefundRequests(tranId: EntityId): Promise<TRefundRequest[]> {
  return httpClient.get<TRefundRequest[]>(`/api/tran/refund-requests/${tranId}`)
}

export function createRefundRequest(id: EntityId, data: CreateRefundRequest): Promise<TRefundRequest> {
  return httpClient.post<TRefundRequest>(`/api/tran/payment/${id}/refund-requests`, data)
}

export function approveRefundRequest(id: EntityId, data: ApproveRefundRequest): Promise<TRefundRequest> {
  return httpClient.put<TRefundRequest>(`/api/tran/refund-requests/${id}/approve`, data)
}

export function executeRefundRequest(id: EntityId, data: ExecuteRefundRequest): Promise<TRefundRequest> {
  return httpClient.post<TRefundRequest>(`/api/tran/refund-requests/${id}/execute`, data)
}

export const getTranList = fetchTranPage
export const getTranDetail = fetchTranDetail
export const getTranProducts = fetchTranProducts
export const getTranInvoiceList = fetchTranInvoiceList
export const getTranPayments = fetchTranPayments
