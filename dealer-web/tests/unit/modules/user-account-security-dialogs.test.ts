import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'

import UserLoginAccountDialog from '@/modules/user/components/UserLoginAccountDialog.vue'
import UserSecurityExpirationDialog from '@/modules/user/components/UserSecurityExpirationDialog.vue'

describe('managed user account security dialogs', () => {
  afterEach(() => cleanup())

  it('emits a normalized login-account command with CAS and reason', async () => {
    const { emitted } = render(UserLoginAccountDialog, {
      props: { open: true, accountVersion: 7, currentLoginAct: 'sales01' },
    })
    await fireEvent.update(await screen.findByLabelText('新登录账号'), 'Sales.Renamed')
    await fireEvent.update(screen.getByLabelText('变更原因'), '账号规范化')
    await fireEvent.click(screen.getByRole('button', { name: '确认修改' }))

    expect(emitted().submit?.[0]?.[0]).toEqual({
      accountVersion: 7,
      loginAct: 'sales.renamed',
      reason: '账号规范化',
    })
  })

  it('distinguishes account expired state from a nullable credential expiration time', async () => {
    const { emitted } = render(UserSecurityExpirationDialog, {
      props: { open: true, accountVersion: 8, accountExpiresAt: null, credentialExpiresAt: null },
    })
    await fireEvent.update(await screen.findByLabelText('账号到期时间'), '2026-07-31T09:30')
    await fireEvent.update(screen.getByLabelText('凭证到期时间'), '2026-08-01T09:30')
    await fireEvent.update(screen.getByLabelText('变更原因'), '安全策略调整')
    await fireEvent.click(screen.getByRole('button', { name: '保存安全设置' }))

    expect(emitted().submit?.[0]?.[0]).toEqual({
      accountVersion: 8,
      accountExpiresAt: new Date('2026-07-31T09:30').toISOString(),
      credentialExpiresAt: new Date('2026-08-01T09:30').toISOString(),
      reason: '安全策略调整',
    })
  })
})
