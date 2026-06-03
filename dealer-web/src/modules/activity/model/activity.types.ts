import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface Activity extends LooseRecord {
  id?: number | string
  name?: string
  ownerId?: number | string
  ownerDO?: LooseRecord
}

export type ActivityQuery = Partial<PageQuery> & LooseRecord
export type ActivityForm = LooseRecord
