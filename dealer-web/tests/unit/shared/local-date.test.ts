import { describe, expect, it } from 'vitest'

import {
  toLocalDateInput,
  fromLocalDateInput,
  toLocalDateTimeInput,
  fromLocalDateTimeInput,
} from '@/shared/datetime/local-date'

describe('local-date utils', () => {
  describe('toLocalDateInput', () => {
    it('extracts date from yyyy-MM-dd HH:mm:ss without UTC conversion', () => {
      expect(toLocalDateInput('2026-06-22 00:00:00')).toBe('2026-06-22')
      expect(toLocalDateInput('2026-06-22 23:59:59')).toBe('2026-06-22')
    })

    it('returns date as-is when already yyyy-MM-dd', () => {
      expect(toLocalDateInput('2026-06-22')).toBe('2026-06-22')
    })

    it('returns empty for null or invalid format', () => {
      expect(toLocalDateInput(null)).toBe('')
      expect(toLocalDateInput('')).toBe('')
      expect(toLocalDateInput('invalid')).toBe('')
    })
  })

  describe('fromLocalDateInput', () => {
    it('generates yyyy-MM-dd 00:00:00 from date input', () => {
      expect(fromLocalDateInput('2026-06-22')).toBe('2026-06-22 00:00:00')
    })

    it('returns null for empty or invalid input', () => {
      expect(fromLocalDateInput('')).toBeNull()
      expect(fromLocalDateInput('invalid')).toBeNull()
    })
  })

  describe('toLocalDateTimeInput', () => {
    it('converts yyyy-MM-dd HH:mm:ss to datetime-local format', () => {
      expect(toLocalDateTimeInput('2026-06-22 14:30:00')).toBe('2026-06-22T14:30')
    })

    it('handles ISO-style with T separator', () => {
      expect(toLocalDateTimeInput('2026-06-22T14:30:00')).toBe('2026-06-22T14:30')
    })

    it('returns empty for null or invalid', () => {
      expect(toLocalDateTimeInput(null)).toBe('')
      expect(toLocalDateTimeInput('')).toBe('')
      expect(toLocalDateTimeInput('invalid')).toBe('')
    })
  })

  describe('fromLocalDateTimeInput', () => {
    it('converts datetime-local to yyyy-MM-dd HH:mm:ss', () => {
      expect(fromLocalDateTimeInput('2026-06-22T14:30')).toBe('2026-06-22 14:30:00')
    })

    it('returns null for empty or invalid', () => {
      expect(fromLocalDateTimeInput('')).toBeNull()
      expect(fromLocalDateTimeInput('invalid')).toBeNull()
    })
  })

  describe('round-trip consistency', () => {
    it('GMT+8 midnight does not roll back a day', () => {
      const serverValue = '2026-06-22 00:00:00'
      const input = toLocalDateTimeInput(serverValue)
      const back = fromLocalDateTimeInput(input)
      expect(back).toBe('2026-06-22 00:00:00')
    })

    it('end of day does not roll forward', () => {
      const serverValue = '2026-06-22 23:59:59'
      const input = toLocalDateTimeInput(serverValue)
      expect(input).toBe('2026-06-22T23:59')
    })
  })
})
