export function toNumber(value: number | string | null | undefined): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

export function formatNumber(value: number | string | null | undefined, fallback = '--'): string {
  const numericValue = toNumber(value)
  return numericValue === null ? fallback : numericValue.toLocaleString('zh-CN')
}

export function formatCurrency(
  value: number | string | null | undefined,
  options: { fractionDigits?: number; suffix?: string } = {}
): string {
  const numericValue = toNumber(value)
  if (numericValue === null) {
    return '--'
  }

  const fractionDigits = options.fractionDigits ?? 2
  const formattedValue = numericValue.toLocaleString('zh-CN', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  })
  return `¥${formattedValue}${options.suffix ?? ''}`
}

export function formatPhone(value: string | null | undefined): string {
  const phone = value?.trim()
  if (!phone) {
    return '--'
  }
  if (phone.length < 7) {
    return phone
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

export function formatDateTime(value: string | null | undefined): string {
  const rawValue = value?.trim()
  if (!rawValue) {
    return '--'
  }

  const date = new Date(rawValue.replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) {
    return rawValue
  }

  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

export function formatPercent(numerator: number, denominator: number): string {
  if (!denominator) {
    return '--'
  }
  return `${((numerator / denominator) * 100).toFixed(1)}%`
}
