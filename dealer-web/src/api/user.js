import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'

// 用户管理
// 获取用户列表
export function getUserList(params) {
    return doGet('/api/users', params)
}

// 获取用户详情
export function getUserDetail(id) {
    return doGet(`/api/user/${id}`, {})
}

// 新增用户
export function createUser(data) {
    return doPost('/api/user', data)
}

// 编辑用户
export function updateUser(data) {
    return doPut('/api/user', data)
}

// 删除用户
export function deleteUser(id) {
    return doDelete(`/api/user/${id}`, {})
}

// 批量删除用户
export function batchDeleteUsers(ids) {
    return doDelete('/api/user', { ids })
} 