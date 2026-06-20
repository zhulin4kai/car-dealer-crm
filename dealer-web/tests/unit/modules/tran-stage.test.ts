import { describe, expect, it } from 'vitest'

import {
  TRAN_STAGE,
  TRAN_STAGE_OPTIONS,
  getTranStageText,
  getTranStageType,
  normalizeTranStage,
} from '@/modules/tran/model/tran-stage'

describe('tran stage model', () => {
  it('exposes backend string stages as options', () => {
    expect(TRAN_STAGE_OPTIONS).toEqual([
      { value: TRAN_STAGE.QUOTATION, label: '待报价' },
      { value: TRAN_STAGE.PENDING, label: '待审批' },
      { value: TRAN_STAGE.APPROVED, label: '已审批' },
      { value: TRAN_STAGE.PAYMENT, label: '待收款' },
      { value: TRAN_STAGE.COMPLETED, label: '已完成' },
      { value: TRAN_STAGE.LOST, label: '丢失关闭' },
      { value: TRAN_STAGE.CANCELLED, label: '已取消' },
    ])
  })

  it('normalizes unknown stages to quotation', () => {
    expect(normalizeTranStage(41)).toBe(TRAN_STAGE.QUOTATION)
    expect(normalizeTranStage('UNKNOWN')).toBe(TRAN_STAGE.QUOTATION)
    expect(normalizeTranStage(TRAN_STAGE.APPROVED)).toBe(TRAN_STAGE.APPROVED)
  })

  it('maps stage display metadata', () => {
    expect(getTranStageType(TRAN_STAGE.COMPLETED)).toBe('success')
    expect(getTranStageText(TRAN_STAGE.LOST)).toBe('丢失关闭')
    expect(getTranStageType(TRAN_STAGE.CANCELLED)).toBe('danger')
    expect(getTranStageText(TRAN_STAGE.CANCELLED)).toBe('已取消')
    expect(getTranStageText('CUSTOM')).toBe('CUSTOM')
  })
})
