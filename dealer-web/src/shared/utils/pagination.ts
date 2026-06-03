export function getTableStartIndex(currentPage: number, pageSize: number, index: number): number {
  return (currentPage - 1) * pageSize + index + 1
}
