export function getTableStartIndex(currentPage: number, pageSize: number, index: number): number {
  return (currentPage - 1) * pageSize + index + 1
}

export function normalizePage(page: number, total: number, pageSize: number): number {
  if (total <= 0) return 1
  const lastPage = Math.ceil(total / pageSize)
  if (lastPage < 1) return 1
  return Math.min(Math.max(page, 1), lastPage)
}
