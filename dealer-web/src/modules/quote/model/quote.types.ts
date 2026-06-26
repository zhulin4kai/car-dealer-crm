import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export type QuoteStatus =
  | 'DRAFT'
  | 'PENDING_SUBMIT'
  | 'PENDING_APPROVAL'
  | 'REJECTED'
  | 'PENDING_CUSTOMER_CONFIRMATION'
  | 'ACCEPTED'
  | 'REFUSED'
  | 'EXPIRED'
  | 'VOIDED'
  | 'CONVERTED_TO_ORDER'

export interface Quote {
  id: EntityId
  quoteNo: string
  customerId: EntityId
  opportunityId?: EntityId
  currentVersionId?: EntityId
  status: QuoteStatus
  remark?: string
  createTime?: string
  createBy?: EntityId
  updateTime?: string
  updateBy?: EntityId
}

export interface QuoteVersion {
  id: EntityId
  quoteId: EntityId
  versionNo: number
  validUntil: string
  totalAmount: number | string
  remark?: string
  createTime?: string
  createBy?: EntityId
}

export interface QuoteVersionItem {
  id: EntityId
  quoteVersionId: EntityId
  productId: EntityId
  productSku?: string
  productName?: string
  productSpecification?: string
  guidePrice?: number | string
  unitPrice: number | string
  quantity: number
  lineAmount: number | string
  promotionId?: EntityId
  promotionName?: string
  promotionAmount?: number | string
  createTime?: string
  createBy?: EntityId
}

export interface QuoteDetail {
  quote: Quote
  currentVersion?: QuoteVersion
  items: QuoteVersionItem[]
}

export interface QuoteItemRequest {
  productId: EntityId
  quantity: number
  promotionId?: EntityId
}

export interface CreateQuoteRequest {
  customerId: EntityId
  opportunityId?: EntityId
  validUntil: string
  remark?: string
  items: QuoteItemRequest[]
}

export interface CreateQuoteVersionRequest {
  validUntil: string
  remark?: string
  items: QuoteItemRequest[]
}

export interface UpdateQuoteStatusRequest {
  expectedStatus: QuoteStatus
  targetStatus: QuoteStatus
  reason: string
  confirmedByName?: string
  confirmedAt?: string
  confirmationMethod?: 'CUSTOMER_SIGNATURE' | 'CALL_RECORD' | 'WECHAT' | 'EMAIL' | 'SYSTEM_EXPIRE' | 'PROXY'
  confirmationEvidence?: string
  proxyConfirmReason?: string
}

export interface QuoteQuery extends PageQuery {
  quoteNo?: string
  customerId?: EntityId
  status?: QuoteStatus
}

export function formatQuoteStatus(status?: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_SUBMIT: '待提交',
    PENDING_APPROVAL: '待审批',
    REJECTED: '已驳回',
    PENDING_CUSTOMER_CONFIRMATION: '待客户确认',
    ACCEPTED: '已接受',
    REFUSED: '已拒绝',
    EXPIRED: '已过期',
    VOIDED: '已作废',
    CONVERTED_TO_ORDER: '已转订单',
  }
  return map[status ?? ''] ?? status ?? '--'
}

export function getQuoteStatusTone(
  status?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (status === 'ACCEPTED' || status === 'CONVERTED_TO_ORDER') return 'success'
  if (status === 'PENDING_APPROVAL' || status === 'PENDING_CUSTOMER_CONFIRMATION') return 'warning'
  if (status === 'REJECTED' || status === 'REFUSED' || status === 'EXPIRED' || status === 'VOIDED') {
    return 'danger'
  }
  if (status === 'DRAFT') return 'muted'
  return 'info'
}
