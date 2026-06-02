export const TRAN_STAGE = Object.freeze({
  QUOTATION: 'QUOTATION',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  PAYMENT: 'PAYMENT',
  COMPLETED: 'COMPLETED',
  LOST: 'LOST'
})

export const TRAN_STAGE_META = Object.freeze({
  [TRAN_STAGE.QUOTATION]: { type: 'info', text: '待报价' },
  [TRAN_STAGE.PENDING]: { type: 'warning', text: '待审批' },
  [TRAN_STAGE.APPROVED]: { type: 'success', text: '已审批' },
  [TRAN_STAGE.PAYMENT]: { type: 'warning', text: '待收款' },
  [TRAN_STAGE.COMPLETED]: { type: 'success', text: '已完成' },
  [TRAN_STAGE.LOST]: { type: 'danger', text: '丢失关闭' }
})

export const TRAN_STAGE_OPTIONS = Object.freeze(
  Object.entries(TRAN_STAGE_META).map(([value, meta]) => ({
    value,
    label: meta.text
  }))
)

export const getTranStageType = (stage) => TRAN_STAGE_META[stage]?.type || ''

export const getTranStageText = (stage) => TRAN_STAGE_META[stage]?.text || stage || ''

export const normalizeTranStage = (stage) => {
  if (stage && TRAN_STAGE_META[stage]) {
    return stage
  }
  return TRAN_STAGE.QUOTATION
}
