export interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageSize?: number
  pageNum?: number
  pages?: number
  size?: number
  startRow?: number
  endRow?: number
  hasNextPage?: boolean
  hasPreviousPage?: boolean
  isFirstPage?: boolean
  isLastPage?: boolean
}

export interface PageQuery {
  page: number
  size: number
}

export interface DownloadResult {
  blob: Blob
  filename: string
}

export type ApiList<T> = T[]
