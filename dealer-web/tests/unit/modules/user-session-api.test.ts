import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  fetchManagedUserSessions,
  fetchOwnSessions,
  revokeAllManagedUserSessions,
  revokeOwnOtherSessions,
  revokeOwnSession,
} from '@/modules/user/api/user-session-api'

const mockedAxios = vi.mocked(axios)

describe('user session api', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
    mockedAxios.request.mockResolvedValue({ data: { code: 200, msg: 'OK', data: {} } })
  })

  it('loads self and managed-user sessions from separate resources', async () => {
    await fetchOwnSessions()
    await fetchManagedUserSessions(21)

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'get',
      url: '/api/me/sessions',
      signal: undefined,
    })
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'get',
      url: '/api/users/21/sessions',
      signal: undefined,
    })
  })

  it('revokes a selected session with its session revision and audit reason only', async () => {
    await revokeOwnSession('session/id', {
      sessionRevision: 4,
      reason: '用户主动撤销指定会话',
    })

    expect(mockedAxios.request.mock.calls[0]?.[0]).toEqual({
      method: 'post',
      url: '/api/me/sessions/session%2Fid/revoke',
      data: { sessionRevision: 4, reason: '用户主动撤销指定会话' },
    })
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('token')
    expect(mockedAxios.request.mock.calls[0]?.[0]?.data).not.toHaveProperty('userId')
  })

  it('uses explicit self-other and managed-all commands', async () => {
    await revokeOwnOtherSessions({ sessionRevision: 4, reason: '清理其他设备' })
    await revokeAllManagedUserSessions(21, { sessionRevision: 5, reason: '账号风险处置' })

    expect(mockedAxios.request.mock.calls[0]?.[0]?.url).toBe('/api/me/sessions/revoke-others')
    expect(mockedAxios.request.mock.calls[1]?.[0]).toEqual({
      method: 'post',
      url: '/api/users/21/sessions/revoke-all',
      data: { sessionRevision: 5, reason: '账号风险处置' },
    })
  })
})
