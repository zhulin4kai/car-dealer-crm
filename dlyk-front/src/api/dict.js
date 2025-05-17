import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'

// 字典类型管理
export function getDictTypeList(params) {
    return doGet('/api/dict/type/all', params)
}

export function getDictTypeDetail(id) {
    return doGet(`/api/dict/type/get/${id}`)
}

export function createDictType(data) {
    return doPost('/api/dict/type/create', data)
}

export function updateDictType(id, data) {
    return doPut(`/api/dict/type/update/${id}`, data)
}

export function deleteDictType(id) {
    return doDelete(`/api/dict/type/delete/${id}`)
}

export function batchDeleteDictTypes(ids) {
    return doDelete('/api/dict/types/batch', ids)
}

// 字典值管理
export function getDictValueList(params) {
    return doGet('/api/dict/values', params)
}

export function getDictValuesByType(typeCode) {
    return doGet(`/api/dict/values/type/${typeCode}`)
}

export function getDictValueDetail(id) {
    return doGet(`/api/dict/values/${id}`)
}

export function createDictValue(data) {
    return doPost('/api/dict/type/create', data)
}

export function updateDictValue(id, data) {
    return doPut(`/api/dict/values/${id}`, data)
}

export function deleteDictValue(id) {
    return doDelete(`/api/dict/values/${id}`)
}

export function batchDeleteDictValues(ids) {
    return doDelete('/api/dict/values/batch', { ids })
}

