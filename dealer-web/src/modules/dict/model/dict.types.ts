import type { EntityId } from '@/shared/types/id'

export interface DictType {
  id?: EntityId
  typeCode?: string
  typeName?: string
  remark?: string
}

export interface DictValue {
  id?: EntityId
  typeCode?: string
  typeValue?: string
  valueCode?: string
  order?: number
  remark?: string
}

export interface DictQuery {
  page?: number
  size?: number
  typeCode?: string
  typeName?: string
  typeValue?: string
}

export interface DictTypeForm {
  typeCode: string
  typeName: string
  remark?: string
}

export interface DictValueForm {
  typeCode: string
  typeValue: string
  valueCode: string
  order: number
  remark?: string
}

export type DictForm = DictTypeForm | DictValueForm
