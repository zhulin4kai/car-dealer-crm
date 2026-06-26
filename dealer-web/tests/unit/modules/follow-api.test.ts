import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  cancelFollowTask,
  completeFollowTask,
  correctCommunicationRecord,
  createCommunicationRecord,
  createFollowTask,
  fetchCommunicationRecordPage,
  fetchFollowTaskDetail,
  fetchFollowTaskPage,
  postponeFollowTask,
  startFollowTask,
  voidCommunicationRecord,
} from '@/modules/follow/api/follow-api'

const mockedAxios = vi.mocked(axios)

describe('follow api module', () => {
  beforeEach(() => {
    mockedAxios.request.mockClear()
  })

  it('uses stable follow task endpoints', async () => {
    await fetchFollowTaskPage({ page: 1, size: 10, status: 'PENDING' })
    await fetchFollowTaskDetail(8)
    await createFollowTask({
      title: '电话回访',
      taskType: 'PHONE_FOLLOW_UP',
      relatedObjectType: 'CUSTOMER',
      relatedObjectId: 3,
      ownerId: 2,
      dueTime: '2026-07-01T10:00:00',
    })
    await startFollowTask(8)
    await postponeFollowTask(8, {
      newDueTime: '2026-07-02T10:00:00',
      reason: '客户改期',
    })
    await cancelFollowTask(8, { reason: '客户取消' })
    await completeFollowTask(8, {
      communicationMethod: 'PHONE',
      summary: '已电话确认',
      result: '已完成',
    })

    expect(mockedAxios.request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
      ['get', '/api/follow-tasks'],
      ['get', '/api/follow-tasks/8'],
      ['post', '/api/follow-tasks'],
      ['put', '/api/follow-tasks/8/start'],
      ['put', '/api/follow-tasks/8/postpone'],
      ['put', '/api/follow-tasks/8/cancel'],
      ['put', '/api/follow-tasks/8/complete'],
    ])
  })

  it('uses stable communication record endpoints', async () => {
    await fetchCommunicationRecordPage({ page: 1, size: 20, relatedObjectType: 'CUSTOMER' })
    await createCommunicationRecord({
      relatedObjectType: 'CUSTOMER',
      relatedObjectId: 3,
      communicationMethod: 'WECHAT',
      summary: '微信确认到店时间',
    })
    await correctCommunicationRecord(9, {
      communicationMethod: 'PHONE',
      summary: '电话重新确认',
      correctionReason: '摘要修正',
    })
    await voidCommunicationRecord(9, { reason: '误登记' })

    expect(mockedAxios.request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
      ['get', '/api/communication-records'],
      ['post', '/api/communication-records'],
      ['put', '/api/communication-records/9/correct'],
      ['put', '/api/communication-records/9/void'],
    ])
  })
})
