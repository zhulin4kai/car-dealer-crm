import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Clue extends LooseRecord {
  id?: number | string
  fullname?: string
  phone?: string
}

export interface ClueRemark extends LooseRecord {
  id?: number | string
  clueId?: number | string
}

export type ClueQuery = Partial<PageQuery> & LooseRecord
export type ClueForm = LooseRecord
