import { computed, ref, type ComputedRef, type Ref } from 'vue'

export type SortDirection = 'asc' | 'desc'
export type SortAccessor<T> = keyof T | ((row: T) => unknown)

export interface ClientSortState<T> {
  sortBy: Ref<string>
  sortDirection: Ref<SortDirection>
  sortedRows: ComputedRef<T[]>
  toggleSort: (key: string) => void
}

export function useClientSort<T>(
  rows: Ref<T[]>,
  accessors: Record<string, SortAccessor<T>>,
  initialSortBy = '',
  initialDirection: SortDirection = 'asc',
): ClientSortState<T> {
  const sortBy = ref(initialSortBy)
  const sortDirection = ref<SortDirection>(initialDirection)

  const sortedRows = computed(() => {
    const key = sortBy.value
    const accessor = accessors[key]

    if (!key || !accessor) {
      return rows.value
    }

    const directionFactor = sortDirection.value === 'asc' ? 1 : -1

    return [...rows.value].sort((left, right) => {
      const result = compareValues(readSortValue(left, accessor), readSortValue(right, accessor))
      return result * directionFactor
    })
  })

  function toggleSort(key: string): void {
    if (sortBy.value === key) {
      sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
      return
    }

    sortBy.value = key
    sortDirection.value = 'asc'
  }

  return {
    sortBy,
    sortDirection,
    sortedRows,
    toggleSort,
  }
}

function readSortValue<T>(row: T, accessor: SortAccessor<T>): unknown {
  if (typeof accessor === 'function') {
    return accessor(row)
  }
  return row[accessor]
}

function compareValues(left: unknown, right: unknown): number {
  const leftEmpty = left == null || left === ''
  const rightEmpty = right == null || right === ''

  if (leftEmpty && rightEmpty) {
    return 0
  }
  if (leftEmpty) {
    return 1
  }
  if (rightEmpty) {
    return -1
  }

  const leftNumber = toComparableNumber(left)
  const rightNumber = toComparableNumber(right)
  if (leftNumber != null && rightNumber != null) {
    return leftNumber - rightNumber
  }

  return String(left).localeCompare(String(right), 'zh-CN', {
    numeric: true,
    sensitivity: 'base',
  })
}

function toComparableNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (value instanceof Date) {
    return value.getTime()
  }

  if (typeof value !== 'string') {
    return null
  }

  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const timestamp = Date.parse(trimmed.replace(' ', 'T'))
  if (Number.isFinite(timestamp)) {
    return timestamp
  }

  const normalized = Number(trimmed.replace(/[,\s¥￥]/g, ''))
  return Number.isFinite(normalized) ? normalized : null
}
