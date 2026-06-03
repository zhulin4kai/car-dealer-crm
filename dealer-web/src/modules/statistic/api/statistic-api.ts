import { httpClient } from '@/shared/api/http-client'
import type { NameValueData, SummaryData } from '@/modules/statistic/model/statistic.types'

export function fetchSummaryData(): Promise<SummaryData> {
  return httpClient.get<SummaryData>('/api/summary/data')
}

export function fetchSaleFunnelData(): Promise<NameValueData[]> {
  return httpClient.get<NameValueData[]>('/api/saleFunnel/data')
}

export function fetchSourcePieData(): Promise<NameValueData[]> {
  return httpClient.get<NameValueData[]>('/api/sourcePie/data')
}
