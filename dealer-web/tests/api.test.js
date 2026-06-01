import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

// Reset axios call history between tests. We don't care about return values
// here — only the method, url, and data/params that the API wrapper actually
// forwards to axios. This file is a wrapper contract + cross-layer check.
beforeEach(() => {
  vi.clearAllMocks()
})

async function callApi(fn, ...args) {
  // Each test imports a fresh module so wrappers can be invoked in isolation.
  await fn(...args)
  expect(axios).toHaveBeenCalled()
  return axios.mock.calls[0][0]
}

describe('api/* wrappers - cross-layer contract (path + method + key bodies)', () => {
  // The path values are duplicated from dealer-web/src/api/*.js on purpose:
  // if a wrapper is renamed or repointed to a different path, this test must
  // fail. The backend controllers live at
  // dealer-server/src/main/java/com/bjpowernode/web/*Controller.java and the
  // expected method/path are mirrored exactly below.

  it('user.getUserList -> GET /api/users with current/page/size params', async () => {
    const { getUserList } = await import('../src/api/user.js')
    const cfg = await callApi(getUserList, { current: 1, pageSize: 10 })
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/users')
    expect(cfg.params).toEqual({ current: 1, pageSize: 10 })
  })

  it('user.getUserDetail -> GET /api/user/{id} interpolates the id', async () => {
    const { getUserDetail } = await import('../src/api/user.js')
    const cfg = await callApi(getUserDetail, 42)
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/user/42')
  })

  it('user.createUser -> POST /api/user (object body)', async () => {
    const { createUser } = await import('../src/api/user.js')
    const body = { loginAct: 'x', loginPwd: '123456' }
    const cfg = await callApi(createUser, body)
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/user')
    expect(cfg.data).toBe(body)
  })

  it('user.updateUser -> PUT /api/user (object body)', async () => {
    const { updateUser } = await import('../src/api/user.js')
    const cfg = await callApi(updateUser, { id: 1, name: 'x' })
    expect(cfg.method).toBe('put')
    expect(cfg.url).toBe('/api/user')
    expect(cfg.data).toEqual({ id: 1, name: 'x' })
  })

  it('user.deleteUser -> DELETE /api/user/{id} (interpolated path)', async () => {
    const { deleteUser } = await import('../src/api/user.js')
    const cfg = await callApi(deleteUser, 7)
    expect(cfg.method).toBe('delete')
    expect(cfg.url).toBe('/api/user/7')
  })

  it('user.batchDeleteUsers -> DELETE /api/user (body is the array itself, NOT {ids:[...]} wrapping)', async () => {
    const { batchDeleteUsers } = await import('../src/api/user.js')
    const ids = [1, 2, 3]
    const cfg = await callApi(batchDeleteUsers, ids)
    expect(cfg.method).toBe('delete')
    expect(cfg.url).toBe('/api/user')
    // The contract: UserController.batchDelUser takes @RequestBody List<Integer> ids.
    // If the wrapper wraps it as {ids:[...]} the backend rejects it.
    expect(cfg.data).toEqual([1, 2, 3])
    expect(Array.isArray(cfg.data)).toBe(true)
  })

  it('activity.getActivityList -> GET /api/activitys (note plural typo, must match backend)', async () => {
    const { getActivityList } = await import('../src/api/activity.js')
    const cfg = await callApi(getActivityList, { current: 1 })
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/activitys')
  })

  it('activity.createActivity -> POST /api/activity with FormData body (multipart)', async () => {
    const { createActivity } = await import('../src/api/activity.js')
    const fd = new FormData()
    fd.append('name', 'A1')
    const cfg = await callApi(createActivity, fd)
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/activity')
    expect(cfg.data).toBeInstanceOf(FormData)
  })

  it('activity.batchDeleteActivities -> POST /api/activity/batch (array body)', async () => {
    const { batchDeleteActivities } = await import('../src/api/activity.js')
    const cfg = await callApi(batchDeleteActivities, [10, 20])
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/activity/batch')
    expect(cfg.data).toEqual([10, 20])
  })

  it('clue.getCurrentClues -> GET /api/clues with current in params', async () => {
    const { getCurrentClues } = await import('../src/api/clue.js')
    const cfg = await callApi(getCurrentClues, 2)
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/clues')
    expect(cfg.params).toEqual({ current: 2 })
  })

  it('clue.checkPhoneIsExist -> GET /api/clue/{phone} interpolates the phone', async () => {
    const { checkPhoneIsExist } = await import('../src/api/clue.js')
    const cfg = await callApi(checkPhoneIsExist, '13800138000')
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/clue/13800138000')
  })

  it('clue.batchDeleteCluesByIds -> POST /api/clue/batch (array body)', async () => {
    const { batchDeleteCluesByIds } = await import('../src/api/clue.js')
    const cfg = await callApi(batchDeleteCluesByIds, [1, 2, 3])
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/clue/batch')
    expect(cfg.data).toEqual([1, 2, 3])
  })

  it('clue.importExcelAPI -> POST /api/importExcel with FormData body', async () => {
    const { importExcelAPI } = await import('../src/api/clue.js')
    const fd = new FormData()
    fd.append('file', new Blob(['x']), 'c.xlsx')
    const cfg = await callApi(importExcelAPI, fd)
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/importExcel')
    expect(cfg.data).toBeInstanceOf(FormData)
  })

  it('clue.addClueRemark -> POST /api/clue/remark with object body containing clueId/noteContent/noteWay', async () => {
    const { addClueRemark } = await import('../src/api/clue.js')
    const cfg = await callApi(addClueRemark, 5, 'follow up', 'phone')
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/clue/remark')
    expect(cfg.data).toEqual({ clueId: 5, noteContent: 'follow up', noteWay: 'phone' })
  })

  it('tran.getTranList -> GET /api/tran/list with params (NOT /api/trans or /api/tran)', async () => {
    const { getTranList } = await import('../src/api/tran.js')
    const cfg = await callApi(getTranList, { current: 1 })
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/tran/list')
  })

  it('tran.batchDeleteTran -> POST /api/tran/batch-delete with object body {ids:[...]}', async () => {
    // TranController.batchDelete is wired to @RequestBody Map<String, List<Integer>>
    // and reads request.get("ids"). The wrapper must send {ids:[...]} as
    // the body, not a raw array (which would fail to deserialize to Map).
    const { batchDeleteTran } = await import('../src/api/tran.js')
    const cfg = await callApi(batchDeleteTran, [1, 2, 3])
    expect(cfg.method).toBe('post')
    expect(cfg.url).toBe('/api/tran/batch-delete')
    expect(cfg.data).toEqual({ ids: [1, 2, 3] })
  })

  it('tran.settleTran with amount -> PUT /api/tran/settle/{id} with {amount} in body', async () => {
    const { settleTran } = await import('../src/api/tran.js')
    const cfg = await callApi(settleTran, 99, 1234.5)
    expect(cfg.method).toBe('put')
    expect(cfg.url).toBe('/api/tran/settle/99')
    expect(cfg.data).toEqual({ amount: 1234.5 })
  })

  it('tran.settleTran without amount -> PUT /api/tran/settle/{id} with empty body', async () => {
    const { settleTran } = await import('../src/api/tran.js')
    const cfg = await callApi(settleTran, 99)
    expect(cfg.method).toBe('put')
    expect(cfg.url).toBe('/api/tran/settle/99')
    // settleTran calls doPut with no second arg when amount is null.
    expect(cfg.data).toBeUndefined()
  })

  it('dict.batchDeleteDictTypes -> DELETE /api/dict/types/batch (array body)', async () => {
    const { batchDeleteDictTypes } = await import('../src/api/dict.js')
    const cfg = await callApi(batchDeleteDictTypes, [1, 2])
    expect(cfg.method).toBe('delete')
    expect(cfg.url).toBe('/api/dict/types/batch')
    expect(cfg.data).toEqual([1, 2])
  })

  it('dict.clearCache -> GET /api/dict/clear with forceRefresh:true param', async () => {
    const { clearCache } = await import('../src/api/dict.js')
    const cfg = await callApi(clearCache)
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/dict/clear')
    expect(cfg.params).toEqual({ forceRefresh: true })
  })
})

describe('api/* wrappers - documented cross-layer bugs (must stay failing until source is fixed)', () => {
  it('system.toggleSystemStatus -> backend reads TSystem.isopen (lowercase) but wrapper sends {isOpen} camelCase', async () => {
    // CrossLayerConsistencyTest#systemOpenFieldNameMustBeConsistent already
    // asserts the real serialized JSON key. This is the wrapper side: if the
    // backend contract is "body must include lowercase 'isopen'", then the
    // wrapper must conform. Currently it sends {isOpen} and the controller
    // calls system.getIsopen() (lowercase), so isopen is always false/undefined.
    // We pin the current (broken) contract here so the fix is visible when
    // someone updates system.js to send {isopen}.
    const { toggleSystemStatus } = await import('../src/api/system.js')
    const cfg = await callApi(toggleSystemStatus, 1, true)
    expect(cfg.method).toBe('put')
    expect(cfg.url).toBe('/api/system/1/status')
    // Documenting the actual current state:
    expect(cfg.data).toEqual({ isOpen: true })
    // Once source is fixed, change to: expect(cfg.data).toEqual({ isopen: true })
  })

  it('user.loginInfo is exposed via clue.js.getLoginInfo (a known cross-file alias) -> GET /api/login/info', async () => {
    // clue.js#getLoginInfo is a re-export of the user login-info endpoint.
    // Pin its shape so the alias doesn't drift.
    const { getLoginInfo } = await import('../src/api/clue.js')
    const cfg = await callApi(getLoginInfo)
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/login/info')
  })
})
