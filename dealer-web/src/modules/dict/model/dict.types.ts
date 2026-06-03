import type { PageQuery } from '@/shared/api/api-types'
import type { LooseRecord } from '@/shared/types/common'

export interface DictType extends LooseRecord {
  id?: number | string
  typeCode?: string
  typeName?: string
}

export interface DictValue extends LooseRecord {
  id?: number | string
  typeCode?: string
  value?: string
}

export type DictQuery = Partial<PageQuery> & LooseRecord
export type DictForm = LooseRecord
