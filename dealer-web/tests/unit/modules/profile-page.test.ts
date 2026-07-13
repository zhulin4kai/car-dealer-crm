import { createPinia, setActivePinia } from 'pinia'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ProfilePage from '@/pages/dashboard/profile.vue'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { writeStoredToken } from '@/shared/storage/token-storage'
import { useAuthStore } from '@/stores/auth.store'

const profileApiMocks = vi.hoisted(() => ({
  fetchOwnProfile: vi.fn(),
  updateOwnProfile: vi.fn(),
}))
const credentialMocks = vi.hoisted(() => ({
  changeOwnPassword: vi.fn(),
  requestContactVerification: vi.fn(),
}))
const sessionMocks = vi.hoisted(() => ({
  fetchOwnSessions: vi.fn(),
  revokeAllOwnSessions: vi.fn(),
  revokeOwnOtherSessions: vi.fn(),
  revokeOwnSession: vi.fn(),
}))
const feedbackMocks = vi.hoisted(() => ({ messageConfirm: vi.fn(), messageTip: vi.fn() }))
const routerMock = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/modules/user/api/user-profile-api', () => profileApiMocks)
vi.mock('@/modules/user/api/credential-api', () => credentialMocks)
vi.mock('@/modules/user/api/user-session-api', () => sessionMocks)
vi.mock('@/shared/utils/feedback', () => feedbackMocks)
vi.mock('vue-router', () => ({ useRouter: () => routerMock }))

const profile = {
  id: 21,
  loginAct: 'sales01',
  name: '李销售',
  phone: '13800138000',
  email: 'sales@example.com',
  phoneVerified: false,
  emailVerified: true,
  avatarUrl: null,
  employeeNo: 'E00021',
  employmentStatus: 'ACTIVE',
  organizationName: '上海门店',
  positionName: '销售顾问',
  managerName: '王主管',
  roles: [
    {
      id: 8,
      code: 'sales',
      name: '销售人员',
      sourceDescription: '直接分配',
    },
  ],
  effectivePermissions: [
    {
      permissionCode: 'customer:view',
      permissionName: '查看客户',
      sourceNames: ['销售人员'],
      dataScopeLabel: '本人',
      sources: [
        {
          sourceType: 'ROLE',
          sourceName: '销售人员',
          dataScopeCode: 'SELF',
          dataScopeLabel: '本人',
          effectiveFrom: '2026-01-01T09:00:00',
          effectiveTo: null,
          organizations: [],
        },
        {
          sourceType: 'PERSONAL_GRANT',
          sourceName: '个人授权',
          dataScopeCode: 'CUSTOM_ORGS',
          dataScopeLabel: '指定组织',
          effectiveFrom: '2026-03-01T09:00:00',
          effectiveTo: '2026-12-31T23:59:00',
          organizations: [
            { id: 10, code: 'STORE_SH', name: '上海门店' },
            { id: 11, code: 'STORE_HZ', name: '杭州门店' },
          ],
        },
      ],
    },
  ],
  profileVersion: 3,
}

const sessions = {
  targetUserId: 21,
  sessionRevision: 4,
  allowedActions: ['REVOKE_OTHERS', 'REVOKE_ALL'],
  unavailableReasons: {},
  sessions: [
    {
      id: 'current-session',
      deviceSummary: 'Mac 桌面设备',
      clientSummary: 'Chrome 浏览器',
      networkSummary: '上海 · 已脱敏网络',
      loginTime: '2026-07-11T01:00:00Z',
      lastActivityTime: '2026-07-11T02:00:00Z',
      expiresAt: '2026-07-12T01:00:00Z',
      current: true,
      rememberMe: false,
      allowedActions: ['REVOKE'],
      unavailableReasons: {},
    },
    {
      id: 'other-session',
      deviceSummary: '移动设备',
      clientSummary: 'Safari 浏览器',
      networkSummary: '杭州 · 已脱敏网络',
      loginTime: '2026-07-10T01:00:00Z',
      lastActivityTime: '2026-07-10T02:00:00Z',
      expiresAt: '2026-07-17T01:00:00Z',
      current: false,
      rememberMe: true,
      allowedActions: ['REVOKE'],
      unavailableReasons: {},
    },
  ],
}

describe('profile page', () => {
  beforeEach(() => {
    Object.values(profileApiMocks).forEach((mock) => mock.mockReset())
    credentialMocks.changeOwnPassword.mockReset()
    credentialMocks.requestContactVerification.mockReset()
    Object.values(sessionMocks).forEach((mock) => mock.mockReset())
    Object.values(feedbackMocks).forEach((mock) => mock.mockReset())
    routerMock.push.mockReset()
    localStorage.clear()
    sessionStorage.clear()
    profileApiMocks.fetchOwnProfile.mockResolvedValue(profile)
    sessionMocks.fetchOwnSessions.mockResolvedValue(sessions)
    feedbackMocks.messageConfirm.mockResolvedValue(undefined)
  })

  afterEach(() => cleanup())

  it('shows organization and authorization facts as read-only', async () => {
    renderPage()

    expect(await screen.findByText(/组织：上海门店/)).toBeTruthy()
    expect(screen.getByText(/岗位：销售顾问/)).toBeTruthy()
    expect(screen.getByText(/直属管理者：王主管/)).toBeTruthy()
    expect(screen.getAllByText(/销售人员/).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('查看客户')).toBeTruthy()
    expect(screen.getByText('角色：销售人员')).toBeTruthy()
    expect(screen.getByText('直接个人授权')).toBeTruthy()
    expect(screen.getByText('上海门店（STORE_SH · ID 10）')).toBeTruthy()
    expect(screen.getByText('杭州门店（STORE_HZ · ID 11）')).toBeTruthy()
    expect(screen.getByText(/自 1月1日 09:00，无固定结束时间/)).toBeTruthy()
    expect(screen.getByText(/自 3月1日 09:00，至 12月31日 23:59/)).toBeTruthy()
    expect(screen.queryByLabelText('登录账号')).toBeNull()
    expect(screen.queryByRole('button', { name: /角色|权限|组织|岗位/ })).toBeNull()
  })

  it('shows verification state and requests a single contact channel', async () => {
    credentialMocks.requestContactVerification.mockResolvedValue({
      accepted: true,
      deliveryStatus: 'QUEUED',
    })
    renderPage()

    expect(await screen.findByText('联系方式验证')).toBeTruthy()
    expect(screen.getByText('未验证')).toBeTruthy()
    expect(screen.getByText('已验证')).toBeTruthy()
    const buttons = screen.getAllByRole('button', { name: '发送验证' })
    expect(buttons).toHaveLength(2)
    expect(buttons[1]).toHaveProperty('disabled', true)
    await fireEvent.click(buttons[0]!)

    await waitFor(() =>
      expect(credentialMocks.requestContactVerification).toHaveBeenCalledWith({ channel: 'PHONE' }),
    )
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      '如果当前渠道可用，系统将发送一次性验证链接',
      'success',
    )
  })

  it('submits only the profile whitelist and refreshes the auth-store display', async () => {
    const updated = {
      ...profile,
      name: '李顾问',
      phone: '13900139000',
      avatarUrl: 'https://example.com/avatar.png',
      profileVersion: 4,
    }
    profileApiMocks.updateOwnProfile.mockResolvedValue(updated)
    const { pinia } = renderPage()

    await screen.findByDisplayValue('李销售')
    await fireEvent.update(screen.getByLabelText('姓名'), '李顾问')
    await fireEvent.update(screen.getByLabelText('手机'), '139 0013-9000')
    await fireEvent.update(screen.getByLabelText('头像地址'), 'https://example.com/avatar.png')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人资料' }))

    await waitFor(() =>
      expect(profileApiMocks.updateOwnProfile).toHaveBeenCalledWith({
        profileVersion: 3,
        name: '李顾问',
        phone: '13900139000',
        email: 'sales@example.com',
        avatarUrl: 'https://example.com/avatar.png',
      }),
    )
    const request = profileApiMocks.updateOwnProfile.mock.calls[0]?.[0]
    expect(Object.values(request ?? {})).not.toContain(undefined)
    expect(request).not.toHaveProperty('loginAct')
    expect(request).not.toHaveProperty('employeeNo')
    expect(request).not.toHaveProperty('organizationName')
    expect(request).not.toHaveProperty('positionName')
    expect(request).not.toHaveProperty('roles')
    expect(request).not.toHaveProperty('effectivePermissions')
    expect(request).not.toHaveProperty('accountEnabled')
    expect(useAuthStore(pinia).currentUser?.name).toBe('李顾问')
    expect(useAuthStore(pinia).currentUser?.avatarUrl).toBe('https://example.com/avatar.png')
  })

  it.each([
    ['姓名', '超'.repeat(51), '姓名最多 50 个字符'],
    ['手机', '123 (456)', '手机号码格式有误'],
    ['头像地址', 'data:image/svg+xml,test', '头像地址必须使用 HTTP 或 HTTPS'],
  ])('rejects invalid %s without sending a partial update', async (label, value, message) => {
    renderPage()

    await screen.findByDisplayValue('李销售')
    await fireEvent.update(screen.getByLabelText(label), value)
    await fireEvent.click(screen.getByRole('button', { name: '保存个人资料' }))

    expect(await screen.findByText(message)).toBeTruthy()
    expect(profileApiMocks.updateOwnProfile).not.toHaveBeenCalled()
  })

  it.each([
    ['旧业务版本码', new ApiError(API_ERROR_CODE.ROLE_VERSION_CONFLICT, 'conflict', null)],
    ['资料版本码', new ApiError(API_ERROR_CODE.PROFILE_VERSION_CONFLICT, 'conflict', null)],
    ['HTTP 状态', new ApiError(550, 'conflict', null, false, 409)],
  ])('refreshes the latest profile after a version conflict reported by %s', async (_, error) => {
    const latest = { ...profile, name: '服务端最新姓名', profileVersion: 4 }
    profileApiMocks.fetchOwnProfile
      .mockResolvedValueOnce(profile)
      .mockResolvedValueOnce(latest)
    profileApiMocks.updateOwnProfile.mockRejectedValue(error)
    renderPage()

    await screen.findByDisplayValue('李销售')
    await fireEvent.update(screen.getByLabelText('姓名'), '本地过期姓名')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人资料' }))

    await waitFor(() => expect(profileApiMocks.fetchOwnProfile).toHaveBeenCalledTimes(2))
    expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
      '个人资料已被更新，页面将刷新最新内容',
      'error',
    )
    expect(await screen.findByDisplayValue('服务端最新姓名')).toBeTruthy()
  })

  it('keeps the auth store and form draft unchanged when profile update fails', async () => {
    profileApiMocks.updateOwnProfile.mockRejectedValue(new Error('update failed'))
    const { pinia } = renderPage()
    const authStore = useAuthStore(pinia)
    authStore.applyCurrentUserProfile(profile)

    await screen.findByDisplayValue('李销售')
    await fireEvent.update(screen.getByLabelText('姓名'), '未保存姓名')
    await fireEvent.click(screen.getByRole('button', { name: '保存个人资料' }))

    await waitFor(() =>
      expect(feedbackMocks.messageTip).toHaveBeenCalledWith('个人资料保存失败', 'error'),
    )
    expect(authStore.currentUser?.name).toBe('李销售')
    expect((screen.getByLabelText('姓名') as HTMLInputElement).value).toBe('未保存姓名')
  })

  it('retains password input after failure and removes the old session after success', async () => {
    credentialMocks.changeOwnPassword.mockRejectedValueOnce(new Error('wrong current password'))
    credentialMocks.changeOwnPassword.mockResolvedValueOnce({ completed: true })
    writeStoredToken('old-session', false)
    renderPage()

    await screen.findByText('修改密码')
    await fillPasswordForm()
    await fireEvent.click(screen.getByRole('button', { name: '修改自己的密码' }))

    expect(
      await screen.findByText('密码修改失败，请确认当前密码，并使用未在近期使用过的新密码。'),
    ).toBeTruthy()
    expect((screen.getByLabelText('当前密码') as HTMLInputElement).value).toBe('CurrentPassword1')

    await fireEvent.click(screen.getByRole('button', { name: '修改自己的密码' }))
    await waitFor(() => expect(routerMock.push).toHaveBeenCalledWith('/'))
    expect(credentialMocks.changeOwnPassword).toHaveBeenLastCalledWith({
      currentPassword: 'CurrentPassword1',
      newPassword: 'ChangedPassword2',
    })
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
  })

  it('revokes other sessions with the server version while keeping the current session', async () => {
    sessionMocks.revokeOwnOtherSessions.mockResolvedValue({
      ...sessions,
      sessionRevision: 5,
      sessions: [sessions.sessions[0]],
    })
    writeStoredToken('current-session-token', false)
    renderPage()

    await screen.findByText('Mac 桌面设备')
    await fireEvent.click(screen.getByRole('button', { name: '撤销其他会话' }))

    await waitFor(() =>
      expect(sessionMocks.revokeOwnOtherSessions).toHaveBeenCalledWith({
        sessionRevision: 4,
        reason: '用户主动撤销其他会话',
      }),
    )
    expect(screen.getByText('Mac 桌面设备')).toBeTruthy()
    expect(screen.queryByText('移动设备')).toBeNull()
    expect(sessionStorage.getItem('dlyk_token')).toBe('current-session-token')
  })

  it('treats revoking the current session as logout', async () => {
    sessionMocks.revokeOwnSession.mockResolvedValue({
      ...sessions,
      sessionRevision: 5,
      sessions: [sessions.sessions[1]],
    })
    writeStoredToken('current-session-token', false)
    renderPage()

    await screen.findByText('Mac 桌面设备')
    await fireEvent.click(screen.getByRole('button', { name: '退出当前会话' }))

    await waitFor(() =>
      expect(sessionMocks.revokeOwnSession).toHaveBeenCalledWith('current-session', {
        sessionRevision: 4,
        reason: '用户主动退出当前会话',
      }),
    )
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
    expect(routerMock.push).toHaveBeenCalledWith('/')
  })

  it('does not show false success or remove a session when revocation fails', async () => {
    sessionMocks.revokeOwnSession.mockRejectedValue(new Error('redis delete failed'))
    renderPage()

    await screen.findByText('移动设备')
    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))

    await waitFor(() =>
      expect(feedbackMocks.messageTip).toHaveBeenCalledWith('会话撤销失败，请稍后重试', 'error'),
    )
    expect(screen.getByText('移动设备')).toBeTruthy()
    expect(feedbackMocks.messageTip).not.toHaveBeenCalledWith('会话已撤销', 'success')
  })

  it('shows 635 precisely without refreshing a list that may still be authoritative', async () => {
    sessionMocks.revokeOwnSession.mockRejectedValue(
      new ApiError(API_ERROR_CODE.SESSION_CACHE_FAILED, 'cache failed', null, false, 503),
    )
    renderPage()

    await screen.findByText('移动设备')
    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))

    await waitFor(() =>
      expect(feedbackMocks.messageTip).toHaveBeenCalledWith(
        '会话服务暂不可用，请稍后重试',
        'error',
      ),
    )
    expect(sessionMocks.fetchOwnSessions).toHaveBeenCalledTimes(1)
  })
})

function renderPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  render(ProfilePage, { global: { plugins: [pinia] } })
  return { pinia }
}

async function fillPasswordForm(): Promise<void> {
  await fireEvent.update(screen.getByLabelText('当前密码'), 'CurrentPassword1')
  await fireEvent.update(screen.getByLabelText('新密码'), 'ChangedPassword2')
  await fireEvent.update(screen.getByLabelText('确认新密码'), 'ChangedPassword2')
}
