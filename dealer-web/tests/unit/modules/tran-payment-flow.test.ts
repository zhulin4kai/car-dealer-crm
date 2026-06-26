import axios from 'axios'
import fs from 'node:fs'
import path from 'node:path'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  approveRefundRequest,
  confirmPayment,
  createRefundRequest,
  executeRefundRequest,
  recordPayment,
} from '@/modules/tran/api/tran-api'

const mockedAxios = vi.mocked(axios)
const tranDetailPage = path.resolve(__dirname, '../../../src/pages/dashboard/tran/[id].vue')

describe('transaction payment and refund flow', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
  })

  it('registers payment without trusting client amount or payment type', async () => {
    await recordPayment({
      tranId: 9,
      paymentMethod: 'BANK_TRANSFER',
      transactionRef: 'BANK-20260626-001',
      remark: '客户付款',
    })

    expect(mockedAxios.request).toHaveBeenCalledWith({
      data: {
        tranId: 9,
        paymentMethod: 'BANK_TRANSFER',
        transactionRef: 'BANK-20260626-001',
        remark: '客户付款',
      },
      method: 'post',
      url: '/api/tran/payment',
    })
  })

  it('uses separated confirmation, refund request, approval and execution endpoints', async () => {
    await confirmPayment(10, { approved: true, comment: '确认到账' })
    await createRefundRequest(10, {
      refundType: 'ORDER_CANCEL',
      amount: 5000,
      reason: '订单取消退款',
    })
    await approveRefundRequest(99, { approved: true, comment: '同意退款' })
    await executeRefundRequest(99, {
      transactionRef: 'RF-20260626-001',
      success: true,
      remark: '已原路退回',
    })

    expect(mockedAxios.request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
      ['put', '/api/tran/payment/10/confirm'],
      ['post', '/api/tran/payment/10/refund-requests'],
      ['put', '/api/tran/refund-requests/99/approve'],
      ['post', '/api/tran/refund-requests/99/execute'],
    ])
  })

  it('keeps refund labels and cancelled-transaction refund affordance in the transaction detail page', () => {
    const content = fs.readFileSync(tranDetailPage, 'utf8')

    expect(content).toContain("{ value: 'REFUND', label: '退款' }")
    expect(content).toContain('tranDetail.value.stage === TRAN_STAGE.CANCELLED')
    expect(content).toContain('selectedPaymentRefundableAmount')
    expect(content).not.toMatch(/recordPayment\(\{\s*[^}]*\bamount\s*:/s)
  })
})
