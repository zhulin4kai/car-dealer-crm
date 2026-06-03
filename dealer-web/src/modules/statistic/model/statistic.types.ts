import type { LooseRecord } from '@/shared/types/common'

export interface SummaryData extends LooseRecord {
  totalActivityCount?: number
  totalClueCount?: number
  totalCustomerCount?: number
  totalTranAmount?: number
}

export interface NameValueData extends LooseRecord {
  name?: string
  value?: number
}
