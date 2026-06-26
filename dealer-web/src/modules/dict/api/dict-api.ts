import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  DictQuery,
  DictType,
  DictTypeForm,
  DictValue,
  DictValueForm,
} from '@/modules/dict/model/dict.types'

export function fetchDictTypePage(params: DictQuery, signal?: AbortSignal): Promise<PageResult<DictType>> {
  return httpClient.get<PageResult<DictType>>('/api/dict/types', { params, signal })
}

export function fetchDictTypeDetail(id: EntityId): Promise<DictType> {
  return httpClient.get<DictType>(`/api/dict/type/get/${id}`)
}

export function createDictType(data: DictTypeForm): Promise<unknown> {
  return httpClient.post('/api/dict/type/create', data)
}

export function updateDictType(id: EntityId, data: DictTypeForm): Promise<unknown> {
  return httpClient.put(`/api/dict/type/update/${id}`, data)
}

export function deleteDictType(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/dict/type/delete/${id}`)
}

export function batchDeleteDictTypes(ids: EntityId[]): Promise<unknown> {
  return httpClient.delete('/api/dict/types/batch', ids)
}

export function fetchDictValuePage(params: DictQuery, signal?: AbortSignal): Promise<PageResult<DictValue>> {
  return httpClient.get<PageResult<DictValue>>('/api/dict/values', { params, signal })
}

export function fetchDictValueDetail(id: EntityId): Promise<DictValue> {
  return httpClient.get<DictValue>(`/api/dict/value/get/${id}`)
}

export function createDictValue(data: DictValueForm): Promise<unknown> {
  return httpClient.post('/api/dict/value/create', data)
}

export function updateDictValue(id: EntityId, data: DictValueForm): Promise<unknown> {
  return httpClient.put(`/api/dict/value/update/${id}`, data)
}

export function deleteDictValue(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/dict/value/delete/${id}`)
}

export function batchDeleteDictValues(ids: EntityId[]): Promise<unknown> {
  return httpClient.delete('/api/dict/value/batch', ids)
}

export function clearCache(): Promise<unknown> {
  return httpClient.get('/api/dict/clear', { params: { forceRefresh: true } })
}

export const getDictTypeList = fetchDictTypePage
export const getDictTypeDetail = fetchDictTypeDetail
export const getDictValueList = fetchDictValuePage
export const getDictValueDetail = fetchDictValueDetail
