import { describe, expect, it } from 'vitest'

import {
  isUserLifecycleConflict,
  isUserLifecycleSnapshotExpired,
} from '@/modules/user/model/user-lifecycle-error'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

describe('user lifecycle error contract', () => {
  it.each([
    API_ERROR_CODE.USER_LIFECYCLE_CONFLICT,
    API_ERROR_CODE.USER_HANDOVER_QUALIFICATION_CHANGED,
    API_ERROR_CODE.USER_HANDOVER_COUNT_MISMATCH,
    API_ERROR_CODE.USER_HANDOVER_SCHEDULE_CONFLICT,
  ])('treats stable conflict code %s as refresh-required', (code) => {
    expect(isUserLifecycleConflict(new ApiError(code, 'conflict', null))).toBe(true)
  })

  it('recognizes the stable snapshot-expired code without relying on a numeric HTTP code', () => {
    const error = new ApiError(
      API_ERROR_CODE.USER_LIFECYCLE_SNAPSHOT_EXPIRED,
      'expired',
      null,
    )
    expect(isUserLifecycleSnapshotExpired(error)).toBe(true)
    expect(isUserLifecycleConflict(error)).toBe(false)
  })
})
