export interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageSize?: number
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
