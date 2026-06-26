import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  AdvanceOpportunityStageRequest,
  CreateOpportunityRequest,
  Opportunity,
  OpportunityQuery,
  OpportunityResultRequest,
  OpportunityStageHistory,
  UpdateOpportunityRequest,
} from '@/modules/opportunity/model/opportunity.types'

export function fetchOpportunityPage(params: OpportunityQuery): Promise<PageResult<Opportunity>> {
  return httpClient.get<PageResult<Opportunity>>('/api/opportunities', { params })
}

export function fetchOpportunityDetail(id: EntityId): Promise<Opportunity> {
  return httpClient.get<Opportunity>(`/api/opportunities/${id}`)
}

export function createOpportunity(data: CreateOpportunityRequest): Promise<Opportunity> {
  return httpClient.post<Opportunity>('/api/opportunities', data)
}

export function updateOpportunity(id: EntityId, data: UpdateOpportunityRequest): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}`, data)
}

export function fetchOpportunityStageHistory(id: EntityId): Promise<OpportunityStageHistory[]> {
  return httpClient.get<OpportunityStageHistory[]>(`/api/opportunities/${id}/stage-history`)
}

export function advanceOpportunityStage(
  id: EntityId,
  data: AdvanceOpportunityStageRequest,
): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}/stage`, data)
}

export function markOpportunityWon(id: EntityId, data: OpportunityResultRequest): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}/won`, data)
}

export function markOpportunityLost(id: EntityId, data: OpportunityResultRequest): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}/lost`, data)
}

export function shelveOpportunity(id: EntityId, data: OpportunityResultRequest): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}/shelve`, data)
}

export function restoreOpportunity(id: EntityId, data: OpportunityResultRequest): Promise<Opportunity> {
  return httpClient.put<Opportunity>(`/api/opportunities/${id}/restore`, data)
}
