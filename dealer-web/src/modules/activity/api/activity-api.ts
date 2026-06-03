import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { SelectOption } from '@/shared/types/common'
import type { EntityId } from '@/shared/types/id'
import type { Activity, ActivityForm, ActivityQuery } from '@/modules/activity/model/activity.types'

export function fetchActivityPage(params: ActivityQuery): Promise<PageResult<Activity>> {
  return httpClient.get<PageResult<Activity>>('/api/activitys', { params })
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

export function createActivity(formData: ActivityForm): Promise<unknown> {
  return httpClient.post('/api/activity', formData)
}

export function updateActivity(formData: ActivityForm): Promise<unknown> {
  return httpClient.put('/api/activity', formData)
}

export const getActivityList = fetchActivityPage
export const getOwnerList = fetchOwnerList
export const getActivityById = fetchActivityById
