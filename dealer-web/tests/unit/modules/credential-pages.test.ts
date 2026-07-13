import { createPinia, setActivePinia } from 'pinia'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ActivatePage from '@/pages/activate.vue'
import FirstPasswordChangePage from '@/pages/first-password-change.vue'
import ForgotPasswordPage from '@/pages/forgot-password.vue'
import ResetPasswordPage from '@/pages/reset-password.vue'
import VerifyContactPage from '@/pages/verify-contact.vue'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { writeStoredToken } from '@/shared/storage/token-storage'
import { useAuthStore } from '@/stores/auth.store'

const credentialMocks = vi.hoisted(() => ({
  activateAccount: vi.fn(),
  changeFirstPassword: vi.fn(),
  requestPasswordReset: vi.fn(),
  resetPassword: vi.fn(),
  verifyContact: vi.fn(),
}))
const userApiMocks = vi.hoisted(() => ({
  fetchLoginInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}))
const routeMock = vi.hoisted(() => ({
  path: '/activate',
  query: {} as Record<string, string | undefined>,
  hash: '#credential=one-time-secret',
}))
const routerMock = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }))

vi.mock('@/modules/user/api/credential-api', () => credentialMocks)
vi.mock('@/modules/user/api/user-api', () => userApiMocks)
vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => routerMock,
}))

describe('credential lifecycle pages', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    Object.values(credentialMocks).forEach((mock) => mock.mockReset())
    Object.values(userApiMocks).forEach((mock) => mock.mockReset())
    routerMock.push.mockReset()
    routerMock.replace.mockReset()
    routeMock.path = '/activate'
    delete routeMock.query.credential
    routeMock.hash = '#credential=one-time-secret'
  })

  afterEach(() => cleanup())

  it('activates with the URL credential but never sends confirmation fields', async () => {
    credentialMocks.activateAccount.mockResolvedValue({ completed: true })
    const localSetItem = vi.spyOn(localStorage, 'setItem')
    const sessionSetItem = vi.spyOn(sessionStorage, 'setItem')
    render(ActivatePage)

    expect(routerMock.replace).toHaveBeenCalledWith({ path: '/activate', query: {}, hash: '' })
    expect(screen.queryByText('one-time-secret')).toBeNull()
    expect(localSetItem).not.toHaveBeenCalled()
    expect(sessionSetItem).not.toHaveBeenCalled()
    localSetItem.mockRestore()
    sessionSetItem.mockRestore()

    await fireEvent.update(screen.getByLabelText('新密码'), 'NewPassword1')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'NewPassword1')
    await fireEvent.click(screen.getByRole('button', { name: '激活账号' }))

    await waitFor(() =>
      expect(credentialMocks.activateAccount).toHaveBeenCalledWith({
        credential: 'one-time-secret',
        newPassword: 'NewPassword1',
      }),
    )
    expect(credentialMocks.activateAccount.mock.calls[0]?.[0]).not.toHaveProperty('confirmPassword')
    expect(await screen.findByText('账号已激活，请使用新密码登录。')).toBeTruthy()
  })

  it('ignores a credential in the query string and clears it from browser history', () => {
    routeMock.query.credential = 'query-secret-must-not-be-used'
    routeMock.hash = ''
    render(ActivatePage)

    expect(routerMock.replace).toHaveBeenCalledWith({ path: '/activate', query: {}, hash: '' })
    expect(screen.getByText('激活链接缺少有效凭证，请重新获取邀请。')).toBeTruthy()
    expect((screen.getByRole('button', { name: '激活账号' }) as HTMLButtonElement).disabled).toBe(true)
    expect(credentialMocks.activateAccount).not.toHaveBeenCalled()
  })

  it.each([
    [API_ERROR_CODE.CREDENTIAL_INVALID, '邀请凭证无效，请重新获取邀请。', true],
    [API_ERROR_CODE.CREDENTIAL_EXPIRED, '邀请凭证已过期，请重新获取邀请。', true],
    [API_ERROR_CODE.CREDENTIAL_ALREADY_USED, '邀请凭证已使用，请重新获取邀请。', true],
    [API_ERROR_CODE.PASSWORD_POLICY_VIOLATION, '新密码不符合服务端密码策略，请调整后重试。', false],
    [API_ERROR_CODE.PASSWORD_HISTORY_REUSED, '新密码与近期使用过的密码重复，请更换后重试。', false],
    [API_ERROR_CODE.CREDENTIAL_RATE_LIMITED, '请求过于频繁，请稍后再试。', false],
  ])('maps activation error code %s without collapsing the failure reason', async (code, message, terminal) => {
    credentialMocks.activateAccount.mockImplementationOnce(() => {
      throw new ApiError(code, 'stable error', null)
    })
    render(ActivatePage)

    await fireEvent.update(screen.getByLabelText('新密码'), 'NewPassword1')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'NewPassword1')
    await fireEvent.click(screen.getByRole('button', { name: '激活账号' }))

    expect(await screen.findByText(message)).toBeTruthy()
    expect(screen.getByRole('button', { name: '激活账号' })).toHaveProperty('disabled', terminal)
  })

  it('shows a retryable system failure without invalidating the activation credential', async () => {
    credentialMocks.activateAccount.mockImplementationOnce(() => {
      throw new Error('network unavailable')
    })
    render(ActivatePage)

    await fireEvent.update(screen.getByLabelText('新密码'), 'NewPassword1')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'NewPassword1')
    await fireEvent.click(screen.getByRole('button', { name: '激活账号' }))

    expect(await screen.findByText('系统暂时不可用，请稍后重试。')).toBeTruthy()
    expect((screen.getByRole('button', { name: '激活账号' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it.each(['possibly-existing-user', 'unknown-user'])(
    'shows the same queued forgot-password result for %s',
    async (loginAct) => {
      credentialMocks.requestPasswordReset.mockResolvedValue({
        accepted: true,
        deliveryStatus: 'QUEUED',
      })
      render(ForgotPasswordPage)

      await fireEvent.update(screen.getByLabelText('登录账号'), loginAct)
      await fireEvent.click(screen.getByRole('button', { name: '提交找回请求' }))

      expect(
        await screen.findByText('如果该账号可以找回密码，系统将通过已配置的安全渠道发送后续指引。'),
      ).toBeTruthy()
      expect(screen.queryByText(/存在|不存在|禁用|锁定/)).toBeNull()
    },
  )

  it('shows a non-leaking retry failure when the forgot-password request cannot be processed', async () => {
    credentialMocks.requestPasswordReset.mockImplementationOnce(() => {
      throw new Error('network failure')
    })
    render(ForgotPasswordPage)

    await fireEvent.update(screen.getByLabelText('登录账号'), 'possibly-existing-user')
    await fireEvent.click(screen.getByRole('button', { name: '提交找回请求' }))

    expect(await screen.findByText('系统暂时无法处理找回请求，请稍后重试。')).toBeTruthy()
    expect(screen.queryByText(/存在|不存在|禁用|锁定/)).toBeNull()
    expect((screen.getByLabelText('登录账号') as HTMLInputElement).value).toBe('possibly-existing-user')
    expect((screen.getByRole('button', { name: '提交找回请求' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('resets a password through a one-time credential and clears the form', async () => {
    credentialMocks.resetPassword.mockResolvedValue({ completed: true })
    routeMock.path = '/reset-password'
    delete routeMock.query.credential
    routeMock.hash = '#credential=fragment-secret'
    render(ResetPasswordPage)

    expect(routerMock.replace).toHaveBeenCalledWith({
      path: '/reset-password',
      query: {},
      hash: '',
    })
    expect(screen.queryByText('fragment-secret')).toBeNull()

    await fireEvent.update(screen.getByLabelText('新密码'), 'ResetPassword1')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'ResetPassword1')
    await fireEvent.click(screen.getByRole('button', { name: '重置密码' }))

    await waitFor(() =>
      expect(credentialMocks.resetPassword).toHaveBeenCalledWith({
        credential: 'fragment-secret',
        newPassword: 'ResetPassword1',
      }),
    )
    expect(await screen.findByText('密码已重置，请重新登录。')).toBeTruthy()
  })

  it('consumes a contact verification credential from the URL fragment', async () => {
    credentialMocks.verifyContact.mockResolvedValue({ completed: true })
    routeMock.path = '/verify-contact'
    routeMock.hash = '#credential=contact-secret'
    render(VerifyContactPage)

    expect(routerMock.replace).toHaveBeenCalledWith({
      path: '/verify-contact',
      query: {},
      hash: '',
    })
    expect(screen.queryByText('contact-secret')).toBeNull()
    await fireEvent.click(screen.getByRole('button', { name: '确认验证' }))

    await waitFor(() =>
      expect(credentialMocks.verifyContact).toHaveBeenCalledWith({
        credential: 'contact-secret',
      }),
    )
    expect(await screen.findByText('联系方式验证完成。')).toBeTruthy()
  })

  it.each([
    [API_ERROR_CODE.CREDENTIAL_EXPIRED, '重置凭证已过期，请重新发起找回。'],
    [API_ERROR_CODE.PASSWORD_HISTORY_REUSED, '新密码与近期使用过的密码重复，请更换后重试。'],
    [API_ERROR_CODE.CREDENTIAL_RATE_LIMITED, '请求过于频繁，请稍后再试。'],
    [500, '系统暂时不可用，请稍后重试。'],
  ])('maps reset-password error code %s', async (code, message) => {
    credentialMocks.resetPassword.mockImplementationOnce(() => {
      throw new ApiError(code, 'stable error', null)
    })
    routeMock.path = '/reset-password'
    routeMock.hash = '#credential=fragment-secret'
    render(ResetPasswordPage)

    await fireEvent.update(screen.getByLabelText('新密码'), 'ResetPassword1')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'ResetPassword1')
    await fireEvent.click(screen.getByRole('button', { name: '重置密码' }))

    expect(await screen.findByText(message)).toBeTruthy()
  })

  it('changes the first password and removes the old local session', async () => {
    credentialMocks.changeFirstPassword.mockResolvedValue({ completed: true })
    writeStoredToken('old-session', false)
    const authStore = useAuthStore()
    authStore.restoreSession()
    render(FirstPasswordChangePage, { global: { plugins: [createPinia()] } })

    await fireEvent.update(screen.getByLabelText('当前密码'), 'InitialPassword1')
    await fireEvent.update(screen.getByLabelText('新密码'), 'ChangedPassword2')
    await fireEvent.update(screen.getByLabelText('确认新密码'), 'ChangedPassword2')
    await fireEvent.click(screen.getByRole('button', { name: '修改密码并重新登录' }))

    await waitFor(() =>
      expect(credentialMocks.changeFirstPassword).toHaveBeenCalledWith({
        currentPassword: 'InitialPassword1',
        newPassword: 'ChangedPassword2',
      }),
    )
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
    expect(routerMock.push).toHaveBeenCalledWith('/')
  })
})
