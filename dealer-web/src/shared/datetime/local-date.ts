export type LocalDateText = string
export type LocalDateTimeText = string

const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/
const DATETIME_REGEX = /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$/

export function toLocalDateInput(value: LocalDateText | LocalDateTimeText | null | undefined): string {
  if (!value) return ''
  if (DATE_REGEX.test(value)) return value
  if (DATETIME_REGEX.test(value)) return value.slice(0, 10)
  return ''
}

export function fromLocalDateInput(value: string): LocalDateTimeText | null {
  if (!value || !DATE_REGEX.test(value)) return null
  return `${value} 00:00:00`
}

export function toLocalDateTimeInput(value: LocalDateTimeText | null | undefined): string {
  if (!value) return ''
  const normalized = value.replace(' ', 'T')
  if (DATETIME_REGEX.test(normalized)) {
    return normalized.slice(0, 16)
  }
  return ''
}

export function fromLocalDateTimeInput(value: string): LocalDateTimeText | null {
  if (!value) return null
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    return value.replace('T', ' ') + ':00'
  }
  if (DATETIME_REGEX.test(value)) {
    return value.replace('T', ' ')
  }
  return null
}
