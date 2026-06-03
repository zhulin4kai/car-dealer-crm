import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Customer extends LooseRecord {
  id?: number | string
  name?: string
}

export type CustomerQuery = Partial<PageQuery> & LooseRecord
