import type { LooseRecord } from '@/shared/types/common'

export interface SummaryData extends LooseRecord {
  totalActivityCount?: number
  effectiveActivityCount?: number
  totalClueCount?: number
  totalCustomerCount?: number
  totalTranAmount?: number | string
  successTranAmount?: number | string
}

export interface NameValueData extends LooseRecord {
  name?: string
  value?: number
}
