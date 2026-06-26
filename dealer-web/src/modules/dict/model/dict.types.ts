import type { EntityId } from '@/shared/types/id'

export interface DictType {
  id?: EntityId
  typeCode?: string
  typeName?: string
  applicableModule?: string
  enabled?: boolean
  builtIn?: boolean
  disableReason?: string
  disabledBy?: number
  disabledTime?: string
  remark?: string
}

export interface DictValue {
  id?: EntityId
  typeCode?: string
  typeValue?: string
  valueCode?: string
  order?: number
  applicableModule?: string
  enabled?: boolean
  builtIn?: boolean
  disableReason?: string
  disabledBy?: number
  disabledTime?: string
  remark?: string
}

export interface DictQuery {
  page?: number
  size?: number
  typeCode?: string
  typeName?: string
  typeValue?: string
  valueCode?: string
  applicableModule?: string
  enabled?: boolean
}

export interface DictTypeForm {
  typeCode: string
  typeName: string
  applicableModule?: string
  enabled?: boolean
  disableReason?: string
  remark?: string
}

export interface DictValueForm {
  typeCode: string
  typeValue: string
  valueCode: string
  order: number
  applicableModule?: string
  enabled?: boolean
  disableReason?: string
  remark?: string
}
