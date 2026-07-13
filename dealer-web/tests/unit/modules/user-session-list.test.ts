import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { describe, expect, it, vi } from 'vitest'

import UserSessionList from '@/modules/user/components/UserSessionList.vue'
import type { UserSessionCollection } from '@/modules/user/model/user-session.types'

const collection: UserSessionCollection = {
  targetUserId: 21,
  sessionRevision: 4,
  allowedActions: ['REVOKE_OTHERS', 'REVOKE_ALL'],
  unavailableReasons: {},
  sessions: [
    {
      id: 'opaque-current-session-id',
      deviceSummary: 'Mac 桌面设备',
      clientSummary: 'Chrome · macOS',
      networkSummary: '上海 · 192.168.*.*',
      loginTime: '2026-07-11T01:00:00Z',
      lastActivityTime: '2026-07-11T02:00:00Z',
      expiresAt: '2026-07-12T01:00:00Z',
      current: true,
      rememberMe: false,
      allowedActions: ['REVOKE'],
      unavailableReasons: {},
    },
    {
      id: 'opaque-other-session-id',
      deviceSummary: '移动设备',
      clientSummary: 'Safari · iOS',
      networkSummary: '杭州 · 10.0.*.*',
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

describe('user session list', () => {
  it('shows current session and only sanitized client summaries', () => {
    render(UserSessionList, { props: { collection } })

    expect(screen.getByText('当前会话')).toBeTruthy()
    expect(screen.getByText('客户端：Chrome · macOS')).toBeTruthy()
    expect(screen.getByText('网络：上海 · 192.168.*.*')).toBeTruthy()
    expect(screen.getByText('记住登录')).toBeTruthy()
    expect(screen.queryByText('opaque-current-session-id')).toBeNull()
    expect(screen.queryByText(/Bearer\s+|eyJ[a-zA-Z0-9_-]{10,}/)).toBeNull()
  })

  it('emits explicit self-session commands with non-secret reasons', async () => {
    const onRevoke = vi.fn()
    const onRevokeOthers = vi.fn()
    const onRevokeAll = vi.fn()
    render(UserSessionList, {
      props: { collection, onRevoke, onRevokeOthers, onRevokeAll },
    })

    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))
    expect(onRevoke).toHaveBeenCalledWith(collection.sessions[1], '用户主动撤销指定会话')
    await fireEvent.click(screen.getByRole('button', { name: '撤销其他会话' }))
    expect(onRevokeOthers).toHaveBeenCalledWith('用户主动撤销其他会话')
    await fireEvent.click(screen.getByRole('button', { name: '撤销全部会话' }))
    expect(onRevokeAll).toHaveBeenCalledWith('用户主动撤销全部会话')
  })

  it('requires an audit reason for manager revocation', async () => {
    const onRevoke = vi.fn()
    render(UserSessionList, {
      props: { collection, requireReason: true, onRevoke },
    })

    expect((screen.getByRole('button', { name: '撤销会话' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    await fireEvent.update(screen.getByLabelText('撤销原因'), '检测到设备遗失')
    await waitFor(() =>
      expect((screen.getByRole('button', { name: '撤销会话' }) as HTMLButtonElement).disabled).toBe(
        false,
      ),
    )
    await fireEvent.click(screen.getByRole('button', { name: '撤销会话' }))

    expect(onRevoke).toHaveBeenCalledWith(collection.sessions[1], '检测到设备遗失')
  })

  it('keeps server-denied session actions read-only', () => {
    render(UserSessionList, {
      props: {
        collection,
        mutationAllowed: false,
        disabledReason: '目标用户超出当前管理范围',
      },
    })

    expect(screen.getByText('目标用户超出当前管理范围')).toBeTruthy()
    expect((screen.getByRole('button', { name: '撤销会话' }) as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect(
      (screen.getByRole('button', { name: '撤销全部会话' }) as HTMLButtonElement).disabled,
    ).toBe(true)
  })

  it('does not expose mutations omitted from the session response allowed actions', () => {
    render(UserSessionList, {
      props: {
        collection: {
          ...collection,
          allowedActions: [],
          sessions: collection.sessions.map(session => ({
            ...session,
            allowedActions: [],
            unavailableReasons: { REVOKE: '服务端未允许撤销该会话' },
          })),
        },
        mutationAllowed: true,
      },
    })

    expect(screen.queryByRole('button', { name: '撤销其他会话' })).toBeNull()
    expect(screen.queryByRole('button', { name: '撤销全部会话' })).toBeNull()
    expect(screen.queryByRole('button', { name: /撤销会话|退出当前会话/ })).toBeNull()
    expect(screen.getAllByText('服务端未允许撤销该会话')).toHaveLength(2)
  })
})
