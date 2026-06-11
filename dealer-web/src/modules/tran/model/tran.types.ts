import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Tran extends LooseRecord {
  id?: number | string
  name?: string
  money?: number
  stage?: string
  tranNo?: string
  customerName?: string
}

export interface TranProduct extends LooseRecord {
  id?: number | string
  productId?: number | string
  productName?: string
  quantity?: number
  price?: number
}

export interface TranInvoice extends LooseRecord {
  id?: number | string
  invoiceNo?: string
  amount?: number
  status?: string
}

export interface TPayment extends LooseRecord {
  id?: number | string
  tranId?: number | string
  paymentNo?: string
  amount?: number
  paymentMethod?: string
  paymentType?: string
  paymentStatus?: string
  paymentTime?: string
  transactionRef?: string
  remark?: string
  createTime?: string
}

export type TranQuery = Partial<PageQuery> & LooseRecord
export type TranForm = LooseRecord
export type PaymentForm = LooseRecord
