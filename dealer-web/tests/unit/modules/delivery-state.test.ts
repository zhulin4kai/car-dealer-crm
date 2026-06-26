import { describe, expect, it } from 'vitest'

import {
  canCancelDelivery,
  formatDeliveryStatus,
  type DeliveryStatus,
} from '@/modules/delivery/model/delivery.types'

describe('delivery state model', () => {
  it('allows cancellation only before signed terminal states', () => {
    const cancellableStatuses: DeliveryStatus[] = [
      'PENDING_PREPARE',
      'PREPARING',
      'WAITING_CUSTOMER',
      'WAITING_DELIVERY',
      'DELIVERING',
      'EXCEPTION',
    ]

    for (const status of cancellableStatuses) {
      expect(canCancelDelivery(status)).toBe(true)
    }
    expect(canCancelDelivery('SIGNED')).toBe(false)
    expect(canCancelDelivery('COMPLETED')).toBe(false)
    expect(canCancelDelivery('CANCELLED')).toBe(false)
    expect(canCancelDelivery(undefined)).toBe(false)
  })

  it('maps delivery status labels to Chinese text', () => {
    expect(formatDeliveryStatus('EXCEPTION')).toBe('交付异常')
    expect(formatDeliveryStatus('CANCELLED')).toBe('已取消')
  })
})
