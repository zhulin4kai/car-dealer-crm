import type { UpdateOwnProfileRequest, UserProfile } from '@/modules/user/model/user-profile.types'
import { httpClient } from '@/shared/api/http-client'

export function fetchOwnProfile(signal?: AbortSignal): Promise<UserProfile> {
  return httpClient.get<UserProfile>('/api/profile', { signal })
}

export function updateOwnProfile(request: UpdateOwnProfileRequest): Promise<UserProfile> {
  return httpClient.put<UserProfile>('/api/profile', request)
}
