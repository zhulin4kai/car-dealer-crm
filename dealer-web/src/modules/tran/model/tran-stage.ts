export const TRAN_STAGE = {
  QUOTATION: 'QUOTATION',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  PAYMENT: 'PAYMENT',
  DELIVERY: 'DELIVERY',
  COMPLETED: 'COMPLETED',
  LOST: 'LOST',
  CLOSED: 'CLOSED',
  CANCELLED: 'CANCELLED',
} as const

export type TranStage = (typeof TRAN_STAGE)[keyof typeof TRAN_STAGE]

export type TranStageTagType = 'success' | 'warning' | 'info' | 'primary' | 'danger'

export const TRAN_STAGE_META = {
  [TRAN_STAGE.QUOTATION]: { type: 'info', text: '待报价' },
  [TRAN_STAGE.PENDING]: { type: 'warning', text: '待审批' },
  [TRAN_STAGE.APPROVED]: { type: 'success', text: '已审批' },
  [TRAN_STAGE.PAYMENT]: { type: 'warning', text: '待收款' },
  [TRAN_STAGE.DELIVERY]: { type: 'primary', text: '待交付' },
  [TRAN_STAGE.COMPLETED]: { type: 'success', text: '已完成' },
  [TRAN_STAGE.LOST]: { type: 'danger', text: '丢失关闭' },
  [TRAN_STAGE.CLOSED]: { type: 'danger', text: '已关闭' },
  [TRAN_STAGE.CANCELLED]: { type: 'danger', text: '已取消' },
} as const satisfies Record<TranStage, { type: TranStageTagType; text: string }>

export const TRAN_STAGE_OPTIONS = Object.entries(TRAN_STAGE_META).map(([value, meta]) => ({
  value: value as TranStage,
  label: meta.text,
}))

export function isTranStage(stage: unknown): stage is TranStage {
  return typeof stage === 'string' && stage in TRAN_STAGE_META
}

export function getTranStageType(stage: unknown): TranStageTagType | '' {
  return isTranStage(stage) ? TRAN_STAGE_META[stage].type : ''
}

export function getTranStageText(stage: unknown): string {
  if (isTranStage(stage)) {
    return TRAN_STAGE_META[stage].text
  }

  return typeof stage === 'string' ? stage : ''
}

export function normalizeTranStage(stage: unknown): TranStage {
  return isTranStage(stage) ? stage : TRAN_STAGE.QUOTATION
}
