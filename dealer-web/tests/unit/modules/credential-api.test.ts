import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  activateAccount,
  changeOwnPassword,
  changeFirstPassword,
  reinviteManagedUser,
  requestPasswordReset,
  requestContactVerification,
  resetPassword,
  verifyContact,
} from '@/modules/user/api/credential-api'

const mockedAxios = vi.mocked(axios)

describe('credential api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({
      data: { code: 200, msg: 'OK', data: { completed: true } },
    })
  })

  it('sends invitation credentials only in the activation body', async () => {
    await activateAccount({ credential: 'invite-secret', newPassword: 'NewPassword1' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/credentials/activate',
      data: { credential: 'invite-secret', newPassword: 'NewPassword1' },
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.url).not.toContain('invite-secret')
  })

  it('uses one indistinguishable forgot-password command', async () => {
    await requestPasswordReset({ loginAct: 'unknown-or-known' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/credentials/forgot-password',
      data: { loginAct: 'unknown-or-known' },
    })
  })

  it('re-invites a managed user with the account aggregate version and no client token', async () => {
    await reinviteManagedUser(21, { accountVersion: 7, reason: '原邀请已过期' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/users/21/invitation',
      data: { accountVersion: 7, reason: '原邀请已过期' },
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('credential')
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('token')
  })

  it('sends reset credentials and the new password without confirmation fields', async () => {
    await resetPassword({ credential: 'reset-secret', newPassword: 'AnotherPassword1' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/credentials/reset-password',
      data: { credential: 'reset-secret', newPassword: 'AnotherPassword1' },
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('confirmPassword')
  })

  it('uses the authenticated first-password-change endpoint', async () => {
    await changeFirstPassword({
      currentPassword: 'InitialPassword1',
      newPassword: 'ChangedPassword2',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/credentials/first-password-change',
      data: { currentPassword: 'InitialPassword1', newPassword: 'ChangedPassword2' },
    })
  })

  it('uses a separate current-password-verified command for own password changes', async () => {
    await changeOwnPassword({
      currentPassword: 'CurrentPassword1',
      newPassword: 'ChangedPassword2',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'put',
      url: '/api/credentials/change-password',
      data: { currentPassword: 'CurrentPassword1', newPassword: 'ChangedPassword2' },
    })
  })

  it('uses separate authenticated issue and public consume commands for contact verification', async () => {
    await requestContactVerification({ channel: 'EMAIL' })
    await verifyContact({ credential: 'contact-secret' })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/profile/contact-verification',
      data: { channel: 'EMAIL' },
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'post',
      url: '/api/credentials/verify-contact',
      data: { credential: 'contact-secret' },
    })
  })
})
