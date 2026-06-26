import { httpClient } from '@/shared/api/http-client'
import type { DownloadResult, PageResult } from '@/shared/api/api-types'
import type { SelectOption } from '@/shared/types/common'
import type { EntityId } from '@/shared/types/id'
import type {
  Activity,
  ActivityForm,
  ActivityQuery,
  ActivityRemark,
  ActivityRoi,
  ReviewActivityForm,
} from '@/modules/activity/model/activity.types'

export function fetchActivityPage(params: ActivityQuery): Promise<PageResult<Activity>> {
  return httpClient.get<PageResult<Activity>>('/api/activities', { params })
}

export function fetchOwnerList(): Promise<SelectOption[]> {
  return httpClient.get<SelectOption[]>('/api/owner')
}

export function batchDeleteActivities(ids: EntityId[]): Promise<unknown> {
  return httpClient.post('/api/activity/batch', ids)
}

export function deleteActivity(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/activity/${id}`)
}

export function fetchActivityById(id: EntityId): Promise<Activity> {
  return httpClient.get<Activity>(`/api/activity/${id}`)
}

export function fetchActivityRoi(id: EntityId): Promise<ActivityRoi> {
  return httpClient.get<ActivityRoi>(`/api/activity/${id}/roi`)
}

export function createActivity(formData: ActivityForm): Promise<unknown> {
  return httpClient.post('/api/activity', formData)
}

export function updateActivity(formData: ActivityForm): Promise<unknown> {
  return httpClient.put('/api/activity', formData)
}

export function publishActivity(id: EntityId): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/publish`)
}

export function startActivity(id: EntityId): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/start`)
}

export function endActivity(id: EntityId): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/end`)
}

export function reviewActivity(id: EntityId, data: ReviewActivityForm): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/review`, data)
}

export function cancelActivity(id: EntityId, reason: string): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/cancel`, { reason })
}

export function closeActivity(id: EntityId, reason: string): Promise<Activity> {
  return httpClient.put<Activity>(`/api/activity/${id}/close`, { reason })
}

export function exportActivities(params: ActivityQuery): Promise<DownloadResult> {
  return httpClient.download('/api/activity/export', { params })
}

export function createActivityRemark(activityId: EntityId, noteContent: string): Promise<unknown> {
  return httpClient.post('/api/activity/remark', { activityId, noteContent })
}

export function fetchActivityRemarkPage(current: number, activityId: EntityId): Promise<PageResult<ActivityRemark>> {
  return httpClient.get<PageResult<ActivityRemark>>('/api/activity/remark', {
    params: { current, activityId },
  })
}

export function deleteActivityRemark(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/activity/remark/${id}`)
}

export const getActivityList = fetchActivityPage
export const getOwnerList = fetchOwnerList
export const getActivityById = fetchActivityById
