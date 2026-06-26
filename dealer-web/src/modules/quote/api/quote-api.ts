import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  CreateQuoteRequest,
  CreateQuoteVersionRequest,
  Quote,
  QuoteDetail,
  QuoteQuery,
  QuoteVersion,
  UpdateQuoteStatusRequest,
} from '@/modules/quote/model/quote.types'

export function fetchQuotePage(params: QuoteQuery): Promise<PageResult<Quote>> {
  return httpClient.get<PageResult<Quote>>('/api/quotes', { params })
}

export function fetchQuoteDetail(id: EntityId): Promise<QuoteDetail> {
  return httpClient.get<QuoteDetail>(`/api/quotes/${id}`)
}

export function createQuote(data: CreateQuoteRequest): Promise<QuoteDetail> {
  return httpClient.post<QuoteDetail>('/api/quotes', data)
}

export function createQuoteVersion(
  id: EntityId,
  data: CreateQuoteVersionRequest,
): Promise<QuoteDetail> {
  return httpClient.post<QuoteDetail>(`/api/quotes/${id}/versions`, data)
}

export function fetchQuoteVersions(id: EntityId): Promise<QuoteVersion[]> {
  return httpClient.get<QuoteVersion[]>(`/api/quotes/${id}/versions`)
}

export function updateQuoteStatus(id: EntityId, data: UpdateQuoteStatusRequest): Promise<Quote> {
  return httpClient.put<Quote>(`/api/quotes/${id}/status`, data)
}
