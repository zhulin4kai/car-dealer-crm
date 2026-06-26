import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  CancelFollowTaskRequest,
  CommunicationRecord,
  CommunicationRecordQuery,
  CompleteFollowTaskRequest,
  CorrectCommunicationRecordRequest,
  CreateCommunicationRecordRequest,
  CreateFollowTaskRequest,
  FollowTask,
  FollowTaskQuery,
  PostponeFollowTaskRequest,
  VoidCommunicationRecordRequest,
} from '@/modules/follow/model/follow.types'

export function fetchFollowTaskPage(params: FollowTaskQuery): Promise<PageResult<FollowTask>> {
  return httpClient.get<PageResult<FollowTask>>('/api/follow-tasks', { params })
}

export function fetchFollowTaskDetail(id: EntityId): Promise<FollowTask> {
  return httpClient.get<FollowTask>(`/api/follow-tasks/${id}`)
}

export function createFollowTask(data: CreateFollowTaskRequest): Promise<FollowTask> {
  return httpClient.post<FollowTask>('/api/follow-tasks', data)
}

export function startFollowTask(id: EntityId): Promise<FollowTask> {
  return httpClient.put<FollowTask>(`/api/follow-tasks/${id}/start`)
}

export function postponeFollowTask(id: EntityId, data: PostponeFollowTaskRequest): Promise<FollowTask> {
  return httpClient.put<FollowTask>(`/api/follow-tasks/${id}/postpone`, data)
}

export function cancelFollowTask(id: EntityId, data: CancelFollowTaskRequest): Promise<FollowTask> {
  return httpClient.put<FollowTask>(`/api/follow-tasks/${id}/cancel`, data)
}

export function completeFollowTask(id: EntityId, data: CompleteFollowTaskRequest): Promise<FollowTask> {
  return httpClient.put<FollowTask>(`/api/follow-tasks/${id}/complete`, data)
}

export function fetchCommunicationRecordPage(
  params: CommunicationRecordQuery,
): Promise<PageResult<CommunicationRecord>> {
  return httpClient.get<PageResult<CommunicationRecord>>('/api/communication-records', { params })
}

export function createCommunicationRecord(
  data: CreateCommunicationRecordRequest,
): Promise<CommunicationRecord> {
  return httpClient.post<CommunicationRecord>('/api/communication-records', data)
}

export function correctCommunicationRecord(
  id: EntityId,
  data: CorrectCommunicationRecordRequest,
): Promise<CommunicationRecord> {
  return httpClient.put<CommunicationRecord>(`/api/communication-records/${id}/correct`, data)
}

export function voidCommunicationRecord(
  id: EntityId,
  data: VoidCommunicationRecordRequest,
): Promise<CommunicationRecord> {
  return httpClient.put<CommunicationRecord>(`/api/communication-records/${id}/void`, data)
}
