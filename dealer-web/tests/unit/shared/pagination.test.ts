import { describe, expect, it } from 'vitest'

import { normalizePage } from '@/shared/utils/pagination'

describe('normalizePage', () => {
  it('returns 1 when total is 0', () => {
    expect(normalizePage(3, 0, 10)).toBe(1)
  })

  it('clamps page above lastPage', () => {
    expect(normalizePage(5, 45, 10)).toBe(5)
  })

  it('returns 1 for page below 1', () => {
    expect(normalizePage(0, 100, 10)).toBe(1)
  })

  it('returns lastPage when page exceeds it', () => {
    expect(normalizePage(10, 95, 10)).toBe(10)
  })

  it('returns 1 when last page is 1', () => {
    expect(normalizePage(3, 5, 10)).toBe(1)
  })

  it('keeps page 1 when within range', () => {
    expect(normalizePage(1, 100, 10)).toBe(1)
  })
})
