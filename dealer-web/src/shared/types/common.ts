export type DictionaryValue = string | number | boolean | null

export type LooseRecord = {
  [key: string]: unknown
}

export type SelectOption = {
  id?: number | string
  label?: string
  name?: string
  value?: string | number
  [key: string]: unknown
}

export type MessageType = 'success' | 'warning' | 'info' | 'error'
