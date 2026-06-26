import type { PageQuery } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'

export type OpportunityStage =
  | 'INITIAL_CONTACT'
  | 'NEEDS_ANALYSIS'
  | 'VEHICLE_MATCHING'
  | 'TEST_DRIVE_INVITED'
  | 'QUOTING'
  | 'NEGOTIATION'
  | 'PENDING_APPROVAL'
  | 'WON'
  | 'LOST'
  | 'SHELVED'
  | 'CLOSED'

export interface Opportunity {
  id: EntityId
  opportunityNo: string
  customerId: EntityId
  customerName?: string
  clueId?: EntityId
  ownerId: EntityId
  ownerName?: string
  productId?: EntityId
  productName?: string
  sourceType?: string
  stage: OpportunityStage
  requirement: string
  expectedAmount?: number | string
  expectedCloseDate?: string
  nextActionTime?: string
  lostReason?: string
  lostCompetitor?: string
  resultRemark?: string
  orderTranId?: EntityId
  version?: number
  createTime?: string
  createBy?: EntityId
  updateTime?: string
  updateBy?: EntityId
}

export interface OpportunityStageHistory {
  id: EntityId
  opportunityId: EntityId
  fromStage?: OpportunityStage
  toStage: OpportunityStage
  reason: string
  operateBy: EntityId
  operateTime: string
}

export interface CreateOpportunityRequest {
  customerId: EntityId
  clueId?: EntityId
  productId?: EntityId
  sourceType?: string
  requirement: string
  expectedAmount?: number | string
  expectedCloseDate?: string
  nextActionTime?: string
}

export interface UpdateOpportunityRequest {
  id: EntityId
  productId?: EntityId
  requirement: string
  expectedAmount?: number | string
  expectedCloseDate?: string
  nextActionTime?: string
}

export interface AdvanceOpportunityStageRequest {
  expectedStage: OpportunityStage
  targetStage: OpportunityStage
  reason: string
  nextActionTime?: string
}

export interface OpportunityResultRequest {
  orderTranId?: EntityId
  reason: string
  competitor?: string
  remark?: string
  nextActionTime?: string
}

export interface OpportunityQuery extends PageQuery {
  customerId?: EntityId
  ownerId?: EntityId
  stage?: OpportunityStage
  keyword?: string
}

export const OPPORTUNITY_STAGE_OPTIONS: Array<{ value: OpportunityStage; label: string }> = [
  { value: 'INITIAL_CONTACT', label: '初步接触' },
  { value: 'NEEDS_ANALYSIS', label: '需求确认' },
  { value: 'VEHICLE_MATCHING', label: '车型匹配' },
  { value: 'TEST_DRIVE_INVITED', label: '试驾邀约' },
  { value: 'QUOTING', label: '报价中' },
  { value: 'NEGOTIATION', label: '价格协商' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'WON', label: '已赢单' },
  { value: 'LOST', label: '已输单' },
  { value: 'SHELVED', label: '已搁置' },
  { value: 'CLOSED', label: '已关闭' },
]

export function formatOpportunityStage(stage?: string): string {
  return OPPORTUNITY_STAGE_OPTIONS.find((option) => option.value === stage)?.label ?? stage ?? '--'
}

export function getOpportunityStageTone(
  stage?: string,
): 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'purple' {
  if (stage === 'WON') return 'success'
  if (stage === 'LOST' || stage === 'CLOSED') return 'danger'
  if (stage === 'SHELVED') return 'muted'
  if (stage === 'PENDING_APPROVAL' || stage === 'NEGOTIATION' || stage === 'QUOTING') return 'warning'
  if (stage === 'TEST_DRIVE_INVITED' || stage === 'VEHICLE_MATCHING') return 'info'
  return 'purple'
}

export function isOpportunityTerminal(stage?: OpportunityStage): boolean {
  return stage === 'WON' || stage === 'LOST' || stage === 'CLOSED'
}

export function isOpportunityRestorable(stage?: OpportunityStage): boolean {
  return stage === 'LOST' || stage === 'SHELVED'
}
