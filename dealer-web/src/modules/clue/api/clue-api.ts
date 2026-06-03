import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { Clue, ClueForm, ClueRemark } from '@/modules/clue/model/clue.types'

export function batchDeleteCluesByIds(ids: EntityId[]): Promise<unknown> {
  return httpClient.post('/api/clue/batch', ids)
}

export function fetchCurrentClues(current: number): Promise<PageResult<Clue>> {
  return httpClient.get<PageResult<Clue>>('/api/clues', { params: { current } })
}

export function importExcelAPI(file: FormData): Promise<unknown> {
  return httpClient.post('/api/importExcel', file)
}

export function delClueById(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/clue/${id}`)
}

export function checkPhoneIsExist(phone: string): Promise<unknown> {
  return httpClient.get(`/api/clue/${phone}`)
}

export function getLoginInfo(): Promise<unknown> {
  return httpClient.get('/api/login/info')
}

export function fetchClueDetail(id: EntityId): Promise<Clue> {
  return httpClient.get<Clue>(`/api/clue/detail/${id}`)
}

export function addClue(formData: ClueForm | FormData): Promise<unknown> {
  return httpClient.post('/api/clue', formData)
}

export function updateClue(formData: ClueForm | FormData): Promise<unknown> {
  return httpClient.put('/api/clue', formData)
}

export function addClueRemark(
  clueId: EntityId,
  noteContent: string,
  noteWay: string,
): Promise<unknown> {
  return httpClient.post('/api/clue/remark', {
    clueId,
    noteContent,
    noteWay,
  })
}

export function fetchClueRemarkPage(current: number, clueId: EntityId): Promise<PageResult<ClueRemark>> {
  return httpClient.get<PageResult<ClueRemark>>('/api/clue/remark', {
    params: {
      current,
      clueId,
    },
  })
}

export function convertClueToCustomer(
  clueId: EntityId,
  product: string,
  description: string,
  nextContactTime: string,
): Promise<unknown> {
  return httpClient.post('/api/clue/customer', {
    clueId,
    product,
    description,
    nextContactTime,
  })
}

export const getCurrentClues = fetchCurrentClues
export const getClueDetail = fetchClueDetail
export const getClueRemarkList = fetchClueRemarkPage
