import type { EntityId } from '@/shared/types/id'

export type InvoiceStatus = 'PENDING' | 'ISSUED' | 'VOID'
export type InvoiceStatusCommand = 'ISSUED' | 'VOID'

export interface TranProduct {
  id?: EntityId
  productId: EntityId
  productName?: string
  productSku?: string
  productSpecification?: string
  guidePrice?: number
  quantity: number
  price: number
}

export interface Tran {
  id: EntityId
  tranNo?: string
  customerId?: EntityId
  customerName?: string
  money?: number
  stage?: string
  description?: string
  expectedDate?: string
  nextContactTime?: string
  createTime?: string
  editTime?: string
  version?: number
  promotionId?: EntityId | null
  originalAmount?: number | null
  discountAmount?: number
  products?: TranProduct[]
}

export interface TranQuery {
  page?: number
  size?: number
  tranNo?: string
  customerId?: EntityId
  customerName?: string
  stage?: string
}

export interface TranProductRequest {
  productId: EntityId
  quantity: number
}

export interface CreateTranRequest {
  customerId: EntityId
  description?: string
  expectedDeliveryDate: string | null
  products: TranProductRequest[]
}

export interface UpdateTranRequest extends CreateTranRequest {
  id: EntityId
}

export interface ApproveTranRequest {
  approved: boolean
  comment: string
}

export interface TranApproval {
  id?: EntityId
  tranId?: EntityId
  approveResult?: boolean
  approveComment?: string
  approveTime?: string
}

export interface TranInvoice {
  id: EntityId
  tranId?: EntityId
  invoiceNo?: string
  type?: string
  title?: string
  taxNumber?: string
  bankName?: string
  bankAccount?: string
  address?: string
  phone?: string
  amount?: number
  status: InvoiceStatus
  remark?: string
  issueTime?: string
  createTime?: string
}

export interface CreateInvoiceRequest {
  tranId: number
  type: string
  title: string
  taxNumber: string
  bankName?: string
  bankAccount?: string
  address?: string
  phone?: string
  amount: number
  remark?: string
}

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'
export type RefundRequestStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'EXECUTED'
export type RefundType = 'ORDER_CANCEL' | 'OVERPAY' | 'PRICE_ADJUSTMENT' | 'CUSTOMER_BREACH' | 'INTERNAL_CORRECTION'

export interface PaymentRequest {
  tranId: number
  amount?: number
  paymentMethod: string
  paymentType?: string
  transactionRef?: string
  remark?: string
}

export interface ConfirmPaymentRequest {
  approved: boolean
  comment?: string
}

export interface TPayment {
  id: EntityId
  tranId?: EntityId
  paymentNo?: string
  amount: number
  paymentMethod?: string
  paymentType?: string
  paymentStatus?: PaymentStatus
  paymentTime?: string
  transactionRef?: string
  idempotencyKey?: string
  remark?: string
  createTime?: string
}

export interface CreateRefundRequest {
  refundType: RefundType
  amount: number
  reason: string
}

export interface ApproveRefundRequest {
  approved: boolean
  comment?: string
}

export interface ExecuteRefundRequest {
  transactionRef?: string
  remark?: string
}

export interface TRefundRequest {
  id: EntityId
  tranId?: EntityId
  originalPaymentId: EntityId
  refundPaymentId?: EntityId
  amount: number
  refundType: RefundType
  reason: string
  status: RefundRequestStatus
  requestedTime?: string
  approvedTime?: string
  approveComment?: string
  executedTime?: string
}

export interface SettlementPreviewRequest {
  promotionId?: EntityId
}

export interface SettlementPromotionOption {
  id: EntityId
  productId: EntityId
  name: string
  type: string
  discount: number | string
  startTime?: string
  endTime?: string
  status?: string
}

export interface SettleRequest {
  promotionId?: EntityId
  expectedVersion: number
  pricingFingerprint: string
}

export interface SettlementPreviewResponse {
  tranId: number
  promotionId?: number | null
  originalAmount: number
  discountAmount: number
  finalAmount: number
  transactionVersion: number
  pricingFingerprint: string
  promotion?: {
    id: number
    name: string
    type: string
    discount: number
    productId: number
    startTime?: string
    endTime?: string
    updateTime?: string
  }
}
