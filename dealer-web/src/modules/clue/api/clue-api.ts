import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  Clue,
  ClueForm,
  ClueLifecycleRequest,
  ClueOwnerHistory,
  ImportResult,
  ClueRemark,
  TransferClueOwnerRequest,
} from '@/modules/clue/model/clue.types'
import type { User } from '@/modules/user/model/user.types'

export function batchDeleteCluesByIds(ids: EntityId[]): Promise<unknown> {
  return httpClient.post('/api/clue/batch', ids)
}

export function fetchCurrentClues(current: number): Promise<PageResult<Clue>> {
  return httpClient.get<PageResult<Clue>>('/api/clues', { params: { current } })
}

export function importExcelAPI(file: FormData): Promise<ImportResult> {
  return httpClient.post<ImportResult>('/api/importExcel', file)
}

export function delClueById(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/clue/${id}`)
}

export async function isCluePhoneAvailable(phone: string): Promise<boolean> {
  try {
    await httpClient.get(`/api/clue/${phone}`)
    return true
  } catch {
    return false
  }
}

export function getLoginInfo(): Promise<User> {
  return httpClient.get<User>('/api/login/info')
}

export function fetchClueDetail(id: EntityId): Promise<Clue> {
  return httpClient.get<Clue>(`/api/clue/detail/${id}`)
}

export function fetchClueOwnerHistory(id: EntityId): Promise<ClueOwnerHistory[]> {
  return httpClient.get<ClueOwnerHistory[]>(`/api/clue/${id}/owner-history`)
}

export function transferClueOwner(
  id: EntityId,
  data: TransferClueOwnerRequest,
): Promise<unknown> {
  return httpClient.put(`/api/clue/${id}/owner`, data)
}

export function closeClue(id: EntityId, data: ClueLifecycleRequest): Promise<unknown> {
  return httpClient.put(`/api/clue/${id}/close`, data)
}

export function restoreClue(id: EntityId, data: ClueLifecycleRequest): Promise<unknown> {
  return httpClient.put(`/api/clue/${id}/restore`, data)
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
export const getClueOwnerHistory = fetchClueOwnerHistory
export const getClueRemarkList = fetchClueRemarkPage
